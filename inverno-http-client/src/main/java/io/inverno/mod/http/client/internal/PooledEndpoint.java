/*
 * Copyright 2022 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.concurrent.CommandExecutor;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.ConnectionTimeoutException;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.InterceptedExchange;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * An endpoint implementation managing connections in a pool.
 * </p>
 *
 * <p>
 * The pool is configured using {@code pool_} properties from the {@link HttpClientConfiguration}. It has been designed to optimize connection usage relying on a lock-free {@link CommandExecutor}.
 * </p>
 *
 * <p>
 * Connections follow following lifecycle:
 * </p>
 *
 * <ol>
 * <li>connection is created if the pool is not full whenever number of inflight requests exceeds existing connections capacity (sum of max concurrent requests) and there's no parked connections.</li>
 * <li>connection is parked whenever the number of inflight requests could be handled by fewer connections. A parked connection can be put back into the active pool if needed on new requests.</li>
 * <li>connection is closed when it has been parked for more than the connection keep alive timeout.<li>
 * </ol>
 *
 * <li>
 * In case, the amount of requests exceeds the pool's capacity (sum of concurrent requests for maximum number of connections allowed in the pool), requests are buffered up to the pool buffer size
 * beyond which requests start being rejected.
 * </li>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 *
 * @param <A> the exchange context type
 */
public class PooledEndpoint<A extends ExchangeContext> extends AbstractEndpoint<A> {

	private static final Logger LOGGER = LogManager.getLogger(PooledEndpoint.class);

	private final Reactor reactor;
	private final EventLoop eventLoop;

	private final int maxSize;
	private final Integer bufferSize;
	private final long cleanPeriod;
	private final long connectTimeout;
	private final float connectLoadThreshold;
	private final int connectChoiceCount;

	private final CommandExecutor<PooledEndpoint<A>> commandExecutor;
	private final PooledHttpConnection[] connections;
	private final Deque<PooledHttpConnection> parkedConnections;
	private final ConnectionRequestBuffer requestBuffer;

	private volatile int size;
	private volatile int connecting;

	private volatile long totalCapacity;
	private volatile long capacity;

	private ScheduledFuture<?> cleanFuture;

	private volatile boolean closing;
	private volatile boolean closed;

	/**
	 * <p>
	 * Creates a pooled endpoint.
	 * </p>
	 *
	 * @param reactor               the reactor
	 * @param netService            the net service
	 * @param sslContextProvider    the SSL context provider
	 * @param channelConfigurer     the endpoint channel configurer
	 * @param localAddress          the local address
	 * @param remoteAddress         the remote endpoint address
	 * @param configuration         the HTTP client configuration
	 * @param netConfiguration      the net configuration
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyEncoder the URL encoded body encoder
	 * @param multipartBodyEncoder  the multipart body encoder
	 * @param partFactory           the part factory
	 * @param exchangeInterceptor   an optional exchange interceptor
	 */
	@SuppressWarnings("unchecked")
	public PooledEndpoint(
		Reactor reactor,
		NetService netService,
		SslContextProvider sslContextProvider,
		EndpointChannelConfigurer channelConfigurer,
		InetSocketAddress localAddress,
		InetSocketAddress remoteAddress,
		HttpClientConfiguration configuration,
		NetClientConfiguration netConfiguration,
		HeaderService headerService,
		ObjectConverter<String> parameterConverter,
		MultipartEncoder<Parameter> urlEncodedBodyEncoder,
		MultipartEncoder<Part<?>> multipartBodyEncoder,
		Part.Factory partFactory,
		ExchangeInterceptor<A, InterceptedExchange<A>> exchangeInterceptor) {
		super(netService, sslContextProvider, channelConfigurer, localAddress, remoteAddress, configuration, netConfiguration, headerService, parameterConverter, urlEncodedBodyEncoder, multipartBodyEncoder, partFactory, exchangeInterceptor);

		this.maxSize = Math.max(1, this.configuration.pool_max_size());
		this.bufferSize = this.configuration.pool_buffer_size();
		this.cleanPeriod = this.configuration.pool_clean_period();
		this.connectTimeout = this.configuration.pool_connect_timeout();
		this.connectLoadThreshold = Math.max(0, Math.min(1f, this.configuration.pool_select_connection_load_threshold()));
		this.connectChoiceCount = Math.max(1, Math.min(this.configuration.pool_select_choice_count(), this.maxSize));

		this.commandExecutor = new CommandExecutor<>(this);
		this.connections = new PooledEndpoint.PooledHttpConnection[this.maxSize];
		this.parkedConnections = new ArrayDeque<>();
		this.requestBuffer = new ConnectionRequestBuffer();

		this.reactor = reactor;
		this.eventLoop = reactor.getEventLoop();
	}

