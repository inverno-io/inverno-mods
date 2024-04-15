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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.internal.AbstractResponse;
import io.inverno.mod.http.server.internal.GenericResponseBody;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Stream;

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

	private final Http2Stream stream;
	
	private final Http2ConnectionEncoder encoder;
	
	private final ObjectConverter<String> parameterConverter;
	
	private final boolean validateHeaders;
	
	/**
	 * <p>
	 * Creates a HTTP/2 server response.
	 * </p>
	 *
	 * @param context            the channel handler context
	 * @param stream             the HTTP/2 the underlying HTTP/2 stream
	 * @param encoder            the HTTP/2 connection encoder
	 * @param headerService      the header service
	 * @param parameterConverter a string object converter
	 * @param validateHeaders    true to validate headers, false otherwise
	 */
	public Http2Response(ChannelHandlerContext context, Http2Stream stream, Http2ConnectionEncoder encoder, HeaderService headerService, ObjectConverter<String> parameterConverter, boolean validateHeaders) {
		super(context, headerService, new Http2ResponseHeaders(headerService, parameterConverter, validateHeaders));
		this.stream = stream;
		this.encoder = encoder;
		this.parameterConverter = parameterConverter;
		this.validateHeaders = validateHeaders;
		this.responseBody = new GenericResponseBody(this);
	}

	@Override
	public Http2ResponseHeaders headers() {
		return (Http2ResponseHeaders)this.responseHeaders;
	}

	@Override
	protected OutboundHeaders<?> createTrailers() {
		return new Http2ResponseTrailers(this.headerService, this.parameterConverter, this.validateHeaders);
	}
	
	@Override
	public Http2ResponseTrailers trailers() {
		return (Http2ResponseTrailers)super.trailers();
	}

	@Override
	public Response sendContinue() {
		if(this.isHeadersWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		// we might have an issue here if this run outside the event loop
		this.encoder.writeHeaders(this.context, this.stream.id(), new DefaultHttp2Headers().status("100"), 0, false, this.context.voidPromise());
		return this;
	}
}
