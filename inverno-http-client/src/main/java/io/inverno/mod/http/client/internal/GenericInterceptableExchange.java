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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.InterceptableExchange;

/**
 * <p>
 * Generic {@link InterceptableExchange} implementation.
 * </p>
 * 
 * <p>
 * This implementation also implements {@link Exchange} which allows it to act as a proxy for the actual exchange created once the actual request has been sent to the endpoint. This allows to expose
 * the actual exchange to interceptors which is required to be able to intercept the response payload for instance. The {@link #setExchange(io.inverno.mod.http.client.Exchange)} shall be invoked to
 * make this instance delegates to the actual exchange. At this point both exchange request and response should become immutable.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the type of the exchange context
 * @param <B> the base client request type (HTTP or WebSocket)
 */
public class GenericInterceptableExchange<A extends ExchangeContext, B extends BaseClientRequest> implements InterceptableExchange<A>, Exchange<A> {

	private final A exchangeContext;
	
	private final B request;
	
	private final GenericInterceptableResponse response;
	
	private Exchange<A> sentExchange;
	
	/**
	 * <p>
	 * Creates a generic interceptable exchange.
	 * </p>
	 * 
	 * @param context  the context
	 * @param request  the interceptable request
	 * @param response the interceptable response
	 */
	public GenericInterceptableExchange(A context, B request, GenericInterceptableResponse response) {
		this.exchangeContext = context;
		this.request = request;
		this.response = response;
	}
	
	/**
	 * <p>
	 * Injects the actual exchange created when the request is actually sent to the endpoint.
	 * </p>
	 * 
	 * @param exchange the exchange
	 */
	public void setExchange(Exchange<A> exchange) {
		this.sentExchange = exchange;
		this.request.setSentRequest((AbstractRequest)exchange.request());
		this.response.setReceivedResponse(exchange.response());
	}
	
	@Override
	public A context() {
		return this.exchangeContext;
	}

	@Override
	public HttpVersion getProtocol() {
		return this.sentExchange != null ? this.sentExchange.getProtocol() : HttpVersion.HTTP;
	}
	
	@Override
	public B request() {
		return this.request;
	}

	@Override
	public GenericInterceptableResponse response() {
		return this.response;
	}
}
