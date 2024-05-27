/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.server.internal.mock;

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRequest;
import io.inverno.mod.web.server.WebRequestBody;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class MockWebRequest implements WebRequest {

	private final String authority;
	private final String scheme;
	private final String path;
	private final URIBuilder pathBuilder;
	private final Method method;
	private final MockRequestHeaders headers;
	private final MockQueryParameters queryParameters;
	private final SocketAddress localAddress;
	private Certificate[] localCertificates;
	private final SocketAddress remoteAddress;
	private Certificate[] remoteCertificates;
	private final MockPathParameters pathParameters;
	private final WebRequestBody mockBody;
	
	public MockWebRequest(String authority, String scheme, String path, Method method, MockRequestHeaders headers, MockQueryParameters queryParameters, SocketAddress localAddress, Certificate[] localCertificates, SocketAddress remoteAddress, Certificate[] remoteCertificates, WebRequestBody mockBody) {
		this.authority = authority;
		this.scheme = scheme;
		this.path = path;
		this.pathBuilder = URIs.uri(path, false, URIs.Option.NORMALIZED);
		this.method = method;
		this.headers = headers;
		this.queryParameters = queryParameters;
		this.localAddress = localAddress;
		this.localCertificates = localCertificates;
		this.remoteAddress = remoteAddress;
		this.remoteCertificates = remoteCertificates;
		this.pathParameters = new MockPathParameters();
		this.mockBody = mockBody;
	}

	@Override
	public InboundRequestHeaders headers() {
		return this.headers;
	}
	
	@Override
	public MockQueryParameters queryParameters() {
		return this.queryParameters;
	}

	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public String getScheme() {
		return this.scheme;
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
		return this.pathBuilder.buildPath();
	}
	
	@Override
	public URIBuilder getPathBuilder() {
		return this.pathBuilder.clone();
	}

	@Override
	public String getQuery() {
		return this.pathBuilder.buildQuery();
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return Optional.ofNullable(this.localCertificates);
	}
	
	@Override
	public MockPathParameters pathParameters() {
		return this.pathParameters;
	}

	@Override
	public Optional<WebRequestBody> body() {
		return Optional.ofNullable(this.mockBody);
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return Optional.ofNullable(this.remoteCertificates);
	}
}
