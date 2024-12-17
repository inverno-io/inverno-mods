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
 * An error route.
 * </p>
 *
 * <p>
 * This is used to define route based on the error specified in an input. For instance, in order to resolve an handler for an error exchange with a {@code NotFoundException} error, an error route must
 * be defined with {@code NotFoundException} error type targeting a handler able to handle {@code NotFoundException}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see io.inverno.mod.http.base.router.link.ErrorRoutingLink
 *
 * @param <A> the resource type
 */
public interface ErrorRoute<A> extends Route<A> {

	/**
	 * <p>
	 * Returns an error type.
	 * </p>
	 *
	 * @return an error type or null
	 */
	Class<? extends Throwable> getErrorType();

	/**
	 * <p>
	 * An error route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the error route type
	 * @param <C> the error route extractor
	 */
	interface Extractor<A, B extends ErrorRoute<A>, C extends ErrorRoute.Extractor<A, B, C>> extends RouteExtractor<A, B> {

		/**
		 * <p>
		 * Sets the extractor to extract routes defined with the specified error type.
		 * </p>
		 *
		 * @param errorType an error type
		 *
		 * @return a route extractor
		 */
		C errorType(Class<? extends Throwable> errorType);
	}

	/**
	 * <p>
	 * An error route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the error route type
	 * @param <D> the error route manager type
	 * @param <E> the router type
	 */
	interface Manager<A, B, C extends ErrorRoute<A>, D extends ErrorRoute.Manager<A, B, C, D, E>, E extends Router<A, B, C, D, E>> extends RouteManager<A, B, C, D, E> {

		/**
		 * <p>
		 * Specifies the error type matching the error in an input.
		 * </p>
		 *
		 * @param errorType an error type
		 *
		 * @return the route manager
		 */
		D errorType(Class<? extends Throwable> errorType);
	}
}
