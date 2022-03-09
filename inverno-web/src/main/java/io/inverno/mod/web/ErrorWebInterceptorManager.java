/*
 * Copyright 2022 Jeremy KUHN
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

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.ErrorInterceptorManager;
import io.inverno.mod.web.spi.PathAware;

/**
 * <p>
 * An error web interceptor manager is used to define interceptors in an error web router.
 * </p>
 *
 * <p>
 * It is created by an error web router and allows to define interceptors in an error web intercepted router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of Error Web interceptable
 */
public interface ErrorWebInterceptorManager<A extends ErrorWebInterceptable<A>> extends ErrorInterceptorManager<ErrorWebExchange<Throwable>, A, ErrorWebInterceptorManager<A>> {

	/**
	 * <p>
	 * Specifies the error web exchange interceptor to apply to the resources matching the criteria defined in the web
	 * interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the interceptor and the associated route criteria to the web intercepted router it
	 * comes from.
	 * </p>
	 *
	 * @param interceptor the error web exchange interceptor
	 *
	 * @return the error router
	 */
	A interceptor(ErrorWebExchangeInterceptor<? extends Throwable> interceptor);

	/**
	 * <p>
	 * Specifies the path without matching trailing slash to the route to intercept.
	 * </p>
	 *
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *},
	 * {@code **} as defined by {@link URIBuilder}. Note that this path is only meant to filter routes and as a result
	 * path parameters have no use.
	 * </p>
	 *
	 * @param path a path
	 *
	 * @return the error web interceptor manager
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	default ErrorWebInterceptorManager<A> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}

	/**
	 * <p>
	 * Specifies the path matching or not trailing slash to the route to intercept.
	 * </p>
	 *
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *},
	 * {@code **} as defined by {@link URIBuilder}. Note that this path is only meant to filter routes and as a result
	 * path parameters have no use.
	 * </p>
	 *
	 * @param path               a path
	 * @param matchTrailingSlash true to match path with or without trailing slash, false otherwise
	 *
	 * @return the error web interceptor manager
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	ErrorWebInterceptorManager<A> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;

	/**
	 * <p>
	 * Specifies the media range matching the content type produced by the route to intercept.
	 * </p>
	 *
	 * @param mediaRange a media range
	 *
	 * @return the error web interceptor manager
	 *
	 * @see AcceptAware
	 */
	ErrorWebInterceptorManager<A> produces(String mediaRange);

	/**
	 * <p>
	 * Specifies the language range matching the language tag produced by the route to intercept.
	 * </p>
	 *
	 * @param languageRange a language range
	 *
	 * @return the error web interceptor manager
	 *
	 * @see AcceptAware
	 */
	ErrorWebInterceptorManager<A> language(String languageRange);
}
