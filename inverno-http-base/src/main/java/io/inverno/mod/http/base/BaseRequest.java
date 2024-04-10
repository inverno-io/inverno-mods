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
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * <p>
 * Base HTTP request for representing client or server requests.
 * </p>
 *
 * <p>
 * It exposes content information following HTTP request message format as defined by <a href="https://tools.ietf.org/html/rfc7230">RFC7230</a> and
 * <a href="https://tools.ietf.org/html/rfc7231">RFC7231</a>.
 * </p>
 *
 * <p>
 * Considering a client exchange, where the request is created and sent from the client to the server, implementation shall provide methods to set HTTP request content. Considering a server exchange,
 * where the request is received by the server from the client, implementation shall provide methods to access HTTP request content.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface BaseRequest {
	
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
	 * Returns the socket address of the interface on which the request was sent or received.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getLocalAddress();
	
	/**
	 * <p>
	 * Returns the certificates that were sent to the remote peer during handshaking.
	 * </p>
	 * 
	 * @return an optional returning the list of local certificates or an empty optional if no certificates were sent.
	 */
	Optional<Certificate[]> getLocalCertificates();
	
	/**
	 * <p>
	 * Returns the socket address of the client or last proxy that sent or received the request.
	 * </p>
	 * 
	 * @return a socket address
	 */
	SocketAddress getRemoteAddress();
	
	/**
	 * <p>
	 * Returns the certificates that were received from the remote peer during handshaking.
	 * </p>
	 * 
	 * @return an optional returning the list of remote certificates or an empty optional if no certificates were received.
	 */
	Optional<Certificate[]> getRemoteCertificates();

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
	 * This path corresponds to the origin form as defined by <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section 5.3.1</a>, as such it may contain a query URI component.
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
	 * This path corresponds to the absolute path of the origin form as defined by <a href="https://tools.ietf.org/html/rfc7230#section-5.3.1">RFC 7230 Section 5.3.1</a>, as such it only contains the
	 * path URI component.
	 * </p>
	 *
	 * <p>
	 * The resulting path is also normalized as defined by <a href="https://tools.ietf.org/html/rfc3986#section-6">RFC 3986 Section 6</a>.
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
	 * This method always returns a new {@link URIBuilder} instance with {@link URIs.Option#NORMALIZED} option. It is then safe to use it to build relative paths.
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
	 * Returns the query parameters of the request.
	 * </p>
	 * 
	 * @return the request query parameters
	 */
	QueryParameters queryParameters();
	
	/**
	 * <p>
	 * Returns the HTTP headers of the request.
	 * </p>
	 * 
	 * @return the request headers
	 */
	InboundRequestHeaders headers();
}
