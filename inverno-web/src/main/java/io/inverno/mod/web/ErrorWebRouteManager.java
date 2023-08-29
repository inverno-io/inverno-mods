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
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.ErrorRouteManager;
import io.inverno.mod.web.spi.PathAware;

/**
 * <p>
 * An error web route manager is used to manage the routes of an error web router. It is created by an error web router and allows to define, enable, disable, remove and find error web routes in an
 * error web router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ErrorWebExchange
 * @see ErrorWebRoute
 * @see ErrorWebRouter
 *
 * @param <A> the type of the exchange context
 * @param <B> the Error Web routable type
 */
public interface ErrorWebRouteManager<A extends ExchangeContext, B extends ErrorWebRoutable<A, B>> extends ErrorRouteManager<A, ErrorWebExchange<A>, B, ErrorWebRouteManager<A, B>, ErrorWebRoute<A>> {

	/**
	 * <p>
	 * Specifies the path to the resource served by the error web route without matching trailing slash.
	 * </p>
	 *
	 * <p>
	 * The specified path can be a parameterized path including path parameters as defined by {@link URIBuilder}.
	 * </p>
	 *
	 * @param path the path to the resource
	 *
	 * @return the error web route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	default ErrorWebRouteManager<A, B> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}

	/**
	 * <p>
	 * Specifies the path to the resource served by the error web route matching or not trailing slash.
	 * </p>
	 *
	 * <p>
	 * The specified path can be a parameterized path including path parameters as defined by {@link URIBuilder}.
	 * </p>
	 *
	 * @param path               the path to the resource
	 * @param matchTrailingSlash true to match path with or without trailing slash, false otherwise
	 *
	 * @return the error web route manager
	 *
	 * @throws IllegalArgumentException if the specified path is not absolute
	 *
	 * @see PathAware
	 */
	ErrorWebRouteManager<A, B> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;

	/**
	 * <p>
	 * Specifies the media type of the resource served by the error web route.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * 
	 * @return the error web route manager
	 * 
	 * @see AcceptAware
	 */
	ErrorWebRouteManager<A, B> produces(String mediaType);
	
	/**
	 * <p>
	 * Specifies the language of the resource served by the error web route.
	 * </p>
	 * 
	 * @param language a language tag
	 * 
	 * @return the error web route manager
	 * 
	 * @see AcceptAware
	 */
	ErrorWebRouteManager<A, B> language(String language);
}
