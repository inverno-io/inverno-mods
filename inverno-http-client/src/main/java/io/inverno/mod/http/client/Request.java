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
package io.inverno.mod.http.client;

import io.inverno.mod.http.base.BaseRequest;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import java.util.function.Consumer;

/**
 * <p>
 * Represents a client request in a client exchange.
 * </p>
 * 
 * <p>
 * Once the request has been sent to the endpoint it is no longer possible to modify it resulting in {@link IllegalStateException} on such operations.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see Exchange
 */
public interface Request extends BaseRequest {
	
	/**
	 * <p>
	 * Determines whether the request headers have been sent to the endpoint.
	 * </p>
	 *
	 * @return true if headers have been sent, false otherwise
	 */
	boolean isHeadersWritten();
	
	/**
	 * <p>
	 * Sets the request method.
	 * </p>
	 * 
	 * <p>
	 * This actually overrides the method provided when the enclosing exchange was created by the {@link Endpoint}. It defaults to {@link Method#GET} if none is specified.
	 * </p>
	 * 
	 * @param method the request method
	 * 
	 * @return the request
	 * 
	 * @throws IllegalStateException if the request has already been sent to the endpoint
	 */
	Request method(Method method) throws IllegalStateException;
	
	/**
	 * <p>
	 * Sets the request authority.
	 * </p>
	 * 
	 * @param authority the request authority
	 * 
	 * @return the request
	 * 
	 * @throws IllegalStateException if the request has already been sent to the endpoint
	 */
	Request authority(String authority) throws IllegalStateException;
	
	/**
	 * <p>
	 * Sets the request path.
	 * </p>
	 * 
	 * <p>
	 * This actually overrides the request target path provided when the request was created using {@link Endpoint#exchange(io.inverno.mod.http.base.Method, java.lang.String) } or 
	 * {@link Endpoint#exchange(io.inverno.mod.http.base.Method, java.lang.String, io.inverno.mod.http.base.ExchangeContext)}.
	 * </p>
	 * 
	 * @param path the request target path
	 * 
	 * @return the request
	 * 
	 * @throws IllegalStateException if the request has already been sent to the endpoint
	 */
	Request path(String path) throws IllegalStateException;
	
	/**
	 * <p>
	 * Configures the HTTP headers to send in the request.
	 * </p>
	 *
	 * @param headersConfigurer an outbound request headers configurer
	 *
	 * @return the request
	 *
	 * @throws IllegalStateException if the request has already been sent to the endpoint
	 */
	Request headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException;

	/**
	 * <p>
	 * Returns the request body when the request method allows it.
	 * </p>
	 * 
	 * @return the request body when the method allows it (i.e. {@code POST} {@code PUT}...)
	 * 
	 * @throws IllegalStateException if the method does not allow a body in the request (i.e. {@code GET}...) or if the request has already been sent to the endpoint
	 */
	RequestBody body() throws IllegalStateException;
}
