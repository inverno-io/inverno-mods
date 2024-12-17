/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.boot.internal.net;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.net.NetAddressResolverConfiguration;
import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.base.net.NetServerConfiguration;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.boot.BootConfiguration;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.incubator.channel.uring.IOUringChannelOption;
import io.netty.incubator.channel.uring.IOUringDatagramChannel;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.NameResolver;
import io.netty.resolver.RoundRobinInetAddressResolver;
import io.netty.resolver.dns.DefaultDnsCache;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsCache;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.DnsServerAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link NetService} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(name = "netService")
public class GenericNetService implements @Provide NetService {
	
	private static final String IDLE_HANDLER_NAME = "idle";
	
	private final BootConfiguration configuration;
	private final Reactor reactor;
	
	private final TransportType transportType;
	
	private final ByteBufAllocator pooledAllocator;
	private final ByteBufAllocator unpooledAllocator;
	
	private final ByteBufAllocator allocator;
	private final ByteBufAllocator directAllocator;

	private final AddressResolverGroup<InetSocketAddress> addressResolverGroup;

	/**
	 * <p>
	 * Creates a generic net service with the specified configuration.
	 * </p>
	 * 
	 * @param configuration the net configuration
	 */
	public GenericNetService(BootConfiguration configuration, Reactor reactor, TransportType transportType) {
		this.configuration = configuration;
		this.reactor = reactor;
		this.transportType = transportType;

		this.addressResolverGroup = this.createAddressResolver(configuration.address_resolver());
				
		this.pooledAllocator = new PooledByteBufAllocator(true);
		this.unpooledAllocator = new UnpooledByteBufAllocator(false);

		this.allocator = new NetByteBufAllocator();
		this.directAllocator = new DirectNetByteBufAllocator();
	}

	@Destroy
	public void destroy() {
		this.addressResolverGroup.close();
	}

	private AddressResolverGroup<InetSocketAddress> createAddressResolver(NetAddressResolverConfiguration addressResolverConfiguration) {
		if(addressResolverConfiguration.dns_enabled()) {
			Collection<InetSocketAddress> nameServers = addressResolverConfiguration.name_servers() != null && !addressResolverConfiguration.name_servers().isEmpty() ? addressResolverConfiguration.name_servers() : DnsServerAddresses.defaultAddressList();
			DnsServerAddresses nameServerAddresses = addressResolverConfiguration.rotate_servers() ? DnsServerAddresses.rotational(nameServers) : DnsServerAddresses.sequential(nameServers);

			DnsCache resolveCache = new DefaultDnsCache(addressResolverConfiguration.cache_min_ttl(), addressResolverConfiguration.cache_max_ttl(), addressResolverConfiguration.cache_negative_ttl());
			DnsCache authoritativeDnsServerCache = new DefaultDnsCache(addressResolverConfiguration.cache_min_ttl(), addressResolverConfiguration.cache_max_ttl(), addressResolverConfiguration.cache_negative_ttl());

			DnsNameResolverBuilder builder = new DnsNameResolverBuilder()
				.nameServerProvider(hostname -> nameServerAddresses.stream())
				.resolveCache(resolveCache)
				.authoritativeDnsServerCache(authoritativeDnsServerCache)
				.maxPayloadSize(addressResolverConfiguration.max_payload_size())
				.optResourceEnabled(addressResolverConfiguration.include_optional_records())
				.queryTimeoutMillis(addressResolverConfiguration.query_timeout())
				.maxQueriesPerResolve(addressResolverConfiguration.max_queries_per_resolve())
				.recursionDesired(addressResolverConfiguration.recursion_desired())
				.ndots(addressResolverConfiguration.ndots())
				.decodeIdn(addressResolverConfiguration.decode_idn())
				.consolidateCacheSize(addressResolverConfiguration.consolidate_cache_size());

			if(addressResolverConfiguration.search_domains() != null) {
				builder.searchDomains(addressResolverConfiguration.search_domains());
			}

			switch(this.transportType) {
				case KQUEUE: {
					builder.channelFactory(KQueueDatagramChannel::new);
					builder.socketChannelFactory(KQueueSocketChannel::new);
					break;
				}
				case EPOLL: {
					builder.channelFactory(EpollDatagramChannel::new);
					builder.socketChannelFactory(EpollSocketChannel::new);
					break;
				}
				case IO_URING: {
					builder.channelFactory(IOUringDatagramChannel::new);
					builder.socketChannelFactory(IOUringSocketChannel::new);
					break;
				}
				default: {
					builder.channelFactory(NioDatagramChannel::new);
					builder.socketChannelFactory(NioSocketChannel::new);
				}
			}

			return new DnsAddressResolverGroup(builder) {

				@Override
				protected AddressResolver<InetSocketAddress> newAddressResolver(EventLoop eventLoop, NameResolver<InetAddress> resolver) throws Exception {
					if(addressResolverConfiguration.round_robin_addresses()) {
						return new RoundRobinInetAddressResolver(eventLoop, resolver).asAddressResolver();
					}
					return super.newAddressResolver(eventLoop, resolver);
				}
			};
		}
		else {
			return DefaultAddressResolverGroup.INSTANCE;
		}
	}

