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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketFrame;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketMessage;
import io.inverno.mod.http.base.ws.WebSocketException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.ws.WebSocket;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * The WebSocket protocol connection used to perform the WebSocket upgrade.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class WebSocketConnection extends WebSocketServerProtocolHandler {

	private static final Logger LOGGER = LogManager.getLogger(WebSocket.class);
	
	private final Exchange<ExchangeContext> exchange;
	private final GenericWebSocketFrame.GenericFactory frameFactory;
	private final GenericWebSocketMessage.GenericFactory messageFactory;
	
	private final boolean closeOnOutboundComplete;
	private final long inboundCloseFrameTimeout;
	
	private final Sinks.One<GenericWebSocketExchange> handshake;
	
	private GenericWebSocketExchange webSocketExchange;
	
	/**
	 * <p>
	 * Creates a WebSocket protocol handler
	 * </p>
	 *
	 * @param configuration  the HTTP server configurartion
	 * @param protocolConfig the WebServer protocol configuration
	 * @param exchange       the original HTTP/1.x exchange
	 * @param frameFactory   the WebSocket frame factory
	 * @param messageFactory the WebSocket message factory
	 */
	public WebSocketConnection(
			HttpServerConfiguration configuration,
			WebSocketServerProtocolConfig protocolConfig, 
			Exchange<ExchangeContext> exchange, 
			GenericWebSocketFrame.GenericFactory frameFactory, 
			GenericWebSocketMessage.GenericFactory messageFactory
		) {
		super(protocolConfig);
		this.exchange = exchange;
		this.frameFactory = frameFactory;
		this.messageFactory = messageFactory;
		this.closeOnOutboundComplete = configuration.ws_close_on_outbound_complete();
		this.inboundCloseFrameTimeout = configuration.ws_inbound_close_frame_timeout();
		
		this.handshake = Sinks.one();
	}

	/**
	 * <p>
	 * Returns a mono which emits the resulting WebSocket exchange on success or fails on handshake failure.
	 * </p>
	 * 
	 * @return the opening handshake mono
	 */
	public Mono<GenericWebSocketExchange> getWebSocketExchange() {
		return this.handshake.asMono();
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
			WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete)evt;
			this.webSocketExchange = new GenericWebSocketExchange(
				ctx, 
				this.exchange, 
				handshakeComplete.selectedSubprotocol(), 
				this.frameFactory, 
				this.messageFactory, 
				this.closeOnOutboundComplete, 
				this.inboundCloseFrameTimeout
			);
			this.handshake.tryEmitValue(this.webSocketExchange);
		}
		else if(evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_TIMEOUT) {
			this.handshake.tryEmitError(new WebSocketException("Handshake timeout"));
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
			CloseWebSocketFrame closeFrame = (CloseWebSocketFrame)frame;
			this.webSocketExchange.onCloseReceived((short)closeFrame.statusCode(), closeFrame.reasonText());
		}
		else {
			super.decode(ctx, frame, out);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(this.webSocketExchange != null) {
			if(cause instanceof WebSocketHandshakeException) {
				// No need to finalize exchange as it hasn't been emitted yet
				LOGGER.error("WebSocket handshake error", cause);
				this.handshake.tryEmitError(cause);
				ctx.close();
			}
			else if(cause instanceof CorruptedWebSocketFrameException) {
				LOGGER.error("WebSocket procotol error", cause);
				CorruptedWebSocketFrameException corruptedWSFrameException = (CorruptedWebSocketFrameException)cause;
				this.webSocketExchange.close((short)corruptedWSFrameException.closeStatus().code(), corruptedWSFrameException.closeStatus().reasonText());
				this.webSocketExchange.dispose();
			}
			else {
				LOGGER.error("WebSocket procotol error", cause);
				this.webSocketExchange.dispose(cause);
				ChannelPromise closePromise = ctx.newPromise();
				ctx.close(closePromise);
				this.webSocketExchange.finalizeExchange(closePromise);
			}
		}
		
		if (cause instanceof WebSocketHandshakeException) {
			this.handshake.tryEmitError(cause);
		}
		else if(cause instanceof CorruptedWebSocketFrameException) {
			LOGGER.error("WebSocket procotol error", cause);
			CorruptedWebSocketFrameException corruptedWSFrameException = (CorruptedWebSocketFrameException)cause;
			this.webSocketExchange.close((short)corruptedWSFrameException.closeStatus().code(), corruptedWSFrameException.closeStatus().reasonText());
			this.webSocketExchange.dispose(cause);
		}
		else {
			// This is the last channel handler so we don't want to propagate the error
//			ctx.fireExceptionCaught(cause);
			LOGGER.error("WebSocket procotol error", cause);
			ctx.close();
		}
	}
}