	@Override
	public Mono<HttpConnection.Handle> connection() {
		return Mono.defer(() -> {
			if(this.closing || this.closed) {
				return Mono.error(new ConnectionPoolException("Pool closed"));
			}
			ConnectionRequest request = new ConnectionRequest(this.connectTimeout);
			this.acquire(request);
			return request.asMono();
		});
	}

	@Override
	public long getActiveRequests() {
		return this.connecting + (this.totalCapacity - this.capacity) + this.bufferSize;
	}

	@Override
	public float getLoadFactor() {
		float totalConnectionLoad = 0;
		for(PooledHttpConnection connection : this.connections) {
			if(connection != null) {
				totalConnectionLoad += connection.getLoadFactor();
			}
		}
		return totalConnectionLoad / this.maxSize;
	}

	/**
	 * <p>
	 * Selects a connection from the pool.
	 * </p>
	 *
	 * <p>
	 * It select the connection with the minimum load factor from a set of {@code configuration.} randomly selected connections in the pool, that connection is returned when its load factor is less
	 * than {@link HttpClientConfiguration#pool_select_connection_load_threshold()} ()} or strictly less than {@code 1} and no additional connection can be created in the pool, otherwise {@code null}
	 * is returned to indicate that a new connection must be created if possible or the request must be buffered.
	 * </p>
	 *
	 * @return a pooled connection or {@code null} if there's not enough capacity left
	 */
	@SuppressWarnings("unchecked")
	private PooledHttpConnection selectConnection() {
		// This could go in a subclass to provide different strategy or in a dedicated strategy
		// Note that the strategy will probably also include parking selection and eviction strategy
		if(this.size == 0 || this.capacity == 0) {
			return null;
		}

//		LOGGER.debug(() -> "Snap: " + IntStream.range(0, this.size).mapToObj(i -> this.connections[i].allocated + "").collect(Collectors.joining(",")));

		ThreadLocalRandom random = ThreadLocalRandom.current();
		BitSet bs = new BitSet(this.size);
		int cardinality = 0;
		while(cardinality < this.connectChoiceCount && cardinality < this.size) {
			int randomIndex = random.nextInt(this.size);
			if(!bs.get(randomIndex)) {
				bs.set(randomIndex);
				cardinality++;
			}
		}

		PooledHttpConnection connection = bs.stream().mapToObj(i -> connections[i]).min(Comparator.comparing(PooledHttpConnection::getLoadFactor)).orElse(null);
		if(connection == null) {
			return null;
		}
		float loadFactor = connection.getLoadFactor();

		if(loadFactor == 1 || (loadFactor > this.connectLoadThreshold && size + connecting < this.maxSize)) {
			return null;
		}
		else {
			return connection;
		}
	}

