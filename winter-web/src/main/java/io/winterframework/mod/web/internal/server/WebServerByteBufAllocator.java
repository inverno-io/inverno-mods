/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.web.internal.server;

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;

/**
 * 
 * @author jkuhn
 *
 */
@Deprecated
public final class WebServerByteBufAllocator implements ByteBufAllocator {
	
	private static final ByteBufAllocator POOLED = new PooledByteBufAllocator(true);
	public static final ByteBufAllocator UNPOOLED = new UnpooledByteBufAllocator(false);

	public static final WebServerByteBufAllocator INSTANCE = new WebServerByteBufAllocator();

	private WebServerByteBufAllocator() {
	}

	@Override
	public ByteBuf buffer() {
		return UNPOOLED.heapBuffer();
	}

	@Override
	public ByteBuf buffer(int initialCapacity) {
		return UNPOOLED.heapBuffer(initialCapacity);
	}

	@Override
	public ByteBuf buffer(int initialCapacity, int maxCapacity) {
		return UNPOOLED.heapBuffer(initialCapacity, maxCapacity);
	}

	@Override
	public ByteBuf ioBuffer() {
		return POOLED.directBuffer();
	}

	@Override
	public ByteBuf ioBuffer(int initialCapacity) {
		return POOLED.directBuffer(initialCapacity);
	}

	@Override
	public ByteBuf ioBuffer(int initialCapacity, int maxCapacity) {
		return POOLED.directBuffer(initialCapacity, maxCapacity);
	}

	@Override
	public ByteBuf heapBuffer() {
		return UNPOOLED.heapBuffer();
	}

	@Override
	public ByteBuf heapBuffer(int initialCapacity) {
		return UNPOOLED.heapBuffer(initialCapacity);
	}

	@Override
	public ByteBuf heapBuffer(int initialCapacity, int maxCapacity) {
		return UNPOOLED.heapBuffer(initialCapacity, maxCapacity);
	}

	@Override
	public ByteBuf directBuffer() {
		return POOLED.directBuffer();
	}

	@Override
	public ByteBuf directBuffer(int initialCapacity) {
		return POOLED.directBuffer(initialCapacity);
	}

