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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketException;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebRequest;
import reactor.core.publisher.Mono;

/**
 * <p>
 * {@link WebSocket} implementation that processes {@link Web2SocketExchange}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWeb2Socket implements WebSocket<ExchangeContext, Web2SocketExchange<ExchangeContext>> {

	private final WebSocket<ExchangeContext, ? extends WebSocketExchange<ExchangeContext>> webSocket;
	
	private final WebRequest request;
	
	private final DataConversionService dataConversionService;

	/**
	 * <p>
	 * Creates a generic Web2Socket.
	 * </p>
	 *
	 * @param webSocket             the original WebSocket
	 * @param request               the web request
	 * @param dataConversionService the data conversion service
	 */
	public GenericWeb2Socket(WebSocket<ExchangeContext, ? extends WebSocketExchange<ExchangeContext>> webSocket, WebRequest request, DataConversionService dataConversionService) {
		this.webSocket = webSocket;
		this.request = request;
		this.dataConversionService = dataConversionService;
	}
	
	@Override
	public WebSocket<ExchangeContext, Web2SocketExchange<ExchangeContext>> handler(WebSocketExchangeHandler<? super ExchangeContext, Web2SocketExchange<ExchangeContext>> handler) {
		this.webSocket.handler(new WebSocketExchangeHandlerAdapter<>(handler));
		return this;
	}

	@Override
	public WebSocket<ExchangeContext, Web2SocketExchange<ExchangeContext>> or(Mono<Void> fallback) {
		this.webSocket.or(fallback);
		return this;
	}
	
	/**
	 * <p>
	 * A WebSocket handler adapter that wraps the {@link WebSocketExchange} into a {@link Web2SocketExchange} and delegates the exchange handling to a Web2Socket exchange handler.
	 * </p>
	 * 
	 * @param <A> the original WebSocker exchange type
	 */
	private class WebSocketExchangeHandlerAdapter<A extends WebSocketExchange<ExchangeContext>> implements WebSocketExchangeHandler<ExchangeContext, A> {

		private final WebSocketExchangeHandler<? super ExchangeContext, Web2SocketExchange<ExchangeContext>> handler;

		/**
		 * <p>
		 * Creates a WebSocket handler adaper.
		 * </p>
		 * 
		 * @param handler the Web2Socket handler to adapt
		 */
		public WebSocketExchangeHandlerAdapter(WebSocketExchangeHandler<? super ExchangeContext, Web2SocketExchange<ExchangeContext>> handler) {
			this.handler = handler;
		}

		@Override
		public Mono<Void> defer(A webSocketExchange) throws WebSocketException {
			return this.handler.defer(new GenericWeb2SocketExchange(webSocketExchange, GenericWeb2Socket.this.request, GenericWeb2Socket.this.dataConversionService));
		}

		@Override
		public void handle(A webSocketExchange) throws WebSocketException {
			throw new UnsupportedOperationException();
		}
	} 
}
