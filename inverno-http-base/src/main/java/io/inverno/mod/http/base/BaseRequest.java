/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.base;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface BaseRequest {
	
	/**
	 * <p>
	 * Returns the HTTP headers sent in the request.
	 * </p>
	 * 
	 * @return the headers
	 */
	InboundRequestHeaders headers();

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
	 * Returns the request authority.
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
	 * <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section 5.3.1</a>, as such it may contain a query URI component.
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
	 * <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section 5.3.1</a>, as such it only contains the path URI component.
	 * </p>
	 *
	 * <p>
	 * The resulting path is also normalized as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-6">RFC 3986 Section 6</a>.
	 * </p>
	 *
	 * @return the normalized absolute path to the targeted resource
	 */
	String getPathAbsolute();
	
	/**
	 * <p>
	 * Returns a path builder initialized with the path of the request.
	 * </p>
	 *
	 * <p>
	 * This method always returns a new {@link URIBuilder} instance with {@link URIs.Option#NORMALIZED} option. It is then safe to use it to build
	 * relative paths.
	 * </p>
	 *
	 * @return a new URI builder
	 */
	URIBuilder getPathBuilder();
	
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
	 * Returns the query parameters sent in the request.
	 * </p>
	 * 
	 * @return the query parameters
	 */
	QueryParameters queryParameters();
}
