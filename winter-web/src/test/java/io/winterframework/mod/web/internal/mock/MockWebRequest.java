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
package io.winterframework.mod.web.internal.mock;

import java.net.SocketAddress;
import java.util.Optional;

import io.winterframework.mod.base.net.URIBuilder;
import io.winterframework.mod.base.net.URIs;
import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.web.WebRequest;
import io.winterframework.mod.web.WebRequestBody;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
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
	private final MockRequestCookies cookies;
	private final SocketAddress remoteAddress;
	private final MockPathParameters pathParameters;
	private final WebRequestBody mockBody;
	
	public MockWebRequest(String authority, String scheme, String path, Method method, MockRequestHeaders headers, MockQueryParameters queryParameters, MockRequestCookies cookies, SocketAddress remoteAddress, WebRequestBody mockBody) {
		this.authority = authority;
		this.scheme = scheme;
		this.path = path;
		this.pathBuilder = URIs.uri(path, false, URIs.Option.NORMALIZED);
		this.method = method;
		this.headers = headers;
		this.queryParameters = queryParameters;
		this.cookies = cookies;
		this.remoteAddress = remoteAddress;
		this.pathParameters = new MockPathParameters();
		this.mockBody = mockBody;
	}
	
	@Override
	public MockRequestHeaders headers() {
		return this.headers;
	}

	@Override
	public MockQueryParameters queryParameters() {
		return this.queryParameters;
	}

	@Override
	public MockRequestCookies cookies() {
		return this.cookies;
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
	public MockPathParameters pathParameters() {
		return this.pathParameters;
	}

	@Override
	public Optional<WebRequestBody> body() {
		return Optional.ofNullable(this.mockBody);
	}

}
