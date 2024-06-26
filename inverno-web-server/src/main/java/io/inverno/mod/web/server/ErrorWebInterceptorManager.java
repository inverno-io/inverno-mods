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
package io.inverno.mod.web.server;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.spi.AcceptAware;
import io.inverno.mod.web.server.spi.ContentAware;
import io.inverno.mod.web.server.spi.ErrorInterceptorManager;
import io.inverno.mod.web.server.spi.PathAware;
import java.util.List;

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
 * @param <A> the type of the exchange context
 * @param <B> the type of Error Web interceptable
 */
public interface ErrorWebInterceptorManager<A extends ExchangeContext, B extends ErrorWebInterceptable<A, B>> extends ErrorInterceptorManager<A, ErrorWebExchange<A>, B, ErrorWebInterceptorManager<A, B>> {

	/**
	 * <p>
	 * Specifies an error web exchange interceptor to apply to the resources matching the criteria defined in the web interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the interceptor and the associated route criteria to the web intercepted router it comes from.
	 * </p>
	 *
	 * @param interceptor the error web exchange interceptor
	 *
	 * @return the error router
	 */
	@Override
	B interceptor(ExchangeInterceptor<? super A, ErrorWebExchange<A>> interceptor);

	/**
	 * <p>
	 * Specifies multiple error web exchange interceptors to apply to the resources matching the criteria defined in the web interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the interceptors and the associated route criteria to the web intercepted router it comes from.
	 * </p>
	 *
	 * @param interceptors a list of error web exchange interceptors
	 *
	 * @return the error router
	 */
	@Override
			B interceptors(List<ExchangeInterceptor<? super A, ErrorWebExchange<A>>> interceptors);

	/**
	 * <p>
	 * Specifies the path without matching trailing slash to the route to intercept.
	 * </p>
	 *
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *},
	 * {@code **} as defined by {@link URIBuilder}. Note that this path is only meant to filter routes and as a result path parameters have no use.
	 * </p>
	 *
	 * @param path a path
	 *
	 * @return the error web interceptor manager
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	default ErrorWebInterceptorManager<A, B> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}

	/**
	 * <p>
	 * Specifies the path matching or not trailing slash to the route to intercept.
	 * </p>
	 *
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *},
	 * {@code **} as defined by {@link URIBuilder}. Note that this path is only meant to filter routes and as a result path parameters have no use.
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
	ErrorWebInterceptorManager<A, B> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;

	/**
	 * <p>
	 * Specifies the media range defining the content types accepted by the route to intercept as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>.
	 * </p>
	 *
	 * @param mediaRange a media range
	 *
	 * @return the web interceptor manager
	 *
	 * @see ContentAware
	 */
	ErrorWebInterceptorManager<A, B> consumes(String mediaRange);
	
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
	ErrorWebInterceptorManager<A, B> produces(String mediaRange);

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
	ErrorWebInterceptorManager<A, B> language(String languageRange);
}
