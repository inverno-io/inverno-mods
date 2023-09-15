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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketFrame;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketMessage;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.internal.http1x.ws.WebSocketProtocolHandler;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameServerExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateServerExtensionHandshaker;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * HTTP/1.x {@link WebSocket} implementation.
 * </p>
 * 
 * <p>
 * Note that WebSocket upgrade is only supported by HTTP/1.x protocol.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class Http1xWebSocket implements WebSocket<ExchangeContext, WebSocketExchange<ExchangeContext>> {
	
	private final HttpServerConfiguration configuration;
	private final ChannelHandlerContext context;
	private final Http1xExchange exchange;
	private final GenericWebSocketFrame.GenericFactory frameFactory;
	private final GenericWebSocketMessage.GenericFactory messageFactory;
	
	private final WebSocketServerProtocolConfig protocolConfig;
	
	private WebSocketExchangeHandler<? super ExchangeContext, WebSocketExchange<ExchangeContext>> handler;
	private Mono<Void> fallback;

	private Map<String, ChannelHandler> initialChannelHandlers;
	
	/**
	 * <p>
	 * Creates an HTTP/1.x WebSocket.
	 * </p>
	 *
	 * @param configuration  the server configuration
	 * @param context        the channel handler context
	 * @param request        the original HTTP/1.x exchange
	 * @param frameFactory   the WebSocket frame factory
	 * @param messageFactory the WebSocket message factory
	 * @param subProtocols   the list of supported subprotocols
	 */
	public Http1xWebSocket(HttpServerConfiguration configuration, ChannelHandlerContext context, Http1xExchange exchange, GenericWebSocketFrame.GenericFactory frameFactory, GenericWebSocketMessage.GenericFactory messageFactory, String[] subProtocols) {
		this.configuration = configuration;
		this.context = context;
		this.exchange = exchange;
		this.frameFactory = frameFactory;
		this.messageFactory = messageFactory;
		
		WebSocketServerProtocolConfig.Builder webSocketConfigBuilder = WebSocketServerProtocolConfig.newBuilder()
			.websocketPath(exchange.request().getPath())
			.subprotocols(subProtocols != null && subProtocols.length > 0 ? Arrays.stream(subProtocols).collect(Collectors.joining(",")) : null)
			.checkStartsWith(false)
			.handleCloseFrames(false)
			.dropPongFrames(true)
			.expectMaskedFrames(true)
			.closeOnProtocolViolation(false)
			.allowMaskMismatch(configuration.ws_allow_mask_mismatch())
			.forceCloseTimeoutMillis(configuration.ws_close_timeout())
			.maxFramePayloadLength(configuration.ws_max_frame_size())
			.allowExtensions(configuration.ws_frame_compression_enabled() || configuration.ws_message_compression_enabled());
		
		if(configuration.ws_handshake_timeout() > 0) {
			webSocketConfigBuilder.handshakeTimeoutMillis(configuration.ws_handshake_timeout());
		}
		
		this.protocolConfig = webSocketConfigBuilder.build();
	}
	
	/**
	 * <p>
	 * Setups the channel pipeline to handle the WebSocket upgrade and opening handshake.
	 * </p>
	 *
	 * <p>
	 * The resulting mono completes once the opening handshake is completed or fails if the handshake failed. It is used to notify the original {@link Http1xExchange} which restores the pipeline (see 
	 * {@link #restorePipeline() }) in case the upgrade fails.
	 * </p>
	 *
	 * @return a Mono that completes or fails with the opening handshake
	 */
	Mono<Void> handshake() {
		return Mono.defer(() -> {
			WebSocketProtocolHandler webSocketProtocolHandler = new WebSocketProtocolHandler(
				this.configuration, 
				this.protocolConfig, 
				this.handler, 
				this.exchange, 
				this.frameFactory, 
				this.messageFactory
			);
		
			ChannelPipeline pipeline = this.context.pipeline();
			this.initialChannelHandlers = pipeline.toMap();
			
			List<WebSocketServerExtensionHandshaker> extensionHandshakers = new LinkedList<>();
			if(this.configuration.ws_frame_compression_enabled()) {
				extensionHandshakers.add(new DeflateFrameServerExtensionHandshaker(this.configuration.ws_frame_compression_level()));
			}
			if(this.configuration.ws_message_compression_enabled()) {
				extensionHandshakers.add(new PerMessageDeflateServerExtensionHandshaker(
					this.configuration.ws_message_compression_level(),
					this.configuration.ws_message_allow_server_window_size(),
					this.configuration.ws_message_prefered_client_window_size(),
					this.configuration.ws_message_allow_server_no_context(),
					this.configuration.ws_message_preferred_client_no_context()
				));
			}
			if(!extensionHandshakers.isEmpty()) {
				pipeline.addLast(new WebSocketServerExtensionHandler(extensionHandshakers.toArray(WebSocketServerExtensionHandshaker[]::new)));
			}
			pipeline.addLast(webSocketProtocolHandler);

			this.context.fireChannelRead(((Http1xRequest)exchange.request()).getUnderlyingRequest());
			
			return webSocketProtocolHandler.getHandshake();
		});
	}

	/**
	 * <p>
	 * Restores the pipeline to its original state typically after a failed upgrade in order to continue process HTTP requests on that channel.
	 * </p>
	 */
	public void restorePipeline() {
		if(this.initialChannelHandlers != null) {
			ChannelPipeline pipeline = this.context.pipeline();
			
			for(String currentHandlerName : pipeline.toMap().keySet()) {
				if(!this.initialChannelHandlers.containsKey(currentHandlerName)) {
					pipeline.remove(currentHandlerName);
				}
			}
			
			Map<String, ChannelHandler> currentChannelHandlers = pipeline.toMap();
			if(currentChannelHandlers.size() != this.initialChannelHandlers.size()) {
				// We may have missing handlers
				Iterator<String> currentHandlerNamesIterator = currentChannelHandlers.keySet().iterator();
				if(currentHandlerNamesIterator.hasNext()) {
					String currentHandlerName = currentHandlerNamesIterator.next();
					for(Map.Entry<String, ChannelHandler> currentInitialHandler : this.initialChannelHandlers.entrySet()) {
						if(!currentInitialHandler.getKey().equals(currentHandlerName)) {
							pipeline.addBefore(currentHandlerName, currentInitialHandler.getKey(), currentInitialHandler.getValue());
						}
						else {
							if(!currentHandlerNamesIterator.hasNext()) {
								break;
							}
							currentHandlerName = currentHandlerNamesIterator.next();
						}
					}
				}
			}
		}
	}
	
	/**
	 * <p>
	 * Returns the fallback mono to subscribe in case the opening handshake failed.
	 * </p>
	 * 
	 * @return a fallback handler
	 */
	public Mono<Void> getFallback() {
		return fallback;
	}

	@Override
	public WebSocket<ExchangeContext, WebSocketExchange<ExchangeContext>> handler(WebSocketExchangeHandler<? super ExchangeContext, WebSocketExchange<ExchangeContext>> handler) {
		this.handler = handler;
		return this;
	}

	@Override
	public WebSocket<ExchangeContext, WebSocketExchange<ExchangeContext>> or(Mono<Void> fallback) {
		this.fallback = fallback;
		return this;
	}
}
