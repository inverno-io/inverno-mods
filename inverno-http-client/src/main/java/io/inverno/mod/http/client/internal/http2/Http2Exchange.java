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
import java.util.List;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.MonoSink;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
class Http2Exchange extends AbstractHttp2Exchange {

	private final Endpoint<Http2LocalFlowController> localEndpoint;
	private final Http2ConnectionEncoder encoder;
	
	protected Http2Stream stream;
	
	public Http2Exchange(
			ChannelHandlerContext context, 
			MonoSink<Exchange<ExchangeContext>> exchangeSink,
			ExchangeContext exchangeContext, 
			Http2Request request, 
			Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer, 
			Endpoint<Http2LocalFlowController> localEndpoint, 
			Http2ConnectionEncoder encoder) {
		super(context, exchangeSink, exchangeContext, request, responseBodyTransformer);
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
		this.request.body().ifPresentOrElse(
			body -> {
				Http2Exchange.DataSubscriber dataSubscriber = new Http2Exchange.DataSubscriber(body.isSingle());
				body.dataSubscribe(dataSubscriber);
				this.disposable = dataSubscriber;
			},
			() -> {
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
				Http2Exchange.this.encoder.writeHeaders(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Http2Exchange.this.fixHeaders(headers.toHttp2Headers()), 0, true, finalizePromise);
				headers.setWritten(true);
				this.context.channel().flush();
			}	
		);
	}
	
	private Http2Headers fixHeaders(Http2Headers headers) {
		return headers
			.method(this.request.getMethod().name())
			.scheme(this.request.getScheme())
			.authority(this.request.getAuthority())
			.path(this.request.getPath());
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
			Http2RequestHeaders headers = ((Http2Request)Http2Exchange.this.request).headers();

			if( (this.single || !this.many) && this.singleChunk == null) {
				this.singleChunk = value;
			}
			else {
				this.many = true;
				Http2Exchange.this.executeInEventLoop(() -> {
					if(!headers.isWritten()) {
						List<String> transferEncodings = headers.getAll(Headers.NAME_TRANSFER_ENCODING);
						if(headers.getContentLength() == null && !transferEncodings.contains(Headers.VALUE_CHUNKED)) {
							headers.set(Headers.NAME_TRANSFER_ENCODING, Headers.VALUE_CHUNKED);
						}
						Http2Exchange.this.encoder.writeHeaders(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Http2Exchange.this.fixHeaders(headers.toHttp2Headers()), 0, false, Http2Exchange.this.context.voidPromise());
						headers.setWritten(true);
						if(this.singleChunk != null) {
							Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), this.singleChunk, 0, false, Http2Exchange.this.context.voidPromise());
							this.singleChunk = null;
						}
					}
					else {
						Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), value, 0, false, Http2Exchange.this.context.voidPromise());
					}
					Http2Exchange.this.context.channel().flush();
				});
			}
		}
		
		@Override
		protected void hookOnComplete() {
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
				Http2Exchange.this.executeInEventLoop(() -> {
					Http2Exchange.this.encoder.writeHeaders(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Http2Exchange.this.fixHeaders(headers.toHttp2Headers()), 0, true, finalizePromise);
					headers.setWritten(true);
				});
			}
			else if(this.singleChunk != null) {
				// single
				if(headers.getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
					headers.contentLength(this.transferedLength);
				}
				Http2Exchange.this.executeInEventLoop(() -> {
					Http2Exchange.this.encoder.writeHeaders(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Http2Exchange.this.fixHeaders(headers.toHttp2Headers()), 0, false, Http2Exchange.this.context.voidPromise());
					Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), this.singleChunk, 0, true, finalizePromise);
					headers.setWritten(true);
				});
			}
			else {
				// many
				Http2Exchange.this.executeInEventLoop(() -> {
					Http2Exchange.this.encoder.writeData(Http2Exchange.this.context, Http2Exchange.this.stream.id(), Unpooled.EMPTY_BUFFER, 0, true, finalizePromise);
				});
			}
			Http2Exchange.this.context.channel().flush();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http2Exchange.this.executeInEventLoop(() -> {
				Http2Exchange.this.handler.exchangeError(Http2Exchange.this, throwable);
			});
		}
	}
}
