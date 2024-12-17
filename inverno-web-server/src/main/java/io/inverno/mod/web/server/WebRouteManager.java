/*
 * Copyright 2020 Jeremy Kuhn
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
import io.inverno.mod.http.server.ExchangeHandler;

/**
 * <p>
 * Manages Web routes in the Web server.
 * </p>
 *
 * <p>
 * A Web route manager is obtained from {@link WebServer#route()}, it allows to specify the criteria a Web exchange must match to be handled by the Web exchange handler defined in
 * {@link #handler(ExchangeHandler)}.
 * </p>
 *
 * <p>
 * When setting the exchange handler, interceptors defined in an intercepted Web server are applied to the route when criteria are matching.
 * </p>
 *
 * <p>
 * It is possible to specify multiple values for any given criteria resulting in multiple Web routes being created in the Web router. For instance, the following code will result in the creation of
 * two routes:
 * </p>
 *
 * <pre>{@code
 * webServer
 *     .route()
 *         .path("/path/to/resource")
 *         .method(Method.GET)
 *         .method(Method.POST)
 *         .handler(exchange -> {...})
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebRouter
 * @see WebServer
 *
 * @param <A> the exchange context type
 * @param <B> the Web router type
 */
public interface WebRouteManager<A extends ExchangeContext, B extends WebRouter<A>> extends BaseWebRouteManager<A, WebExchange<A>, WebRoute<A>, B> {

	/**
	 * <p>
	 * Specifies the absolute path that must be matched by a Web exchange to be processed by the route.
	 * </p>
	 *
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that
	 * this path is only meant to filter routes and as a result path parameters have no use.
	 * </p>
	 *
	 * @param path a path
	 *
	 * @return the route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 */
	default WebRouteManager<A, B> path(String path) {
		return this.path(path, false);
	}

	/**
	 * <p>
	 * Specifies the absolute path that must be matched with or without trailing slash by a Web exchange to be processed by the route.
	 * </p>
	 *
	 * <p>
	 * The specified path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that
	 * this path is only meant to filter routes and as a result path parameters have no use.
	 * </p>
	 *
	 * @param path a path
	 * @param matchTrailingSlash true to match path with or without trailing slash, false otherwise
	 *
	 * @return the route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 */
	WebRouteManager<A, B> path(String path, boolean matchTrailingSlash);

	/**
	 * <p>
	 * Specifies the HTTP method that must be matched by a Web exchange to be processed by the route.
	 * </p>
	 *
	 * @param method an HTTP method
	 *
	 * @return the route manager
	 */
	WebRouteManager<A, B> method(Method method);

	/**
	 * <p>
	 * Specifies the media range as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> defining the content types accepted by a Web exchange to be
	 * processed by the route.
	 * </p>
	 *
	 * @param mediaRange a media range (e.g. {@code application/*})
	 *
	 * @return the route manager
	 */
	WebRouteManager<A, B> consume(String mediaRange);

	/**
	 * <p>
	 * Specifies the media type as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> that must be accepted by a Web exchange to be processed by the
	 * route.
	 * </p>
	 *
	 * @param mediaType a media type
	 *
	 * @return the route manager
	 */
	WebRouteManager<A, B> produce(String mediaType);

	/**
	 * <p>
	 * Specifies the language tag as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a> that must be accepted by a Web exchange to be processed by the
	 * route.
	 * </p>
	 *
	 * @param languageTag a language tag (e.g. {@code fr-FR})
	 *
	 * @return the route manager
	 */
	WebRouteManager<A, B> language(String languageTag);

	/**
	 * <p>
	 * Specifies the Web exchange handler used to process Web exchanges matching the criteria specified in the route manager and returns the originating Web router.
	 * </p>
	 *
	 * <p>
	 * The Web route manager is usually obtained from {@link WebServer#route()} in which case the resulting Web router is then a {@link WebServer} instance.
	 * </p>
	 *
	 * <p>
	 * Any Web route interceptor defined in an intercepted Web server and matching the criteria specified in the route manager is applied to the resulting Web routes.
	 * </p>
	 *
	 * @param handler a Web exchange handler
	 *
	 * @return the originating Web router
	 */
	B handler(ExchangeHandler<? super A, WebExchange<A>> handler);
}
