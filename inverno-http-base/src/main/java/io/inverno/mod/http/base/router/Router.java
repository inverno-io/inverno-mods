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

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * A router resolves the best matching resource for a specific input.
 * </p>
 *
 * <p>
 * In order to match a resource, a router uses route definitions that provides the target resource and the criteria used to match the input. A {@link Route} is defined on the router using a
 * {@link RouteManager} obtained from {@link #route()}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the resource type
 * @param <B> the router input type
 * @param <C> the route type
 * @param <D> the route manager type
 * @param <E> the router type
 */
public interface Router<A, B, C extends Route<A>, D extends RouteManager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> {

	/**
	 * <p>
	 * Returns a new route manager to define a route in the router.
	 * </p>
	 *
	 * @return a new route manager
	 */
	D route();

	/**
	 * <p>
	 * Defines a route in the router using a configurer.
	 * </p>
	 *
	 * @param configurer a route configurer
	 *
	 * @return the router
	 */
	E route(Consumer<D> configurer);

	/**
	 * <p>
	 * Returns the routes defined in the router.
	 * </p>
	 *
	 * @return a set of routes
	 */
	Set<C> getRoutes();

	/**
	 * <p>
	 * Resolves the resource best matching the specified input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return the best matching resource or null if the input is not matching any route
	 */
	A resolve(B input);

	/**
	 * <p>
	 * Resolves all resources matching the specified input ordered from the best matching to the least matching.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return a list of resources ordered from the best matching to the least matching or an empty if no route is matching the input
	 */
	Collection<A> resolveAll(B input);
}
