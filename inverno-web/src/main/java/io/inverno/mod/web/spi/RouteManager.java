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
package io.inverno.mod.web.spi;

import java.util.Set;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;

/**
 * <p>
 * Base route manager interface.
 * </p>
 *
 * <p>
 * A route manager is used to manage the routes of a router. It is created by a router and allows to define, enable, disable, remove and find routes in a router.
 * </p>
 *
 * <p>
 * A typical implementation should define methods to set criteria used by the router to match an incoming exchange to a route and an exchange handler that eventually handles the matched exchange.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Exchange
 * @see Route
 * @see Router
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the router type
 * @param <D> the route manager type
 * @param <E> the route type
 * @param <F> the router exchange type
 * @param <G> the interceptable route type
 * @param <H> the type of exchange handled by the router
 */
public interface RouteManager<
		A extends ExchangeContext, 
		B extends Exchange<A>, 
		C extends Router<A, B, C, D, E, F, G, H>, 
		D extends InterceptedRouter<A, B, C, D, E, F, G, H>, 
		E extends RouteManager<A, B, C, D, E, F, G, H>, 
		F extends InterceptorManager<A, B, C, D, E, F, G, H>, 
		G extends InterceptableRoute<A, B>, 
		H extends Exchange<A>
	> {
	
	/**
	 * <p>
	 * Enables all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	C enable();
	
	/**
	 * <p>
	 * Disables all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	C disable();
	
	/**
	 * <p>
	 * Removes all the routes that matches the criteria specified in the route
	 * manager and defined in the router it comes from.
	 * </p>
	 * 
	 * @return the router
	 */
	C remove();
	
	/**
	 * <p>
	 * Finds all the routes that matches the criteria specified in the route manager
	 * and defined in the router it comes from.
	 * </p>
	 * 
	 * @return a set of routes or an empty set if no route matches the criteria
	 */
	Set<G> findRoutes();
}
