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

import io.inverno.mod.http.base.BaseExchange;
import io.inverno.mod.http.base.ExchangeContext;

/**
 * <p>
 * An iterceptable exchange is created by the endpoint before sending the request in order to implement the interception logic.
 * </p>
 *
 * <p>
 * It exposes a {@link InterceptableRequest} which acts as a proxy for the original request allowing an interceptor to modify the original request.
 * </p>
 *
 * <p>
 * When the request is actually sent to the endpoint, the interceptable request becomes a proxy for the sent request and an {@link IllegalStateException} shall be thrown when trying to set the path,
 * the authority, headers or transform the body.
 * </p>
 *
 * <p>
 * It exposes a {@link InterceptableResponse} which allows an interceptor to provide a complete response when returning an empty exchange Mono to prevent the request from being sent. A response body
 * transformer can be specified on the {@link InterceptableResponseBody} in order to decorate the actual response body that will be received from the endpoint.
 * </p>
 *
 * <p>
 * When a response is actually received from the endpoint the interceptable response becomes a proxy for the received response and an {@link IllegalStateException} shall be thrown when trying to set
 * headers or the body of the response. It might still be possible to invoke {@link InterceptableResponseBody#transform(java.util.function.Function) } in order to transform the body assuming it hasn't
 * been subscribed yet otherwise an {@link IllegalStateException} is thrown.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see InterceptableRequest
 * @see InterceptableResponse
 * 
 * @param <A> the type of the exchange context
 */
public interface InterceptableExchange<A extends ExchangeContext> extends BaseExchange<A> {

	/**
	 * <p>
	 * Returns an interceptable request which proxies the original request and allows to override values before it is sent to the endpoint.
	 * </p>
	 * 
	 * <p>
	 * Once the request is sent to the endpoint, the interceptable request becomes a proxy for the sent request and it can no longer be used to modify request data.
	 * </p>
	 */
	@Override
	InterceptableRequest request();
	
	/**
	 * <p>
	 * Returns an interceptable response which allows to provide a complete response when bypassing the sending of the request or transform the actual response body.
	 * </p>
	 * 
	 * <p>
	 * Once a response is received from the endpoint the interceptable response becomes a proxy for the received response and it can no longer be used to modify response data.
	 * </p>
	 */
	@Override
	InterceptableResponse response();
}
