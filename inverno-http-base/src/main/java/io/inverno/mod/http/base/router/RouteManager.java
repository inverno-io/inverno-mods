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

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <p>
 * A route manager is used to fluently define {@link Route routes} in a {@link Router}.
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
public interface RouteManager<A, B, C extends Route<A>, D extends RouteManager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> {

	/**
	 * <p>
	 * Sets the routes targeting the specified resource in the router and returns the router.
	 * </p>
	 *
	 * <p>
	 * Note that the same resource instance is targeted by all the resulting routes created in the router.
	 * </p>
	 *
	 * @param resource the target resource
	 *
	 * @return the router
	 */
	default E set(A resource) {
		return this.set(() -> resource);
	}

	/**
	 * <p>
	 * Sets the routes targeting resources provided by the specified factory in the router and returns the router.
	 * </p>
	 *
	 * <p>
	 * Unlike {@link #set(Object)}, each resulting routes target a different resource instance provided by the specified resource factory.
	 * </p>
	 *
	 * @param resourceFactory a resource factory
	 *
	 * @return the router
	 */
	default E set(Supplier<A> resourceFactory) {
		return this.set(resourceFactory, ign -> {});
	}

	/**
	 * <p>
	 * Sets the routes in the router targeting resources provided by the specified factory, configure the resulting routes with the specified configurer and returns the router.
	 * </p>
	 *
	 * <p>
	 * Unlike {@link #set(Object)}, each resulting routes target a different resource instance provided by the specified resource factory.
	 * </p>
	 *
	 * <p>
	 * The route configurer allows to post process the route before adding it to the server. This especially allows to set specific route information into the resource instance.
	 * </p>
	 *
	 * @param resourceFactory a resource factory
	 * @param routeConfigurer a route configurer
	 *
	 * @return the router
	 */
	E set(Supplier<A> resourceFactory, Consumer<C> routeConfigurer);

	/**
	 * <p>
	 * Enables all routes matching the criteria specified in the route manager and defined in the router it comes from.
	 * </p>
	 *
	 * @return the router
	 */
	E enable();

	/**
	 * <p>
	 * Disables all routes matching the criteria specified in the route manager and defined in the router it comes from.
	 * </p>
	 *
	 * @return the router
	 */
	E disable();

	/**
	 * <p>
	 * Removes all routes matching the criteria specified in the route manager and defined in the router it comes from.
	 * </p>
	 *
	 * @return the router
	 */
	E remove();

	/**
	 * <p>
	 * Finds all routes matching the criteria specified in the route manager and defined in the router it comes from.
	 * </p>
	 *
	 * @return a set of routes or an empty set if no route matches the criteria
	 */
	Set<C> findRoutes();
}
