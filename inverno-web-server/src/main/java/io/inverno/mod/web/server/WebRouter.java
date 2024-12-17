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
package io.inverno.mod.web.server;

import io.inverno.mod.http.base.ExchangeContext;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Entry point for configuring the Web routes used to route Web exchanges to a matching Web exchange handlers.
 * </p>
 *
 * <p>
 * It is implemented by the {@link WebServer}. Handlers are defined using a {@link WebRouteManager} or {@link WebSocketRouteManager} which allows to specify the criteria a Web exchange must match to
 * be processed by the Web exchange handler defined in the route.
 * </p>
 *
 * <p>
 * When defining a route, the Web route interceptors defined in an intercepted Web server and matching the route's criteria are applied to the Web exchange handler.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebRouteManager
 * @see WebServer
 *
 * @param <A> the exchange context type
 */
public interface WebRouter<A extends ExchangeContext> extends BaseWebRouter {

	/**
	 * <p>
	 * Returns a new route manager for defining a Web route.
	 * </p>
	 *
	 * @return a new Web route manager
	 */
	WebRouteManager<A, ? extends WebRouter<A>> route();

	/**
	 * <p>
	 * Configures a Web route and returns the originating Web router.
	 * </p>
	 *
	 * @param configurer a Web route configurer function
	 *
	 * @return the originating Web router
	 */
	default WebRouter<A> route(Consumer<WebRouteManager<A, ? extends WebRouter<A>>> configurer) {
		configurer.accept(this.route());
		return this;
	}

	/**
	 * <p>
	 * Returns a new route manager for defining a WebSocket route.
	 * </p>
	 *
	 * @return a new WebSocket route manager
	 */
	WebSocketRouteManager<A, ? extends WebRouter<A>> webSocketRoute();

	/**
	 * <p>
	 * Configures a WebSocket route and returns the originating Web router.
	 * </p>
	 *
	 * @param configurer a WebSocket route configurer function
	 *
	 * @return the originating Web router
	 */
	default WebRouter<A> webSocketRoute(Consumer<WebSocketRouteManager<A, ? extends WebRouter<A>>> configurer) {
		configurer.accept(this.webSocketRoute());
		return this;
	}

	/**
	 * <p>
	 * Configures multiple Web routes or WebSocket routes and returns the originating Web router.
	 * </p>
	 *
	 * @param configurer a Web route configurer
	 *
	 * @return the originating Web router
	 */
	WebRouter<A> configureRoutes(WebRouter.Configurer<? super A> configurer);

	/**
	 * <p>
	 * Configures multiple Web routes or WebSocket routes and returns the originating Web router.
	 * </p>
	 *
	 * @param configurers a list of Web route configurers
	 *
	 * @return the originating Web router
	 */
	WebRouter<A> configureRoutes(List<WebRouter.Configurer<? super A>> configurers);

	/**
	 * <p>
	 * Returns the Web routes defined in the router.
	 * </p>
	 *
	 * @return a set of Web routes
	 */
	Set<WebRoute<A>> getRoutes();

	/**
	 * <p>
	 * Returns the WebSocket routes defined in the router.
	 * </p>
	 *
	 * @return a set of WebSocket routes
	 */
	Set<WebSocketRoute<A>> getWebSocketRoutes();

	/**
	 * <p>
	 * A configurer used to configure Web routes in a Web server.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the type of the exchange context
	 */
	@FunctionalInterface
	interface Configurer<A extends ExchangeContext> {

		/**
		 * <p>
		 * Configures routes.
		 * </p>
		 *
		 * @param routes the Web router to use to define Web routes or WebSocket routes
		 */
		void configure(WebRouter<A> routes);
	}
}
