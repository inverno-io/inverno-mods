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

import java.net.SocketAddress;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.boot.BootConfiguration;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;

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
	
	private final BootConfiguration configuration;
	private final Reactor reactor;
	
	private final TransportType transportType;
	
	private final ByteBufAllocator pooledAllocator;
	private final ByteBufAllocator unpooledAllocator;
	
	private final ByteBufAllocator allocator;
	private final ByteBufAllocator directAllocator;
	
	/**
	 * <p>
	 * Creates a generic net service with the specified configuration.
	 * </p>
	 * 
	 * @param netConfiguration the net configuration
	 */
	public GenericNetService(BootConfiguration netConfiguration, Reactor reactor, TransportType transportType) {
		this.configuration = netConfiguration;
		this.reactor = reactor;
		this.transportType = transportType;
				
		this.pooledAllocator = new PooledByteBufAllocator(true);
		this.unpooledAllocator = new UnpooledByteBufAllocator(false); 
		
		this.allocator = new NetByteBufAllocator();
		this.directAllocator = new DirectNetByteBufAllocator();
	}

	@Override
	public TransportType getTransportType() {
		return this.transportType;
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress) {
		return this.createClient(socketAddress, this.reactor.getCoreIoEventLoopGroupSize());
	}

	@Override
	public Bootstrap createClient(SocketAddress socketAddress, int nThreads) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(this.reactor.createIoEventLoopGroup(nThreads));
		if(this.transportType == TransportType.KQUEUE) {
			if(socketAddress instanceof DomainSocketAddress) {
				bootstrap.channelFactory(KQueueDomainSocketChannel::new);
			}
			else {
				bootstrap.channelFactory(KQueueSocketChannel::new);
			}
		}
		else if(this.transportType == TransportType.EPOLL) {
			if(socketAddress instanceof DomainSocketAddress) {
				bootstrap.channelFactory(EpollDomainSocketChannel::new);
			}
			else {
				bootstrap.channelFactory(EpollSocketChannel::new);
			}
		}
		else {
			bootstrap.channelFactory(NioSocketChannel::new);
		}
		
		// TODO client bootstrap configuration 
		return bootstrap;
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress) {
		return this.createServer(socketAddress, this.reactor.getCoreIoEventLoopGroupSize());
	}

	@Override
	public ServerBootstrap createServer(SocketAddress socketAddress, int nThreads) {
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(this.reactor.getAcceptorEventLoopGroup(), this.reactor.createIoEventLoopGroup(nThreads));
		if(this.transportType == TransportType.KQUEUE) {
			if(socketAddress instanceof DomainSocketAddress) {
				bootstrap.channelFactory(KQueueServerDomainSocketChannel::new);
			}
			else {
				bootstrap.channelFactory(KQueueServerSocketChannel::new);
			}
		}
		else if(this.transportType == TransportType.EPOLL) {
			if(socketAddress instanceof DomainSocketAddress) {
				bootstrap.channelFactory(EpollServerDomainSocketChannel::new);
			}
			else {
				bootstrap.channelFactory(EpollServerSocketChannel::new);
			}
			bootstrap.option(EpollChannelOption.SO_REUSEPORT, this.configuration.reuse_port())
				.childOption(EpollChannelOption.TCP_QUICKACK, this.configuration.tcp_quickack())
				.childOption(EpollChannelOption.TCP_CORK, this.configuration.tcp_cork());
		}
		else {
			bootstrap.channelFactory(NioServerSocketChannel::new);
		}
		
		bootstrap.option(ChannelOption.SO_REUSEADDR, this.configuration.reuse_address())
			.childOption(ChannelOption.SO_KEEPALIVE, this.configuration.keep_alive())
			.childOption(ChannelOption.TCP_NODELAY, this.configuration.tcp_no_delay())
			.childOption(ChannelOption.ALLOCATOR, this.allocator);
		
		if(this.configuration.accept_backlog() != null) {
			bootstrap.option(ChannelOption.SO_BACKLOG, this.configuration.accept_backlog());
		}
		return bootstrap;
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
