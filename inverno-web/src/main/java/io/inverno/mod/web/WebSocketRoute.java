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
package io.inverno.mod.web;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;

/**
 * <p>
 * A web route that upgrade to the WebSocket procotol.
 * </p>
 * 
 * <p>
 * A WebSocket route specified a {@link WebSocketExchangeHandler} used to handle a {@link Web2SocketExchange} once the WebSocket connection has been established. The method shall always be {@code GET}
 * and consume, produce and handler always {@code null}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface WebSocketRoute<A extends ExchangeContext> extends WebRoute<A>, WebSocketProtocolAware {
	
	/**
	 * <p>
	 * Always returns {@code GET}.
	 * </p>
	 */
	@Override
	default Method getMethod() {
		return Method.GET;
	}

	/**
	 * <p>
	 * Always returns {@code null}.
	 * </p>
	 */
	@Override
	default String getConsume() {
		return null;
	}

	/**
	 * <p>
	 * Always returns {@code null}.
	 * </p>
	 */
	@Override
	default String getProduce() {
		return null;
	}

	/**
	 * <p>
	 * Always returns {@code null}.
	 * </p>
	 */
	@Override
	default ReactiveExchangeHandler<A, WebExchange<A>> getHandler() {
		return null;
	}
	
	/**
	 * <p>
	 * Returns the route WebSocket handler used to process a WebSocket exchange for a WebSocket upgrade request matching the route's criteria.
	 * </p>
	 * 
	 * @return a WebSocket exchange handler
	 */
	WebSocketExchangeHandler<A, Web2SocketExchange<A>> getWebSocketHandler();
}