	/**
	 * <p>
	 * Selects parked connection sorted by load factor (from smallest to largest).
	 * </p>
	 *
	 * @return a deque of parked connections
	 */
	@SuppressWarnings("unchecked")
	private Deque<PooledHttpConnection> selectParkable() {
		Deque<PooledHttpConnection>[] buckets = new Deque[10];

		long correctedTotalCapacity = 0;
		long correctedInflights = 0;
		// Sort connections in load factor buckets
		// Maybe we could maintain this globally in the pool
		for(int i=0;i<this.size;i++) {
			PooledHttpConnection connection = this.connections[i];
			if(connection.getLoadFactor() <= this.connectLoadThreshold) {
				correctedTotalCapacity += connection.capacity;
				correctedInflights += connection.allocated;

				int loadIndex = Math.min(9, (int)Math.floor(connection.getLoadFactor() * 10f));
				Deque<PooledHttpConnection> bucket = buckets[loadIndex];
				if(bucket == null) {
					bucket = buckets[loadIndex] = new ArrayDeque<>(this.size);
				}
				bucket.add(connection);
			}
		}

		Deque<PooledHttpConnection> parkableConnections = null;

		bucketLoop:
		for(int i=0;i<10;i++) {
			Deque<PooledHttpConnection> bucket = buckets[i];
			if(bucket != null) {
				for(PooledHttpConnection connection : bucket) {
					correctedTotalCapacity -= connection.capacity;
					if(correctedTotalCapacity < correctedInflights) {
						break bucketLoop;
					}
					else {
						if(parkableConnections == null) {
							parkableConnections = new ArrayDeque<>(this.size);
						}
						parkableConnections.add(connection);
					}
				}
			}
		}
		return parkableConnections;
	}

	/**
	 * <p>
	 * Clean the pool by closing parked inactive connections that exceed connection keep alive timeout.
	 * </p>
	 */
	private void clean() {
		// this has to be processed on the command executor as well
		this.commandExecutor.execute(pool -> {
			if(pool.closing || pool.closed) {
				return;
			}
			try {
				LOGGER.debug(
					"Clean: size={}, connecting={}, parked={}, capacity={}, allocated={}, totalCapacity={}, buffered={}",
					() -> pool.size,
					() -> pool.connecting,
					() -> pool.parkedConnections.size(),
					() -> pool.capacity,
					() -> (Stream.concat(Arrays.stream(pool.connections), pool.parkedConnections.stream()).filter(Objects::nonNull).mapToLong(c -> c.allocated).sum()),
					() -> pool.totalCapacity,
					() -> pool.requestBuffer.size
				);
				// We can just close expired connections from the parked list
				Deque<Mono<Void>> expiredConnections = null;
				for(Iterator<PooledHttpConnection> iterator = pool.parkedConnections.iterator(); iterator.hasNext();) {
					PooledHttpConnection parkedConnection = iterator.next();
					if(parkedConnection.isExpired()) {
						iterator.remove();
						if(expiredConnections == null) {
							expiredConnections = new ArrayDeque<>();
						}

						expiredConnections.add(parkedConnection.shutdown()
							.doOnError(e -> LOGGER.warn(() -> "Error shutting down pooled connection", e))
							.onErrorResume(e -> true, e -> Mono.empty())
						);
					}
				}

				if(expiredConnections != null) {
					Flux.merge(expiredConnections).subscribe();
				}

				// we should park connection and then try to close
				Deque<PooledHttpConnection> parkableConnections = this.selectParkable();

				if(parkableConnections != null) {
					parkableConnections.forEach(this::park);
				}
			}
			catch(Exception e) {
				LOGGER.fatal("Failed to clean pool", e);
			}
			finally {
				this.cleanFuture = null;
				this.scheduleClean();
			}
		});
	}

