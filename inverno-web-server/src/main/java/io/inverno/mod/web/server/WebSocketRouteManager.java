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
package io.inverno.mod.web.server;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.server.spi.AcceptAware;
import io.inverno.mod.web.server.spi.PathAware;
import java.util.Set;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
/**
 * <p>
 * A WebSocket route manager is used to manage WebSocket routes in a web router.
 * </p>
 *
 * <p>
 * It is created by a web router and allows to define, enable, disable, remove and find WebSocket routes in a web router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @see WebExchange
 * @see WebRoute
 * @see WebRouter
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of web routable
 */
public interface WebSocketRouteManager<A extends ExchangeContext, B extends WebRoutable<A, B>> {
	
	/**
	 * <p>
	 * Specifies the path to the WebSocket resource served by the route without matching trailing slash.
	 * </p>
	 *
	 * <p>
	 * The specified path can be a parameterized path including path parameters as defined by {@link URIBuilder}.
	 * </p>
	 *
	 * @param path the path to the resource
	 *
	 * @return the WebSocket route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	default WebSocketRouteManager<A, B> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}
	
	/**
	 * <p>
	 * Specifies the path to the WebSocket resource served by the route matching or not trailing slash.
	 * </p>
	 *
	 * <p>
	 * The specified path can be a parameterized path including path parameters as defined by {@link URIBuilder}.
	 * </p>
	 *
	 * @param path               the path to the resource
	 * @param matchTrailingSlash true to match path with or without trailing slash, false otherwise
	 *
	 * @return the WebSocket route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	WebSocketRouteManager<A, B> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Specifies the language of the WebSocket resource served by the web route.
	 * </p>
	 *
	 * @param language a language tag
	 *
	 * @return the WebSocket route manager
	 *
	 * @see AcceptAware
	 */
	WebSocketRouteManager<A, B> language(String language);
	
	/**
	 * <p>
	 * Specifies the subprotocol supported by the WebSocket resource served by the route.
	 * </p>
	 *
	 * @param subprotocol a WebSocket subprotocol
	 *
	 * @return the WebSocket route manager
	 *
	 * @see WebSocketProtocolAware
	 */
	WebSocketRouteManager<A, B> subprotocol(String subprotocol);
	
	/**
	 * <p>
	 * Specifies the route WebSocket exchange handler.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the route specified in the WebSocket route manager to the router it comes from.
	 * </p>
	 *
	 * @param handler the route WebSocket exchange handler
	 *
	 * @return the Web routable
	 */
	B handler(WebSocketExchangeHandler<? super A, Web2SocketExchange<A>> handler);
	
	/**
	 * <p>
	 * Enables all WebSocket routes that matches the criteria specified in the WebSocket route manager and defined in the router it comes from.
	 * </p>
	 *
	 * @return the Web routable
	 */
	B enable();

	/**
	 * <p>
	 * Disables all WebSocket routes that matches the criteria specified in the WebSocket route manager and defined in the router it comes from.
	 * </p>
	 *
	 * @return the Web routable
	 */
	B disable();

	/**
	 * <p>
	 * Removes all WebSocket routes that matches the criteria specified in the WebSocket route manager and defined in the router it comes from.
	 * </p>
	 *
	 * @return the Web routable
	 */
	B remove();

	/**
	 * <p>
	 * Finds all WebSocket routes that matches the criteria specified in the WebSocket route manager and defined in the router it comes from.
	 * </p>
	 *
	 * @return a set of WebSocketRoute routes or an empty set if no route matches the criteria
	 */
	Set<WebSocketRoute<A>> findRoutes();
}
