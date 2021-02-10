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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.internal.header.GenericHeaderService;
import io.winterframework.mod.web.router.WebRequestBody;
import io.winterframework.mod.web.router.WebResponseBody;

/**
 * @author jkuhn
 *
 */
public class MockExchangeBuilder {
	
	private static final HeaderService HEADER_SERVICE = new GenericHeaderService(List.of(new AcceptCodec(), new AcceptLanguageCodec(), new ContentTypeCodec()));
	
	private String authority = "localhost";
	private String scheme = "http";
	private String path = "/";
	private Method method = Method.GET;
	private Map<String, List<String>> headers = Map.of();

	private Map<String, List<String>> requestQueryParameters = Map.of();
	private Map<String, List<String>> requestCookies = Map.of();
	private SocketAddress remoteAddress = new InetSocketAddress("localhost", 8080);
	
	private WebRequestBody mockRequestBody;
	
	private WebResponseBody mockResponseBody;
	
	public MockExchangeBuilder authority(String authority) {
		this.authority = authority;
		return this;
	}
	
	public MockExchangeBuilder scheme(String scheme) {
		this.scheme = scheme;
		return this;
	}
	
	public MockExchangeBuilder path(String path) {
		this.path = path;
		return this;
	}
	
	public MockExchangeBuilder method(Method method) {
		this.method = method;
		return this;
	}
	
	public MockExchangeBuilder headers(Map<String, List<String>> headers) {
		this.headers = headers;
		return this;
	}
	
	public MockExchangeBuilder queryParameters(Map<String, List<String>> queryParameters) {
		this.requestQueryParameters = queryParameters;
		return this;
	}
	
	public MockExchangeBuilder cookies(Map<String, List<String>> cookies) {
		this.requestCookies = cookies;
		return this;
	}
	
	public MockExchangeBuilder remoteAddress(SocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		return this;
	}
	
	public MockExchangeBuilder requestBody(WebRequestBody mockRequestBody) {
		this.mockRequestBody = mockRequestBody;
		return this;
	}
	
	public MockExchangeBuilder responseBody(WebResponseBody mockResponseBody) {
		this.mockResponseBody = mockResponseBody;
		return this;
	}
	
	public MockWebExchange build() {
		MockWebRequest mockRequest = new MockWebRequest(new MockRequestHeaders(HEADER_SERVICE, this.authority, this.scheme, this.path, this.method, this.headers), new MockQueryParameters(this.requestQueryParameters), new MockRequestCookies(this.requestCookies), this.remoteAddress, this.mockRequestBody);
		MockWebResponse mockResponse = new MockWebResponse(HEADER_SERVICE, this.mockResponseBody);
		
		return new MockWebExchange(mockRequest, mockResponse);
	}

}
