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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.internal.AbstractExchange;
import io.inverno.mod.http.server.internal.GenericErrorExchange;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;

/**
 * <p>
 * HTTP/2 {@link Exchange} implementation.
 * </p>
 * 
 * <p>
 * This implementation provides the logic to send HTTP/2 response data to the
 * client.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractExchange
 */
public class Http2Exchange extends AbstractExchange {

	private final Http2Stream stream;
	private final Http2ConnectionEncoder encoder;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	/**
	 * <p>
	 * Creates a HTTP/2 server exchange.
	 * </p>
	 * 
	 * @param context               the channel handler context
	 * @param stream                the underlying HTTP/2 stream
	 * @param httpHeaders           the underlying HTTP/2 request headers
	 * @param encoder               the HTTP/2 connection encoder
	 * @param headerService         the header service
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body
	 *                              decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param rootHandler           the server root exchange handler
	 * @param errorHandler          the server error exchange handler
	 */
	public Http2Exchange(
			ChannelHandlerContext context, 
			Http2Stream stream, 
			Http2Headers httpHeaders, 
			Http2ConnectionEncoder encoder,
			HeaderService headerService,
			ObjectConverter<String> parameterConverter,
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder,
			ExchangeHandler<Exchange> rootHandler, 
			ExchangeHandler<ErrorExchange<Throwable>> errorHandler
		) {
		super(context, rootHandler, errorHandler, new Http2Request(context, new Http2RequestHeaders(httpHeaders, headerService, parameterConverter), parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder), new Http2Response(context, headerService, parameterConverter));
		this.stream = stream;
		this.encoder = encoder;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}
	
	/**
	 * <p>
	 * Sets the content encoding of the response negotiated from the request.
	 * </p>
	 * 
	 * @param contentEncoding the target content encoding of the response resolved
	 *                        from the {@code accept-encoding} header of the request
	 */
	public void setContentEncoding(String contentEncoding) {
		if(contentEncoding != null) {
			this.response.headers().set(Headers.NAME_CONTENT_ENCODING, contentEncoding);
		}
	}
	
	@Override
	protected ErrorExchange<Throwable> createErrorExchange(Throwable error) {
		return new GenericErrorExchange(this.request, new Http2Response(this.context, this.headerService, this.parameterConverter), error);
	}
	
	@Override
	protected void onNextMany(ByteBuf value) {
		try {
			Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.headers();
			if(!headers.isWritten()) {
				this.encoder.writeHeaders(this.context, this.stream.id(), headers.getUnderlyingHeaders(), 0, false, this.context.voidPromise());
				headers.setWritten(true);
			}
			// TODO implement back pressure with the flow controller
			/*this.encoder.flowController().listener(new Listener() {
				
				@Override
				public void writabilityChanged(Http2Stream stream) {
					
				}
			});*/
			
			this.encoder.writeData(this.context, this.stream.id(), value, 0, false, this.context.voidPromise());
			this.context.channel().flush();
		}
		finally {
			this.handler.exchangeNext(this.context, value);
		}
	}
	
	@Override
	protected void onCompleteWithError(Throwable throwable) {
		this.handler.exchangeError(this.context, throwable);
	}
	
	@Override
	protected void onCompleteEmpty() {
		Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.headers();
		Http2ResponseTrailers trailers = (Http2ResponseTrailers)this.response.trailers();
		this.encoder.writeHeaders(this.context, this.stream.id(), headers.getUnderlyingHeaders(), 0, trailers == null, this.context.voidPromise());
		headers.setWritten(true);
		if(trailers != null) {
			this.encoder.writeHeaders(this.context, this.stream.id(), trailers.getUnderlyingTrailers(), 0, true, this.context.voidPromise());
		}
		this.context.channel().flush();
		this.handler.exchangeComplete(this.context);
	}
	
	@Override
	protected void onCompleteSingle(ByteBuf value) {
		Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.headers();
		this.encoder.writeHeaders(this.context, this.stream.id(), headers.getUnderlyingHeaders(), 0, false, this.context.voidPromise());
		headers.setWritten(true);
		Http2ResponseTrailers trailers = (Http2ResponseTrailers)this.response.trailers();
		this.encoder.writeData(this.context, this.stream.id(), value, 0, trailers == null, this.context.voidPromise());
		if(trailers != null) {
			this.encoder.writeHeaders(this.context, this.stream.id(), trailers.getUnderlyingTrailers(), 0, true, this.context.voidPromise());
		}
		this.context.channel().flush();
		this.handler.exchangeNext(this.context, value);
		this.handler.exchangeComplete(this.context);
	}
	
	@Override
	protected void onCompleteMany() {
		Http2ResponseHeaders headers = (Http2ResponseHeaders)this.response.headers();
		Http2ResponseTrailers trailers = (Http2ResponseTrailers)this.response.trailers();
		if(!headers.isWritten()) {
			this.encoder.writeHeaders(this.context, this.stream.id(), headers.getUnderlyingHeaders(), 0, trailers == null, this.context.voidPromise());
			headers.setWritten(true);
			if(trailers != null) {
				this.encoder.writeHeaders(this.context, this.stream.id(), trailers.getUnderlyingTrailers(), 0, true, this.context.voidPromise());
			}
		}
		else {
			if(trailers != null) {
				this.encoder.writeHeaders(this.context, this.stream.id(), trailers.getUnderlyingTrailers(), 0, true, this.context.voidPromise());
			}
			else {
				this.encoder.writeData(this.context, this.stream.id(), Unpooled.EMPTY_BUFFER, 0, true, this.context.voidPromise());
			}
		}
		this.context.channel().flush();
		this.handler.exchangeComplete(this.context);
	}
}
