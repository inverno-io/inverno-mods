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
package io.winterframework.mod.web.router.mock;

import java.net.SocketAddress;
import java.util.Optional;

import io.winterframework.mod.web.router.WebRequest;
import io.winterframework.mod.web.router.WebRequestBody;

/**
 * @author jkuhn
 *
 */
public class MockWebRequest implements WebRequest {

	private final MockRequestHeaders headers;
	private final MockQueryParameters queryParameters;
	private final MockRequestCookies cookies;
	private final SocketAddress remoteAddress;
	private final MockPathParameters pathParameters;
	private final WebRequestBody mockBody;
	
	public MockWebRequest(MockRequestHeaders headers, MockQueryParameters queryParameters, MockRequestCookies cookies, SocketAddress remoteAddress, WebRequestBody mockBody) {
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
