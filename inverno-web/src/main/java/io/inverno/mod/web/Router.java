/*
 * Copyright 2020 Jeremy KUHN
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

import java.util.Set;
import java.util.function.Consumer;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;

/**
 * <p>
 * Base router interface.
 * </p>
 * 
 * <p>
 * A router uses route definitions to determine the exchange handler to invoke
 * in order to process a request.
 * </p>
 * 
 * <p>
 * Routes are defined in the router using a route manager that allows to specify
 * route criteria and eventually the exchange handler to invoke to process a
 * request that matches the criteria.
 * </p>
 * 
 * <p>
 * A router is itself an exchange handler that implements a routing logic to
 * delegate the actual exchange processing to the exchange handler defined in
 * the route matching the original request. A router is typically used as root
 * or error handler in a HTTP server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Exchange
 * @see ExchangeHandler
 * @see Route
 * @see RouteManager
 * 
 * @param <A> the type of exchange handled by the route
 * @param <B> the router type
 * @param <C> the route manager type
 * @param <D> the route type
 * @param <E> the type of exchange handled by the router
 */
public interface Router<A extends Exchange, B extends Router<A, B, C, D, E>, C extends RouteManager<A, B, C, D, E>, D extends Route<A>, E extends Exchange>
		extends ExchangeHandler<E> {

	/**
	 * <p>
	 * Returns a route manager to define, enable, disable, remove or find routes
	 * in the router.
	 * </p>
	 * 
	 * @return a route manager
	 */
	C route();

	/**
	 * <p>
	 * Invokes the specified route configurer on a route manager.
	 * </p>
	 * 
	 * @param routeConfigurer a route configurer
	 * 
	 * @return the router
	 */
	@SuppressWarnings("unchecked")
	default B route(Consumer<C> routeConfigurer) {
		routeConfigurer.accept(this.route());
		return (B) this;
	}

	/**
	 * <p>
	 * Returns the routes defined in the router.
	 * </p>
	 * 
	 * @return a set of routes or an empty set if no route is defined in the router
	 */
	Set<D> getRoutes();
}
