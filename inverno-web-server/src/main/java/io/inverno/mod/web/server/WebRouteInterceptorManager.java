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
package io.inverno.mod.web.server;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.List;

/**
 * <p>
 * Defines Web route interceptors and creates decorated Web servers intercepting Web routes.
 * </p>
 *
 * <p>
 * A Web route interceptor manager is obtained from {@link WebServer#intercept()} or more generally {@link WebRouteInterceptor#intercept()}, it allows to specify the criteria a Web route must match to
 * be intercepted by the exchange interceptors defined in {@link #interceptor(ExchangeInterceptor)} or {@link #interceptors(List)}. These methods return the intercepted Web server which shall be used
 * to define Web routes intercepted when they are matching the defined criteria.
 * </p>
 *
 * <p>
 * It is possible to specify multiple values for any given criteria resulting in multiple Web route interceptor definitions being created in the resulting Web route interceptor. For instance, the
 * following code will result in two definitions being created:
 * </p>
 *
 * <pre>{@code
 * webServer
 *     .intercept()
 *         .path("/path/to/resource")
 *         .method(Method.GET)
 *         .method(Method.POST)
 *         .interceptor(exchange -> {...})
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebRouteInterceptor
 * @see WebServer
 *
 * @param <A> the exchange context type
 * @param <B> the Web route interceptor type
 */
public interface WebRouteInterceptorManager<A extends ExchangeContext, B extends WebRouteInterceptor<A>> {

	/**
	 * <p>
	 * Specifies the absolute path that must be matched by a Web route to be intercepted.
	 * </p>
	 *
	 * <p>
	 * The path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this path is
	 * only meant to filter routes and as a result path parameters have no use.
	 * </p>
	 *
	 * @param path a path
	 *
	 * @return the interceptor manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 */
	default WebRouteInterceptorManager<A, B> path(String path) {
		return this.path(path, false);
	}

	/**
	 * <p>
	 * Specifies the absolute path that must be matched with or without trailing slash by a Web route to be intercepted.
	 * </p>
	 *
	 * <p>
	 * The path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this path is
	 * only meant to filter routes and as a result path parameters have no use.
	 * </p>
	 *
	 * @param path               a path
	 * @param matchTrailingSlash true to match path with or without trailing slash, false otherwise
	 *
	 * @return the interceptor manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 */
	WebRouteInterceptorManager<A, B> path(String path, boolean matchTrailingSlash);

	/**
	 * <p>
	 * Specifies the HTTP method that must be matched by a Web route to be intercepted.
	 * </p>
	 *
	 * @param method an HTTP method
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> method(Method method);

	/**
	 * <p>
	 * Specifies the media range as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> defining the content types accepted by a Web route to be
	 * intercepted.
	 * </p>
	 *
	 * @param mediaRange a media range (e.g. {@code application/*})
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> consume(String mediaRange);

	/**
	 * <p>
	 * Specifies the media range as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> matching the content type produced by a Web route to be
	 * intercepted.
	 * </p>
	 *
	 * @param mediaRange a media range (e.g. {@code application/*})
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> produce(String mediaRange);

	/**
	 * <p>
	 * Specifies the language range as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a> matching the language produced by a Web route to be intercepted.
	 * </p>
	 *
	 * @param languageRange a language range (e.g. {@code *-FR})
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> language(String languageRange);

	/**
	 * <p>
	 * Specifies the Web exchange interceptor to apply to a Web route matching the criteria specified in the route manager and returns a Web route interceptor containing the interceptor definitions
	 * specified in the interceptor manager.
	 * </p>
	 *
	 * <p>
	 * The Web route interceptor manager is usually obtained from {@link WebServer#intercept()} in which case the resulting Web route interceptor is a {@link WebServer.Intercepted} instance.
	 * </p>
	 *
	 * @param interceptor a Web exchange interceptor
	 *
	 * @return a new Web route interceptor containing one or more interceptor definitions
	 */
	B interceptor(ExchangeInterceptor<? super A, WebExchange<A>> interceptor);

	/**
	 * <p>
	 * Specifies a list of Web exchange interceptors to apply to a Web route matching the criteria specified in the route manager and returns a Web route interceptor containing the interceptor
	 * definitions specified in the interceptor manager.
	 * </p>
	 *
	 * <p>
	 * The Web route interceptor manager is usually obtained from {@link WebServer#intercept()} in which case the resulting Web route interceptor is a {@link WebServer.Intercepted} instance.
	 * </p>
	 *
	 * @param interceptors a list of Web exchange interceptors
	 *
	 * @return a new Web route interceptor containing one or more interceptor definitions
	 */
	B interceptors(List<ExchangeInterceptor<? super A, WebExchange<A>>> interceptors);
}
