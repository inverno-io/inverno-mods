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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;
import io.inverno.mod.http.client.InterceptableExchange;

/**
 * <p>
 * The {@link Endpoint.Request} implementation based on {@link HttpClientRequest}.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @version 1.6
 * 
 * @param <A> the context type
 */
public class EndpointRequest<A extends ExchangeContext> extends HttpClientRequest<A> implements Endpoint.Request<A, Exchange<A>, InterceptableExchange<A>>  {

	private final AbstractEndpoint endpoint;
	
	/**
	 * <p>
	 * Creates an endpoint request.
	 * </p>
	 * 
	 * @param endpoint           the endpoint
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param method             the HTTP method
	 * @param path               the path of the resource targeted by the request
	 * @param context            the context.
	 */
	public EndpointRequest(AbstractEndpoint endpoint, HeaderService headerService, ObjectConverter<String> parameterConverter, Method method, String path, A context) {
		super(headerService, parameterConverter, method, path, context);
		this.endpoint = endpoint;
	}

	@Override
	public EndpointRequest<A> intercept(ExchangeInterceptor<A, InterceptableExchange<A>> interceptor) {
		super.intercept(interceptor);
		return this;
	}
	
	@Override
	public EndpointRequest<A> authority(String authority) {
		super.authority(authority);
		return this;
	}

	@Override
	public EndpointRequest<A> headers(Consumer<OutboundRequestHeaders> headersConfigurer) {
		super.headers(headersConfigurer);
		return this;
	}

	@Override
	public EndpointRequest<A> body(Consumer<RequestBodyConfigurator> bodyConfigurer) {
		super.body(bodyConfigurer);
		return this;
	}

	@Override
	public Mono<Exchange<A>> send() {
		return this.endpoint.send(this);
	}
}
