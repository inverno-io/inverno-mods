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
package io.winterframework.mod.http.server.internal.http2;

import java.util.function.Consumer;

import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.http.base.header.HeaderService;
import io.winterframework.mod.http.server.Response;
import io.winterframework.mod.http.server.ResponseTrailers;
import io.winterframework.mod.http.server.internal.AbstractResponse;

/**
 * @author jkuhn
 *
 */
class Http2Response extends AbstractResponse {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	/**
	 * @param headerService
	 * @param responseHeaders
	 */
	public Http2Response(ChannelHandlerContext context, HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(context, headerService, new Http2ResponseHeaders(headerService, parameterConverter));
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}

	@Override
	public Http2ResponseHeaders headers() {
		return (Http2ResponseHeaders)this.responseHeaders;
	}
	
	@Override
	public Response trailers(Consumer<ResponseTrailers> trailersConfigurer) {
		if(this.responseTrailers == null) {
			this.responseTrailers = new Http2ResponseTrailers(this.headerService, this.parameterConverter);
		}
		trailersConfigurer.accept(this.responseTrailers);
		return this;
	}
	
	@Override
	public Http2ResponseTrailers trailers() {
		return (Http2ResponseTrailers)this.responseTrailers;
	}
}
