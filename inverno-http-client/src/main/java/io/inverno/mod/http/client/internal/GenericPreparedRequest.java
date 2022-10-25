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
import io.inverno.mod.http.client.PreparedRequest;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericPreparedRequest<A extends ExchangeContext> implements PreparedRequest<A, Exchange<A>> {

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
	
	private GenericRequestCookies requestCookies;
	
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
	public GenericRequestHeaders headers() {
		return this.requestHeaders;
	}

	@Override
	public GenericPreparedRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException {
		headersConfigurer.accept(this.requestHeaders);
		return this;
	}

	/*@Override
	public GenericPreparedRequest cookies(Consumer<RequestCookies> cookiesConfigurer) throws IllegalStateException {
		if(this.requestCookies == null) {
			this.requestCookies = new GenericRequestCookies(this.headerService, this.requestHeaders, parameterConverter);
		}
		cookiesConfigurer.accept(this.requestCookies);
		this.requestCookies.commit();
		return this;
	}*/
	
	@Override
	public GenericPreparedRequest authority(String authority) {
		this.authority = authority;
		return this;
	}
	
	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public String getAuthority() {
		return this.authority;
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
	public PreparedRequest<A, Exchange<A>> intercept(ExchangeInterceptor<? super A, ? extends Exchange<A>> interceptor) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Mono<Exchange<A>> send(List<Object> parameters, A context) {
		// Get a connection from pool now: this is given by the endpoint
		// the endpoint uses its configuration to establish the connection which can be either HTTP/1.x or HTTP/2
		// in case of HTTP/2 a stream is actually allocated, it must attached to the request
		// Once we have the connection we can create a typed request corresponding to the protocol version
		// So ideally the connection must be the one creating the exchange => connection.send() must return Mono<Exchange> instead of Mono<Response>
		// check the context type matches the endpoint context type
		
		// connection should create the exchange which is intercepted by the endpoint and after that actually send the exchange
		
		
		return this.endpoint.connection().flatMap(connection -> connection.send(this.method, this.authority, this.requestHeaders.getAll(), this.getPathBuilder().buildString(parameters), null, context));
	}

	@Override
	public Mono<Exchange<A>> send(Map<String, ?> parameters, A context) {
		// Timeout after connection() <= this must be done in the endpoint, the pooled endpoint has an event loop
		// Let's differentiate connection timeout from request timeout shall we?
		// timeout after send() <= done within the connection using the connection context
		
		// Global request timeout that start on subscribe 
		return this.endpoint.connection().flatMap(connection -> connection.send(this.method, this.authority, this.requestHeaders.getAll(), this.getPathBuilder().buildString(parameters), null, context));
	}

	@Override
	public Mono<Exchange<A>> send(List<Object> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer, A context) {
		return this.endpoint.connection().flatMap(connection -> connection.send(this.method, this.authority, this.requestHeaders.getAll(), this.getPathBuilder().buildString(parameters), bodyConfigurer, context));
	}

	@Override
	public Mono<Exchange<A>> send(Map<String, ?> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer, A context) {
		return this.endpoint.connection().flatMap(connection -> connection.send(this.method, this.authority, this.requestHeaders.getAll(), this.getPathBuilder().buildString(parameters), bodyConfigurer, context));
	}
}
