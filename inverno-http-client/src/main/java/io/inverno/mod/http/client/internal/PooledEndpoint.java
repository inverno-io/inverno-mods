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
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.ConnectionPoolException;
import io.inverno.mod.http.client.ConnectionTimeoutException;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class PooledEndpoint<A extends ExchangeContext> extends AbstractEndpoint<A> {

	private static final Logger LOGGER = LogManager.getLogger(PooledEndpoint.class);
	
	private final Reactor reactor;
	private final EventLoop eventLoop;
	
	private final int maxSize;
	private final Integer bufferSize;
	private final long cleanPeriod;
	private final Long connectTimeout;
	
	private final CommandExecutor<PooledEndpoint<A>> commandExecutor;
	private final PooledEndpoint.PooledHttpConnection[] connections;
	private final Deque<PooledEndpoint.PooledHttpConnection> parkedConnections;
	private final PooledEndpoint.ConnectionRequestBuffer requestBuffer;

	private volatile int size;
	private volatile int connecting;
	
	private volatile long totalCapacity;
	private volatile long capacity;
	
	private volatile boolean closing;
	private volatile boolean closed;

	private ScheduledFuture<?> cleanFuture;
	
	public PooledEndpoint(
			Reactor reactor,
			NetService netService, 
			SslContextProvider sslContextProvider, 
			EndpointChannelConfigurer channelConfigurer, 
			InetSocketAddress localAddress, 
			InetSocketAddress remoteAddress, 
			HttpClientConfiguration configuration, 
			NetClientConfiguration netConfiguration, 
			Class<A> contextType, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter) {
		super(netService, sslContextProvider, channelConfigurer, localAddress, remoteAddress, configuration, netConfiguration, contextType, headerService, parameterConverter);
		
		this.maxSize = this.configuration.pool_max_size();
		this.bufferSize = this.configuration.pool_buffer_size();
		this.cleanPeriod = this.configuration.pool_clean_period();
		this.connectTimeout = this.configuration.connect_timeout();
		
		if(this.maxSize <= 0) {
			throw new IllegalArgumentException("max_pool_size must be a greater than 1");
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
	
	// This could go in a subclass to provide different strategy or in a dedicated strategy
	// Note that the strategy will probably also include parking selection and eviction strategy
	private int activeIndex;
	
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
	
	private void clean() {
		// this has to be processed on the command executor as well
		this.commandExecutor.execute(pool -> {
			if(pool.closing || pool.closed) {
				return;
			}
			try {
				LOGGER.info("Clean: size=" + pool.size + ", capacity=" + pool.capacity + ", totalCapacity=" + pool.totalCapacity + ", parked=" + pool.parkedConnections.size() + ", buffered=" + pool.requestBuffer.size + ", connecting=" + pool.connecting);
				// We can just close expired connections from the parked list
				Deque<Mono<Void>> expiredConnections = null;
				for(Iterator<PooledEndpoint.PooledHttpConnection> iterator = pool.parkedConnections.iterator();iterator.hasNext();) {
					PooledEndpoint.PooledHttpConnection parkedConnection = iterator.next();
					if(parkedConnection.isExpired()) {
						iterator.remove();
						if(expiredConnections == null) {
							expiredConnections = new ArrayDeque<>();
						}
						expiredConnections.add(parkedConnection.close()
							.doOnError(e -> LOGGER.warn("Error closing pooled connection", e))
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
	
	private void scheduleClean() {
		this.cleanFuture = this.eventLoop.schedule(this::clean, this.cleanPeriod, TimeUnit.MILLISECONDS);
	}
	
	private void cancelRequest(PooledEndpoint.ConnectionRequest request) {
		this.commandExecutor.execute(pool -> {
			if(this.closing || this.closed) {
				return;
			}
			// The command might execute after the connection success command, this is handled in the pendingConnection 
			pool.requestBuffer.remove(request);
		});
	}
	
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
						connection.close().subscribe();
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
						
						/*PooledEndpoint.ConnectionRequest bufferedRequest = null;
						while(pooledConnection.allocated < pooledConnection.capacity && (bufferedRequest = pool.requestBuffer.poll()) != null) {
							pooledConnection.allocated++;
							bufferedRequest.success(pooledConnection);
						}*/
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
	
	private void recycle(PooledHttpConnection connection) {
		this.commandExecutor.execute(pool -> {
			if(pool.closing || pool.closed) {
				return;
			}
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
		});
	}
	
	private void remove(PooledHttpConnection connection) {
		// Remove the connection from the pool
		this.commandExecutor.execute(pool -> {
			if(!connection.removed && !pool.closing && !pool.closed) {
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
	
	private void park(PooledHttpConnection connection) {
		// Remove the connection from the pool and park it
		this.commandExecutor.execute(pool -> {
			if(!connection.parked && !pool.closing && !pool.closed && pool.connections[connection.index] == connection) {
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
	
	private void setCapacity(PooledHttpConnection connection, long capacity) {
		this.commandExecutor.execute(pool -> {
			long oldCapacity = connection.capacity;
			connection.capacity = capacity;
			pool.capacity += (capacity - oldCapacity);
			pool.totalCapacity += (capacity - oldCapacity);
			if(capacity > oldCapacity) {
				pool.drainBuffer();
			}
		});
	}

	@Override
	public Mono<Void> close() {
		return Mono.defer(() -> {
			this.closing = true;
			Sinks.Many<PooledEndpoint.PooledHttpConnection> sink = Sinks.many().unicast().onBackpressureBuffer();
			this.commandExecutor.execute(pool -> {
				for(int i=0;i<pool.size;i++) {
					sink.tryEmitNext(pool.connections[i]);
					pool.connections[i] = null;
				}
				pool.size = 0;
				PooledEndpoint.PooledHttpConnection parkedConnection = null;
				while( (parkedConnection = pool.parkedConnections.poll()) != null) {
					sink.tryEmitNext(parkedConnection);
				}
				sink.tryEmitComplete();
			});
			
			return sink.asFlux()
				.flatMap(connection -> connection.close()
					.doOnError(e -> LOGGER.warn("Error closing pooled connection", e))
					.onErrorResume(e -> true, e -> Mono.empty())
				)
				.doOnTerminate(() -> this.closed = true)
				.then();
		});
	}
	
	protected class ConnectionRequest implements Supplier<Mono<HttpConnection>> {
		
		private final Sinks.One<HttpConnection> connectionSink;
		
		private Long timeout;
		private Optional<EventLoop> requestEventLoop;
		private ScheduledFuture<?> timeoutFuture;
		
		ConnectionRequest next;
		ConnectionRequest previous;
		
		boolean queued;
		
		// For ConnectionRequestBuffer head
		public ConnectionRequest() {
			this.connectionSink = null;
		}
		
		public ConnectionRequest(Long timeout) {
			this.connectionSink = Sinks.one();
			
			if(timeout != null) {
				this.timeout = timeout;
				this.requestEventLoop = PooledEndpoint.this.reactor.eventLoop();
			}
		}
		
		@Override
		public Mono<HttpConnection> get() {
			return this.connectionSink.asMono()
				.doOnCancel(() -> {
					PooledEndpoint.this.cancelRequest(this);
				});
		}
		
		public void success(PooledHttpConnection connection) {
			this.cancelTimeout();
			if(this.connectionSink.tryEmitValue(connection) != Sinks.EmitResult.OK) {
				PooledEndpoint.this.recycle(connection);
			}
		}
		
		public void error(Throwable t) {
			this.cancelTimeout();
			this.connectionSink.tryEmitError(t);
		}
		
		public void startTimeout() {
			if(this.requestEventLoop != null && this.timeoutFuture == null) {
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
		
		public void cancelTimeout() {
			if(this.timeoutFuture != null) {
				this.timeoutFuture.cancel(false);
			}
		}
	}
	
	private class ConnectionRequestBuffer implements Iterable<ConnectionRequest> {

		private final ConnectionRequest head;
		private int size;
		
		private ConnectionRequestBuffer() {
			this.head = new ConnectionRequest();
			this.head.previous = this.head.next = this.head;
		}
		
		public ConnectionRequest poll() {
			if(this.head == this.head.next) {
				return null;
			}
			ConnectionRequest request = this.head.next;
			this.remove(request);
			return request;
		}
		
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
		
		public int size() {
			return this.size;
		}
		
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
	
	protected class PooledHttpConnection implements HttpConnection, HttpConnection.Handler {

		HttpConnection connection;
		final Long keepAliveTimeout;
		
		int index;		
		boolean parked;
		boolean removed;
		
		Long expirationTime;
		
		long allocated;
		long capacity;
		
		public PooledHttpConnection(int index, HttpConnection connection) {
			this.index = index;
			this.connection = connection;
			this.connection.setHandler(this);
			
			this.keepAliveTimeout = PooledEndpoint.this.configuration.pool_keep_alive_timeout();
			this.expirationTime = this.keepAliveTimeout != null ? System.currentTimeMillis() + this.keepAliveTimeout : null;
			
			Long mcr = connection.getMaxConcurrentRequests();
			this.capacity = mcr != null ? mcr : Long.MAX_VALUE;
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
		public Long getMaxConcurrentRequests() {
			return this.connection.getMaxConcurrentRequests();
		}
		
		public float getLoadFactor() {
			return (float)(Math.min(this.allocated, this.capacity)) / (float)this.capacity;
		}
		
		public boolean isExpired() {
			return this.expirationTime != null && this.allocated == 0 && System.currentTimeMillis() > this.expirationTime;
		}
		
		public void touch() {
			this.expirationTime =  this.keepAliveTimeout != null ? System.currentTimeMillis() + this.keepAliveTimeout : null;
		}
		
		@Override
		public void setHandler(Handler handler) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <A extends ExchangeContext> Mono<Exchange<A>> send(Method method, String authority, List<Map.Entry<String, String>> headers, String path, Consumer<RequestBodyConfigurator> bodyConfigurer, A exchangeContext) {
			return this.connection.send(method, authority, headers, path, bodyConfigurer, exchangeContext);
		}

		@Override
		public Mono<Void> close() {
			return this.connection.close()
				.doFirst(() -> {
					// Let's try to remove the connection from the pool first
					// This is best effort to prevent the connection from being acquierd as this might be executed by another thread after the connection is actually closed.
					PooledEndpoint.this.remove(this);
				});
		}
		
		@Override
		public void onSettingsChange(long maxConcurrentRequests) {
			PooledEndpoint.this.setCapacity(this, maxConcurrentRequests);
		}
		
		@Override
		public void onClose() {
			// Make sure the connection is removed
			PooledEndpoint.this.remove(this);
		}

		@Override
		public void onError(Throwable t) {
			// Don't wait for the connection to be closed and remove the connection from the pool
			PooledEndpoint.this.remove(this);
		}

		@Override
		public void onExchangeTerminate(Exchange<?> exchange) {
			PooledEndpoint.this.recycle(this);
		}

		@Override
		public void onUpgrade(HttpConnection upgradedConnection) {
			this.connection = upgradedConnection;
			this.connection.setHandler(this);
			PooledEndpoint.this.setCapacity(this, upgradedConnection.getMaxConcurrentRequests());
		}
	}
}
