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
package io.winterframework.mod.http.server.internal.http1x;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.winterframework.mod.base.Charsets;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.header.HeaderService;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.Exchange;
import io.winterframework.mod.http.server.ExchangeHandler;
import io.winterframework.mod.http.server.Part;
import io.winterframework.mod.http.server.internal.AbstractExchange;
import io.winterframework.mod.http.server.internal.GenericErrorExchange;
import io.winterframework.mod.http.server.internal.multipart.MultipartDecoder;
import io.winterframework.mod.http.server.internal.netty.FlatFullHttpResponse;
import io.winterframework.mod.http.server.internal.netty.FlatHttpResponse;
import io.winterframework.mod.http.server.internal.netty.FlatLastHttpContent;
import reactor.core.publisher.BaseSubscriber;

/**
 * <p>
 * HTTP1.x {@link Exchange} implementation.
 * </p>
 * 
 * <p>
 * This implementation provides the logic to send HTTP1.x response data to the
 * client.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractExchange
 */
public class Http1xExchange extends AbstractExchange {

	private final Http1xConnectionEncoder encoder;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private boolean manageChunked;
	private Charset charset;
	
	Http1xExchange next;
	boolean keepAlive;
	boolean trailers;
	
	/**
	 * <p>
	 * Creates a HTTP1.x server exchange.
	 * </p>
	 * 
	 * @param context               the channel handler context
	 * @param httpRequest           the underlying HTTP request
	 * @param encoder               the HTTP1.x connection encoder
	 * @param headerService         the header service
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body
	 *                              decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param rootHandler           the root exchange handler
	 * @param errorHandler          the error exchange handler
	 */
	public Http1xExchange(
			ChannelHandlerContext context, 
			HttpRequest httpRequest,
			Http1xConnectionEncoder encoder,
			HeaderService headerService,
			ObjectConverter<String> parameterConverter,
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder,
			ExchangeHandler<Exchange> rootHandler, 
			ExchangeHandler<ErrorExchange<Throwable>> errorHandler
		) {
		super(context, rootHandler, errorHandler, new Http1xRequest(context, httpRequest, new Http1xRequestHeaders(httpRequest, headerService, parameterConverter), parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder), new Http1xResponse(context, headerService, parameterConverter));
		this.encoder = encoder;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.keepAlive = !httpRequest.headers().contains(Headers.NAME_CONNECTION, Headers.VALUE_CLOSE, true);
		String te = httpRequest.headers().get(Headers.NAME_TE);
		this.trailers = te != null && te.contains(Headers.VALUE_TRAILERS);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if(this.next != null) {
			this.next.dispose();
		}
	}
	
	@Override
	protected ErrorExchange<Throwable> createErrorExchange(Throwable error) {
		return new GenericErrorExchange(this.request, new Http1xResponse(this.context, this.headerService, this.parameterConverter), error);
	}
	
