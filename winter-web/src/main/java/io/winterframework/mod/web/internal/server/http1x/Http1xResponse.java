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
package io.winterframework.mod.web.internal.server.http1x;

import java.util.function.Consumer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.ssl.SslHandler;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.internal.server.AbstractResponse;
import io.winterframework.mod.web.server.Response;
import io.winterframework.mod.web.server.ResponseTrailers;

/**
 * @author jkuhn
 *
 */
class Http1xResponse extends AbstractResponse {

	private final HeaderService headerService;
	
	/**
	 * @param headerService
	 */
	public Http1xResponse(ChannelHandlerContext context, HeaderService headerService) {
		super(context, headerService, new Http1xResponseHeaders(headerService));
		this.headerService = headerService;
		this.responseBody = new Http1xResponseBody(this);
	}
	
	protected boolean supportsFileRegion() {
		return this.context.pipeline().get(SslHandler.class) == null && this.context.pipeline().get(HttpContentCompressor.class) == null;
	}
	
	@Override
	public Response trailers(Consumer<ResponseTrailers> trailersConfigurer) {
		if(this.responseTrailers == null) {
			this.responseTrailers = new Http1xResponseTrailers(this.headerService);
		}
		trailersConfigurer.accept(this.responseTrailers);
		return this;
	}
	
	@Override
	public Http1xResponseHeaders headers() {
		return (Http1xResponseHeaders)this.responseHeaders;
	}
	
	@Override
	public Http1xResponseTrailers trailers() {
		return (Http1xResponseTrailers)this.responseTrailers;
	}
	
	@Override
	public Http1xResponseBody body() {
		return (Http1xResponseBody)super.body();
	}
}
