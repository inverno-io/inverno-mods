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
package io.inverno.mod.web.server.internal.ws;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ws.WebSocketException;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.server.WebRequest;
import io.inverno.mod.web.server.internal.ServerDataConversionService;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * {@link WebSocket} implementation that processes {@link Web2SocketExchange}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the exchange context type
 */
public class GenericWeb2Socket<A extends ExchangeContext> implements WebSocket<A, Web2SocketExchange<A>> {

	private final ServerDataConversionService dataConversionService;
	private final WebRequest request;
	private final WebSocket<A, ? extends WebSocketExchange<A>> webSocket;

	/**
	 * <p>
	 * Creates a generic Web2Socket.
	 * </p>
	 *
	 * @param webSocket             the original WebSocket
	 * @param request               the Web request
	 * @param dataConversionService the data conversion service
	 */
	public GenericWeb2Socket(ServerDataConversionService dataConversionService, WebRequest request, WebSocket<A, ? extends WebSocketExchange<A>> webSocket) {
		this.webSocket = webSocket;
		this.request = request;
		this.dataConversionService = dataConversionService;
	}

	@Override
	public WebSocket<A, Web2SocketExchange<A>> handler(WebSocketExchangeHandler<? super A, Web2SocketExchange<A>> handler) {
		this.webSocket.handler(new WebSocketExchangeHandlerAdapter<>(handler));
		return this;
	}

	@Override
	public WebSocket<A, Web2SocketExchange<A>> or(Mono<Void> fallback) {
		this.webSocket.or(fallback);
		return this;
	}

	/**
	 * <p>
	 * A WebSocket handler adapter that wraps the {@link WebSocketExchange} into a {@link Web2SocketExchange} and delegates the exchange handling to a Web2Socket exchange handler.
	 * </p>
	 *
	 * @param <B> the original WebSocket exchange type
	 */
	private class WebSocketExchangeHandlerAdapter<B extends WebSocketExchange<A>> implements WebSocketExchangeHandler<A, B> {

		private final WebSocketExchangeHandler<? super A, Web2SocketExchange<A>> handler;

		/**
		 * <p>
		 * Creates a WebSocket handler adaper.
		 * </p>
		 *
		 * @param handler the Web2Socket handler to adapt
		 */
		public WebSocketExchangeHandlerAdapter(WebSocketExchangeHandler<? super A, Web2SocketExchange<A>> handler) {
			this.handler = handler;
		}

		@Override
		public Mono<Void> defer(B webSocketExchange) throws WebSocketException {
			return this.handler.defer(new GenericWeb2SocketExchange<>(GenericWeb2Socket.this.dataConversionService, GenericWeb2Socket.this.request, webSocketExchange));
		}

		@Override
		public void handle(B webSocketExchange) throws WebSocketException {
			throw new UnsupportedOperationException();
		}
	}
}
