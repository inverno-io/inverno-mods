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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.winterframework.mod.commons.resource.MediaTypes;
import io.winterframework.mod.web.Charsets;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.netty.FlatFullHttpResponse;
import io.winterframework.mod.web.internal.netty.FlatHttpResponse;
import io.winterframework.mod.web.internal.server.AbstractExchange;
import io.winterframework.mod.web.internal.server.AbstractRequest;
import io.winterframework.mod.web.internal.server.GenericResponseCookies;
import reactor.core.publisher.BaseSubscriber;

/**
 * @author jkuhn
 *
 */
public class Http1xExchange extends AbstractExchange {

	private boolean manageChunked;
	
	private Charset charset;
	
	private boolean flush;
	
	public Http1xExchange(ChannelHandlerContext context, ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler, AbstractRequest request, Http1xResponse response) {
		super(context, rootHandler, errorHandler, request, response);
	}
	
	public void onReadComplete() {
		this.flush = true;
	}
	
	private ChannelFuture write(Object msg, ChannelPromise promise) {
		if(this.flush) {
			return this.context.writeAndFlush(msg, promise);
		}
		else {
			return this.context.write(msg, promise);
		}
	}
	
	private Charset getCharset() {
		if(this.response.isHeadersWritten()) {
			return this.response().getHeaders().<Headers.ContentType>get(Headers.CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		if(this.charset == null) {
			this.charset = this.response().getHeaders().<Headers.ContentType>get(Headers.CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		return this.charset;
	}

	private HttpResponse createHttpResponse(Http1xResponseHeaders headers, GenericResponseCookies cookies) {
		cookies.getAll().stream()
			.forEach(header -> {
				headers.add(header.getHeaderName(), header.getHeaderValue());
			});
		return new FlatHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(headers.getStatus()), headers.getHttpHeaders(), false);
	}
	
	private HttpResponse createFullHttpResponse(Http1xResponseHeaders headers, GenericResponseCookies cookies, ByteBuf content) {
		cookies.getAll().stream()
			.forEach(header -> {
				headers.add(header.getHeaderName(), header.getHeaderValue());
			});
		return new FlatFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(headers.getStatus()), headers.getHttpHeaders(), content, EmptyHttpHeaders.INSTANCE);
	}
	
	@Override
	protected void doHookOnNext(ByteBuf value) {
		try {
			Http1xResponseHeaders headers = (Http1xResponseHeaders)this.response.getHeaders();
			if(this.response.body().getChunkCount() == 1) {
				// Response has one chunk => send a FullHttpResponse
				this.write(this.createFullHttpResponse(headers, this.response.getCookies(), value), this.context.voidPromise());
				headers.setWritten(true);
			}
			else {
				if(!headers.isWritten()) {
					List<String> transferEncodings = headers.getAllString(Headers.TRANSFER_ENCODING);
					if(headers.getSize() == null && !transferEncodings.contains("chunked")) {
						headers.add(Headers.TRANSFER_ENCODING, "chunked");
						// TODO accessing the string and using a region matches for TEXT_EVENT_STREAM might be more efficient
						this.manageChunked = headers.getContentType().map(contentType -> contentType.getMediaType().equals(MediaTypes.TEXT_EVENT_STREAM)).orElse(false);
					}
					this.write(this.createHttpResponse(headers, this.response.getCookies()), this.context.voidPromise());
					headers.setWritten(true);
				}
				if(this.manageChunked) {
					// We must handle chunked transfer encoding
					ByteBuf chunked_header = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(Integer.toHexString(value.readableBytes()) + "\r\n", Charsets.orDefault(this.getCharset())));
					ByteBuf chunked_trailer = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("\r\n", Charsets.orDefault(this.getCharset())));
					
					this.write(new DefaultHttpContent(Unpooled.wrappedBuffer(chunked_header, value, chunked_trailer)), this.context.voidPromise());
				}
				else {
					this.write(new DefaultHttpContent(value), this.context.voidPromise());
				}
			}
		}
		// TODO what happen when an error is thrown
		finally {
			this.exchangeSubscriber.onExchangeNext(value);
		}
	}
	
	@Override
	protected void doHookOnError(Throwable throwable) {
		// TODO
		// either we have written headers or we have not
		// What kind of error can be sent if we have already sent a 200 OK in the response headers
		
		// The stream can be opened or closed here:
		// - if closed => client side have probably ended the stream (RST_STREAM or close connection)
		// - if not closed => we should send a 5xx error or other based on the exception
		throwable.printStackTrace();
		this.exchangeSubscriber.onExchangeError(throwable);
	}
	
	@Override
	protected void doHookOnComplete() {
		try {
			long chunkCount = this.response.body().getChunkCount();
			if(chunkCount == 1) {
				this.exchangeSubscriber.onExchangeComplete();
			}
			else if(chunkCount == 0) {
				// empty response or file region
				Http1xResponse http1xResponse = (Http1xResponse)this.response;
				Http1xResponseHeaders headers = http1xResponse.getHeaders();
				
				http1xResponse.body().getFileRegionData().ifPresentOrElse(
					fileRegionData -> {
						if(!headers.isWritten()) { // This should always be true since we have an empty response
							this.write(this.createHttpResponse(headers, this.response.getCookies()), this.context.voidPromise());
							headers.setWritten(true);
						}
						fileRegionData.subscribe(new FileRegionDataSubscriber());
					},
					() -> {
						// just write headers in a fullHttpResponse
						if(!headers.isWritten()) {
							this.write(this.createFullHttpResponse(headers, this.response.getCookies(), Unpooled.buffer(0)), this.context.voidPromise());
							headers.setWritten(true);
						}
						this.exchangeSubscriber.onExchangeComplete();
					}
				);
			}
			else {
				this.write(LastHttpContent.EMPTY_LAST_CONTENT, this.context.voidPromise());
				this.exchangeSubscriber.onExchangeComplete();
			}
		}
		catch(Exception e) {
			this.exchangeSubscriber.onExchangeError(e);
		} 
	}
	
	private class FileRegionDataSubscriber extends BaseSubscriber<FileRegion> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			this.request(1);
		}

		@Override
		protected void hookOnNext(FileRegion fileRegion) {
			Http1xExchange.this.scheduleOnEventLoop(() -> {
				Http1xExchange.this.write(fileRegion, Http1xExchange.this.context.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						this.request(1);
					}
					else {
						// TODO log/report error
						this.cancel();
					}
				}));
			});
		}
		
		@Override
		protected void hookOnComplete() {
			Http1xExchange.this.scheduleOnEventLoop(() -> {
				// TODO if not keep alive we should close the connection here
				Http1xExchange.this.write(LastHttpContent.EMPTY_LAST_CONTENT, Http1xExchange.this.context.newPromise().addListener(future -> {
					Http1xExchange.this.exchangeSubscriber.onExchangeComplete();
				}));
			});
		}
	}
}