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
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import io.netty.buffer.ByteBuf;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import io.inverno.mod.http.client.InterceptableExchange;
import io.inverno.mod.http.client.InterceptableRequest;
import io.inverno.mod.http.client.InterceptableRequestBody;
import java.security.cert.Certificate;

/**
 * <p>
 * Main HTTP client request implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the exchange context type
 * 
 * @see EndpointRequest
 */
public class HttpClientRequest<A extends ExchangeContext> implements HttpClient.Request<A, Exchange<A>, InterceptableExchange<A>>, BaseClientRequest {

	protected final HeaderService headerService;
	protected final ObjectConverter<String> parameterConverter;
	protected final Method method;
	protected final GenericRequestHeaders requestHeaders;
	protected final A context;
	
	protected ExchangeInterceptor<A, InterceptableExchange<A>> interceptor;
	
	protected String authority;
	protected String path;
	protected URIBuilder primaryPathBuilder;
	protected String pathAbsolute;
	protected String queryString;
	protected GenericQueryParameters queryParameters;
	protected Consumer<RequestBodyConfigurator> requestBodyConfigurer;
	protected GenericInterceptableRequestBody requestBody;
	
	protected AbstractRequest sentRequest;
	
	/**
	 * <p>
	 * Creates an HTTP client request.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param method             the HTTP method
	 * @param path               the request target path
	 * @param context            the context
	 */
	public HttpClientRequest(HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			Method method, 
			String path,
			A context) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.method = method;
		this.requestHeaders = new GenericRequestHeaders(headerService, parameterConverter);
		this.context = context;
		this.path(path);
	}
	
	/**
	 * <p>
	 * Determines whether the request was sent to the endpoint.
	 * </p>
	 * 
	 * @throws IllegalArgumentException if the request was already sent
	 */
	private void checkNotSent() throws IllegalArgumentException {
		if(this.sentRequest != null) {
			throw new IllegalStateException("Request already sent");
		}
	}
	
	/**
	 * <p>
	 * Returns the context.
	 * </p>
	 * 
	 * @return the context
	 */
	public A getContext() {
		return this.context;
	}
	
	/**
	 * <p>
	 * Returns HTTP headers entries.
	 * </p>
	 * 
	 * @return a list of header entries
	 */
	public List<Map.Entry<String, String>> getHeaders() {
		return this.requestHeaders.getAll();
	}
	
	/**
	 * <p>
	 * Returns the request payload data publisher transformer.
	 * </p>
	 * 
	 * @return a request payload data transformer
	 */
	public Function<Publisher<ByteBuf>, Publisher<ByteBuf>> getBodyTransformer() {
		return this.requestBody != null ? this.requestBody.getTransformer() : null;
	}
	
	@Override
	public HttpClient.Request<A, Exchange<A>, InterceptableExchange<A>> intercept(ExchangeInterceptor<A, InterceptableExchange<A>> interceptor) throws IllegalStateException {
		this.checkNotSent();
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
		return this.sentRequest != null ? this.sentRequest.getMethod(): this.method;
	}

	@Override
	public String getAuthority() {
		return this.sentRequest != null ? this.sentRequest.getAuthority() : this.authority;
	}

	@Override
	public HttpClientRequest<A> authority(String authority) throws IllegalStateException {
		this.checkNotSent();
		this.authority = authority;
		return this;
	}
	
	@Override
	public String getPath() {
		return this.sentRequest != null ? this.sentRequest.getPath() : this.path;
	}

	@Override
	public final InterceptableRequest path(String path) throws IllegalStateException {
		this.checkNotSent();
		this.path = path;
		// TODO make sure this is a path with no scheme or authority
		this.primaryPathBuilder = URIs.uri(path, false, URIs.Option.NORMALIZED);
		this.pathAbsolute = null;
		return this;
	}
	
	@Override
	public String getPathAbsolute() {
		if(this.sentRequest != null) {
			return this.sentRequest.getPathAbsolute();
		}
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.primaryPathBuilder.buildRawString();
		}
		return this.pathAbsolute;
	}

	@Override
	public URIBuilder getPathBuilder() {
		if(this.sentRequest != null) {
			return this.sentRequest.getPathBuilder();
		}
		return this.primaryPathBuilder.clone();
	}

	@Override
	public String getQuery() {
		if(this.sentRequest != null) {
			return this.sentRequest.getQuery();
		}
		if(this.queryString == null) {
			this.queryString = this.primaryPathBuilder.buildRawQuery();
		}
		return this.queryString;
	}

	@Override
	public QueryParameters queryParameters() {
		if(this.sentRequest != null) {
			return this.sentRequest.queryParameters();
		}
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.primaryPathBuilder.getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}

	@Override
	public InternalRequestHeaders headers() {
		if(this.sentRequest != null) {
			return sentRequest.headers();
		}
		return this.requestHeaders;
	}

	@Override
	public HttpClientRequest<A> headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException {
		this.checkNotSent();
		headersConfigurer.accept(this.requestHeaders);
		return this;
	}

	@Override
	public HttpClient.Request<A, Exchange<A>, InterceptableExchange<A>> body(Consumer<RequestBodyConfigurator> bodyConfigurer) throws IllegalStateException {
		this.checkNotSent();
		if(this.requestBodyConfigurer == null) {
			this.requestBodyConfigurer = bodyConfigurer;
			this.requestBody = new GenericInterceptableRequestBody();
		}
		else {
			this.requestBodyConfigurer = this.requestBodyConfigurer.andThen(bodyConfigurer);
		}
		return this;
	}

	@Override
	public Optional<InterceptableRequestBody> body() {
		return Optional.ofNullable(this.requestBody);
	}
	
	@Override
	public boolean isHeadersWritten() {
		return this.sentRequest != null ? this.sentRequest.isHeadersWritten() : false;
	}

	@Override
	public String getScheme() {
		return this.sentRequest != null ? this.sentRequest.getScheme() : "http";
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.sentRequest != null ? this.sentRequest.getLocalAddress() : null;
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return this.sentRequest != null ? this.sentRequest.getLocalCertificates() : null;
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		return this.sentRequest != null ? this.sentRequest.getRemoteAddress() : null;
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return this.sentRequest != null ? this.sentRequest.getRemoteCertificates() : null;
	}
	
	@Override
	public void setSentRequest(AbstractRequest sentRequest) {
		this.sentRequest = sentRequest;
		if(this.requestBody != null) {
			this.sentRequest.body().ifPresent(this.requestBody::setSentRequestBody);
		}
	}

	@Override
	public boolean isSent() {
		return this.sentRequest != null;
	}
}
