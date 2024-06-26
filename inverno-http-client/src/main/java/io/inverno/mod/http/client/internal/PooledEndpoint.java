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
import io.inverno.mod.http.client.InterceptableExchange;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
 * Connections follow following lifecyle:
 * </p>
 *
 * <ol>
 * <li>connection is created if the pool is not full whenever number of inflight requests exceeds existing connections capacity (sum of max concurrent requests) and there's no parked connections.</li>
 * <li>connection is parked whenever the number of inflight requests could be handled by fewer connections. A parked connection can be put back into the active pool if needed on new requests.</li>
 * <li>conncetion is closed when it has been parked for more than the connection keep alive timeout.<li>
 * </ol>
 *
 * <li>
 * In case, the amount of requests exceeds the pool's capacity (sum of concurrent requests for maximum number of connections allowed in the pool), requests are buffered up to the pool buffer size
 * beyonf which requests start being rejected.
 * </li>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
	
	private final CommandExecutor<PooledEndpoint<A>> commandExecutor;
	private final PooledEndpoint.PooledHttpConnection[] connections;
	private final Deque<PooledEndpoint.PooledHttpConnection> parkedConnections;
	private final PooledEndpoint.ConnectionRequestBuffer requestBuffer;

	// This could go in a subclass to provide different strategy or in a dedicated strategy
	// Note that the strategy will probably also include parking selection and eviction strategy
	private int activeIndex;
	private volatile int size;
	private volatile int connecting;
	
	private volatile long totalCapacity;
	private volatile long capacity;
	
	private volatile boolean closing;
	private volatile boolean closed;

	private ScheduledFuture<?> cleanFuture;
	
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
			ExchangeInterceptor<? super A, InterceptableExchange<A>> exchangeInterceptor) {
		super(netService, sslContextProvider, channelConfigurer, localAddress, remoteAddress, configuration, netConfiguration, headerService, parameterConverter, urlEncodedBodyEncoder, multipartBodyEncoder, partFactory, exchangeInterceptor);
		
		this.maxSize = this.configuration.pool_max_size();
		this.bufferSize = this.configuration.pool_buffer_size();
		this.cleanPeriod = this.configuration.pool_clean_period();
		this.connectTimeout = this.configuration.pool_connect_timeout();
		
		if(this.maxSize <= 0) {
			throw new IllegalArgumentException("pool_max_size must be a greater than 1");
		}
		
		this.commandExecutor = new CommandExecutor<>(this);
		this.connections = new PooledEndpoint.PooledHttpConnection[this.maxSize];
		this.parkedConnections = new ArrayDeque<>();
		this.requestBuffer = new PooledEndpoint.ConnectionRequestBuffer();
		
		this.reactor = reactor;
		this.eventLoop = reactor.getEventLoop();
		this.scheduleClean();
	}

	@Override
	public Mono<HttpConnection> connection() {
		return Mono.defer(() -> {
			if(this.closing || this.closed) {
				return Mono.error(new ConnectionPoolException("Pool closed"));
			}
			PooledEndpoint.ConnectionRequest request = new PooledEndpoint.ConnectionRequest(this.connectTimeout);
			this.acquire(request);
			return request.get();
		});
	}
	
	/**
	 * <p>
	 * Selects a connection from the pool.
	 * </p>
	 * 
	 * @return a pooled connection or null if there's no capacity left
	 */
	protected PooledHttpConnection selectConnection() {
		// Select next with better capacity
		if(this.size == 0 || this.capacity == 0) {
			return null;
		}
		
		PooledHttpConnection currentConnection = this.connections[this.activeIndex];
		float loadFactor = currentConnection.getLoadFactor();
		for(int i=1;i<this.size;i++) {
			int currentIndex = (this.activeIndex + i) % this.size;
			PooledHttpConnection connection = this.connections[currentIndex];
			float currentLoadFactor = connection.getLoadFactor();
			if(currentLoadFactor < loadFactor) {
				this.activeIndex = currentIndex;
				return connection;
			}
		}
		if(loadFactor == 1) {
			// There's no capacity left
			return null;
		}
		return currentConnection;
	}

	/**
	 * <p>
	 * Selects parked connection sorted by load factor (from smallest to largest).
	 * </p>
	 * 
	 * @return a deque of parked connections
	 */
	@SuppressWarnings("unchecked")
	protected Deque<PooledEndpoint.PooledHttpConnection> selectParkable() {
		Deque<PooledEndpoint.PooledHttpConnection>[] buckets = new Deque[10];

		// Sort connections in load factor buckets
		// Maybe we could maintain this globally in the pool 
		for(int i=0;i<this.size;i++) {
			PooledEndpoint.PooledHttpConnection connection = this.connections[i];
			int loadIndex = Math.min(9, (int)Math.floor(connection.getLoadFactor() * 10f));
			Deque<PooledEndpoint.PooledHttpConnection> bucket = buckets[loadIndex];
			if(bucket == null) {
				bucket = buckets[loadIndex] = new ArrayDeque<>(this.size);
			}
			bucket.add(connection);
		}
		
		Deque<PooledEndpoint.PooledHttpConnection> parkableConnections = null;
		long newTotalCapacity = this.totalCapacity;
		long inflights = this.totalCapacity - this.capacity;
		
		bucketLoop:
		for(int i=0;i<10;i++) {
			Deque<PooledEndpoint.PooledHttpConnection> bucket = buckets[i];
			if(bucket != null) {
				for(PooledHttpConnection connection : bucket) {
					newTotalCapacity -= connection.capacity;
					if(newTotalCapacity < inflights) {
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
				LOGGER.debug("Clean: size=" + pool.size + ", capacity=" + pool.capacity + ", totalCapacity=" + pool.totalCapacity + ", parked=" + pool.parkedConnections.size() + ", buffered=" + pool.requestBuffer.size + ", connecting=" + pool.connecting);
				// We can just close expired connections from the parked list
				Deque<Mono<Void>> expiredConnections = null;
				for(Iterator<PooledEndpoint.PooledHttpConnection> iterator = pool.parkedConnections.iterator();iterator.hasNext();) {
					PooledEndpoint.PooledHttpConnection parkedConnection = iterator.next();
					if(parkedConnection.isExpired()) {
						iterator.remove();
						if(expiredConnections == null) {
							expiredConnections = new ArrayDeque<>();
						}
						expiredConnections.add(parkedConnection.shutdown()
							.doOnError(e -> LOGGER.warn("Error shutting down pooled connection", e))
							.onErrorResume(e -> true, e -> Mono.empty())
						);
					}
				}

				if(expiredConnections != null) {
					Flux.merge(expiredConnections).subscribe();
				}

				// we should park connection and then try to close
				Deque<PooledEndpoint.PooledHttpConnection> parkableConnections = this.selectParkable();

				if(parkableConnections != null) {
					parkableConnections.forEach(this::park);
				}
			}
			catch(Exception e) {
				LOGGER.fatal("Failed to clean pool", e);
			}
			finally {
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
		this.cleanFuture = this.eventLoop.schedule(this::clean, this.cleanPeriod, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * <p>
	 * Cancels a connection request.
	 * </p>
	 * 
	 * @param request the connection request to cancel
	 */
	private void cancelRequest(PooledEndpoint.ConnectionRequest request) {
		this.commandExecutor.execute(pool -> {
			if(this.closing || this.closed) {
				return;
			}
			// The command might execute after the connection success command, this is handled in the pendingConnection 
			pool.requestBuffer.remove(request);
		});
	}
	
	/**
	 * <p>
	 * Tries to acquire a connection.
	 * </p>
	 * 
	 * @param request a connection request
	 */
	private void acquire(PooledEndpoint.ConnectionRequest request) {
		this.commandExecutor.execute(pool -> {
			if(pool.closing || pool.closed) {
				request.error(new ConnectionPoolException("Pool closed"));
			}

			// 1. Select a connection
			PooledHttpConnection connection = pool.selectConnection();
			if(connection != null) {
				connection.allocated++;
				pool.capacity--;
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
	private void connect(PooledEndpoint.ConnectionRequest request) {
		// Try to recover a parked connection
		if(!this.parkedConnections.isEmpty()) {
			for(Iterator<PooledEndpoint.PooledHttpConnection> iterator = this.parkedConnections.iterator();iterator.hasNext();) {
				PooledEndpoint.PooledHttpConnection parkedConnection = iterator.next();
				if(!parkedConnection.isExpired() && parkedConnection.allocated < parkedConnection.capacity) {
					this.connecting--;
					// restore the connection
					this.activeIndex = parkedConnection.index = this.size++;
					this.connections[parkedConnection.index] = parkedConnection;
					this.totalCapacity += parkedConnection.capacity;
					parkedConnection.allocated++;
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
			connection -> {
				this.commandExecutor.execute(pool -> {
					pool.connecting--;
					if(pool.closing || pool.closed) {
						request.error(new ConnectionPoolException("Pool closed"));
						connection.shutdown().subscribe();
					}
					else {
						// we have a new connection
						PooledHttpConnection pooledConnection = new PooledHttpConnection(pool.size, connection);
						pool.connections[pool.size++] = pooledConnection;
						pool.activeIndex = pooledConnection.index;
						pool.totalCapacity += pooledConnection.capacity;
						pool.capacity += pooledConnection.capacity - 1;
						pooledConnection.allocated++;
						request.success(pooledConnection);
						this.drainBuffer();
					}
				});
			},
			e -> {
				this.commandExecutor.execute(pool -> {
					pool.connecting--;
					request.error(e);
				});
			},
			() -> {
			}
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
			PooledEndpoint.ConnectionRequest request = null;
			for(int i=0;i<pool.capacity && (request = pool.requestBuffer.poll()) != null;i++) {
				pool.acquire(request);
			}
		});
	}
	
	/**
	 * <p>
	 * Recycles the specified connection.
	 * </p>
	 * 
	 * <p>
	 * This basically increments connection's capacity by 1.
	 * </p>
	 * 
	 * @param connection 
	 */
	private void recycle(PooledHttpConnection connection) {
		this.commandExecutor.execute(pool -> {
			if(!pool.closing && !pool.closed) {
				LOGGER.debug("Recyle connection...");
				connection.touch();
				connection.allocated--;
				if(!connection.parked && !connection.removed) {
					ConnectionRequest request = pool.requestBuffer.poll();
					if(request != null) {
						connection.allocated++;
						request.success(connection);
					}
					else {
						pool.capacity++;
					}
				}
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
					
					if(pool.activeIndex == pool.size) {
						pool.activeIndex = 0;
					}
					connection.index = -1;
					connection.removed = true;

					pool.drainBuffer();
				}
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
				
				if(pool.activeIndex == pool.size) {
					pool.activeIndex = 0;
				}
				
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
	 * Changes current connection capacity taking inflight requests into account.
	 * </p>
	 * 
	 * @param connection the connection to update
	 * @param capacity   the new capacity
	 */
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
		if(this.closing || this.closed) {
			return Mono.empty();
		}
		return Mono.defer(() -> {
			LOGGER.debug("Shutting down: size=" + this.size + ", capacity=" + this.capacity + ", totalCapacity=" + this.totalCapacity + ", parked=" + this.parkedConnections.size() + ", buffered=" + this.requestBuffer.size + ", connecting=" + this.connecting);
			this.closing = true;
			this.cleanFuture.cancel(false);
			Sinks.Many<PooledEndpoint.PooledHttpConnection> sink = Sinks.many().unicast().onBackpressureBuffer();
			this.commandExecutor.execute(pool -> {
				for(int i=0;i<pool.size;i++) {
					sink.tryEmitNext(pool.connections[i]);
					pool.connections[i] = null;
				}
				pool.size = 0;
				PooledEndpoint.PooledHttpConnection parkedConnection;
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
		if(this.closing || this.closed) {
			return Mono.empty();
		}
		return Mono.defer(() -> {
			LOGGER.debug("Shutting down gracefully: size=" + this.size + ", capacity=" + this.capacity + ", totalCapacity=" + this.totalCapacity + ", parked=" + this.parkedConnections.size() + ", buffered=" + this.requestBuffer.size + ", connecting=" + this.connecting);
			this.closing = true;
			this.cleanFuture.cancel(false);
			Sinks.Many<PooledEndpoint.PooledHttpConnection> sink = Sinks.many().unicast().onBackpressureBuffer();
			this.commandExecutor.execute(pool -> {
				for(int i=0;i<pool.size;i++) {
					sink.tryEmitNext(pool.connections[i]);
					pool.connections[i] = null;
				}
				pool.size = 0;
				PooledEndpoint.PooledHttpConnection parkedConnection;
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
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class ConnectionRequest implements Supplier<Mono<HttpConnection>> {
		
		private final Sinks.One<HttpConnection> connectionSink;
		
		private long timeout;
		private Optional<EventLoop> requestEventLoop;
		private ScheduledFuture<?> timeoutFuture;
		
		ConnectionRequest next;
		ConnectionRequest previous;
		
		boolean queued;
		
		/**
		 * <p>
		 * Creates a dummy connection request with no connection sink.
		 * </p>
		 * 
		 * <p>
		 * This is used to represent connection request buffers's head.
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
			this.requestEventLoop = PooledEndpoint.this.reactor.eventLoop();
		}
		
		/**
		 * <p>
		 * Returns the connection.
		 * </p>
		 * 
		 * @return a mono emitting the acquired connection or an error mono if no connection could be acquired
		 */
		@Override
		public Mono<HttpConnection> get() {
			return this.connectionSink.asMono()
				.doOnCancel(() -> {
					PooledEndpoint.this.cancelRequest(this);
				});
		}
		
		/**
		 * <p>
		 * Emits the acquired connection to the connection sink.
		 * </p>
		 * 
		 * @param connection an acquired connection
		 */
		public void success(PooledHttpConnection connection) {
			this.cancelTimeout();
			if(this.connectionSink.tryEmitValue(connection) != Sinks.EmitResult.OK) {
				PooledEndpoint.this.recycle(connection);
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
		 * Starts the connection timeout.
		 * </p>
		 */
		public void startTimeout() {
			if(this.timeoutFuture == null) {
				this.timeoutFuture = this.requestEventLoop
					.orElse(PooledEndpoint.this.reactor.getEventLoop())
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
	}
	
	/**
	 * <p>
	 * The connection request buffer used when pool's capacity has been reached.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
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
			return new Iterator<ConnectionRequest>() {
				
				ConnectionRequest cursor = ConnectionRequestBuffer.this.head;
				
				@Override
				public boolean hasNext() {
					return this.cursor.next != ConnectionRequestBuffer.this.head;
				}

				@Override
				public ConnectionRequest next() {
					this.cursor = this.cursor.next;
					if(this.cursor == ConnectionRequestBuffer.this.head) {
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
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	protected class PooledHttpConnection implements HttpConnection, HttpConnection.Handler {

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
		 * @return 
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
		public <A extends ExchangeContext> Mono<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> send(EndpointExchange<A> endpointExchange) {
			return this.connection.send(endpointExchange);
		}

		@Override
		public Mono<Void> shutdown() {
			return this.connection.shutdown()
				.doFirst(() -> {
					// Let's try to remove the connection from the pool first
					// This is best effort to prevent the connection from being acquierd as this might be executed by another thread after the connection is actually closed.
					PooledEndpoint.this.remove(this);
				});
		}

		@Override
		public Mono<Void> shutdownGracefully() {
			return this.connection.shutdownGracefully()
				.doFirst(() -> {
					// Let's try to remove the connection from the pool first
					// This is best effort to prevent the connection from being acquierd as this might be executed by another thread after the connection is actually closed.
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
		public void recycle() {
			PooledEndpoint.this.recycle(this);
		}
		
		@Override
		public void close() {
			PooledEndpoint.this.remove(this);
		}
	}
}
