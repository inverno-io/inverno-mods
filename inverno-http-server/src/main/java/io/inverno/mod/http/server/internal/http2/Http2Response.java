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
package io.inverno.mod.http.server.internal.http2;

import java.util.function.Consumer;

import io.netty.channel.ChannelHandlerContext;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.ResponseTrailers;
import io.inverno.mod.http.server.internal.AbstractResponse;
import io.inverno.mod.http.server.internal.GenericResponseBody;

/**
 * <p>
 * HTTP/2 {@link Response} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractResponse
 */
class Http2Response extends AbstractResponse {

	private final ObjectConverter<String> parameterConverter;
	
	/**
	 * <p>
	 * Creates a HTTP/2 server response.
	 * </p>
	 * 
	 * @param context            the channel handler context
	 * @param headerService      the header service
	 * @param parameterConverter a string object converter
	 */
	public Http2Response(ChannelHandlerContext context, HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(context, headerService, new Http2ResponseHeaders(headerService, parameterConverter));
		this.parameterConverter = parameterConverter;
		this.responseBody = new GenericResponseBody(this);
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