	@Override
	public TransportType getTransportType() {
		return this.transportType;
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress) {
		return this.createClient(socketAddress, this.configuration.net_client(), this.reactor.getCoreIoEventLoopGroupSize());
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress, NetClientConfiguration clientConfiguration) {
		return this.createClient(socketAddress, clientConfiguration, this.reactor.getCoreIoEventLoopGroupSize());
	}
	
	@Override
	public Bootstrap createClient(SocketAddress socketAddress, int nThreads) {
		return this.createClient(socketAddress, this.configuration.net_client(), nThreads);
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress, NetClientConfiguration clientConfiguration, int nThreads) throws IllegalArgumentException {
		if(clientConfiguration == null) {
			clientConfiguration = this.configuration.net_client();
		}
		
		long idleTimeout = clientConfiguration.idle_timeout() != null ? clientConfiguration.idle_timeout() : 0;
		long idleReadTimeout = clientConfiguration.idle_read_timeout() != null ? clientConfiguration.idle_read_timeout() : 0;
		long idleWriteTimeout = clientConfiguration.idle_write_timeout() != null ? clientConfiguration.idle_write_timeout() : 0;
		
		Bootstrap bootstrap = new Bootstrap() {
			@Override
			public Bootstrap handler(ChannelHandler handler) {
				if(idleTimeout > 0 || idleReadTimeout > 0 || idleWriteTimeout > 0) {
					return super.handler(new ChannelHandler() {

						@Override
						public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
							ctx.pipeline().addLast(IDLE_HANDLER_NAME, new IdleStateHandler(idleReadTimeout, idleWriteTimeout, idleTimeout, TimeUnit.MILLISECONDS));
							ctx.pipeline().addLast(handler);
							ctx.pipeline().remove(this);
						}

						@Override
						public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
						}

						@Override
						@Deprecated
						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
							ctx.close();
						}
					});
				}
				return super.handler(handler);
			}
		};
		
		bootstrap.group(this.reactor.createIoEventLoopGroup(nThreads));
		boolean isDomain = socketAddress instanceof DomainSocketAddress;
		switch(this.transportType) {
			case KQUEUE: {
				if(isDomain) {
					bootstrap.channelFactory(KQueueDomainSocketChannel::new);
				}
				else {
					bootstrap.channelFactory(KQueueSocketChannel::new);
				}
				break;
			}
			case EPOLL: {
				if(isDomain) {
					bootstrap.channelFactory(EpollDomainSocketChannel::new);
				}
				else {
					bootstrap.channelFactory(EpollSocketChannel::new);
				}
				bootstrap
					.option(EpollChannelOption.TCP_QUICKACK, clientConfiguration.tcp_quickack())
					.option(EpollChannelOption.TCP_CORK, clientConfiguration.tcp_cork());
				
				break;
			}
			case IO_URING: {
				bootstrap.channelFactory(IOUringSocketChannel::new);
				bootstrap
					.option(IOUringChannelOption.TCP_QUICKACK, clientConfiguration.tcp_quickack())
					.option(IOUringChannelOption.TCP_CORK, clientConfiguration.tcp_cork());
				break;
			}
			default: {
				bootstrap.channelFactory(NioSocketChannel::new);
			}
		}
		
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfiguration.connect_timeout())
			.option(ChannelOption.ALLOCATOR, this.allocator);
		
		if(!isDomain) {
			bootstrap
				.option(ChannelOption.SO_REUSEADDR, clientConfiguration.reuse_address())
				.option(ChannelOption.SO_KEEPALIVE, clientConfiguration.keep_alive())
				.option(ChannelOption.TCP_NODELAY, clientConfiguration.tcp_no_delay());
		}
		if(clientConfiguration.snd_buffer() != null) {
			bootstrap.option(ChannelOption.SO_SNDBUF, clientConfiguration.snd_buffer());
		}
		if(clientConfiguration.rcv_buffer() != null) {
			bootstrap
				.option(ChannelOption.SO_RCVBUF, clientConfiguration.rcv_buffer())
				.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(clientConfiguration.rcv_buffer()));
		}
		if(clientConfiguration.linger() != null) {
			bootstrap.option(ChannelOption.SO_LINGER, clientConfiguration.linger());
		}
		if(clientConfiguration.ip_tos() != null) {
			bootstrap.option(ChannelOption.IP_TOS, clientConfiguration.ip_tos());
		}
		if(clientConfiguration.tcp_fast_open_connect()) {
			bootstrap.option(ChannelOption.TCP_FASTOPEN_CONNECT, clientConfiguration.tcp_fast_open_connect());
		}
		return bootstrap;
	}
	
	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress) {
		return this.createServer(socketAddress, this.configuration.net_server(), this.reactor.getCoreIoEventLoopGroupSize());
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress, NetServerConfiguration serverConfiguration) {
		return this.createServer(socketAddress, serverConfiguration, this.reactor.getCoreIoEventLoopGroupSize());
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress, int nThreads) {
		return this.createServer(socketAddress, this.configuration.net_server(), nThreads);
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress, NetServerConfiguration serverConfiguration, int nThreads) throws IllegalArgumentException {
		if(serverConfiguration == null) {
			serverConfiguration = this.configuration.net_server();
		}
		
		long idleTimeout = serverConfiguration.idle_timeout() != null ? serverConfiguration.idle_timeout() : 0;
		long idleReadTimeout = serverConfiguration.idle_read_timeout() != null ? serverConfiguration.idle_read_timeout() : 0;
		long idleWriteTimeout = serverConfiguration.idle_write_timeout() != null ? serverConfiguration.idle_write_timeout() : 0;
		
		ServerBootstrap bootstrap = new ServerBootstrap() {
			@Override
			public ServerBootstrap childHandler(ChannelHandler childHandler) {
				if(idleTimeout > 0 || idleReadTimeout > 0 || idleWriteTimeout > 0) {
					return super.childHandler(new ChannelHandler() {

						@Override
						public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
							ctx.pipeline().addLast(IDLE_HANDLER_NAME, new IdleStateHandler(idleReadTimeout, idleWriteTimeout, idleTimeout, TimeUnit.MILLISECONDS));
							ctx.pipeline().addLast(childHandler);
							ctx.pipeline().remove(this);
						}

						@Override
						public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
						}

						@Override
						@Deprecated
						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
							ctx.close();
						}
					});
				}
				return super.childHandler(childHandler);
			}
		};
		
		bootstrap.group(this.reactor.getAcceptorEventLoopGroup(), this.reactor.createIoEventLoopGroup(nThreads));
		boolean isDomain = socketAddress instanceof DomainSocketAddress;
		switch(this.transportType) {
			case KQUEUE: {
				if(isDomain) {
					bootstrap.channelFactory(KQueueServerDomainSocketChannel::new);
				}
				else {
					bootstrap.channelFactory(KQueueServerSocketChannel::new);
				}
				break;
			}
			case EPOLL: {
				if(isDomain) {
					bootstrap.channelFactory(EpollServerDomainSocketChannel::new);
				}
				else {
					bootstrap.channelFactory(EpollServerSocketChannel::new);
				}
				bootstrap
					.option(EpollChannelOption.SO_REUSEPORT, serverConfiguration.reuse_port())
					.childOption(EpollChannelOption.TCP_QUICKACK, serverConfiguration.tcp_quickack())
					.childOption(EpollChannelOption.TCP_CORK, serverConfiguration.tcp_cork());
				break;
			}
			case IO_URING: {
				bootstrap.channelFactory(IOUringServerSocketChannel::new);
				bootstrap
					.option(IOUringChannelOption.SO_REUSEPORT, serverConfiguration.reuse_port())
					.childOption(IOUringChannelOption.TCP_QUICKACK, serverConfiguration.tcp_quickack())
					.childOption(IOUringChannelOption.TCP_CORK, serverConfiguration.tcp_cork());
				break;
			}
			default: {
				bootstrap.channelFactory(NioServerSocketChannel::new);
			}
		}
		
		bootstrap.option(ChannelOption.SO_REUSEADDR, serverConfiguration.reuse_address());
		if(serverConfiguration.accept_backlog() != null) {
			bootstrap.option(ChannelOption.SO_BACKLOG, serverConfiguration.accept_backlog());
		}
		
		bootstrap.childOption(ChannelOption.ALLOCATOR, this.allocator);
		if(!isDomain) {
			bootstrap
				.childOption(ChannelOption.SO_KEEPALIVE, serverConfiguration.keep_alive())
				.childOption(ChannelOption.TCP_NODELAY, serverConfiguration.tcp_no_delay());
		}
		if(serverConfiguration.snd_buffer() != null) {
			bootstrap.childOption(ChannelOption.SO_SNDBUF, serverConfiguration.snd_buffer());
		}
		if(serverConfiguration.rcv_buffer() != null) {
			bootstrap
				.childOption(ChannelOption.SO_RCVBUF, serverConfiguration.rcv_buffer())
				.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(serverConfiguration.rcv_buffer()));
		}
		if(serverConfiguration.linger() != null) {
			bootstrap.childOption(ChannelOption.SO_LINGER, serverConfiguration.linger());
		}
		if(serverConfiguration.ip_tos() != null) {
			bootstrap.childOption(ChannelOption.IP_TOS, serverConfiguration.ip_tos());
		}
		if(serverConfiguration.tcp_fast_open() != null) {
			bootstrap.option(ChannelOption.TCP_FASTOPEN, serverConfiguration.tcp_fast_open());
		}
		return bootstrap;
	}

	@Override
	public Mono<InetSocketAddress> resolve(InetSocketAddress socketAddress) {
		return Mono.create(sink -> {
			EventLoop eventLoop = this.reactor.eventLoop().orElseGet(() -> this.reactor.getEventLoop());
			this.addressResolverGroup.getResolver(eventLoop).resolve(socketAddress).addListener(result -> {
				if(result.isSuccess()) {
					sink.success((InetSocketAddress)result.get());
				}
				else {
					sink.error(result.cause());
				}
			});
		});
	}

	@Override
	public Mono<List<InetSocketAddress>> resolveAll(InetSocketAddress socketAddress) {
		return Mono.create(sink -> {
			EventLoop eventLoop = this.reactor.eventLoop().orElseGet(() -> this.reactor.getEventLoop());
			this.addressResolverGroup.getResolver(eventLoop).resolveAll(socketAddress).addListener(result -> {
				if(result.isSuccess()) {
					sink.success((List<InetSocketAddress>)result.get());
				}
				else {
					sink.error(result.cause());
				}
			});
		});
	}

	@Override
	public AddressResolverGroup<InetSocketAddress> getResolver() {
		return this.addressResolverGroup;
	}

	@Override
	public ByteBufAllocator getByteBufAllocator() {
		return this.allocator;
	}
	
	@Override
	public ByteBufAllocator getDirectByteBufAllocator() {
		return this.directAllocator;
	}
	
	private class NetByteBufAllocator implements ByteBufAllocator {
		
		@Override
		public ByteBuf buffer() {
			return GenericNetService.this.unpooledAllocator.buffer();
		}

		@Override
		public ByteBuf buffer(int initialCapacity) {
			return GenericNetService.this.unpooledAllocator.buffer(initialCapacity);
		}

		@Override
		public ByteBuf buffer(int initialCapacity, int maxCapacity) {
			return GenericNetService.this.unpooledAllocator.buffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf ioBuffer() {
			return GenericNetService.this.pooledAllocator.directBuffer();
		}

		@Override
		public ByteBuf ioBuffer(int initialCapacity) {
			return GenericNetService.this.pooledAllocator.directBuffer(initialCapacity);
		}

		@Override
		public ByteBuf ioBuffer(int initialCapacity, int maxCapacity) {
			return GenericNetService.this.pooledAllocator.directBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf heapBuffer() {
			return GenericNetService.this.unpooledAllocator.heapBuffer();
		}

		@Override
		public ByteBuf heapBuffer(int initialCapacity) {
			return GenericNetService.this.unpooledAllocator.heapBuffer(initialCapacity);
		}

		@Override
		public ByteBuf heapBuffer(int initialCapacity, int maxCapacity) {
			return GenericNetService.this.unpooledAllocator.heapBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf directBuffer() {
			return GenericNetService.this.pooledAllocator.buffer();
		}

		@Override
		public ByteBuf directBuffer(int initialCapacity) {
			return GenericNetService.this.pooledAllocator.buffer(initialCapacity);
		}

		@Override
		public ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
			return GenericNetService.this.pooledAllocator.buffer(initialCapacity, maxCapacity);
		}

		@Override
		public CompositeByteBuf compositeBuffer() {
			return GenericNetService.this.unpooledAllocator.compositeHeapBuffer();
		}

		@Override
		public CompositeByteBuf compositeBuffer(int maxNumComponents) {
			return GenericNetService.this.unpooledAllocator.compositeHeapBuffer(maxNumComponents);
		}

		@Override
		public CompositeByteBuf compositeHeapBuffer() {
			return GenericNetService.this.unpooledAllocator.compositeHeapBuffer();
		}

		@Override
		public CompositeByteBuf compositeHeapBuffer(int maxNumComponents) {
			return GenericNetService.this.unpooledAllocator.compositeHeapBuffer(maxNumComponents);
		}

		@Override
		public CompositeByteBuf compositeDirectBuffer() {
			return GenericNetService.this.pooledAllocator.compositeDirectBuffer();
		}

		@Override
		public CompositeByteBuf compositeDirectBuffer(int maxNumComponents) {
			return GenericNetService.this.pooledAllocator.compositeDirectBuffer(maxNumComponents);
		}

		@Override
		public boolean isDirectBufferPooled() {
			return true;
		}

		@Override
		public int calculateNewCapacity(int minNewCapacity, int maxCapacity) {
			return GenericNetService.this.pooledAllocator.calculateNewCapacity(minNewCapacity, maxCapacity);
		}
	}
	
	private class DirectNetByteBufAllocator implements ByteBufAllocator {

		@Override
		public ByteBuf buffer() {
			return GenericNetService.this.pooledAllocator.directBuffer();
		}

		@Override
		public ByteBuf buffer(int initialCapacity) {
			return GenericNetService.this.pooledAllocator.directBuffer(initialCapacity);
		}

		@Override
		public ByteBuf buffer(int initialCapacity, int maxCapacity) {
			return GenericNetService.this.pooledAllocator.directBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf ioBuffer() {
			return GenericNetService.this.pooledAllocator.directBuffer();
		}

		@Override
		public ByteBuf ioBuffer(int initialCapacity) {
			return GenericNetService.this.pooledAllocator.directBuffer(initialCapacity);
		}

		@Override
		public ByteBuf ioBuffer(int initialCapacity, int maxCapacity) {
			return GenericNetService.this.pooledAllocator.directBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf heapBuffer() {
			return GenericNetService.this.unpooledAllocator.heapBuffer();
		}

		@Override
		public ByteBuf heapBuffer(int initialCapacity) {
			return GenericNetService.this.unpooledAllocator.heapBuffer(initialCapacity);
		}

		@Override
		public ByteBuf heapBuffer(int initialCapacity, int maxCapacity) {
			return GenericNetService.this.unpooledAllocator.heapBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf directBuffer() {
			return GenericNetService.this.pooledAllocator.buffer();
		}

		@Override
		public ByteBuf directBuffer(int initialCapacity) {
			return GenericNetService.this.pooledAllocator.buffer(initialCapacity);
		}

		@Override
		public ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
			return GenericNetService.this.pooledAllocator.buffer(initialCapacity, maxCapacity);
		}

		@Override
		public CompositeByteBuf compositeBuffer() {
			return GenericNetService.this.unpooledAllocator.compositeHeapBuffer();
		}

		@Override
		public CompositeByteBuf compositeBuffer(int maxNumComponents) {
			return GenericNetService.this.unpooledAllocator.compositeHeapBuffer(maxNumComponents);
		}

		@Override
		public CompositeByteBuf compositeHeapBuffer() {
			return GenericNetService.this.unpooledAllocator.compositeHeapBuffer();
		}

		@Override
		public CompositeByteBuf compositeHeapBuffer(int maxNumComponents) {
			return GenericNetService.this.unpooledAllocator.compositeHeapBuffer(maxNumComponents);
		}

		@Override
		public CompositeByteBuf compositeDirectBuffer() {
			return GenericNetService.this.pooledAllocator.compositeDirectBuffer();
		}

		@Override
		public CompositeByteBuf compositeDirectBuffer(int maxNumComponents) {
			return GenericNetService.this.pooledAllocator.compositeDirectBuffer(maxNumComponents);
		}

		@Override
		public boolean isDirectBufferPooled() {
			return true;
		}

		@Override
		public int calculateNewCapacity(int minNewCapacity, int maxCapacity) {
			return GenericNetService.this.pooledAllocator.calculateNewCapacity(minNewCapacity, maxCapacity);
		}
	}
}
