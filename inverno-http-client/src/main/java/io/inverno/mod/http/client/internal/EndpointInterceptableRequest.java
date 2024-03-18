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

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.InterceptableRequest;
import io.inverno.mod.http.client.InterceptableRequestBody;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * <p>
 * An {@link InterceptableRequest} implementation that wraps the {@link EndpointRequest} so it can be intercepted in an {@link ExchangeInterceptor}.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointInterceptableRequest implements InterceptableRequest {
	
	private final EndpointRequest endpointRequest;
	
	private Optional<InterceptableRequestBody> requestBody;
	
	/**
	 * <p>
	 * Creates an interceptable endpoint request.
	 * </p>
	 * 
	 * @param endpointRequest the endpoint request
	 */
	public EndpointInterceptableRequest(EndpointRequest endpointRequest) {
		this.endpointRequest = endpointRequest;
	}

	@Override
	public EndpointInterceptableRequest method(Method method) throws IllegalStateException {
		this.endpointRequest.method(method);
		return this;
	}
	
	@Override
	public EndpointInterceptableRequest authority(String authority) throws IllegalStateException {
		this.endpointRequest.authority(authority);
		return this;
	}

	@Override
	public EndpointInterceptableRequest path(String path) throws IllegalStateException {
		this.endpointRequest.path(path);
		return this;
	}
	
	@Override
	public EndpointInterceptableRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException {
		this.endpointRequest.headers(headersConfigurer);
		return this;
	}

	@Override
	public Optional<InterceptableRequestBody> body() {
		if(this.requestBody == null) {
			switch(this.getMethod()) {
				case POST:
				case PUT:
				case PATCH:
				case DELETE: {
					this.requestBody = Optional.of(new EndpointInterceptableRequestBody(this.endpointRequest.getBody()));
					break;
				}
				default: {
					this.requestBody = Optional.empty();
				}
			}
		}
		return this.requestBody;
	}

	@Override
	public boolean isSent() {
		return this.endpointRequest.isSent();
	}

	@Override
	public String getScheme() {
		return this.endpointRequest.getScheme();
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.endpointRequest.getLocalAddress();
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return this.endpointRequest.getLocalCertificates();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.endpointRequest.getRemoteAddress();
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return this.endpointRequest.getRemoteCertificates();
	}

	@Override
	public Method getMethod() {
		return this.endpointRequest.getMethod();
	}

	@Override
	public String getAuthority() {
		return this.endpointRequest.getAuthority();
	}

	@Override
	public String getPath() {
		return this.endpointRequest.getPath();
	}

	@Override
	public String getPathAbsolute() {
		return this.endpointRequest.getPathAbsolute();
	}

	@Override
	public URIBuilder getPathBuilder() {
		return this.endpointRequest.getPathBuilder();
	}

	@Override
	public String getQuery() {
		return this.endpointRequest.getQuery();
	}

	@Override
	public QueryParameters queryParameters() {
		return this.endpointRequest.queryParameters();
	}

	@Override
	public InboundRequestHeaders headers() {
		return this.endpointRequest.headers();
	}
}
