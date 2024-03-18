/*
 * Copyright 2024 Jeremy Kuhn
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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.InterceptableExchange;

/**
 * <p>
 * An {@link InterceptableExchange} implementation that wraps the {@link EndpointExchange} so it can be intercepted in an {@link ExchangeInterceptor}.
 * </p>
 * 
 * <p>
 * An interceptable exchange is created in the {@link EndpointExchange} before sending the request to the endpoint if an {@link ExchangeInterceptor} has been specified on the endpoint.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointInterceptableExchange<A extends ExchangeContext> implements InterceptableExchange<A> {
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final EndpointExchange<A> exchange;
	
	private EndpointInterceptableRequest interceptableRequest;
	private EndpointInterceptableResponse interceptableResponse;

	/**
	 * <p>
	 * Creates an interceptable endpoint exchange.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param exchange           the endpoint exchange
	 */
	public EndpointInterceptableExchange(HeaderService headerService, ObjectConverter<String> parameterConverter, EndpointExchange<A> exchange) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.exchange = exchange;
	}

	@Override
	public HttpVersion getProtocol() {
		return this.exchange.getProtocol();
	}

	@Override
	public A context() {
		return this.exchange.context();
	}

	@Override
	public EndpointInterceptableRequest request() {
		if(this.interceptableRequest == null) {
			this.interceptableRequest = new EndpointInterceptableRequest(this.exchange.request());
		}
		return this.interceptableRequest;
	}

	@Override
	public EndpointInterceptableResponse response() {
		if(this.interceptableResponse == null) {
			this.interceptableResponse = new EndpointInterceptableResponse(this.headerService, this.parameterConverter);
		}
		return this.interceptableResponse;
	}

}
