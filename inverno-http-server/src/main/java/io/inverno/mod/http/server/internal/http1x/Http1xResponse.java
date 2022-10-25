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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.internal.AbstractResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;

/**
 * <p>
 * HTTP1.x {@link Response} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractResponse
 */
class Http1xResponse extends AbstractResponse {

	private final HttpVersion version;
	
	private final ObjectConverter<String> parameterConverter;

	/**
	 * <p>
	 * Creates a HTTP1.x server response.
	 * </p>
	 * 
	 * @param context            the channel handler context
	 * @param headerService      the header service
	 * @param parameterConverter a string object converter
	 */
	public Http1xResponse(HttpVersion version, ChannelHandlerContext context, HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(context, headerService, new Http1xResponseHeaders(headerService, parameterConverter));
		this.version = version;
		this.parameterConverter = parameterConverter;
		this.responseBody = new Http1xResponseBody(this);
	}
	
	/**
	 * <p>
	 * Determines whether file region is supported.
	 * </p>
	 * 
	 * @return true if the file region is supported, false otherwise
	 */
	protected boolean supportsFileRegion() {
		return this.context.pipeline().get(SslHandler.class) == null && this.context.pipeline().get(HttpContentCompressor.class) == null;
	}
	
	@Override
	public Http1xResponseHeaders headers() {
		return (Http1xResponseHeaders)this.responseHeaders;
	}

	@Override
	protected OutboundHeaders<?> createTrailers() {
		return new Http1xResponseTrailers(this.headerService, this.parameterConverter);
	}
	
	@Override
	public Http1xResponseTrailers trailers() {
		return (Http1xResponseTrailers)this.responseTrailers;
	}

	@Override
	public Response sendContinue() {
		this.context.writeAndFlush(new DefaultFullHttpResponse(this.version, HttpResponseStatus.CONTINUE));
		return this;
	}
	
	@Override
	public Http1xResponseBody body() {
		return (Http1xResponseBody)super.body();
	}
}
