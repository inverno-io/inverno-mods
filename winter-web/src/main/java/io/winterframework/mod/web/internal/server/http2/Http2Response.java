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
package io.winterframework.mod.web.internal.server.http2;

import java.util.function.Consumer;

import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.ResponseTrailers;
import io.winterframework.mod.web.internal.server.AbstractResponse;
import io.winterframework.mod.web.internal.server.http1x.Http1xResponseTrailers;

/**
 * @author jkuhn
 *
 */
public class Http2Response extends AbstractResponse {

	/**
	 * @param headerService
	 * @param responseHeaders
	 */
	public Http2Response(ChannelHandlerContext context, HeaderService headerService) {
		super(context, headerService, new Http2ResponseHeaders(headerService));
	}

	@Override
	public Http2ResponseHeaders getHeaders() {
		return (Http2ResponseHeaders)super.getHeaders();
	}
	
	@Override
	public Response<ResponseBody> trailers(Consumer<ResponseTrailers> trailersConfigurer) {
		if(this.responseTrailers == null) {
			this.responseTrailers = new Http2ResponseTrailers();
		}
		trailersConfigurer.accept(this.responseTrailers);
		return this;
	}
	
	@Override
	public Http1xResponseTrailers getTrailers() {
		return (Http1xResponseTrailers)this.responseTrailers;
	}
}