	/**
	 * <p>
	 * Schedules the clean task.
	 * </p>
	 */
	private void scheduleClean() {
		if(this.cleanFuture == null && !this.closed && !this.closing && (this.size > 0 || this.connecting > 0 || !this.parkedConnections.isEmpty())) {
			this.cleanFuture = this.eventLoop.schedule(this::clean, this.cleanPeriod, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * <p>
	 * Tries to acquire a connection.
	 * </p>
	 *
	 * @param request a connection request
	 */
	@SuppressWarnings("NonAtomicOperationOnVolatileField") // handled by the command executor
	private void acquire(ConnectionRequest request) {
		this.commandExecutor.execute(pool -> {
			if(pool.closing || pool.closed) {
				request.error(new ConnectionPoolException("Pool closed"));
			}
			// 1. Select a connection
			PooledHttpConnection connection = pool.selectConnection();
			if(connection != null) {
				request.success(connection);
			}
			else if(pool.connecting + pool.size < pool.maxSize) {
				// 2. Create a connection or recover a parked connection
				// the caller is waiting until the connection is available
				pool.connecting++;
				pool.connect(request);
			}
			else if(pool.bufferSize == null || pool.bufferSize < 0 || pool.requestBuffer.size() + pool.connecting < pool.bufferSize) {
				// 3. Buffer the request
				pool.requestBuffer.addFirst(request);
				request.startTimeout();
			}
			else {
				// 4. Fail
				request.error(new ConnectionPoolException("Maximum pending connections exceeded"));
			}
			this.scheduleClean();
		});
	}

	/**
	 * <p>
	 * Restores a parked connection or creates a new one.
	 * </p>
	 *
	 * <p>
	 * The acquired connection is assigned to the request and its capacity decreased by one.
	 * </p>
	 *
	 * @param request a connection request
	 */
	@SuppressWarnings("NonAtomicOperationOnVolatileField") // handled by the command executor
	private void connect(ConnectionRequest request) {
		// Try to recover a parked connection
		if(!this.parkedConnections.isEmpty()) {
			for(Iterator<PooledHttpConnection> iterator = this.parkedConnections.iterator(); iterator.hasNext();) {
				PooledHttpConnection parkedConnection = iterator.next();
				if(!parkedConnection.isExpired() && parkedConnection.allocated < parkedConnection.capacity) {
					this.connecting--;
					// restore the connection
					parkedConnection.index = this.size++;
					this.connections[parkedConnection.index] = parkedConnection;
					this.totalCapacity += parkedConnection.capacity;
					this.capacity += (parkedConnection.capacity - parkedConnection.allocated);
					parkedConnection.parked = false;
					iterator.remove();
					request.success(parkedConnection);
					return;
				}
			}
		}

		request.startTimeout();
		// Create a new connection
		this.createConnection().subscribe(
			connection -> this.commandExecutor.execute(pool -> {
				pool.connecting--;
				if(pool.closing || pool.closed) {
					request.error(new ConnectionPoolException("Pool closed"));
					connection.shutdown().subscribe();
				}
				else {
					// we have a new connection
					PooledHttpConnection pooledConnection = new PooledHttpConnection(pool.size, connection);
					pool.connections[pool.size++] = pooledConnection;
					pool.totalCapacity += pooledConnection.capacity;
					pool.capacity += pooledConnection.capacity;
					request.success(pooledConnection);
					this.drainBuffer();
				}
			}),
			e -> this.commandExecutor.execute(pool -> {
				pool.connecting--;
				request.error(e);
			}),
			() -> {}
		);
	}

	/**
	 * <p>
	 * Drains buffered connection requests and tries to acquire as many connections as possible.
	 * </p>
	 */
	private void drainBuffer() {
		this.commandExecutor.execute(pool -> {
			// if we have capacity we should use it
			// if we don't we should just connect with the first request
			ConnectionRequest request;
			for(int i=0;i<pool.capacity && (request = pool.requestBuffer.poll()) != null;i++) {
				pool.acquire(request);
			}
		});
	}

	/**
	 * <p>
	 * Parks a connection.
	 * </p>
	 *
	 * @param connection the connection to park
	 */
	@SuppressWarnings("NonAtomicOperationOnVolatileField") // handled by the command executor
	private void park(PooledHttpConnection connection) {
		// Remove the connection from the pool and park it
		this.commandExecutor.execute(pool -> {
			if(!connection.removed && !connection.parked && !pool.closing && !pool.closed && pool.connections[connection.index] == connection) {
				LOGGER.debug("Park connection...");
				PooledHttpConnection last = pool.connections[--pool.size];
				last.index = connection.index;
				pool.connections[connection.index] = last;
				pool.connections[pool.size] = null;
				pool.totalCapacity -= connection.capacity;
				pool.capacity -= (connection.capacity - connection.allocated);
				pool.parkedConnections.addFirst(connection);

				connection.index = -1;
				connection.parked = true;

				// this should result in a noop as this would basically mean we parked a connection while we actually needed it
				pool.drainBuffer();
			}
		});
	}

	/**
	 * <p>
	 * Removes a connection from the pool.
	 * </p>
	 *
	 * @param connection the connection to remove
	 */
	@SuppressWarnings("NonAtomicOperationOnVolatileField") // handled by the command executor
	private void remove(PooledHttpConnection connection) {
		// Remove the connection from the pool
		this.commandExecutor.execute(pool -> {
			if(!connection.removed && !pool.closing && !pool.closed) {
				LOGGER.debug("Remove connection...");
				if(connection.parked) {
					pool.parkedConnections.remove(connection);
					connection.index = -1;
					connection.parked = false;
					connection.removed = true;
				}
				else if(pool.connections[connection.index] == connection) {
					PooledHttpConnection last = pool.connections[--pool.size];
					last.index = connection.index;
					pool.connections[connection.index] = last;
					pool.connections[pool.size] = null;
					pool.totalCapacity -= connection.capacity;
					pool.capacity -= (connection.capacity - connection.allocated);

					connection.index = -1;
					connection.removed = true;

					pool.drainBuffer();
				}
			}
		});
	}

	/**
	 * <p>
	 * Changes current connection capacity taking inflight requests into account.
	 * </p>
	 *
	 * @param connection the connection to update
	 * @param capacity   the new capacity
	 */
	@SuppressWarnings("NonAtomicOperationOnVolatileField") // handled by the command executor
	private void setCapacity(PooledHttpConnection connection, long capacity) {
		this.commandExecutor.execute(pool -> {
			LOGGER.debug("Set connection capacity... ");
			if(!connection.removed) {
				long oldCapacity = connection.capacity;
				connection.capacity = capacity;
				pool.capacity += (capacity - oldCapacity);
				pool.totalCapacity += (capacity - oldCapacity);
				if(capacity > oldCapacity) {
					pool.drainBuffer();
				}
			}
		});
	}

	@Override
	public Mono<Void> shutdown() {
		return Mono.defer(() -> {
			LOGGER.debug(
				"Shutting down: size={}, connecting={}, parked={}, capacity={}, allocated={}, totalCapacity={}, buffered={}",
				() -> this.size,
				() -> this.connecting,
				() -> this.parkedConnections.size(),
				() -> this.capacity,
				() -> (Stream.concat(Arrays.stream(this.connections), this.parkedConnections.stream()).filter(Objects::nonNull).mapToLong(c -> c.allocated).sum()),
				() -> this.totalCapacity,
				() -> this.requestBuffer.size
			);
			this.closing = true;
			if(this.cleanFuture != null) {
				this.cleanFuture.cancel(false);
			}
			Sinks.Many<PooledHttpConnection> sink = Sinks.many().unicast().onBackpressureBuffer();
			this.commandExecutor.execute(pool -> {
				for(int i=0;i<pool.size;i++) {
					sink.tryEmitNext(pool.connections[i]);
					pool.connections[i] = null;
				}
				pool.size = 0;
				PooledHttpConnection parkedConnection;
				while( (parkedConnection = pool.parkedConnections.poll()) != null) {
					sink.tryEmitNext(parkedConnection);
				}
				sink.tryEmitComplete();
			});

			return sink.asFlux()
				.flatMap(connection -> connection.shutdown()
					.doOnError(e -> LOGGER.warn("Error shutting down pooled connection", e))
					.onErrorResume(e -> true, e -> Mono.empty())
				)
				.doOnTerminate(() -> {
					this.closed = true;
					this.closing = false;
				})
				.then();
		});
	}

	@Override
	public Mono<Void> shutdownGracefully() {
		return Mono.defer(() -> {
			LOGGER.debug(
				"Shutting down gracefully: size={}, connecting={}, parked={}, capacity={}, allocated={}, totalCapacity={}, buffered={}",
				() -> this.size,
				() -> this.connecting,
				() -> this.parkedConnections.size(),
				() -> this.capacity,
				() -> (Stream.concat(Arrays.stream(this.connections), this.parkedConnections.stream()).filter(Objects::nonNull).mapToLong(c -> c.allocated).sum()),
				() -> this.totalCapacity,
				() -> this.requestBuffer.size
			);
			this.closing = true;
			if(this.cleanFuture != null) {
				this.cleanFuture.cancel(false);
			}
			Sinks.Many<PooledHttpConnection> sink = Sinks.many().unicast().onBackpressureBuffer();
			this.commandExecutor.execute(pool -> {
				for(int i=0;i<pool.size;i++) {
					sink.tryEmitNext(pool.connections[i]);
					pool.connections[i] = null;
				}
				pool.size = 0;
				PooledHttpConnection parkedConnection;
				while( (parkedConnection = pool.parkedConnections.poll()) != null) {
					sink.tryEmitNext(parkedConnection);
				}
				sink.tryEmitComplete();
			});

			return sink.asFlux()
				.flatMap(connection -> connection.shutdownGracefully()
					.doOnError(e -> LOGGER.warn("Error shutting down pooled connection", e))
					.onErrorResume(e -> true, e -> Mono.empty())
				)
				.doOnTerminate(() -> {
					this.closed = true;
					this.closing = false;
				})
				.then();
		});
	}

	/**
	 * <p>
	 * Represents a request for a connection (see {@link AbstractEndpoint#connection() }).
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class ConnectionRequest implements HttpConnection.Handle {

		private final Sinks.One<HttpConnection.Handle> connectionSink;

		private PooledHttpConnection connection;

		private long timeout;
		private ScheduledFuture<?> timeoutFuture;

		ConnectionRequest next;
		ConnectionRequest previous;

		boolean canceled;
		boolean recycled;

		boolean queued;

		/**
		 * <p>
		 * Creates a dummy connection request with no connection sink.
		 * </p>
		 *
		 * <p>
		 * This is used to represent connection request buffers head.
		 * </p>
		 */
		public ConnectionRequest() {
			this.connectionSink = null;
		}

		/**
		 * <p>
		 * Creates a connection request with the specified connection timeout.
		 * <p>
		 *
		 * @param timeout a timeout
		 */
		public ConnectionRequest(long timeout) {
			this.connectionSink = Sinks.one();

			this.timeout = timeout;
		}

		/**
		 * <p>
		 * Starts the connection timeout.
		 * </p>
		 */
		public void startTimeout() {
			if(this.timeoutFuture == null) {
				this.timeoutFuture =  PooledEndpoint.this.reactor.eventLoop()
					.orElseGet(PooledEndpoint.this.reactor::getEventLoop)
					.schedule(
						() -> {
							this.timeoutFuture = null;
							this.connectionSink.tryEmitError(new ConnectionTimeoutException("Exceeded timeout " + this.timeout + "ms"));
						},
						this.timeout,
						TimeUnit.MILLISECONDS
					);
			}
		}

		/**
		 * <p>
		 * Cancels the connection timeout.
		 * </p>
		 */
		public void cancelTimeout() {
			if(this.timeoutFuture != null) {
				this.timeoutFuture.cancel(false);
			}
		}

		/**
		 * <p>
		 * Returns the connection handle publisher.
		 * </p>
		 *
		 * @return a mono emitting the connection handle when a connection has been acquired or an error mono if no connection could be acquired
		 */
		public Mono<HttpConnection.Handle> asMono() {
			return this.connectionSink.asMono()
				.doOnCancel(() -> PooledEndpoint.this.commandExecutor.execute(pool -> {
						if(pool.closing || pool.closed) {
							return;
						}
						this.cancelTimeout();
						this.release();
						// The command might execute after the connection success command, this is handled in the pendingConnection
						pool.requestBuffer.remove(this);
						this.canceled = true;
					})
				);
		}

		/**
		 * <p>
		 * Emits the acquired connection to the connection sink.
		 * </p>
		 *
		 * @param connection an acquired connection
		 */
		@SuppressWarnings("NonAtomicOperationOnVolatileField") // handled by the command executor
		public void success(PooledHttpConnection connection) {
			if(!this.canceled) {
				this.cancelTimeout();
				this.connection = connection;
				connection.allocated++;
				PooledEndpoint.this.capacity--;
				if(this.connectionSink.tryEmitValue(this) != Sinks.EmitResult.OK) {
					this.release();
				}
			}
		}

		/**
		 * <p>
		 * Fails the connection sink with the specified error.
		 * </p>
		 *
		 * @param t an error
		 */
		public void error(Throwable t) {
			this.cancelTimeout();
			this.connectionSink.tryEmitError(t);
		}

		/**
		 * <p>
		 * Releases the underlying pooled connection and give it back to the pool.
		 * </p>
		 *
		 * <p>
		 * The connection is reassigned to a buffered request, if any, otherwise it is returned to the pool.
		 * </p>
		 */
		@SuppressWarnings("NonAtomicOperationOnVolatileField") // handled by the command executor
		public void release() {
			PooledEndpoint.this.commandExecutor.execute(pool -> {
				if(this.connection != null && !this.recycled) {
					if(!pool.closing && !pool.closed) {
						connection.touch();
						connection.allocated--;
						if(!connection.parked && !connection.removed) {
							ConnectionRequest request = pool.requestBuffer.poll();
							pool.capacity++;
							if(request != null) {
								request.success(connection);
							}
						}
					}
					this.recycled = true;
				}
			});
		}

		@Override
		public <T extends ExchangeContext> Mono<HttpConnectionExchange<T, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> send(EndpointExchange<T> endpointExchange) {
			if(this.connection == null) {
				throw new IllegalStateException();
			}
			return this.connection.send(endpointExchange, this);
		}
	}

	/**
	 * <p>
	 * The connection request buffer used when pool's capacity has been reached.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class ConnectionRequestBuffer implements Iterable<ConnectionRequest> {

		private final ConnectionRequest head;
		private int size;

		/**
		 * Creates a connection request buffer.
		 */
		private ConnectionRequestBuffer() {
			this.head = new ConnectionRequest();
			this.head.previous = this.head.next = this.head;
		}

		/**
		 * <p>
		 * Returns an remove the head of the buffer.
		 * </p>
		 *
		 * @return a connection request or null of the buffer is empty
		 */
		public ConnectionRequest poll() {
			if(this.head == this.head.next) {
				return null;
			}
			ConnectionRequest request = this.head.next;
			this.remove(request);
			return request;
		}

		/**
		 * <p>
		 * Adds a request to the head of the buffer.
		 * </p>
		 *
		 * @param request a connection request
		 */
		public void addFirst(ConnectionRequest request) {
			if(request == null || request.queued) {
				throw new IllegalStateException();
			}
			request.queued = true;
			request.previous = this.head;
			request.next = this.head.next;
			this.head.next = this.head.next.previous = request;
			this.size++;
		}

		/**
		 * <p>
		 * Adds a request to the end of the buffer.
		 * </p>
		 *
		 * @param request a connection request
		 */
		public void addLast(ConnectionRequest request) {
			if(request == null || request.queued) {
				throw new IllegalStateException();
			}
			request.queued = true;
			request.next = this.head;
			request.previous = this.head.previous;
			this.head.previous = this.head.previous.next = request;
			this.size++;
		}

		/**
		 * <p>
		 * Removes the request from the buffer
		 * </p>
		 *
		 * @param request a connection request
		 *
		 * @return true if the connection request was removed, false otherwise
		 */
		public boolean remove(ConnectionRequest request) {
			if(request == null || !request.queued) {
				return false;
			}

			request.next.previous = request.previous;
			request.previous.next = request.next;
			request.next = request.previous = null;
			request.queued = false;
			this.size--;
			return true;
		}

		/**
		 * <p>
		 * Returns the size of the buffer.
		 * </p>
		 *
		 * @return the buffer size
		 */
		public int size() {
			return this.size;
		}

		/**
		 * <p>
		 * Determines whether the buffer is empty.
		 * </p>
		 *
		 * @return true if the buffer is empty, false otherwise
		 */
		public boolean isEmpty() {
			return this.size > 0;
		}

		@Override
		public Iterator<ConnectionRequest> iterator() {
			return new Iterator<>() {

				ConnectionRequest cursor = PooledEndpoint.ConnectionRequestBuffer.this.head;

				@Override
				public boolean hasNext() {
					return this.cursor.next != PooledEndpoint.ConnectionRequestBuffer.this.head;
				}

				@Override
				public ConnectionRequest next() {
					this.cursor = this.cursor.next;
					if(this.cursor == PooledEndpoint.ConnectionRequestBuffer.this.head) {
						throw new NoSuchElementException();
					}
					return this.cursor;
				}
			};
		}
	}

	/**
	 * <p>
	 * A pooled HTTP connection wrapper.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class PooledHttpConnection implements HttpConnection, HttpConnection.Handler {

		HttpConnection connection;
		final Long keepAliveTimeout;

		int index;
		boolean parked;
		boolean removed;

		Long expirationTime;

		long allocated;
		long capacity;

		/**
		 * <p>
		 * Creates a pooled HTTP connection wrapping specified connection.
		 * </p>
		 *
		 * @param index      the index of the connection on the pool
		 * @param connection the HTTP connection to wrap
		 */
		public PooledHttpConnection(int index, HttpConnection connection) {
			this.index = index;
			this.connection = connection;
			this.keepAliveTimeout = PooledEndpoint.this.configuration.pool_keep_alive_timeout();
			this.expirationTime = this.keepAliveTimeout != null ? System.currentTimeMillis() + this.keepAliveTimeout : null;

			Long mcr = connection.getMaxConcurrentRequests();
			this.capacity = mcr != null ? mcr : Long.MAX_VALUE;

			this.init();
		}

		private void init() {
			this.connection.setHandler(this);
		}

		@Override
		public boolean isTls() {
			return this.connection.isTls();
		}

		@Override
		public HttpVersion getProtocol() {
			return this.connection.getProtocol();
		}

		@Override
		public SocketAddress getLocalAddress() {
			return this.connection.getLocalAddress();
		}

		@Override
		public Optional<Certificate[]> getLocalCertificates() {
			return this.connection.getLocalCertificates();
		}

		@Override
		public SocketAddress getRemoteAddress() {
			return this.connection.getRemoteAddress();
		}

		@Override
		public Optional<Certificate[]> getRemoteCertificates() {
			return this.connection.getRemoteCertificates();
		}

		@Override
		public Long getMaxConcurrentRequests() {
			return this.connection.getMaxConcurrentRequests();
		}

		/**
		 * <p>
		 * Returns current connection load factor (number of allocations / capacity).
		 * </p>
		 *
		 * @return the load factor
		 */
		public float getLoadFactor() {
			return (float)(Math.min(this.allocated, this.capacity)) / (float)this.capacity;
		}

		/**
		 * <p>
		 * Determines whether the connection is expired.
		 * </p>
		 *
		 * @return true if the connection is expired false otherwise
		 */
		public boolean isExpired() {
			return this.expirationTime != null && this.allocated == 0 && System.currentTimeMillis() > this.expirationTime;
		}

		/**
		 * <p>
		 * Touches the connection to reset expiration time.
		 * </p>
		 */
		public void touch() {
			this.expirationTime =  this.keepAliveTimeout != null ? System.currentTimeMillis() + this.keepAliveTimeout : null;
		}

		@Override
		public void setHandler(Handler handler) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T extends ExchangeContext> Mono<HttpConnectionExchange<T, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> send(EndpointExchange<T> endpointExchange, Object state) {
			return this.connection.send(endpointExchange, state);
		}

		@Override
		public Mono<Void> shutdown() {
			return this.connection.shutdown()
				.doFirst(() -> {
					// Let's try to remove the connection from the pool first
					// This is best effort to prevent the connection from being acquired as this might be executed by another thread after the connection is actually closed.
					PooledEndpoint.this.remove(this);
				});
		}

		@Override
		public Mono<Void> shutdownGracefully() {
			return this.connection.shutdownGracefully()
				.doFirst(() -> {
					// Let's try to remove the connection from the pool first
					// This is best effort to prevent the connection from being acquired as this might be executed by another thread after the connection is actually closed.
					PooledEndpoint.this.remove(this);
				});
		}

		@Override
		public boolean isClosed() {
			return this.connection.isClosed();
		}

		@Override
		public void onUpgrade(HttpConnection upgradedConnection) {
			this.connection = upgradedConnection;
			this.connection.setHandler(this);
			PooledEndpoint.this.setCapacity(this, upgradedConnection.getMaxConcurrentRequests());
		}

		@Override
		public void onSettingsChange(long maxConcurrentRequests) {
			PooledEndpoint.this.setCapacity(this, maxConcurrentRequests);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void onRelease(Object state) {
			((ConnectionRequest)state).release();
		}

		@Override
		public void onClose() {
			PooledEndpoint.this.remove(this);
		}
	}
}
