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
package io.winterframework.mod.http.server;

import java.net.SocketAddress;
import java.util.Optional;

import io.winterframework.mod.http.base.Method;

/**
 * <p>
 * Represents a client request in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Exchange
 */
public interface Request {

	/**
	 * <p>
	 * Returns the HTTP headers sent in the request.
	 * </p>
	 * 
	 * @return the headers
	 */
	RequestHeaders headers();
	
	/**
	 * <p>
	 * Returns the query parameters sent in the request.
	 * </p>
	 * 
	 * @return the query parameters
	 */
	QueryParameters queryParameters();
	
	/**
	 * <p>
	 * Returns the cookies sent in the request.
	 * </p>
	 * 
	 * @return the cookies
	 */
	RequestCookies cookies();
	
	/**
	 * <p>
	 * Returns the HTTP method of the request.
	 * </p>
	 * 
	 * @return the HTTP method
	 */
	Method getMethod();
	
	/**
	 * <p>
	 * Returns the name of the scheme used to send the request (eg. http, https...).
	 * </p>
	 * 
	 * @return the name of the scheme
	 */
	String getScheme();
	
	/**
	 * <p>
	 * Returns the requested authority.
	 * </p>
	 * 
	 * @return the authority
	 */
	String getAuthority();
	
	/**
	 * <p>
	 * Returns the path to the resource targeted in the request.
	 * </p>
	 * 
	 * <p>
	 * This path corresponds to the origin form as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section
	 * 5.3.1</a>, as such it may contain a query URI component.
	 * </p>
	 * 
	 * @return the path to the targeted resource
	 */
	String getPath();
	
	/**
	 * <p>
	 * Returns the absolute path to the resource targeted in the request.
	 * </p>
	 * 
	 * <p>
	 * This path corresponds to the absolute path of the origin form as defined by
	 * <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section
	 * 5.3.1</a>, as such it only contains the path URI component.
	 * </p>
	 * 
	 * <p>
	 * The resulting path is also normalized as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-6">RFC 3986 Section
	 * 6</a>.
	 * </p>
	 * 
	 * @return the normalized absolute path to the targeted resource
	 */
	String getPathAbsolute();
	
	/**
	 * <p>
	 * Returns the query URI component.
	 * </p>
	 * 
	 * @return a URI query component
	 */
	String getQuery();
	
	/**
	 * <p>
	 * Returns the socket address of the client or last proxy that sent the request.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getRemoteAddress();
	
	/**
	 * <p>
	 * Returns the request body used to consume request payload.
	 * </p>
	 * 
	 * @return an optional returning the request body or an empty optional if the
	 *         request has no payload
	 */
	Optional<? extends RequestBody> body();
}
