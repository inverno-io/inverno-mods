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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import java.net.SocketAddress;
import java.util.List;

/**
 * <p>
 * HTTP1.x {@link HttpResponseEncoder} implementation.
 * </p>
 *
 * <p>
 * This implementation basically encodes {@link LinkedHttpHeaders} used instead of Netty's {@link DefaultHttpHeaders} in order to increase performances.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class Http1xResponseEncoder extends HttpResponseEncoder {

	private final ByteBufAllocator byteBufAllocator;
	
	private ChannelHandlerContext context;
	
	/**
	 * <p>
	 * Creates an HTTP1.x response encoder with the specified BytBuf allocator.
	 * </p>
	 * 
	 * @param byteBufAllocator a ByteBuf allocator
	 */
	public Http1xResponseEncoder(ByteBufAllocator byteBufAllocator) {
		this.byteBufAllocator = byteBufAllocator;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
		super.encode(this.context, msg, out);
	}

	@Override
	protected void encodeHeaders(HttpHeaders headers, ByteBuf buf) {
		if(headers instanceof LinkedHttpHeaders) {
			((LinkedHttpHeaders)headers).encode(buf);
		}
		else {
			super.encodeHeaders(headers, buf);
		}
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.context = new ChannelHandlerContextProxy(ctx);
		super.handlerAdded(ctx);
	}

	private final class ChannelHandlerContextProxy implements ChannelHandlerContext {

		private final ChannelHandlerContext context;
		
		public ChannelHandlerContextProxy(ChannelHandlerContext context) {
			this.context = context;
		}
		
		@Override
		public ChannelFuture bind(SocketAddress localAddress) {
			return this.context.bind(localAddress);
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress) {
			return this.context.connect(remoteAddress);
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
			return this.context.connect(remoteAddress, localAddress);
		}

		@Override
		public ChannelFuture disconnect() {
			return this.context.disconnect();
		}

		@Override
		public ChannelFuture close() {
			return this.context.close();
		}

		@Override
		public ChannelFuture deregister() {
			return this.context.deregister();
		}

		@Override
		public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
			return this.context.bind(localAddress, promise);
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
			return this.context.connect(remoteAddress, promise);
		}

		@Override
		public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
			return this.context.connect(remoteAddress, localAddress, promise);
		}

		@Override
		public ChannelFuture disconnect(ChannelPromise promise) {
			return this.context.disconnect(promise);
		}

		@Override
		public ChannelFuture close(ChannelPromise promise) {
			return this.context.close(promise);
		}

		@Override
		public ChannelFuture deregister(ChannelPromise promise) {
			return this.context.deregister(promise);
		}

		@Override
		public ChannelFuture write(Object msg) {
			return this.context.write(msg);
		}

		@Override
		public ChannelFuture write(Object msg, ChannelPromise promise) {
			return this.context.write(msg, promise);
		}

		@Override
		public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
			return this.context.writeAndFlush(msg, promise);
		}

		@Override
		public ChannelFuture writeAndFlush(Object msg) {
			return this.context.writeAndFlush(msg);
		}

		@Override
		public ChannelPromise newPromise() {
			return this.context.newPromise();
		}

		@Override
		public ChannelProgressivePromise newProgressivePromise() {
			return this.context.newProgressivePromise();
		}

		@Override
		public ChannelFuture newSucceededFuture() {
			return this.context.newSucceededFuture();
		}

		@Override
		public ChannelFuture newFailedFuture(Throwable cause) {
			return this.context.newFailedFuture(cause);
		}

		@Override
		public ChannelPromise voidPromise() {
			return this.context.voidPromise();
		}

		@Override
		public Channel channel() {
			return this.context.channel();
		}

		@Override
		public EventExecutor executor() {
			return this.context.executor();
		}

		@Override
		public String name() {
			return this.context.name();
		}

		@Override
		public ChannelHandler handler() {
			return this.context.handler();
		}

		@Override
		public boolean isRemoved() {
			return this.context.isRemoved();
		}

		@Override
		public ChannelHandlerContext fireChannelRegistered() {
			this.context.fireChannelRegistered();
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelUnregistered() {
			this.context.fireChannelUnregistered();
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelActive() {
			this.context.fireChannelActive();
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelInactive() {
			this.context.fireChannelInactive();
			return this;
		}

		@Override
		public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
			this.context.fireExceptionCaught(cause);
			return this;
		}

		@Override
		public ChannelHandlerContext fireUserEventTriggered(Object evt) {
			this.context.fireUserEventTriggered(evt);
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelRead(Object msg) {
			this.context.fireChannelRead(msg);
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelReadComplete() {
			this.context.fireChannelReadComplete();
			return this;
		}

		@Override
		public ChannelHandlerContext fireChannelWritabilityChanged() {
			this.context.fireChannelWritabilityChanged();
			return this;
		}

		@Override
		public ChannelHandlerContext read() {
			this.context.read();
			return this;
		}

		@Override
		public ChannelHandlerContext flush() {
			this.context.flush();
			return this;
		}

		@Override
		public ChannelPipeline pipeline() {
			return this.context.pipeline();
		}

		@Override
		public ByteBufAllocator alloc() {
			return Http1xResponseEncoder.this.byteBufAllocator;
		}

		@Override
		@Deprecated
		public <T> Attribute<T> attr(AttributeKey<T> key) {
			return this.context.channel().attr(key);
		}

		@Override
		@Deprecated
		public <T> boolean hasAttr(AttributeKey<T> key) {
			return this.context.channel().hasAttr(key);
		}
	}
}
