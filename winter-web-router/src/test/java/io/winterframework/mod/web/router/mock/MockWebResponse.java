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

import java.util.function.Consumer;

import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.router.WebResponse;
import io.winterframework.mod.web.router.WebResponseBody;
import io.winterframework.mod.web.server.ResponseCookies;
import io.winterframework.mod.web.server.ResponseHeaders;
import io.winterframework.mod.web.server.ResponseTrailers;

/**
 * @author jkuhn
 *
 */
public class MockWebResponse implements WebResponse {

	private final WebResponseBody mockBody;
	
	private final MockResponseHeaders headers;
	private final MockResponseTrailers trailers;
	private final MockResponseCookies cookies;
	
	public MockWebResponse(HeaderService headerService, WebResponseBody mockBody) {
		this.mockBody = mockBody;
		
		this.headers = new MockResponseHeaders(headerService);
		this.trailers = new MockResponseTrailers(headerService);
		this.cookies = new MockResponseCookies();
	}
	
	@Override
	public boolean isHeadersWritten() {
		return false;
	}

	@Override
	public MockResponseHeaders headers() {
		return this.headers;
	}

	@Override
	public MockResponseTrailers trailers() {
		return this.trailers;
	}
	
	public MockResponseCookies cookies() {
		return this.cookies;
	}
	
	@Override
	public WebResponseBody body() {
		return this.mockBody;
	}

	@Override
	public MockWebResponse headers(Consumer<ResponseHeaders> headersConfigurer) throws IllegalStateException {
		headersConfigurer.accept(this.headers);
		return this;
	}

	@Override
	public MockWebResponse trailers(Consumer<ResponseTrailers> trailersConfigurer) {
		trailersConfigurer.accept(this.trailers);
		return this;
	}

	@Override
	public MockWebResponse cookies(Consumer<ResponseCookies> cookiesConfigurer) throws IllegalStateException {
		cookiesConfigurer.accept(this.cookies);
		return this;
	}
}