	private Charset getCharset() {
		if(this.response.isHeadersWritten()) {
			return this.response().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		if(this.charset == null) {
			this.charset = this.response().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		return this.charset;
	}
	
	private void preProcessResponseInternals(HttpResponseStatus status, HttpHeaders internalHeaders, HttpHeaders internalTrailers) {
		if(!this.keepAlive) {
			internalHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		}
		if(status == HttpResponseStatus.NOT_MODIFIED) {
			internalHeaders.remove(HttpHeaderNames.TRANSFER_ENCODING);
			internalHeaders.remove(HttpHeaderNames.CONTENT_LENGTH);
		}
		if(this.trailers && internalTrailers != null) {
			internalHeaders.set(Headers.NAME_TRAILER, internalTrailers.names().stream().collect(Collectors.joining(", ")));
		}
	}
	
	private HttpResponse createHttpResponse(Http1xResponseHeaders headers, Http1xResponseTrailers trailers) {
		HttpResponseStatus status = HttpResponseStatus.valueOf(headers.getStatusCode());
		HttpHeaders httpHeaders = headers.getUnderlyingHeaders();
		HttpHeaders httpTrailers = trailers != null ? trailers.getUnderlyingTrailers() : null;
		this.preProcessResponseInternals(status, httpHeaders, httpTrailers);
		return new FlatHttpResponse(HttpVersion.HTTP_1_1, status, httpHeaders, false);
	}
	
	private HttpResponse createFullHttpResponse(Http1xResponseHeaders headers, ByteBuf content) {
		HttpResponseStatus status = HttpResponseStatus.valueOf(headers.getStatusCode());
		HttpHeaders httpHeaders = headers.getUnderlyingHeaders();
		this.preProcessResponseInternals(status, httpHeaders, null); // trailers are only authorized in chunked transfer encoding
		return new FlatFullHttpResponse(HttpVersion.HTTP_1_1, status, httpHeaders, content, EmptyHttpHeaders.INSTANCE);
	}
	
	@Override
	protected void onNextMany(ByteBuf value) {
		try {
			Http1xResponseHeaders headers = (Http1xResponseHeaders)this.response.headers();
			if(!headers.isWritten()) {
				List<String> transferEncodings = headers.getAll(Headers.NAME_TRANSFER_ENCODING);
				if(headers.getContentLength() == null && !transferEncodings.contains(Headers.VALUE_CHUNKED)) {
					headers.set(Headers.NAME_TRANSFER_ENCODING, Headers.VALUE_CHUNKED);
					headers.get(Headers.NAME_CONTENT_TYPE).ifPresent(contentType -> this.manageChunked = contentType.regionMatches(true, 0, MediaTypes.TEXT_EVENT_STREAM, 0, MediaTypes.TEXT_EVENT_STREAM.length()));
				}
				this.encoder.writeFrame(this.context, this.createHttpResponse(headers, (Http1xResponseTrailers)this.response.trailers()), this.context.voidPromise());
				headers.setWritten(true);
			}
			if(this.manageChunked) {
				// We must handle chunked transfer encoding
				ByteBuf chunked_header = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(Integer.toHexString(value.readableBytes()) + "\r\n", Charsets.orDefault(this.getCharset())));
				ByteBuf chunked_trailer = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("\r\n", Charsets.orDefault(this.getCharset())));
				this.encoder.writeFrame(this.context, new DefaultHttpContent(Unpooled.wrappedBuffer(chunked_header, value, chunked_trailer)), this.context.voidPromise());
			}
			else {
				this.encoder.writeFrame(this.context, new DefaultHttpContent(value), this.context.voidPromise());
			}
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
		// empty response or file region
		Http1xResponse http1xResponse = (Http1xResponse)this.response;
		Http1xResponseHeaders headers = http1xResponse.headers();
		
		http1xResponse.body().getFileRegionData().ifPresentOrElse(
			fileRegionData -> {
				// Headers are not written here since we have an empty response
				this.encoder.writeFrame(this.context, this.createHttpResponse(headers, http1xResponse.trailers()), this.context.voidPromise());
				headers.setWritten(true);
				fileRegionData.subscribe(new FileRegionDataSubscriber());
			},
			() -> {
				// just write headers in a fullHttpResponse
				// Headers are not written here since we have an empty response
				this.encoder.writeFrame(this.context, this.createFullHttpResponse(headers, Unpooled.buffer(0)), this.context.voidPromise());
				headers.setWritten(true);
				this.handler.exchangeComplete(this.context);
			}
		);
	}
	
	@Override
	protected void onCompleteSingle(ByteBuf value) {
		// Response has one chunk => send a FullHttpResponse
		Http1xResponseHeaders headers = (Http1xResponseHeaders)this.response.headers();
		this.encoder.writeFrame(this.context, this.createFullHttpResponse(headers, value), this.context.voidPromise());
		headers.setWritten(true);
		this.handler.exchangeNext(this.context, value);
		this.handler.exchangeComplete(this.context);
	}
	
	@Override
	protected void onCompleteMany() {
		Http1xResponseTrailers trailers = (Http1xResponseTrailers)this.response.trailers();
		if(trailers != null) {
			this.encoder.writeFrame(this.context, new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, trailers.getUnderlyingTrailers()), this.context.voidPromise());
		}
		else {
			this.encoder.writeFrame(this.context, LastHttpContent.EMPTY_LAST_CONTENT, this.context.voidPromise());
		}
		this.handler.exchangeComplete(this.context);
	}
	
	private class FileRegionDataSubscriber extends BaseSubscriber<FileRegion> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			this.request(1);
		}

		@Override
		protected void hookOnNext(FileRegion fileRegion) {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, fileRegion, Http1xExchange.this.context.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						// TODO here we put null as next value because we don't have access to the actual buffer, can we do better?
						Http1xExchange.this.handler.exchangeNext(Http1xExchange.this.context, null);
						this.request(1);
					}
					else {
						Http1xExchange.this.handler.exchangeError(Http1xExchange.this.context, future.cause());
						this.cancel();
						Http1xExchange.this.onCompleteWithError(future.cause());
					}
				}));
			});
		}
		
		@Override
		protected void hookOnComplete() {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xResponseTrailers trailers = (Http1xResponseTrailers)Http1xExchange.this.response.trailers();
				
				ChannelPromise promise = Http1xExchange.this.context.newPromise().addListener(future -> {
					Http1xExchange.this.handler.exchangeComplete(Http1xExchange.this.context);
				});
				
				if(trailers != null) {
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, trailers.getUnderlyingTrailers()), promise);
				}
				else {
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, LastHttpContent.EMPTY_LAST_CONTENT, promise);
				}
			});
		}
	}
}