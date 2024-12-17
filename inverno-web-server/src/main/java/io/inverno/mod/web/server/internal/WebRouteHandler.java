/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ws.WebSocketException;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.ws.Web2SocketExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Web route handler used as a resource in the {@link io.inverno.mod.web.server.internal.router.InternalWebRouter}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public class WebRouteHandler<A extends ExchangeContext> extends AbstractWebRouteHandler<A, WebExchange<A>> {

	private String subprotocol;
	private WebSocketExchangeHandler<A, Web2SocketExchange<A>> webSocketHandler;

	/**
	 * <p>
	 * Creates a Web route handler.
	 * </p>
	 *
	 * @param handler a Web exchange handler
	 */
	public WebRouteHandler(ReactiveExchangeHandler<A, WebExchange<A>> handler) {
		super(handler);
	}

	/**
	 * <p>
	 * Creates a WebSocket route handler.
	 * </p>
	 *
	 * @param webSocketHandler a WebSocket exchange handler
	 */
	public WebRouteHandler(WebSocketExchangeHandler<A, Web2SocketExchange<A>> webSocketHandler) {
		this.setWebSocketHandler(webSocketHandler);
	}

	/**
	 * <p>
	 * Sets the WebSocket subProtocol supported by the route.
	 * </p>
	 *
	 * <p>
	 * This information is used when upgrading the exchange to negotiate WebSocket subProtocol with the client.
	 * </p>
	 *
	 * @param subprotocol the WebSocket subProtocol supported by the route
	 */
	public void setSubprotocol(String subprotocol) {
		this.subprotocol = subprotocol;
	}

	/**
	 * <p>
	 * Returns the WebSocket subProtocol supported by the route.
	 * </p>
	 *
	 * @return a WebSocket subProtocol or null if no protocol was specified when defining the route or if this is not a WebSocket route handler
	 */
	public String getSubprotocol() {
		return subprotocol;
	}

	/**
	 * <p>
	 * Returns the WebSocket exchange handler.
	 * </p>
	 *
	 * @return a WebSocket exchange handler
	 */
	public WebSocketExchangeHandler<A, Web2SocketExchange<A>> getWebSocketHandler() {
		return this.webSocketHandler;
	}

	/**
	 * <p>
	 * Sets the WebSocket exchange handler.
	 * </p>
	 *
	 * @param webSocketHandler a Web Socket exchange handler
	 */
	private void setWebSocketHandler(WebSocketExchangeHandler<A, Web2SocketExchange<A>> webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
		this.setHandler(exchange -> Mono.fromRunnable(
			() -> exchange.webSocket(this.subprotocol != null ? new String[] { this.subprotocol} : new String[0])
				.orElseThrow(() -> new WebSocketException("WebSocket upgrade not supported"))
				.handler(webSocketHandler)
		));
	}
}
