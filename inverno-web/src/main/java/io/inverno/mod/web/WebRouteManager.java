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
package io.inverno.mod.web;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.Exchange;

/**
 * <p>
 * A web route manager is used to manage the routes of an web router. It is
 * created by a web router and allows to define, enable, disable, remove and
 * find error routes in a web router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebExchange
 * @see WebRoute
 * @see WebRouter
 * 
 * @param <A> the type of web exchange handled by the route
 */
public interface WebRouteManager<A extends WebExchange> extends RouteManager<A, WebRouter<A>, WebRouteManager<A>, WebRoute<A>, Exchange> {
	
	/**
	 * <p>
	 * Specifies the route web exchange handler.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the route specified in the web route manager
	 * to the web router it comes from.
	 * </p>
	 * 
	 * @param handler the route web exchange handler
	 * 
	 * @return the router
	 */
	WebRouter<A> handler(WebExchangeHandler<? super A> handler);
	
	/**
	 * <p>
	 * Specifies the path to the resource served by the web route without matching
	 * trailing slash.
	 * </p>
	 * 
	 * <p>
	 * The specified path can be a parameterized path including path parameters as
	 * defined by {@link URIBuilder}.
	 * </p>
	 * 
	 * @param path the path to the resource
	 * 
	 * @return the web route manager
	 * @throws IllegalArgumentException if the specified path is not absolute
	 * 
	 * @see PathAwareRoute
	 */
	default WebRouteManager<A> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}
	
	/**
	 * <p>
	 * Specifies the path to the resource served by the web route matching or not
	 * trailing slash.
	 * </p>
	 * 
	 * <p>
	 * The specified path can be a parameterized path including path parameters as
	 * defined by {@link URIBuilder}.
	 * </p>
	 * 
	 * @param path               the path to the resource
	 * @param matchTrailingSlash true to match path with or without trailing slash,
	 *                           false otherwise
	 * 
	 * @return the web route manager
	 * @throws IllegalArgumentException if the specified path is not absolute
	 * 
	 * @see PathAwareRoute
	 */
	WebRouteManager<A> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Specifies the method used to access the resource served by the web route.
	 * </p>
	 * 
	 * @param method a HTTP method
	 * 
	 * @return the web route manager
	 */
	WebRouteManager<A> method(Method method);
	
	/**
	 * <p>
	 * Specifies the media range defining the content types accepted by the resource
	 * served by the web route as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section
	 * 5.3.2</a>
	 * </p>
	 * 
	 * @param mediaRange a media range
	 * 
	 * @return the web route manager
	 * 
	 * @see ContentAwareRoute
	 */
	WebRouteManager<A> consumes(String mediaRange);
	
	/**
	 * <p>
	 * Specifies the media type of the resource served by the web route.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * 
	 * @return the web route manager
	 * 
	 * @see AcceptAwareRoute
	 */
	WebRouteManager<A> produces(String mediaType);
	
	/**
	 * <p>
	 * Specifies the language of the resource served by the web route.
	 * </p>
	 * 
	 * @param language a language tag
	 * 
	 * @return the web route manager
	 * 
	 * @see AcceptAwareRoute
	 */
	WebRouteManager<A> language(String language);
}
