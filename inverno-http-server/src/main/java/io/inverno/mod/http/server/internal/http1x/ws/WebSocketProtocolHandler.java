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
package io.inverno.mod.http.server.internal.http1x.ws;

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import java.util.List;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * The WebSocket protocol channel handler used to perform the WebSocket upgrade.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class WebSocketProtocolHandler extends WebSocketServerProtocolHandler {

	private final WebSocketExchangeHandler<ExchangeContext, WebSocketExchange<ExchangeContext>> handler;
	private final Request request;
	private final GenericWebSocketFrame.GenericFactory frameFactory;
	private final GenericWebSocketMessage.GenericFactory messageFactory;
	
	private final Sinks.One<Void> handshake;
	
	private GenericWebSocketExchange webSocketExchange;
	
	/**
	 * <p>
	 * Creates a WebSocket protocol handler
	 * </p>
	 *
	 * @param config         the WebServer protocol configuration
	 * @param handler        the WebSocket exchange handler
	 * @param request        the HTTP/1.x exchange request
	 * @param frameFactory   the WebSocket frame factory
	 * @param messageFactory the WebSocket message factory
	 */
	public WebSocketProtocolHandler(
			WebSocketServerProtocolConfig config, 
			WebSocketExchangeHandler<ExchangeContext, WebSocketExchange<ExchangeContext>> handler, 
			Request request, 
			GenericWebSocketFrame.GenericFactory frameFactory, 
			GenericWebSocketMessage.GenericFactory messageFactory) {
		super(config);
		this.handler = handler;
		this.request = request;
		this.frameFactory = frameFactory;
		this.messageFactory = messageFactory;
		
		this.handshake = Sinks.one();
	}

	/**
	 * <p>
	 * Returns the handshake mono which completes or fails with the opening handshake.
	 * </p>
	 * 
	 * @return the opening handshake mono
	 */
	public Mono<Void> getHandshake() {
		return handshake.asMono();
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
			WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete)evt;
			this.webSocketExchange = new GenericWebSocketExchange(ctx, this.request, handshakeComplete.selectedSubprotocol(), this.handler, this.frameFactory, this.messageFactory);
			this.handshake.tryEmitEmpty();
			this.webSocketExchange.start();
		}
		else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, io.netty.handler.codec.http.websocketx.WebSocketFrame frame, List<Object> out) throws Exception {
		if(frame instanceof TextWebSocketFrame || frame instanceof BinaryWebSocketFrame || frame instanceof ContinuationWebSocketFrame) {
			// emit frame to the frame sink
			this.webSocketExchange.inboundFrames().ifPresent(framesSink -> framesSink.tryEmitNext(new GenericWebSocketFrame(frame.retain())));
		}
		else if(frame instanceof CloseWebSocketFrame) {
			// handle close properly
			ChannelPromise closePromise = ctx.newPromise();
			ctx.writeAndFlush(Unpooled.EMPTY_BUFFER, closePromise);
			closePromise.addListener(ChannelFutureListener.CLOSE);
			this.webSocketExchange.setClosed();
			this.webSocketExchange.dispose();
			this.webSocketExchange.finalizeExchange(closePromise);
		}
		else {
			super.decode(ctx, frame, out);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof WebSocketHandshakeException) {
			this.handshake.tryEmitError(cause);
		}
		else if(cause instanceof CorruptedWebSocketFrameException) {
			CorruptedWebSocketFrameException corruptedWSFrameException = (CorruptedWebSocketFrameException)cause;
			this.webSocketExchange.close((short)corruptedWSFrameException.closeStatus().code(), corruptedWSFrameException.closeStatus().reasonText());
			this.webSocketExchange.dispose();
		}
		else {
			ctx.fireExceptionCaught(cause);
			ctx.close();
		}
	}
}
