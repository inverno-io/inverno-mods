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
 * A route defines the path to a resource in a router.
 * </p>
 *
 * <p>
 * It is defined in a {@link Router} using a {@link RouteManager} and used to resolve the resource corresponding to particular input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the resource type
 */
public interface Route<A> {

	/**
	 * <p>
	 * Returns the resource defined on the route.
	 * </p>
	 *
	 * @return a resource
	 */
	A get();

	/**
	 * <p>
	 * Merges the route resource with the value that was previously defined in the router.
	 * </p>
	 *
	 * <p>
	 * Implementors can override this method to control how a route is set in a router when a resource has already been defined for the route. The default behaviour is to replace the existing value
	 * with the new one.
	 * </p>
	 *
	 * @param previous the resource value previously defined in the router or null if no resource was defined for the route
	 *
	 * @return the merged resource
	 */
	default A get(A previous) {
		return this.get();
	}

	/**
	 * <p>
	 * Enables the route in the router.
	 * </p>
	 */
	void enable();

	/**
	 * <p>
	 * Disables the route in the router.
	 * </p>
	 */
	void disable();

	/**
	 * <p>
	 * Determines whether the route is disabled in the router.
	 * </p>
	 *
	 * @return true if the route is disabled, false otherwise
	 */
	boolean isDisabled();

	/**
	 * <p>
	 * Removes the route from the router.
	 * </p>
	 */
	void remove();
}
