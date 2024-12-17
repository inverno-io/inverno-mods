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
package io.inverno.mod.http.base.router;

/**
 * <p>
 * A WebSocket subprotocol route.
 * </p>
 *
 * <p>
 * This is used to define route based on the WebSocket subprotocol specified in an input. For instance, in order to resolve a handler for a WebSocket request with subprotocol {@code json}, a WebSocket
 * subprotocol route must be defined with subprotocol {@code json} targeting a WSebSocket handler consuming and producing {@code json} messages.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.AuthorityRoutingLink
 *
 * @param <A> the resource type
 */
public interface WebSocketSubprotocolRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns a WebSocket subprotocol.
	 * </p>
	 *
	 * @return a WebSocket subprotocol or null
	 */
	String getSubprotocol();

	/**
	 * <p>
	 * A WebSocket subprotocol route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the WebSocket subprotocol route type
	 * @param <C> the WebSocket subprotocol route extractor
	 */
	interface Extractor<A, B extends WebSocketSubprotocolRoute<A>, C extends WebSocketSubprotocolRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified WebSocket subprotocol.
		 * </p>
		 *
		 * @param subprotocol a WebSocket subprotocol
		 *
		 * @return a route extractor
		 */
		C subprotocol(String subprotocol);
	}

	/**
	 * <p>
	 * A WebSocket subprotocol route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the WebSocket subprotocol route type
	 * @param <D> the WebSocket subprotocol route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends WebSocketSubprotocolRoute<A>, D extends WebSocketSubprotocolRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the WebSocket subprotocol matching the WebSocket subprotocol in an input.
		 * </p>
		 *
		 * @param subprotocol a WebSocket subprotocol
		 *
		 * @return the route manager
		 */
		D subprotocol(String subprotocol);
	}
}
