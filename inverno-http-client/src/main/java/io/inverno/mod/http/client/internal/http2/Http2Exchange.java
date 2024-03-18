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
package io.inverno.mod.http.client.internal.http2;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Connection.Endpoint;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2LocalFlowController;
import io.netty.handler.codec.http2.Http2Stream;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.MonoSink;

/**
 * <p>
 * HTTP/2 {@link Exchange} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http2Exchange extends AbstractHttp2Exchange {

	private final Endpoint<Http2LocalFlowController> localEndpoint;
	private final Http2ConnectionEncoder encoder;
	
	protected Http2Stream stream;
	
	/**
	 * <p>
	 * Creates an HTTP/2 exchange.
	 * </p>
	 * 
	 * @param context                 the channel context
	 * @param exchangeSink            the exchange sink
	 * @param exchangeContext         the exchange context
	 * @param request                 the HTTP/2 request
	 * @param localEndpoint           the local HTTP/2 endpoint
	 * @param encoder                 the HTTP/2 connection encoder
	 */
	public Http2Exchange(
			ChannelHandlerContext context, 
			MonoSink<HttpConnectionExchange<ExchangeContext, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> exchangeSink,
			ExchangeContext exchangeContext, 
			Http2Request request, 
			Endpoint<Http2LocalFlowController> localEndpoint, 
			Http2ConnectionEncoder encoder) {
		super(context, exchangeSink, exchangeContext, request);
		this.localEndpoint = localEndpoint;
		this.encoder = encoder;
	}

	@Override
	public Http2Stream getStream() {
		return stream;
	}

	@Override
	public Http2Request request() {
		return (Http2Request)super.request();
	}
	
	// this is executed in event loop
	@Override
	protected void doStart() throws HttpClientException {
		// Create the stream
		try {
			int streamId = this.localEndpoint.lastStreamCreated();
			if(streamId == 0) {
				streamId = 1;
			}
			else {
				streamId += 2;
			}
			this.stream = this.localEndpoint.createStream(streamId, false);
		}
		catch(Http2Exception e) {
			throw new HttpClientException(e);
		}
		
		this.handler.exchangeStart(this);
		
		if(this.request.body() == null) {
			// no need to subscribe
			Http2RequestHeaders headers = ((Http2Request)this.request).headers();
			ChannelPromise finalizePromise = this.context.newPromise();
			finalizePromise.addListener(future -> {
				if(future.isSuccess()) {
					this.handler.requestComplete(this);
				}
				else {
					this.handler.exchangeError(this, future.cause());
				}
			});
			Http2Exchange.this.encoder.writeHeaders(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Http2Exchange.this.fixHeaders(headers.getUnderlyingHeaders()), 0, true, finalizePromise);
			headers.setWritten(true);
			this.context.channel().flush();
		}
		else {
			Http2Exchange.DataSubscriber dataSubscriber = new Http2Exchange.DataSubscriber(this.request.body().isSingle());
			this.request.body().dataSubscribe(dataSubscriber);
			this.disposable = dataSubscriber;
		}
	}
	
	/**
	 * <p>
	 * Sets required headers (e.g. {@code :method}, {@code :scheme}, {@code :authority}, {@code :path}...).
	 * </p>
	 * 
	 * @param headers the request HTTP/2 headers
	 * 
	 * @return the HTTP headers
	 */
	private Http2Headers fixHeaders(Http2Headers headers) {
		return headers
			.method(this.request.getMethod().name())
			.scheme(this.request.getScheme())
			.authority(this.request.getAuthority())
			.path(this.request.getPath());
	}
	
	/**
	 * <p>
	 * A data subscriber used to consume request body publisher and send HTTP/2 frames to the remote endpoint.
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
		
		public DataSubscriber(boolean single) {
			this.single = single;
		}

		public long getTransferedLength() {
			return transferedLength;
		}
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			subscription.request(1);
		}

		@Override
		protected void hookOnNext(ByteBuf value) {
			Http2Exchange.this.executeInEventLoop(() -> {
				this.transferedLength += value.readableBytes();
				Http2RequestHeaders headers = ((Http2Request)Http2Exchange.this.request).headers();

				if( (this.single || !this.many) && this.singleChunk == null) {
					this.singleChunk = value;
					this.request(1);
				}
				else {
					this.many = true;
					ChannelPromise nextPromise = Http2Exchange.this.context.newPromise().addListener(future -> {
						if(future.isSuccess()) {
							this.request(1);
						}
						else if(!this.isDisposed()) {
							this.cancel();
							this.hookOnError(future.cause());
						}
					});
					if(!headers.isWritten()) {
						Http2Exchange.this.encoder.writeHeaders(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Http2Exchange.this.fixHeaders(headers.getUnderlyingHeaders()), 0, false, Http2Exchange.this.context.voidPromise());
						headers.setWritten(true);
						if(this.singleChunk != null) {
							Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), this.singleChunk, 0, false, Http2Exchange.this.context.voidPromise());
							this.singleChunk = null;
						}
						Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), value, 0, false, nextPromise);
					}
					else {
						Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), value, 0, false, nextPromise);
					}
					Http2Exchange.this.context.channel().flush();
				}
			})
			.addListener(future -> {
				if(!future.isSuccess() && !this.isDisposed()) {
					this.cancel();
					this.hookOnError(future.cause());
				}
			});
		}
		
		@Override
		protected void hookOnComplete() {
			Http2Exchange.this.executeInEventLoop(() -> {
				// trailers if any should be send here in the last content
				Http2RequestHeaders headers = ((Http2Request)Http2Exchange.this.request).headers();
				ChannelPromise finalizePromise = Http2Exchange.this.context.newPromise();
				finalizePromise.addListener(future -> {
					if(future.isSuccess()) {
						Http2Exchange.this.handler.requestComplete(Http2Exchange.this);
					}
					else {
						Http2Exchange.this.handler.exchangeError(Http2Exchange.this, future.cause());
					}
				});
				if(this.transferedLength == 0) {
					// empty response
					if(headers.getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
						headers.contentLength(0);
					}
					Http2Exchange.this.encoder.writeHeaders(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Http2Exchange.this.fixHeaders(headers.getUnderlyingHeaders()), 0, true, finalizePromise);
					headers.setWritten(true);
				}
				else if(this.singleChunk != null) {
					// single
					if(headers.getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
						headers.contentLength(this.transferedLength);
					}
					Http2Exchange.this.encoder.writeHeaders(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Http2Exchange.this.fixHeaders(headers.getUnderlyingHeaders()), 0, false, Http2Exchange.this.context.voidPromise());
					Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), this.singleChunk, 0, true, finalizePromise);
					headers.setWritten(true);
				}
				else {
					// many
					Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Unpooled.EMPTY_BUFFER, 0, true, finalizePromise);
				}
				Http2Exchange.this.context.channel().flush();
			});
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http2Exchange.this.executeInEventLoop(() -> {
				Http2Exchange.this.handler.exchangeError(Http2Exchange.this, throwable);
			});
		}
	}
}