	@Override
	public ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
		return POOLED.directBuffer(initialCapacity, maxCapacity);
	}

	@Override
	public CompositeByteBuf compositeBuffer() {
		return UNPOOLED.compositeHeapBuffer();
	}

	@Override
	public CompositeByteBuf compositeBuffer(int maxNumComponents) {
		return UNPOOLED.compositeHeapBuffer(maxNumComponents);
	}

	@Override
	public CompositeByteBuf compositeHeapBuffer() {
		return UNPOOLED.compositeHeapBuffer();
	}

	@Override
	public CompositeByteBuf compositeHeapBuffer(int maxNumComponents) {
		return UNPOOLED.compositeHeapBuffer(maxNumComponents);
	}

	@Override
	public CompositeByteBuf compositeDirectBuffer() {
		return POOLED.compositeDirectBuffer();
	}

	@Override
	public CompositeByteBuf compositeDirectBuffer(int maxNumComponents) {
		return POOLED.compositeDirectBuffer();
	}

	@Override
	public boolean isDirectBufferPooled() {
		return true;
	}

	@Override
	public int calculateNewCapacity(int minNewCapacity, int maxCapacity) {
		return POOLED.calculateNewCapacity(minNewCapacity, maxCapacity);
	}

	/**
	 * Create a new {@link io.netty.channel.ChannelHandlerContext} which wraps the
	 * given one anf force the usage of direct buffers.
	 */
	public static ChannelHandlerContext forceDirectAllocator(ChannelHandlerContext ctx) {
		return new PooledChannelHandlerContext(ctx);
	}

	private static final class PooledChannelHandlerContext implements ChannelHandlerContext {
		
		private final ChannelHandlerContext ctx;

		PooledChannelHandlerContext(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
			return ctx.channel().hasAttr(attributeKey);
		}

		@Override
		public Channel channel() {
			return ctx.channel();
		}

		@Override
		public EventExecutor executor() {
			return ctx.executor();
		}

		@Override
		public String name() {
			return ctx.name();
		}

		@Override
		public ChannelHandler handler() {
			return ctx.handler();
		}

		@Override
		public boolean isRemoved() {
			return ctx.isRemoved();
		}

		@Override
		public ChannelHandlerContext fireChannelRegistered() {
			ctx.fireChannelRegistered();
			return this;
		}

		@Deprecated
		@Override
		public ChannelHandlerContext fireChannelUnregistered() {
			ctx.fireChannelUnregistered();
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelActive() {
			ctx.fireChannelActive();
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelInactive() {
			ctx.fireChannelInactive();
			return this;
		}

		@Override
		public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
			ctx.fireExceptionCaught(cause);
			return this;
		}

		@Override
		public ChannelHandlerContext fireUserEventTriggered(Object event) {
			ctx.fireUserEventTriggered(event);
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelRead(Object msg) {
			ctx.fireChannelRead(msg);
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelReadComplete() {
			ctx.fireChannelReadComplete();
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelWritabilityChanged() {
			ctx.fireChannelWritabilityChanged();
			return this;
		}

		@Override
		public ChannelFuture bind(SocketAddress localAddress) {
			return ctx.bind(localAddress);
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress) {
			return ctx.connect(remoteAddress);
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
			return ctx.connect(remoteAddress, localAddress);
		}

		@Override
		public ChannelFuture disconnect() {
			return ctx.disconnect();
		}

		@Override
		public ChannelFuture close() {
			return ctx.close();
		}

		@Deprecated
		@Override
		public ChannelFuture deregister() {
			return ctx.deregister();
		}

		@Override
		public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
			return ctx.bind(localAddress, promise);
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
			return ctx.connect(remoteAddress, promise);
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
			return ctx.connect(remoteAddress, localAddress, promise);
		}

		@Override
		public ChannelFuture disconnect(ChannelPromise promise) {
			return ctx.disconnect(promise);
		}

		@Override
		public ChannelFuture close(ChannelPromise promise) {
			return ctx.close(promise);
		}

		@Deprecated
		@Override
		public ChannelFuture deregister(ChannelPromise promise) {
			return ctx.deregister(promise);
		}

		@Override
		public ChannelHandlerContext read() {
			ctx.read();
			return this;
		}

		@Override
		public ChannelFuture write(Object msg) {
			return ctx.write(msg);
		}

		@Override
		public ChannelFuture write(Object msg, ChannelPromise promise) {
			return ctx.write(msg, promise);
		}

		@Override
		public ChannelHandlerContext flush() {
			ctx.flush();
			return this;
		}

		@Override
		public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
			return ctx.writeAndFlush(msg, promise);
		}

		@Override
		public ChannelFuture writeAndFlush(Object msg) {
			return ctx.writeAndFlush(msg);
		}

		@Override
		public ChannelPipeline pipeline() {
			return ctx.pipeline();
		}

		@Override
		public ByteBufAllocator alloc() {
			return ForceDirectPooledByteBufAllocator.INSTANCE;
		}

		@Override
		public ChannelPromise newPromise() {
			return ctx.newPromise();
		}

		@Override
		public ChannelProgressivePromise newProgressivePromise() {
			return ctx.newProgressivePromise();
		}

		@Override
		public ChannelFuture newSucceededFuture() {
			return ctx.newSucceededFuture();
		}

		@Override
		public ChannelFuture newFailedFuture(Throwable cause) {
			return ctx.newFailedFuture(cause);
		}

		@Override
		public ChannelPromise voidPromise() {
			return ctx.voidPromise();
		}

		@Override
		public <T> Attribute<T> attr(AttributeKey<T> key) {
			return ctx.channel().attr(key);
		}
	}

	private static final class ForceDirectPooledByteBufAllocator implements ByteBufAllocator {
		static ByteBufAllocator INSTANCE = new ForceDirectPooledByteBufAllocator();

		@Override
		public ByteBuf buffer() {
			return WebServerByteBufAllocator.INSTANCE.directBuffer();
		}

		@Override
		public ByteBuf buffer(int initialCapacity) {
			return WebServerByteBufAllocator.INSTANCE.directBuffer(initialCapacity);
		}

		@Override
		public ByteBuf buffer(int initialCapacity, int maxCapacity) {
			return WebServerByteBufAllocator.INSTANCE.directBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf ioBuffer() {
			return WebServerByteBufAllocator.INSTANCE.directBuffer();
		}

		@Override
		public ByteBuf ioBuffer(int initialCapacity) {
			return WebServerByteBufAllocator.INSTANCE.directBuffer(initialCapacity);
		}

		@Override
		public ByteBuf ioBuffer(int initialCapacity, int maxCapacity) {
			return WebServerByteBufAllocator.INSTANCE.directBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf heapBuffer() {
			return WebServerByteBufAllocator.INSTANCE.heapBuffer();
		}

		@Override
		public ByteBuf heapBuffer(int initialCapacity) {
			return WebServerByteBufAllocator.INSTANCE.heapBuffer(initialCapacity);
		}

		@Override
		public ByteBuf heapBuffer(int initialCapacity, int maxCapacity) {
			return WebServerByteBufAllocator.INSTANCE.heapBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf directBuffer() {
			return WebServerByteBufAllocator.INSTANCE.directBuffer();
		}

		@Override
		public ByteBuf directBuffer(int initialCapacity) {
			return WebServerByteBufAllocator.INSTANCE.directBuffer(initialCapacity);
		}

		@Override
		public ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
			return WebServerByteBufAllocator.INSTANCE.directBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public CompositeByteBuf compositeBuffer() {
			return WebServerByteBufAllocator.INSTANCE.compositeBuffer();
		}

		@Override
		public CompositeByteBuf compositeBuffer(int maxNumComponents) {
			return WebServerByteBufAllocator.INSTANCE.compositeBuffer(maxNumComponents);
		}

		@Override
		public CompositeByteBuf compositeHeapBuffer() {
			return WebServerByteBufAllocator.INSTANCE.compositeHeapBuffer();
		}

		@Override
		public CompositeByteBuf compositeHeapBuffer(int maxNumComponents) {
			return WebServerByteBufAllocator.INSTANCE.compositeHeapBuffer(maxNumComponents);
		}

		@Override
		public CompositeByteBuf compositeDirectBuffer() {
			return WebServerByteBufAllocator.INSTANCE.compositeDirectBuffer();
		}

		@Override
		public CompositeByteBuf compositeDirectBuffer(int maxNumComponents) {
			return WebServerByteBufAllocator.INSTANCE.compositeDirectBuffer(maxNumComponents);
		}

		@Override
		public boolean isDirectBufferPooled() {
			return WebServerByteBufAllocator.INSTANCE.isDirectBufferPooled();
		}

		@Override
		public int calculateNewCapacity(int minNewCapacity, int maxCapacity) {
			return WebServerByteBufAllocator.INSTANCE.calculateNewCapacity(minNewCapacity, maxCapacity);
		}
	}
}