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
 * Entry point for configuring the error Web routes used to route error Web exchanges to a matching error Web exchange handlers.
 * </p>
 *
 * <p>
 * It is implemented by the {@link WebServer}. Error handlers are defined using an {@link ErrorWebRouteManager} which allows to specify the criteria an error Web exchange must match to be processed by
 * the error Web exchange handler defined in the route.
 * </p>
 *
 * <p>
 * When defining a route, the error Web route interceptors defined in an intercepted Web server and matching the route's criteria are applied to the error Web exchange handler.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see ErrorWebRouteManager
 * @see WebServer
 *
 * @param <A> the exchange context type
 */
public interface ErrorWebRouter<A extends ExchangeContext> extends BaseWebRouter {

	/**
	 * <p>
	 * Returns a new route manager for defining an error Web route.
	 * </p>
	 *
	 * @return a new error Web route manager
	 */
	ErrorWebRouteManager<A, ? extends ErrorWebRouter<A>> routeError();

	/**
	 * <p>
	 * Configures an error Web route and returns the originating error Web router.
	 * </p>
	 *
	 * @param configurer an error Web route configurer function
	 *
	 * @return the originating error Web router
	 */
	default ErrorWebRouter<A> routeError(Consumer<ErrorWebRouteManager<A, ? extends ErrorWebRouter<A>>> configurer) {
		configurer.accept(this.routeError());
		return this;
	}

	/**
	 * <p>
	 * Configures multiple error Web routes and returns the originating error Web router.
	 * </p>
	 *
	 * @param configurer an error Web route configurer
	 *
	 * @return the originating error Web router
	 */
	ErrorWebRouter<A> configureErrorRoutes(ErrorWebRouter.Configurer<? super A> configurer);

	/**
	 * <p>
	 * Configures multiple error Web routes and returns the originating error Web router.
	 * </p>
	 *
	 * @param configurers a list of error Web route configurers
	 *
	 * @return the originating error Web router
	 */
	ErrorWebRouter<A> configureErrorRoutes(List<ErrorWebRouter.Configurer<? super A>> configurers);

	/**
	 * <p>
	 * Returns the error Web routes defined in the router.
	 * </p>
	 *
	 * @return a set of error Web routes
	 */
	Set<ErrorWebRoute<A>> getErrorRoutes();

	/**
	 * <p>
	 * A configurer used to configure error Web routes in a Web server.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	@FunctionalInterface
	interface Configurer<A extends ExchangeContext> {

		/**
		 * <p>
		 * Configures error Web routes.
		 * </p>
		 *
		 * @param routes the error Web router to use to define error Web routes
		 */
		void configure(ErrorWebRouter<A> routes);
	}
}
