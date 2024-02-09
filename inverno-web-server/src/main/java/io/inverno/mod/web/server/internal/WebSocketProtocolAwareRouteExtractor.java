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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.server.spi.Route;

/**
 * <p>
 * A route extractor to extract {@link WebSocketProtocolAwareRouteExtractor} routes.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 * @param <D> the route extractor type
 */
interface WebSocketProtocolAwareRouteExtractor<A extends ExchangeContext, B extends Exchange<A>, C extends Route<A, B>, D extends WebSocketProtocolAwareRouteExtractor<A, B, C, D>> extends RouteExtractor<A, B, C> {
	
	/**
	 * <p>
	 * Sets the extractor to extract WebSocket routes defined with the specified WebSocket subprotocol.
	 * </p>
	 * 
	 * @param subprotocol the Websocket subprotocol of the routes to extract
	 * 
	 * @return the route extractor
	 */
	D subprotocol(String subprotocol);
}
