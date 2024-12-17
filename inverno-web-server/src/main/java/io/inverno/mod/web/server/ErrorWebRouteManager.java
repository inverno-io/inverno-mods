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
import io.inverno.mod.http.server.ExchangeHandler;

/**
 * <p>
 * Manages error Web routes in the Web server.
 * </p>
 *
 * <p>
 * An error Web route manager is obtained from {@link WebServer#routeError()}, it allows to specify the criteria an error Web exchange must match to be handled by the error Web exchange handler
 * defined in {@link #handler(ExchangeHandler)}.
 * </p>
 *
 * <p>
 * When setting the error exchange handler, interceptors defined in an intercepted Web server are applied to the route when criteria are matching.
 * </p>
 *
 * <p>
 * It is possible to specify multiple values for any given criteria resulting in multiple error Web routes being created in the error Web router. For instance, the following code will result in the
 * creation of two routes:
 * </p>
 *
 * <pre>{@code
 * webServer
 *     .routeError()
 *         .path("/path/to/resource")
 *         .error(IllegalArgumentException.class)
 *         .error(BadRequestException.class)
 *         .handler(exchange -> {...})
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ErrorWebRouter
 * @see WebServer
 *
 * @param <A> the exchange context type
 * @param <B> the error Web router type
 */
public interface ErrorWebRouteManager<A extends ExchangeContext, B extends ErrorWebRouter<A>> extends BaseWebRouteManager<A, ErrorWebExchange<A>, ErrorWebRoute<A>, B> {

	/**
	 * <p>
	 * Specifies the type of errors that must be matched by an error Web exchange to be processed by the route.
	 * </p>
	 *
	 * @param error an error type
	 *
	 * @return the route manager
	 */
	ErrorWebRouteManager<A, B> error(Class<? extends Throwable> error);

	/**
	 * <p>
	 * Specifies the absolute path that must be matched by an error Web exchange to be processed by the route.
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
	default ErrorWebRouteManager<A, B> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}

	/**
	 * <p>
	 * Specifies the absolute path that must be matched with or without trailing slash by an error Web exchange to be processed by the route.
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
	ErrorWebRouteManager<A, B> path(String path, boolean matchTrailingSlash);

	/**
	 * <p>
	 * Specifies the media range as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> defining the content types accepted by an error Web exchange to be
	 * processed by the route.
	 * </p>
	 *
	 * @param mediaRange a media range (e.g. {@code application/*})
	 *
	 * @return the route manager
	 */
	ErrorWebRouteManager<A, B> consume(String mediaRange);

	/**
	 * <p>
	 * Specifies the media type as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> that must be accepted by an error Web exchange to be processed by
	 * the route.
	 * </p>
	 *
	 * @param mediaType a media type
	 *
	 * @return the route manager
	 */
	ErrorWebRouteManager<A, B> produce(String mediaType);

	/**
	 * <p>
	 * Specifies the language tag as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a> that must be accepted by an error Web exchange to be processed by
	 * the route.
	 * </p>
	 *
	 * @param languageTag a language tag (e.g. {@code fr-FR})
	 *
	 * @return the route manager
	 */
	ErrorWebRouteManager<A, B> language(String languageTag);

	/**
	 * <p>
	 * Specifies the error Web exchange handler used to process error Web exchanges matching the criteria specified in the route manager and returns the originating error Web router.
	 * </p>
	 *
	 * <p>
	 * The error Web route manager is usually obtained from {@link WebServer#routeError()} in which case the resulting error Web router is then a {@link WebServer} instance.
	 * </p>
	 *
	 * <p>
	 * Any error Web route interceptor defined in an intercepted Web server and matching the criteria specified in the route manager is applied to the resulting Web routes.
	 * </p>
	 *
	 * @param handler an error Web exchange handler
	 *
	 * @return the originating error Web router
	 */
	B handler(ExchangeHandler<? super A, ErrorWebExchange<A>> handler);
}
