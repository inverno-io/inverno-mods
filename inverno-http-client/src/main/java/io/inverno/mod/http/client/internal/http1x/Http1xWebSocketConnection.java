/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.http.client.internal.http1x.ws.GenericWebSocketExchange;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketFrame;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketMessage;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.internal.EndpointExchange;
import io.inverno.mod.http.client.internal.WebSocketConnection;
import io.inverno.mod.http.client.internal.WebSocketConnectionExchange;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslHandler;
import java.net.URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * HTTP/1.x {@link WebSocketConnection} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http1xWebSocketConnection extends SimpleChannelInboundHandler<Object> implements WebSocketConnection {

	private static final Logger LOGGER = LogManager.getLogger(Http1xWebSocketConnection.class);
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final GenericWebSocketFrame.GenericFactory frameFactory;
	private final GenericWebSocketMessage.GenericFactory messageFactory;
	
	private final boolean closeOnOutboundComplete;
	private final long inboundCloseFrameTimeout;
	
	private ChannelHandlerContext context;
	private boolean tls;
	
	private Mono<Void> close;
	private boolean closing;
	private boolean closed;
	
	private GenericWebSocketExchange webSocketExchange;

	/**
	 * <p>
	 * Creates an HTTP/1.x WebSocket connection.
	 * </p>
	 * 
	 * @param configuration      the HTTP client configurartion
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public Http1xWebSocketConnection(
			HttpClientConfiguration configuration, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.frameFactory = new GenericWebSocketFrame.GenericFactory(configuration.ws_max_frame_size());
		this.messageFactory = new GenericWebSocketMessage.GenericFactory(configuration.ws_max_frame_size());
		this.closeOnOutboundComplete = configuration.ws_close_on_outbound_complete();
		this.inboundCloseFrameTimeout = configuration.ws_inbound_close_frame_timeout();
	}
	
	@Override
	public boolean isTls() {
		return this.tls;
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.tls = ctx.pipeline().get(SslHandler.class) != null;
		this.closed = false;
		this.close = Mono.<Void>create(sink -> {
			if(!this.closed && !this.closing) {
				this.closing = true;
				// Write a closing frame
				ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
					.addListener(ChannelFutureListener.CLOSE)
					.addListener(future -> {
						if(future.isSuccess()) {
							sink.success();
						}
						else {
							sink.error(future.cause());
						}
					});
			}
			else {
				sink.success();
			}
		})
		.subscribeOn(Schedulers.fromExecutor(ctx.executor()));
		this.context = ctx;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof FullHttpResponse) {
			this.webSocketExchange.finishHandshake((FullHttpResponse) msg);
		}
		else if(msg instanceof TextWebSocketFrame || msg instanceof BinaryWebSocketFrame || msg instanceof ContinuationWebSocketFrame) {
			// emit frame to the frame sink
			this.webSocketExchange.inboundFrames().ifPresent(framesSink -> framesSink.tryEmitNext(new GenericWebSocketFrame(((WebSocketFrame)msg).retain())));
		}
		else if(msg instanceof CloseWebSocketFrame) {
			CloseWebSocketFrame closeFrame = (CloseWebSocketFrame)msg;
			this.webSocketExchange.onCloseReceived((short)closeFrame.statusCode(), closeFrame.reasonText());
		}
	}

	@Override
	public <A extends ExchangeContext> Mono<WebSocketConnectionExchange<A>> handshake(EndpointExchange<A> endpointExchange, String subprotocol) {
		return Mono.<WebSocketConnectionExchange<ExchangeContext>>create(exchangeSink -> {
			EventLoop eventLoop = this.context.channel().eventLoop();
			if(eventLoop.inEventLoop()) {
				this.sendHandshake(this.context, exchangeSink, endpointExchange, subprotocol);
			}
			else {
				eventLoop.submit(() -> {
					this.sendHandshake(this.context, exchangeSink, endpointExchange, subprotocol);
				});
			}
		})
		.map(exchange -> (WebSocketConnectionExchange<A>)exchange);
	}
	
	/**
	 * <p>
	 * Sends the WebSocket handshake.
	 * </p>
	 *
	 * @param context          the channel context
	 * @param exchangeSink     the WebSocket exchange sink
	 * @param endpointExchange the original endpoint exchange
	 * @param subprotocol      the subprotocol
	 */
	private void sendHandshake(
			ChannelHandlerContext context, 
			MonoSink<WebSocketConnectionExchange<ExchangeContext>> exchangeSink, 
			EndpointExchange<?> endpointExchange,
			String subprotocol) {
		if(this.webSocketExchange != null) {
			throw new IllegalStateException("Handshake already sent");
		}
		
		Http1xRequest http1xRequest = new Http1xRequest(this.context, this.tls, false, this.parameterConverter, endpointExchange.request());
		
		URI webSocketURI = URI.create((this.tls ? "wss:// ": "ws://") + http1xRequest.getAuthority() + http1xRequest.getPath());
		WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
			webSocketURI, 
			WebSocketVersion.V13, 
			subprotocol, 
			true, 
			http1xRequest.headers().getUnderlyingHeaders());
		
		this.webSocketExchange = new GenericWebSocketExchange(
			context, 
			exchangeSink, 
			handshaker, 
			endpointExchange.context(), 
			http1xRequest, 
			subprotocol, 
			this.frameFactory, 
			this.messageFactory, 
			this.closeOnOutboundComplete, 
			this.inboundCloseFrameTimeout
		);
		this.webSocketExchange.start();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(this.webSocketExchange != null) {
			if(cause instanceof WebSocketHandshakeException) {
				// No need to finalize exchange as it hasn't been emitted yet
				LOGGER.error("WebSocket handshake error", cause);
				this.webSocketExchange.dispose(cause);
				ctx.close();
			}
			else if(cause instanceof CorruptedWebSocketFrameException) {
				LOGGER.error("WebSocket procotol error", cause);
				CorruptedWebSocketFrameException corruptedWSFrameException = (CorruptedWebSocketFrameException)cause;
				this.webSocketExchange.close((short)corruptedWSFrameException.closeStatus().code(), corruptedWSFrameException.closeStatus().reasonText());
				this.webSocketExchange.dispose(cause);
			}
			else {
				LOGGER.error("WebSocket procotol error", cause);
				this.webSocketExchange.dispose(cause);
				ChannelPromise closePromise = this.context.newPromise();
				this.context.close(closePromise);
				this.webSocketExchange.finalizeExchange(closePromise);
			}
		}
	}
	
	@Override
	public Mono<Void> close() {
		if(this.closing || this.closed) {
			return Mono.empty();
		}
		else {
			return this.close;
		}
	}
}
