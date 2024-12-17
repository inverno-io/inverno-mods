/*
 * Copyright 2022 Jeremy Kuhn
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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.inverno.mod.web.server.ws.Web2SocketExchange;

/**
 * <p>
 * Manages WebSocket routes in the Web server.
 * </p>
 *
 * <p>
 * A WebSocket route manager is obtained from {@link WebServer#webSocketRoute()}, it allows to specify the criteria a Web exchange must match to be handled by the WebSocket exchange handler defined in
 * {@link #handler(WebSocketExchangeHandler)}
 * </p>
 *
 * <p>
 * When setting the exchange handler, interceptors defined in an intercepted Web server are applied to the route when criteria are matching.
 * </p>
 *
 * <p>
 * It is possible to specify multiple values for any given criteria resulting in multiple WebSocket routes being created in the Web router. For instance, the following code will result in the creation
 * of two routes:
 * </p>
 *
 * <pre>{@code
 * webServer
 *     .route()
 *         .path("/path/to/resource")
 *         .subProtocol("json")
 *         .subProtocol("xml")
 *         .handler(exchange -> {...})
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the exchange context type
 * @param <B> the Web router type
 */
public interface WebSocketRouteManager<A extends ExchangeContext, B extends WebRouter<A>> extends BaseWebRouteManager<A, WebExchange<A>, WebSocketRoute<A>, B> {

	/**
	 * <p>
	 * Specifies the absolute path that must be matched by a Web exchange to be processed by the route.
	 * </p>
	 *
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that
	 * this path is only meant to filter routes and as a result path parameters have no use.
	 * </p>
	 *
	 * @param path a path
	 *
	 * @return the route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 */
	default WebSocketRouteManager<A, B> path(String path) {
		return this.path(path, false);
	}

	/**
	 * <p>
	 * Specifies the absolute path that must be matched with or without trailing slash by a Web exchange to be processed by the route.
	 * </p>
	 *
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that
	 * this path is only meant to filter routes and as a result path parameters have no use.
	 * </p>
	 *
	 * @param path a path
	 * @param matchTrailingSlash true to match path with or without trailing slash, false otherwise
	 *
	 * @return the route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 */
	WebSocketRouteManager<A, B> path(String path, boolean matchTrailingSlash);

	/**
	 * <p>
	 * Specifies the language tag as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a> that must be accepted by a Web exchange to be processed by the
	 * route.
	 * </p>
	 *
	 * @param languageTag a language tag (e.g. {@code fr-FR})
	 *
	 * @return the route manager
	 */
	WebSocketRouteManager<A, B> language(String languageTag);

	/**
	 * <p>
	 * Specifies the WebSocket subProtocol that must be matched by a Web exchange to be processed by the route.
	 * </p>
	 *
	 * @param subprotocol a subProtocol
	 *
	 * @return the route manager
	 */
	WebSocketRouteManager<A, B> subprotocol(String subprotocol);

	/**
	 * <p>
	 * Specifies the WebSocket exchange handler used to process Web exchanges matching the criteria specified in the route manager and returns the originating Web router.
	 * </p>
	 *
	 * <p>
	 * The WebSocket route manager is usually obtained from {@link WebServer#webSocketRoute()} in which case the resulting Web router is then a {@link WebServer} instance.
	 * </p>
	 *
	 * <p>
	 * Any Web route interceptor defined in an intercepted Web server and matching the criteria specified in the route manager is applied to the resulting WebSocket routes.
	 * </p>
	 *
	 * @param handler a WebSocket exchange handler
	 *
	 * @return the originating Web router
	 */
	B handler(WebSocketExchangeHandler<? super A, Web2SocketExchange<A>> handler);
}
