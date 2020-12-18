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

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.ResponseCookies;
import io.winterframework.mod.web.ResponseHeaders;
import io.winterframework.mod.web.ResponseTrailers;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractResponse implements Response<ResponseBody> {

	protected ChannelHandlerContext context;
	
	protected HeaderService headerService;
	
	protected AbstractResponseHeaders responseHeaders; 
	protected ResponseTrailers responseTrailers;

	protected GenericResponseCookies responseCookies;
	
	protected GenericResponseBody responseBody;
	
	public AbstractResponse(ChannelHandlerContext context, HeaderService headerService, AbstractResponseHeaders responseHeaders) {
		this.context = context;
		this.headerService = headerService;
		this.responseHeaders = responseHeaders;
		this.responseBody = new GenericResponseBody(this);
	}
	
	public Publisher<ByteBuf> data() {
		return this.responseBody.getData();
	}

	public AbstractResponseHeaders getHeaders() {
		return this.responseHeaders;
	}

	public ResponseTrailers getTrailers() {
		return this.responseTrailers;
	}
	
	public GenericResponseCookies getCookies() {
		return this.responseCookies;
	}
	
	@Override
	public boolean isHeadersWritten() {
		return this.responseHeaders.isWritten();
	}
	
	@Override
	public AbstractResponse headers(Consumer<ResponseHeaders> headersConfigurer) {
		if(this.isHeadersWritten()) {
			throw new IllegalStateException("Headers have been already written");
		}
		headersConfigurer.accept(this.responseHeaders);
		return this;
	}

	@Override
	public AbstractResponse cookies(Consumer<ResponseCookies> cookiesConfigurer) {
		if(this.responseCookies == null) {
			this.responseCookies = new GenericResponseCookies(this.headerService, this.responseHeaders);
		}
		cookiesConfigurer.accept(this.responseCookies);
		return this;
	}

	@Override
	public GenericResponseBody body() {
		return this.responseBody;
	}
}
