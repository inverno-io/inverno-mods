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
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.PreExchange;
import io.inverno.mod.http.client.PreparedRequest;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericPreparedRequest<A extends ExchangeContext> implements PreparedRequest<A, Exchange<A>, PreExchange<A>> {

	private final AbstractEndpoint<A> endpoint;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final Method method;
	private final String path;
	private final URIBuilder primaryPathBuilder;
	private final GenericRequestHeaders requestHeaders;
	
	private String authority;
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	private ExchangeInterceptor<A, PreExchange<A>> interceptor;
	
	public GenericPreparedRequest(
			AbstractEndpoint endpoint, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			Method method, 
			String path) {
		this.endpoint = endpoint;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.method = method;
		this.path = path;
		// TODO make sure this is a path with no scheme or authority
		this.primaryPathBuilder = URIs.uri(path, false, URIs.Option.NORMALIZED);
		this.requestHeaders = new GenericRequestHeaders(headerService, parameterConverter);
	}
	
	@Override
	public PreparedRequest<A, Exchange<A>, PreExchange<A>> intercept(ExchangeInterceptor<A, PreExchange<A>> interceptor) {
		if(this.interceptor == null) {
			this.interceptor = interceptor;
		}
		else {
			this.interceptor = this.interceptor.andThen(interceptor);
		}
		return this;
	}
	
	@Override
	public Method getMethod() {
		return this.method;
	}
	
	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public String getPathAbsolute() {
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.primaryPathBuilder.buildRawString();
		}
		return this.pathAbsolute;
	}

	@Override
	public URIBuilder getPathBuilder() {
		return this.primaryPathBuilder.clone();
	}

	@Override
	public String getQuery() {
		if(this.queryString == null) {
			this.queryString = this.primaryPathBuilder.buildRawQuery();
		}
		return this.queryString;
	}

	@Override
	public QueryParameters queryParameters() {
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.primaryPathBuilder.getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}
	
	@Override
	public String getAuthority() {
		return this.authority;
	}

	@Override
	public GenericPreparedRequest authority(String authority) {
		this.authority = authority;
		return this;
	}

	@Override
	public GenericRequestHeaders headers() {
		return this.requestHeaders;
	}

	@Override
	public GenericPreparedRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException {
		headersConfigurer.accept(this.requestHeaders);
		return this;
	}
	
	@Override
	public Mono<Exchange<A>> send(A context, List<Object> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer) {
		return this.send(context, this.getPathBuilder().buildString(parameters), bodyConfigurer);
	}

	@Override
	public Mono<Exchange<A>> send(A context, Map<String, ?> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer) {
		return this.send(context, this.getPathBuilder().buildString(parameters), bodyConfigurer);
	}
	
	private Mono<Exchange<A>> send(A context, String path, Consumer<RequestBodyConfigurator> bodyConfigurer) {
		if(this.interceptor != null) {
			// Create PreExchange, intercept then proceed
			return Mono.defer(() -> {
				GenericPreRequest request = new GenericPreRequest(this.headerService, this.parameterConverter, this.method, path, authority, this.requestHeaders.getAll(), bodyConfigurer);
				GenericPreResponse response = new GenericPreResponse(this.headerService, this.parameterConverter);
				GenericPreExchange<A> preExchange = new GenericPreExchange<>(context, request, response);

				return this.interceptor.intercept(preExchange)
					.flatMap(interceptedExchange -> this.endpoint.connection()
						.flatMap(connection -> connection.send(
							interceptedExchange.context(), 
							interceptedExchange.request().getMethod(), 
							interceptedExchange.request().getAuthority(), 
							interceptedExchange.request().headers().getAll(), 
							path, 
							bodyConfigurer,
							preExchange.request().body().map(preBody -> ((GenericPreRequestBody)preBody).getTransformer()).orElse(null),
							preExchange.response().body().getTransformer()
						))
					)
					.switchIfEmpty(Mono.just(preExchange));
			});
			
		}
		else {
			return this.endpoint.connection().flatMap(connection -> connection.send(context, this.method, this.authority, this.requestHeaders.getAll(), path, bodyConfigurer));
		}
	}
}
