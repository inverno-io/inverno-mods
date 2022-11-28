/*
 * Copyright 2022 Jeremy KUHN
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

package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.netty.FlatFullHttpRequest;
import io.inverno.mod.http.base.internal.netty.FlatHttpRequest;
import io.inverno.mod.http.base.internal.netty.FlatLastHttpContent;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.internal.AbstractExchange;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.util.List;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.MonoSink;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
class Http1xExchange extends AbstractExchange<Http1xRequest, Http1xResponse, Http1xExchange> {

	private final Http1xConnectionEncoder encoder;
	private final HttpVersion httpVersion;
	
	long lastModified;
	Http1xExchange next;
	
	public Http1xExchange(ChannelHandlerContext context, MonoSink<Exchange<ExchangeContext>> exchangeSink, ExchangeContext exchangeContext, Http1xRequest request, Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer, Http1xConnectionEncoder encoder) {
		super(context, exchangeSink, exchangeContext, request, responseBodyTransformer);
		this.encoder = encoder;
		switch(request.getProtocol()) {
			case HTTP_1_0: this.httpVersion = HttpVersion.HTTP_1_0;
				break;
			case HTTP_1_1: this.httpVersion = HttpVersion.HTTP_1_1;
				break;
			default: throw new IllegalStateException("Invalid protocol version: " + request.getProtocol());
		}
	}
	
	// this is executed in event loop
	public void doStart() {
		// Do send the request
		this.handler.exchangeStart(this);
		this.request.body().ifPresentOrElse(
			body -> {
				Publisher<FileRegion> fileRegionData = body.getFileRegionData();
				if(fileRegionData == null) {
					Http1xExchange.DataSubscriber dataSubscriber = new Http1xExchange.DataSubscriber(body.isSingle());
					body.dataSubscribe(dataSubscriber);
					this.disposable = dataSubscriber;
				}
				else {
					// we can write headers
					Http1xRequestHeaders headers = this.request.headers();
					this.encoder.writeFrame(Http1xExchange.this.context, new FlatHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), this.fixHeaders(headers.toHttp1xHeaders()), false), Http1xExchange.this.context.voidPromise());
					headers.setWritten(true);

					Http1xExchange.FileRegionDataSubscriber fileRegionSubscriber = new Http1xExchange.FileRegionDataSubscriber();
					fileRegionData.subscribe(fileRegionSubscriber);
					this.disposable = fileRegionSubscriber;
				}
			},
			() -> {
				// no need to subscribe
				Http1xRequestHeaders headers = this.request.headers();
				ChannelPromise finalizePromise = this.context.newPromise();
				finalizePromise.addListener(future -> {
					if(!future.isSuccess()) {
						this.handler.exchangeError(this, future.cause());
					}
				});
				if(headers.getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
					headers.contentLength(0);
				}

				this.encoder.writeFrame(this.context, new FlatFullHttpRequest(this.httpVersion, HttpMethod.valueOf(this.request.getMethod().name()), this.request.getPath(), this.fixHeaders(headers.toHttp1xHeaders()), Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE), finalizePromise);
				headers.setWritten(true);
				this.handler.requestComplete(this);
			}	
		);
	}
	
	private HttpHeaders fixHeaders(HttpHeaders headers) {
		return headers.set(Headers.NAME_HOST, this.request.getAuthority());
	}

	public void dispose(boolean deep) {
		this.dispose(null, deep);
	}
	
	public void dispose(Throwable error, boolean deep) {
		this.dispose(error);
		
		if(deep && this.next != null) {
			this.next.dispose(error, deep);
		}
	}
	
	private class DataSubscriber extends BaseSubscriber<ByteBuf> {
		
		private final boolean single;
		
		private ByteBuf singleChunk;
		private boolean many;
		private long transferedLength;
		
		public DataSubscriber(boolean single) {
			this.single = single;
		}

		public long getTransferedLength() {
			return transferedLength;
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			this.transferedLength += value.readableBytes();
			Http1xRequestHeaders headers = Http1xExchange.this.request.headers();

			if( (this.single || !this.many) && this.singleChunk == null) {
				this.singleChunk = value;
			}
			else {
				this.many = true;
				Http1xExchange.this.executeInEventLoop(() -> {
					if(!headers.isWritten()) {
						List<String> transferEncodings = headers.getAll(Headers.NAME_TRANSFER_ENCODING);
						if(headers.getContentLength() == null && !transferEncodings.contains(Headers.VALUE_CHUNKED)) {
							headers.set(Headers.NAME_TRANSFER_ENCODING, Headers.VALUE_CHUNKED);
						}
						if(this.singleChunk != null) {
							Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), Http1xExchange.this.fixHeaders(headers.toHttp1xHeaders()), this.singleChunk), Http1xExchange.this.context.voidPromise());
							Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new DefaultHttpContent(value), Http1xExchange.this.context.voidPromise());
							this.singleChunk = null;
						}
						else {
							Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), Http1xExchange.this.fixHeaders(headers.toHttp1xHeaders()), value), Http1xExchange.this.context.voidPromise());
						}
						headers.setWritten(true);
					}
					else {
						Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new DefaultHttpContent(value), Http1xExchange.this.context.voidPromise());
					}
				});
			}
		}
		
		@Override
		protected void hookOnComplete() {
			// trailers if any should be send here in the last content
			
			Http1xRequestHeaders headers = Http1xExchange.this.request.headers();
			ChannelPromise finalizePromise = Http1xExchange.this.context.newPromise();
			finalizePromise.addListener(future -> {
				if(future.isSuccess()) {
					Http1xExchange.this.handler.requestComplete(Http1xExchange.this);
				}
				else {
					Http1xExchange.this.handler.exchangeError(Http1xExchange.this, future.cause());
				}
			});
			if(this.transferedLength == 0) {
				// empty response
				if(headers.getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
					headers.contentLength(0);
				}
				Http1xExchange.this.executeInEventLoop(() -> {
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatFullHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), Http1xExchange.this.fixHeaders(headers.toHttp1xHeaders()), Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE), finalizePromise);
					headers.setWritten(true);
				});
			}
			else if(this.singleChunk != null) {
				// single
				if(headers.getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
					headers.contentLength(this.transferedLength);
				}
				Http1xExchange.this.executeInEventLoop(() -> {
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatFullHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), Http1xExchange.this.fixHeaders(headers.toHttp1xHeaders()), this.singleChunk, EmptyHttpHeaders.INSTANCE), finalizePromise);
					headers.setWritten(true);
				});
			}
			else {
				// many
				Http1xExchange.this.executeInEventLoop(() -> {
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE), finalizePromise);
				});
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.handler.exchangeError(Http1xExchange.this, throwable);
			});
		}
	}
	
	private class FileRegionDataSubscriber extends BaseSubscriber<FileRegion> {

		private long transferedLength;

		public long getTransferedLength() {
			return transferedLength;
		}
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			this.request(1);
		}

		@Override
		protected void hookOnNext(FileRegion fileRegion) {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, fileRegion, Http1xExchange.this.context.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						this.transferedLength += fileRegion.count();
						this.request(1);
					}
					else {
						Http1xExchange.this.handler.exchangeError(Http1xExchange.this, future.cause());
						this.cancel();
					}
				}));
			});
		}
		
		@Override
		protected void hookOnComplete() {
			// trailers if any should be send here in the last content
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE), Http1xExchange.this.context.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						Http1xExchange.this.handler.requestComplete(Http1xExchange.this);
					}
					else {
						Http1xExchange.this.handler.exchangeError(Http1xExchange.this, future.cause());
					}
				}));
			});
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.handler.exchangeError(Http1xExchange.this, throwable);
			});
		}
	}
}
