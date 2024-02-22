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
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.List;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * <p>
 * HTTP/1.x {@link Exchange} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xExchange extends AbstractExchange<Http1xRequest, Http1xResponse, Http1xExchange> {

	private final Http1xConnectionEncoder encoder;
	private final HttpVersion httpVersion;
	
	long lastModified;
	Http1xExchange next;
	
	/**
	 * <p>
	 * Creates an HTTP/1.x exchange.
	 * </p>
	 *
	 * @param context                 the channel context
	 * @param exchangeSink            the exchange sink
	 * @param exchangeContext         the exchange context
	 * @param protocol                the HTTP/1.x protocol version
	 * @param request                 the HTTP/1.x request
	 * @param responseBodyTransformer the response body transformer
	 * @param encoder                 the HTTP/1.x connection encoder
	 */
	public Http1xExchange(
			ChannelHandlerContext context, 
			MonoSink<Exchange<ExchangeContext>> exchangeSink, 
			ExchangeContext exchangeContext, 
			io.inverno.mod.http.base.HttpVersion protocol,
			Http1xRequest request, 
			Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer, 
			Http1xConnectionEncoder encoder) {
		super(context, exchangeSink, exchangeContext, protocol, request, responseBodyTransformer);
		this.encoder = encoder;
		switch(protocol) {
			case HTTP_1_0: this.httpVersion = HttpVersion.HTTP_1_0;
				break;
			case HTTP_1_1: this.httpVersion = HttpVersion.HTTP_1_1;
				break;
			default: throw new IllegalStateException("Invalid protocol version: " + protocol);
		}
	}
	
	/**
	 * <p>
	 * Starts the exchange.
	 * </p>
	 * 
	 * <p>
	 * This basically sends the request to the remote endpoint.
	 * </p>
	 */
	// this is executed in event loop
	protected void doStart() {
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

				this.encoder.writeFrame(this.context, new FlatFullHttpRequest(this.httpVersion, HttpMethod.valueOf(this.request.getMethod().name()), this.request.getPath(), this.fixHeaders(headers.toHttp1xHeaders()), Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE), finalizePromise);
				headers.setWritten(true);
				this.handler.requestComplete(this);
			}	
		);
	}

	/**
	 * <p>
	 * Determines whether the connection will be closed uppon response (i.e. {@code connection: close}).
	 * </p>
	 * 
	 * @return true if the connection will be closed, false otherwise
	 */
	boolean isClose() {
		return this.response != null && this.response.headers().contains(Headers.NAME_CONNECTION, Headers.VALUE_CLOSE);
	}
	
	/**
	 * <p>
	 * Sets required headers (e.g. {@code host}...)
	 * </p>
	 * 
	 * @param headers the request HTTP headers
	 * 
	 * @return the HTTP headers
	 */
	private HttpHeaders fixHeaders(HttpHeaders headers) {
		return headers.set(Headers.NAME_HOST, this.request.getAuthority());
	}

	/**
	 * <p>
	 * Disposes the exchange.
	 * </p>
	 * 
	 * @param deep true to also dispose subsequent exchanges, false otherwise.
	 */
	public void dispose(boolean deep) {
		this.dispose(null, deep);
	}
	
	/**
	 * <p>
	 * Disposes the exchange with the following error.
	 * </p>
	 * 
	 * @param error the error
	 * @param deep true to also dispose subsequent exchanges, false otherwise.
	 */
	public void dispose(Throwable error, boolean deep) {
		this.dispose(error);
		
		if(deep && this.next != null) {
			this.next.dispose(error, deep);
		}
	}
	
	/**
	 * <p>
	 * A data subscriber used to consume request body publisher and send HTTP frames to the remote endpoint.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class DataSubscriber extends BaseSubscriber<ByteBuf> {
		
		private final boolean single;
		
		private ByteBuf singleChunk;
		private boolean many;
		private long transferedLength;
		
		/**
		 * <p>
		 * Creates a data subscriber.
		 * </p>
		 * 
		 * @param single true if the data publisher to consume is a single publisher (i.e. {@link Mono}), false otherwise or if it couldn't be determined.
		 */
		public DataSubscriber(boolean single) {
			this.single = single;
		}

		/**
		 * <p>
		 * Returns the number of bytes that were transfered to the remote endpoint.
		 * </p>
		 * 
		 * @return the number of bytes transfered
		 */
		public long getTransferedLength() {
			return transferedLength;
		}

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			subscription.request(1);
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			Http1xExchange.this.executeInEventLoop(() -> {
				this.transferedLength += value.readableBytes();
				Http1xRequestHeaders headers = Http1xExchange.this.request.headers();

				if( (this.single || !this.many) && this.singleChunk == null) {
					this.singleChunk = value;
					this.request(1);
				}
				else {
					this.many = true;
					ChannelPromise nextPromise = Http1xExchange.this.context.newPromise().addListener(future -> {
						if(future.isSuccess()) {
							this.request(1);
						}
						else if(!this.isDisposed()) {
							this.cancel();
							this.hookOnError(future.cause());
						}
					});

					if(!headers.isWritten()) {
						List<String> transferEncodings = headers.getAll(Headers.NAME_TRANSFER_ENCODING);
						if(headers.getContentLength() == null && !transferEncodings.contains(Headers.VALUE_CHUNKED)) {
							headers.set(Headers.NAME_TRANSFER_ENCODING, Headers.VALUE_CHUNKED);
						}
						if(this.singleChunk != null) {
							Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), Http1xExchange.this.fixHeaders(headers.toHttp1xHeaders()), this.singleChunk), Http1xExchange.this.context.voidPromise());
							Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new DefaultHttpContent(value), nextPromise);
							this.singleChunk = null;
						}
						else {
							Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), Http1xExchange.this.fixHeaders(headers.toHttp1xHeaders()), value), nextPromise);
						}
						headers.setWritten(true);
					}
					else {
						Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new DefaultHttpContent(value), nextPromise);
					}
				}
			})
			.addListener(future -> {
				if(!future.isSuccess() && !this.isDisposed()) {
					this.cancel();
					this.hookOnError(future.cause());
				}
			});
		}

		/**
		 * <p>
		 * This can happens when the exchange is disposed before the request has been fully sent, typically when the server send a complete response before in which case we must try to send a last 
		 * http content to restore the flow.
		 * </p>
		 */
		@Override
		protected void hookOnCancel() {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, LastHttpContent.EMPTY_LAST_CONTENT, Http1xExchange.this.context.voidPromise());
			});
		}
		
		@Override
		protected void hookOnComplete() {
			Http1xExchange.this.executeInEventLoop(() -> {
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

					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatFullHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), Http1xExchange.this.fixHeaders(headers.toHttp1xHeaders()), Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE), finalizePromise);
					headers.setWritten(true);

				}
				else if(this.singleChunk != null) {
					// single
					if(headers.getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
						headers.contentLength(this.transferedLength);
					}
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatFullHttpRequest(Http1xExchange.this.httpVersion, HttpMethod.valueOf(Http1xExchange.this.request.getMethod().name()), Http1xExchange.this.request.getPath(), Http1xExchange.this.fixHeaders(headers.toHttp1xHeaders()), this.singleChunk, EmptyHttpHeaders.INSTANCE), finalizePromise);
					headers.setWritten(true);
				}
				else {
					// many
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, LastHttpContent.EMPTY_LAST_CONTENT, finalizePromise);
				}
			});
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.handler.exchangeError(Http1xExchange.this, throwable);
			});
		}
	}
	
	/**
	 * <p>
	 * A specific data subscriber used to consume request body resource and send {@link FileRegion} to the remote endpoint.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class FileRegionDataSubscriber extends BaseSubscriber<FileRegion> {

		private long transferedLength;

		/**
		 * <p>
		 * Returns the number of bytes that were transfered to the remote endpoint.
		 * </p>
		 * 
		 * @return the number of bytes transfered
		 */
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
		
		/**
		 * <p>
		 * This can happens when the exchange is disposed before the request has been fully sent, typically when the server send a complete response before in which case we must try to send a last 
		 * http content to restore the flow.
		 * </p>
		 */
		@Override
		protected void hookOnCancel() {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, LastHttpContent.EMPTY_LAST_CONTENT, Http1xExchange.this.context.voidPromise());
			});
		}
		
		@Override
		protected void hookOnComplete() {
			// trailers if any should be send here in the last content
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, LastHttpContent.EMPTY_LAST_CONTENT, Http1xExchange.this.context.newPromise().addListener(future -> {
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
