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
import io.inverno.mod.http.client.InterceptedExchange;
import java.util.Optional;

/**
 * <p>
 * An {@link InterceptedExchange} implementation that wraps the {@link EndpointExchange} so it can be intercepted in an {@link io.inverno.mod.http.client.ExchangeInterceptor}.
 * </p>
 * 
 * <p>
 * An intercepted exchange is created in the {@link EndpointExchange} before sending the request to the endpoint if an {@link io.inverno.mod.http.client.ExchangeInterceptor} has been specified on the endpoint.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointInterceptedExchange<A extends ExchangeContext> implements InterceptedExchange<A> {
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final EndpointExchange<A> exchange;
	
	private EndpointInterceptedRequest interceptedRequest;
	private EndpointInterceptedResponse interceptedResponse;

	/**
	 * <p>
	 * Creates an intercepted endpoint exchange.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param exchange           the endpoint exchange
	 */
	public EndpointInterceptedExchange(HeaderService headerService, ObjectConverter<String> parameterConverter, EndpointExchange<A> exchange) {
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
	public EndpointInterceptedRequest request() {
		if(this.interceptedRequest == null) {
			this.interceptedRequest = new EndpointInterceptedRequest(this.exchange.request());
		}
		return this.interceptedRequest;
	}

	@Override
	public EndpointInterceptedResponse response() {
		if(this.interceptedResponse == null) {
			this.interceptedResponse = new EndpointInterceptedResponse(this.headerService, this.parameterConverter);
		}
		return this.interceptedResponse;
	}

	@Override
	public void reset(long code) {
		this.exchange.reset(code);
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return this.exchange.getCancelCause();
	}
}
