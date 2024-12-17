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
package io.inverno.mod.web.client.internal;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.client.InterceptedRequest;
import io.inverno.mod.http.client.InterceptedRequestBody;
import io.inverno.mod.web.client.InterceptedWebRequest;
import java.net.SocketAddress;
import java.net.URI;
import java.security.cert.Certificate;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link InterceptedWebRequest} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericInterceptedWebRequest implements InterceptedWebRequest {

	private final URI uri;
	private final InterceptedRequest request;

	/**
	 * <p>
	 * Creates a generic intercepted Web request.
	 * </p>
	 *
	 * @param uri     the service URI
	 * @param request the originating intercepted request
	 */
	public GenericInterceptedWebRequest(URI uri, InterceptedRequest request) {
		this.uri = uri;
		this.request = request;
	}

	@Override
	public URI getUri() {
		return this.uri;
	}

	@Override
	public InterceptedWebRequest method(Method method) throws IllegalStateException {
		this.request.method(method);
		return this;
	}

	@Override
	public InterceptedWebRequest authority(String authority) throws IllegalStateException {
		this.request.authority(authority);
		return this;
	}

	@Override
	public InterceptedWebRequest path(String path) throws IllegalStateException {
		this.request.path(path);
		return this;
	}

	@Override
	public InterceptedWebRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException {
		this.request.headers(headersConfigurer);
		return this;
	}

	@Override
	public InterceptedRequestBody body() {
		return this.request.body();
	}

	@Override
	public boolean isSent() {
		return this.request.isSent();
	}

	@Override
	public String getScheme() {
		return this.request.getScheme();
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.request.getLocalAddress();
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return this.request.getLocalCertificates();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.request.getRemoteAddress();
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return this.request.getRemoteCertificates();
	}

	@Override
	public Method getMethod() {
		return this.request.getMethod();
	}

	@Override
	public String getAuthority() {
		return this.request.getAuthority();
	}

	@Override
	public String getPath() {
		return this.request.getPath();
	}

	@Override
	public String getPathAbsolute() {
		return this.request.getPathAbsolute();
	}

	@Override
	public URIBuilder getPathBuilder() {
		return this.request.getPathBuilder();
	}

	@Override
	public String getQuery() {
		return this.request.getQuery();
	}

	@Override
	public QueryParameters queryParameters() {
		return this.request.queryParameters();
	}

	@Override
	public InboundRequestHeaders headers() {
		return this.request.headers();
	}
}
