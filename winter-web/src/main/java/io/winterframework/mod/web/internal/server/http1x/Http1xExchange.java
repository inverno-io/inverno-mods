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

import java.nio.charset.Charset;
import java.util.List;

import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
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
import io.winterframework.mod.commons.resource.MediaTypes;
import io.winterframework.mod.web.Charsets;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.netty.FlatFullHttpResponse;
import io.winterframework.mod.web.internal.netty.FlatHttpResponse;
import io.winterframework.mod.web.internal.server.AbstractExchange;
import io.winterframework.mod.web.internal.server.GenericErrorExchange;
import io.winterframework.mod.web.internal.server.GenericResponseCookies;
import reactor.core.publisher.BaseSubscriber;

/**
 * @author jkuhn
 *
 */
public class Http1xExchange extends AbstractExchange {

	private HeaderService headerService;
	
	private boolean manageChunked;
	private Charset charset;
	
	private Http1xConnectionEncoder encoder;
	
	Http1xExchange next;
	boolean keepAlive;
	
	public Http1xExchange(
			ChannelHandlerContext context, 
			HttpRequest httpRequest,
			Http1xConnectionEncoder encoder,
			HeaderService headerService,
			RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, 
			RequestBodyDecoder<Part> multipartBodyDecoder,
			ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, 
			ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler
		) {
		super(context, rootHandler, errorHandler, new Http1xRequest(context, new Http1xRequestHeaders(context, httpRequest, headerService), urlEncodedBodyDecoder, multipartBodyDecoder), new Http1xResponse(context, headerService));
		this.encoder = encoder;
		this.headerService = headerService;
		this.keepAlive = !httpRequest.headers().contains(Headers.NAME_CONNECTION, Headers.VALUE_CLOSE, true);
	}
	
	@Override
	protected ErrorExchange<ResponseBody, Throwable> createErrorExchange(Throwable error) {
		return new GenericErrorExchange(this.request, new Http1xResponse(this.context, this.headerService), error);
	}
	
	private Charset getCharset() {
		if(this.response.isHeadersWritten()) {
			return this.response().getHeaders().<Headers.ContentType>get(Headers.NAME_CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		if(this.charset == null) {
			this.charset = this.response().getHeaders().<Headers.ContentType>get(Headers.NAME_CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		return this.charset;
	}
	
	private void preProcessHttpHeaders(HttpResponseStatus status, HttpHeaders httpHeaders, GenericResponseCookies cookies) {
		if(!this.keepAlive) {
			httpHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		}
		if(status == HttpResponseStatus.NOT_MODIFIED) {
			httpHeaders.remove(HttpHeaderNames.TRANSFER_ENCODING);
			httpHeaders.remove(HttpHeaderNames.CONTENT_LENGTH);
		}
		for(Headers.SetCookie cookie : cookies.getAll()) {
			httpHeaders.add(cookie.getHeaderName(), cookie.getHeaderValue());
		}
	}
	
	private HttpResponse createHttpResponse(Http1xResponseHeaders headers, GenericResponseCookies cookies) {
		HttpResponseStatus status = HttpResponseStatus.valueOf(headers.getStatus());
		HttpHeaders httpHeaders = headers.getHttpHeaders();
		this.preProcessHttpHeaders(status, httpHeaders, cookies);
		return new FlatHttpResponse(HttpVersion.HTTP_1_1, status, httpHeaders, false);
	}
	
	private HttpResponse createFullHttpResponse(Http1xResponseHeaders headers, GenericResponseCookies cookies, ByteBuf content) {
		HttpResponseStatus status = HttpResponseStatus.valueOf(headers.getStatus());
		HttpHeaders httpHeaders = headers.getHttpHeaders();
		this.preProcessHttpHeaders(status, httpHeaders, cookies);
		return new FlatFullHttpResponse(HttpVersion.HTTP_1_1, status, httpHeaders, content, EmptyHttpHeaders.INSTANCE);
	}
	
	@Override
	protected void onNextMany(ByteBuf value) {
		try {
			Http1xResponseHeaders headers = (Http1xResponseHeaders)this.response.getHeaders();
			if(!headers.isWritten()) {
				List<String> transferEncodings = headers.getAllString(Headers.NAME_TRANSFER_ENCODING);
				if(headers.getSize() == null && !transferEncodings.contains("chunked")) {
					headers.add(Headers.NAME_TRANSFER_ENCODING, "chunked");
					// TODO accessing the string and using a region matches for TEXT_EVENT_STREAM might be more efficient
					this.manageChunked = headers.getContentType().map(contentType -> contentType.getMediaType().equals(MediaTypes.TEXT_EVENT_STREAM)).orElse(false);
				}
				this.encoder.writeFrame(this.context, this.createHttpResponse(headers, this.response.getCookies()), this.context.voidPromise());
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
		// TODO what happen when an error is thrown
		finally {
			this.handler.exchangeNext(this.context, value);
		}
	}
	
	@Override
	protected void onCompleteWithError(Throwable throwable) {
		// TODO
		// either we have written headers or we have not
		// What kind of error can be sent if we have already sent a 200 OK in the response headers
		
		// The stream can be opened or closed here:
		// - if closed => client side have probably ended the stream (RST_STREAM or close connection)
		// - if not closed => we should send a 5xx error or other based on the exception
		throwable.printStackTrace();
		this.handler.exchangeError(this.context, throwable);
	}
	
	@Override
	protected void onCompleteEmpty() {
		// empty response or file region
		Http1xResponse http1xResponse = (Http1xResponse)this.response;
		Http1xResponseHeaders headers = http1xResponse.getHeaders();
		
		http1xResponse.body().getFileRegionData().ifPresentOrElse(
			fileRegionData -> {
				// Headers are not written here since we have an empty response
				this.encoder.writeFrame(this.context, this.createHttpResponse(headers, this.response.getCookies()), this.context.voidPromise());
				headers.setWritten(true);
				fileRegionData.subscribe(new FileRegionDataSubscriber());
			},
			() -> {
				// just write headers in a fullHttpResponse
				// Headers are not written here since we have an empty response
				this.encoder.writeFrame(this.context, this.createFullHttpResponse(headers, this.response.getCookies(), Unpooled.buffer(0)), this.context.voidPromise());
				headers.setWritten(true);
				this.handler.exchangeComplete(this.context);
			}
		);
	}
	
	@Override
	protected void onCompleteSingle(ByteBuf value) {
		try {
			// Response has one chunk => send a FullHttpResponse
			Http1xResponseHeaders headers = (Http1xResponseHeaders)this.response.getHeaders();
			this.encoder.writeFrame(this.context, this.createFullHttpResponse(headers, this.response.getCookies(), value), this.context.voidPromise());
			headers.setWritten(true);
		}
		// TODO what happen when an error is thrown
		finally {
			this.handler.exchangeNext(this.context, value);
			this.handler.exchangeComplete(this.context);
		}
	}
	
	@Override
	protected void onCompleteMany() {
		this.encoder.writeFrame(this.context, LastHttpContent.EMPTY_LAST_CONTENT, this.context.voidPromise());
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
						// TODO does this triggers onComplete?
						this.cancel();
					}
				}));
			});
		}
		
		@Override
		protected void hookOnComplete() {
			Http1xExchange.this.executeInEventLoop(() -> {
				// TODO if not keep alive we should close the connection here
				Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, LastHttpContent.EMPTY_LAST_CONTENT, Http1xExchange.this.context.newPromise().addListener(future -> {
					Http1xExchange.this.handler.exchangeComplete(Http1xExchange.this.context);
				}));
			});
		}
	}
}