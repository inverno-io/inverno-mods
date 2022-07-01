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

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.Web2SocketExchange;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRoute;
import io.inverno.mod.web.WebSocketRoute;

/**
 * <p>
 * A route extractor used to extract WebSocket routes.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @see WebSocketRoute
 * @see RoutingLink
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 */
interface WebSocketRouteExtractor<A extends ExchangeContext, B extends WebRoute<A>, C extends WebSocketRouteExtractor<A, B, C>> extends WebRouteExtractor<A, B, C>, WebSocketProtocolAwareRouteExtractor<A, WebExchange<A>, B, C> {
	
	/**
	 * <p>
	 * Extracts the WebSocket route handler and finishes the current route.
	 * </p>
	 * 
	 * @param handler
	 * @param disabled
	 */
	void webSocketHandler(WebSocketExchangeHandler<A, Web2SocketExchange<A>> handler, boolean disabled);
}
