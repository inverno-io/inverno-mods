/*
 * Copyright 2020 Jeremy KUHN
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
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.spi.AcceptAware;
import io.inverno.mod.web.server.spi.ContentAware;
import io.inverno.mod.web.server.spi.PathAware;
import io.inverno.mod.web.server.spi.RouteManager;

/**
 * <p>
 * A web route manager is used to manage web routes in a web router.
 * </p>
 *
 * <p>
 * It is created by a web router and allows to define, enable, disable, remove and find routes in a web router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see WebExchange
 * @see WebRoute
 * @see WebRouter
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of web routable
 */
public interface WebRouteManager<A extends ExchangeContext, B extends WebRoutable<A, B>> extends RouteManager<A, WebExchange<A>, B, WebRouteManager<A, B>, WebRoute<A>> {
	
	/**
	 * <p>
	 * Specifies the path to the resource served by the web route without matching trailing slash.
	 * </p>
	 *
	 * <p>
	 * The specified path can be a parameterized path including path parameters as defined by {@link URIBuilder}.
	 * </p>
	 *
	 * @param path the path to the resource
	 *
	 * @return the web route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	default WebRouteManager<A, B> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}
	
	/**
	 * <p>
	 * Specifies the path to the resource served by the web route matching or not trailing slash.
	 * </p>
	 *
	 * <p>
	 * The specified path can be a parameterized path including path parameters as defined by {@link URIBuilder}.
	 * </p>
	 *
	 * @param path               the path to the resource
	 * @param matchTrailingSlash true to match path with or without trailing slash, false otherwise
	 *
	 * @return the web route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	WebRouteManager<A, B> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Specifies the method used to access the resource served by the web route.
	 * </p>
	 *
	 * @param method a HTTP method
	 *
	 * @return the web route manager
	 */
	WebRouteManager<A, B> method(Method method);
	
	/**
	 * <p>
	 * Specifies the media range defining the content types accepted by the resource served by the web route as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>.
	 * </p>
	 *
	 * @param mediaRange a media range
	 *
	 * @return the web route manager
	 *
	 * @see ContentAware
	 */
	WebRouteManager<A, B> consumes(String mediaRange);
	
	/**
	 * <p>
	 * Specifies the media type of the resource served by the web route.
	 * </p>
	 *
	 * @param mediaType a media type
	 *
	 * @return the web route manager
	 *
	 * @see AcceptAware
	 */
	WebRouteManager<A, B> produces(String mediaType);
	
	/**
	 * <p>
	 * Specifies the language of the resource served by the web route.
	 * </p>
	 *
	 * @param language a language tag
	 *
	 * @return the web route manager
	 *
	 * @see AcceptAware
	 */
	WebRouteManager<A, B> language(String language);
}
