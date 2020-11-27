/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.web.internal.server;

import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.ResponseCookies;
import io.winterframework.mod.web.ResponseHeaders;
import reactor.core.publisher.Flux;

/**
 * @author jkuhn
 *
 */
public class GenericResponse implements Response<ResponseBody> {

	// I need access to the headers, cookies (which is actually set-cookie: header) and body publishers
	
	private GenericResponseHeaders responseHeaders; 

	private GenericResponseCookies responseCookies;
	
	private GenericResponseBody responseBody;
	
	public GenericResponse(HeaderService headerService) {
		this.responseHeaders = new GenericResponseHeaders(headerService);
		this.responseCookies = new GenericResponseCookies(headerService);
		this.responseBody = new GenericResponseBody(this);
	}
	
	public Flux<ByteBuf> data() {
		return this.responseBody.getData();
	}

	public GenericResponseHeaders getHeaders() {
		return this.responseHeaders;
	}
	
	public GenericResponseCookies getCookies() {
		return this.responseCookies;
	}
	
	@Override
	public boolean isHeadersWritten() {
		return this.responseHeaders.isWritten();
	}
	
	@Override
	public GenericResponse headers(Consumer<ResponseHeaders> headersConfigurer) {
		headersConfigurer.accept(this.responseHeaders);
		return this;
	}

	@Override
	public GenericResponse cookies(Consumer<ResponseCookies> cookiesConfigurer) {
		cookiesConfigurer.accept(this.responseCookies);
		return this;
	}

	@Override
	public GenericResponseBody body() {
		return this.responseBody;
	}
}
