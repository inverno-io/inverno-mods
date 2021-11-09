/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.web;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.InterceptorManager2;

/**
 *
 * @author jkuhn
 */
public interface WebInterceptorManager2<A extends ExchangeContext, B extends WebInterceptable<A, B>> extends InterceptorManager2<
		A, 
		WebExchange<A>, 
		B, 
		WebInterceptorManager2<A, B>
	> {
	
	/**
	 * <p>
	 * Specifies the web exchange interceptor to apply to the resources matching the criteria defined in the web interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This method basically appends the interceptor and the associated route criteria to the web intercepted router it comes from.
	 * </p>
	 * 
	 * @param interceptor the route web exchange handler
	 * 
	 * @return the router
	 */
	B interceptor(WebExchangeInterceptor<? super A> interceptor);
	
	/**
	 * <p>
	 * Specifies the path without matching trailing slash to the resource to intercept.
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
	default WebInterceptorManager2<A, B> path(String path) throws IllegalArgumentException {
		return this.path(path, false);
	}
	
	/**
	 * <p>
	 * Specifies the path matching or not trailing slash to the resource to intercept.
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
	WebInterceptorManager2<A, B> path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Specifies the method of the routes that must be intercepted.
	 * </p>
	 * 
	 * @param method a HTTP method 
	 * 
	 * @return the web interceptor manager
	 */
	WebInterceptorManager2<A, B> method(Method method);
	
	/**
	 * <p>
	 * Specifies the media range defining the content types accepted by the resource to intercept as defined by
	 * <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a>
	 * </p>
	 *
	 * @param mediaRange a media range
	 *
	 * @return the web interceptor manager
	 *
	 * @see ContentAware
	 */
	WebInterceptorManager2<A, B> consumes(String mediaRange);
	
	/**
	 * <p>
	 * Specifies the media range matching the content type produced by the resource to intercept.
	 * </p>
	 * 
	 * @param mediaType a media type
	 * 
	 * @return the web interceptor manager
	 * 
	 * @see AcceptAware
	 */
	WebInterceptorManager2<A, B> produces(String mediaType);
	
	/**
	 * <p>
	 * Specifies the language range matching the language tag produced by the resource to intercept.
	 * </p>
	 * 
	 * @param language a language tag
	 * 
	 * @return the web interceptor manager
	 * 
	 * @see AcceptAware
	 */
	WebInterceptorManager2<A, B> language(String language);

}
