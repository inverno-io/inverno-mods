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
import io.inverno.mod.http.base.OutboundRequestHeaders;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * <p>
 * An interceptable request is exposed in the {@link InterceptableExchange} to proxy the original request that is about to be sent to the endpoint and the actual request after it has been sent to the
 * endpoint.
 * </p>
 * 
 * <p>
 * Once the actual request has been sent to the endpoint it is no longer possible to modify the interceptable request resulting in {@link IllegalStateException} to be raised. 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see InterceptableExchange
 */
public interface InterceptableRequest extends BaseRequest {

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
	InterceptableRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException;
	
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
	InterceptableRequest authority(String authority) throws IllegalStateException;
	
	/**
	 * <p>
	 * Sets the request path.
	 * </p>
	 * 
	 * <p>
	 * This actually overrides the request target path provided when the request was created using {@link Endpoint#request(io.inverno.mod.http.base.Method, java.lang.String) } or 
	 * {@link HttpClient#request(io.inverno.mod.http.base.Method, java.lang.String) }.
	 * </p>
	 * 
	 * @param path the request target path
	 * 
	 * @return the request
	 * 
	 * @throws IllegalStateException if the request has already been sent to the endpoint
	 */
	InterceptableRequest path(String path) throws IllegalStateException;

	/**
	 * <p>
	 * Returns the request body when the request method allows it.
	 * </p>
	 * 
	 * @return an optional returning the request body when the method allows it (i.e. {@code POST} {@code PUT}...) or an empty optional (i.e. {@code GET}...).
	 */
	Optional<InterceptableRequestBody> body();
	
	/**
	 * <p>
	 * Determines whether the request was sent to the endpoint.
	 * </p>
	 * 
	 * <p>
	 * Once the request is sent, it is no longer possible to modify it resultig in {@link IllegalArgumentException}.
	 * <p>
	 * 
	 * @return true if the request has been sent, false otherwise
	 */
	boolean isSent();
}
