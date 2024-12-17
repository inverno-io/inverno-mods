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

import io.inverno.mod.http.base.Method;

/**
 * <p>
 * An HTTP method route.
 * </p>
 *
 * <p>
 * This is used to define route based on the HTTP method specified in an input. For instance, in order to resolve a handler for an HTTP request with a {@code POST} method, a method route must be
 * defined with {@code POST} method targeting a {@code POST} handler.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.MethodRoutingLink
 *
 * @param <A> the resource type
 */
public interface MethodRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns an HTTP method.
	 * </p>
	 *
	 * @return an HTTP method
	 */
	Method getMethod();

	/**
	 * <p>
	 * An HTTP method route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the HTTP method route type
	 * @param <C> the HTTP method route extractor
	 */
	interface Extractor<A, B extends MethodRoute<A>, C extends MethodRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified HTTP method.
		 * </p>
		 *
		 * @param method an HTTP method
		 *
		 * @return a route extractor
		 */
		C method(Method method);
	}

	/**
	 * <p>
	 * An HTTP method route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the HTTP method route type
	 * @param <D> the HTTP method route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends MethodRoute<A>, D extends MethodRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the HTTP method matching the HTTP method in an input.
		 * </p>
		 *
		 * @param method an HTTP method
		 *
		 * @return the route manager
		 */
		D method(Method method);
	}
}
