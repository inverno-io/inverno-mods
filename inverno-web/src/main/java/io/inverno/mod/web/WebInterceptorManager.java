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
package io.inverno.mod.web;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.ContentAware;
import io.inverno.mod.web.spi.InterceptorManager;
import io.inverno.mod.web.spi.PathAware;

import java.util.List;

/**
 * <p>
 * A web interceptor manager is used to define interceptors in a web intercepting router.
 * </p>
 * 
 * <p>
 * It is created by a web router and allows to define interceptors in a web intercepting router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of Web interceptable
 */
public interface WebInterceptorManager<A extends ExchangeContext, B extends WebInterceptable<A, B>> extends InterceptorManager<A, WebExchange<A>, B, WebInterceptorManager<A, B>> {
	
	/**
	 * <p>
	 * Specifies the web exchange interceptor to apply to the resources matching the criteria defined in the web interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the interceptor and the associated route criteria to the web intercepted router it comes from.
	 * </p>
	 * 
	 * @param interceptor  the web exchange interceptor
	 *
	 * @return the router
	 */
	B interceptor(WebExchangeInterceptor<? super A> interceptor);

	/**
	 * <p>
	 * Specifies multiple web exchange interceptors to apply to the resources matching the criteria defined in the web
	 * interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the interceptors and the associated route criteria to the web intercepted router it
	 * comes from.
	 * </p>
	 *
	 * @param interceptors a list of web exchange interceptors
	 *
	 * @return the router
	 */
	B interceptors(List<WebExchangeInterceptor<? super A>> interceptors);

	/**
	 * <p>
	 * Specifies the path without matching trailing slash to the route to intercept.
	 * </p>
	 * 
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link URIBuilder}. Note that this path is only meant to
	 * filter routes and as a result path parameters have no use.
	 * </p>
	 * 
	 * @param path a path
	 * 
	 * @return the web interceptor manager
	 * @throws IllegalArgumentException if the specified path is not absolute
	 * 
	 * @see PathAware
	 */
	default WebInterceptorManager<A, B> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}
	
	/**
	 * <p>
	 * Specifies the path matching or not trailing slash to the route to intercept.
	 * </p>
	 * 
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link URIBuilder}. Note that this path is only meant to
	 * filter routes and as a result path parameters have no use.
	 * </p>
	 * 
	 * @param path               a path
	 * @param matchTrailingSlash true to match path with or without trailing slash,
	 *                           false otherwise
	 * 
	 * @return the web interceptor manager
	 * @throws IllegalArgumentException if the specified path is not absolute
	 * 
	 * @see PathAware
	 */
	WebInterceptorManager<A, B> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Specifies the method of the routes that must be intercepted.
	 * </p>
	 * 
	 * @param method a HTTP method 
	 * 
	 * @return the web interceptor manager
	 */
	WebInterceptorManager<A, B> method(Method method);
	
	/**
	 * <p>
	 * Specifies the media range defining the content types accepted by the route to intercept as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>
	 * </p>
	 *
	 * @param mediaRange a media range
	 *
	 * @return the web interceptor manager
	 *
	 * @see ContentAware
	 */
	WebInterceptorManager<A, B> consumes(String mediaRange);
	
	/**
	 * <p>
	 * Specifies the media range matching the content type produced by the route to intercept.
	 * </p>
	 * 
	 * @param mediaRange a media range
	 * 
	 * @return the web interceptor manager
	 * 
	 * @see AcceptAware
	 */
	WebInterceptorManager<A, B> produces(String mediaRange);
	
	/**
	 * <p>
	 * Specifies the language range matching the language tag produced by the route to intercept.
	 * </p>
	 * 
	 * @param languageRange a language range
	 * 
	 * @return the web interceptor manager
	 * 
	 * @see AcceptAware
	 */
	WebInterceptorManager<A, B> language(String languageRange);
}
