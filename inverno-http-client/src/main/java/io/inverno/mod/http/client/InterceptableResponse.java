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

import io.inverno.mod.http.base.BaseResponse;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import java.util.function.Consumer;

/**
 * <p>
 * An interceptable response is exposed in the {@link InterceptableExchange} to allow an exchange interceptor to provide a response when canceling the request sent by returning an empty exhange Mono.
 * </p>
 * 
 * <p>
 * Once the actual response is received from the endpoint, it becomes a proxy for the received response and it is no longer possible to modify it resulting in {@link IllegalStateException} to be
 * raised. It might still be possible to intercept and transform the response payload publisher if it hasn't been subscribed yet.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see InterceptableExchange
 */
public interface InterceptableResponse extends BaseResponse {
	
	/**
	 * <p>
	 * Configures the HTTP headers of the response to return in case the request sent is cancelled.
	 * </p>
	 *
	 * @param headersConfigurer an outbound response headers configurer
	 *
	 * @return the request
	 *
	 * @throws IllegalStateException if the response has already been received from the endpoint
	 */
	InterceptableResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) throws IllegalStateException;
	
	/**
	 * <p>
	 * Configures the HTTP trailers of the response to return in case the request sent is cancelled.
	 * </p>
	 *
	 * @param trailersConfigurer an outbound headers configurer
	 *
	 * @return the request
	 *
	 * @throws IllegalStateException if the response has already been received from the endpoint
	 */
	InterceptableResponse trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) throws IllegalStateException;

	/**
	 * <p>
	 * Returns the response body.
	 * </p>
	 * 
	 * @return the response body
	 */
	InterceptableResponseBody body();
	
	/**
	 * <p>
	 * Determines whether the response body has been received from the endpoint.
	 * </p>
	 * 
	 * @return true if the response has been received, false otherwise
	 */
	boolean isReceived();
}
