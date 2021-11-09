/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.RouteManager2;

/**
 *
 * @author jkuhn
 */
public interface WebRouteManager2<A extends ExchangeContext, B extends WebRoutable<A, B>> extends RouteManager2<
		A, 
		WebExchange<A>, 
		B, 
		WebRouteManager2<A, B>, 
		WebRoute<A>
	> {

	/**
	 * <p>
	 * Specifies the route web exchange handler.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the route specified in the web route manager to the web router it comes from.
	 * </p>
	 *
	 * @param handler the route web exchange handler
	 *
	 * @return the router
	 */
	B handler(WebExchangeHandler<? super A> handler);
	
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
	default WebRouteManager2<A, B> path(String path) throws IllegalArgumentException {
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
	WebRouteManager2<A, B> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Specifies the method used to access the resource served by the web route.
	 * </p>
	 *
	 * @param method a HTTP method
	 *
	 * @return the web route manager
	 */
	WebRouteManager2<A, B> method(Method method);
	
	/**
	 * <p>
	 * Specifies the media range defining the content types accepted by the resource served by the web route as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>
	 * </p>
	 *
	 * @param mediaRange a media range
	 *
	 * @return the web route manager
	 *
	 * @see ContentAware
	 */
	WebRouteManager2<A, B> consumes(String mediaRange);
	
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
	WebRouteManager2<A, B> produces(String mediaType);
	
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
	WebRouteManager2<A, B> language(String language);
}
