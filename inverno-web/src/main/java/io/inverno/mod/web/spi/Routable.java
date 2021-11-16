/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Defines method to specify interceptors on a router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @see InterceptedRouter
 * @see Router
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the routable type
 * @param <D> the route manager type
 * @param <E> the route type
 */
public interface Routable<
		A extends ExchangeContext, 
		B extends Exchange<A>, 
		C extends Routable<A, B, C, D, E>,
		D extends RouteManager<A, B, C, D, E>,
		E extends Route<A, B>
	> {

	/**
	 * <p>
	 * Returns a route manager to define, enable, disable, remove or find routes
	 * in the router.
	 * </p>
	 * 
	 * @return a route manager
	 */
	D route();
	
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
	default C route(Consumer<D> routeConfigurer) {
		routeConfigurer.accept(this.route());
		return (C) this;
	}
	
	/**
	 * <p>
	 * Returns the routes defined in the router.
	 * </p>
	 * 
	 * @return a set of routes or an empty set if no route is defined in the router
	 */
	Set<E> getRoutes();
}
