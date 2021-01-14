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
package io.winterframework.mod.web.router.internal;

import java.util.function.Consumer;

import io.winterframework.mod.web.router.WebResponse;
import io.winterframework.mod.web.router.WebResponseBody;
import io.winterframework.mod.web.server.Response;
import io.winterframework.mod.web.server.ResponseCookies;
import io.winterframework.mod.web.server.ResponseHeaders;
import io.winterframework.mod.web.server.ResponseTrailers;

/**
 * @author jkuhn
 *
 */
public class GenericWebResponse implements WebResponse {

	private Response response;
	
	private WebResponseBody responseBody;
	
	public GenericWebResponse(Response response, BodyConversionService bodyConversionService) {
		this.response = response;
		this.responseBody = new GenericWebResponseBody(this, response.body(), bodyConversionService);
	}

	@Override
	public boolean isHeadersWritten() {
		return this.response.isHeadersWritten();
	}

	@Override
	public ResponseHeaders headers() {
		return this.response.headers();
	}
	
	@Override
	public Response headers(Consumer<ResponseHeaders> headersConfigurer) throws IllegalStateException {
		return this.response.headers(headersConfigurer);
	}
	
	@Override
	public ResponseTrailers trailers() {
		return this.response.trailers();
	}

	@Override
	public Response trailers(Consumer<ResponseTrailers> trailersConfigurer) {
		return this.response.trailers(trailersConfigurer);
	}

	@Override
	public Response cookies(Consumer<ResponseCookies> cookiesConfigurer) throws IllegalStateException {
		return this.response.cookies(cookiesConfigurer);
	}

	@Override
	public WebResponseBody body() {
		return this.responseBody;
	}
}
