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
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.internal.AbstractResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Http/2 {@link Response} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http2Response extends AbstractResponse<Http2ResponseHeaders, Http2ResponseBody, Http2ResponseTrailers, Http2Response> {

	private final boolean validateHeaders;
	private final Http2ConnectionStream connectionStream;
	private final Http2ResponseBody body;
	
	private Http2ResponseTrailers trailers;

	private Disposable disposable;

	/**
	 * <p>
	 * Creates an Http/2 response.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param validateHeaders    true to validate headers, false otherwise
	 * @param connectionStream   the connection stream
	 * @param head               true to indicate a {@code HEAD} request, false otherwise
	 */
	public Http2Response(HeaderService headerService, ObjectConverter<String> parameterConverter, boolean validateHeaders, Http2ConnectionStream connectionStream, boolean head) {
		super(headerService, parameterConverter, head, new Http2ResponseHeaders(headerService, parameterConverter, validateHeaders));
		this.validateHeaders = validateHeaders;
		this.connectionStream = connectionStream;

		this.body = new Http2ResponseBody(this.headers);
	}

	@Override
	public void send() {
		if(this.connectionStream.executor().inEventLoop()) {
			if(!this.head) {
				this.body.getData().subscribe(this.body.getData() instanceof Mono ? new MonoBodyDataSubscriber() : new BodyDataSubscriber());
			}
			else {
				if(this.trailers == null) {
					this.connectionStream.writeHeaders(this.headers.unwrap(), 0, true);
					this.headers.setWritten();
				}
				else {
					this.connectionStream.writeHeaders(this.headers.unwrap(), 0, false);
					this.headers.setWritten();
					this.connectionStream.writeHeaders(this.trailers.unwrap(), 0, true);
					this.trailers.setWritten();
				}
			}
		}
		else {
			this.connectionStream.executor().execute(this::send);
		}
	}

	/**
	 * <p>
	 * Disposes the response.
	 * </p>
	 * 
	 * <p>
	 * This method cancels any active subscription.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	final void dispose(Throwable cause) {
		if(this.disposable != null) {
			this.disposable.dispose();
			this.disposable = null;
		}
	}

	@Override
	public Http2Response sendContinue() throws IllegalStateException {
		if(this.isHeadersWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		if(this.connectionStream.executor().inEventLoop()) {
			// we might have an issue here if this run outside the event loop
			this.connectionStream.writeHeaders(new DefaultHttp2Headers().status("100"), 0, false);
		}
		else {
			this.connectionStream.executor().execute(this::sendContinue);
		}
		return this;
	}

	@Override
	public Http2ResponseBody body() {
		return this.body;
	}
	
	@Override
	public Http2ResponseTrailers trailers() {
		if(this.trailers == null) {
			this.trailers = new Http2ResponseTrailers(this.headerService, this.parameterConverter, this.validateHeaders);
		}
		return this.trailers;
	}

	/**
	 * <p>
	 * The response body data publisher optimized for {@link Mono} publisher that writes response objects to the connection.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class MonoBodyDataSubscriber extends BaseSubscriber<ByteBuf> {

		private ByteBuf data;

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http2Response.this.disposable = this;
			subscription.request(1);
		}

		@Override
		protected void hookOnNext(ByteBuf value) {
			Http2Response.this.transferredLength += value.readableBytes();
			this.data = value;
		}

		@Override
		protected void hookOnComplete() {
			if(!Http2Response.this.connectionStream.isReset()) {
				if(!Http2Response.this.headers.contains(Headers.NAME_CONTENT_LENGTH)) {
					Http2Response.this.headers.contentLength(Http2Response.this.transferredLength);
				}

				if(this.data == null) {
					if(Http2Response.this.trailers == null) {
						Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, true);
						Http2Response.this.headers.setWritten();
					}
					else {
						Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, false);
						Http2Response.this.headers.setWritten();
						Http2Response.this.connectionStream.writeHeaders(Http2Response.this.trailers.unwrap(), 0, true);
						Http2Response.this.trailers.setWritten();
					}
				}
				else {
					if(Http2Response.this.trailers == null) {
						Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, false);
						Http2Response.this.headers.setWritten();
						Http2Response.this.connectionStream.writeData(this.data, 0, true);
					}
					else {
						Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, false);
						Http2Response.this.headers.setWritten();
						Http2Response.this.connectionStream.writeData(this.data, 0, false);
						Http2Response.this.connectionStream.writeHeaders(Http2Response.this.trailers.unwrap(), 0, true);
						Http2Response.this.trailers.setWritten();
					}
				}
			}
			Http2Response.this.connectionStream.onExchangeComplete();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			if(!Http2Response.this.connectionStream.isReset()) {
				Http2Response.this.connectionStream.onExchangeError(throwable);
			}
		}
	}

	/**
	 * <p>
	 * The response body data subscriber that writes response objects to the connection.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class BodyDataSubscriber extends BaseSubscriber<ByteBuf> {

		private ByteBuf singleChunk;
		private boolean many;

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http2Response.this.disposable = this;
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		protected void hookOnNext(ByteBuf value) {
			if(!Http2Response.this.connectionStream.isReset()) {
				Http2Response.this.transferredLength += value.readableBytes();
				if(!this.many && this.singleChunk == null) {
					this.singleChunk = value;
				}
				else {
					this.many = true;
					if (Http2Response.this.headers.isWritten()) {
						Http2Response.this.connectionStream.writeData(value, 0, false);
					} else {
						Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, false);
						Http2Response.this.headers.setWritten();
						Http2Response.this.connectionStream.writeData(Unpooled.wrappedBuffer(this.singleChunk, value), 0, false);
						this.singleChunk = null;
					}
				}
			}
			
			// TODO implement back pressure with the flow controller
			/*this.encoder.flowController().listener(new Listener() {
				
				@Override
				public void writabilityChanged(Http2Stream stream) {
					
				}
			});*/
		}

		@Override
		protected void hookOnComplete() {
			if(!Http2Response.this.connectionStream.isReset()) {
				if (this.many) {
					if(Http2Response.this.trailers == null) {
						Http2Response.this.connectionStream.writeData(Unpooled.EMPTY_BUFFER, 0, true);
					}
					else {
						Http2Response.this.connectionStream.writeHeaders(Http2Response.this.trailers.unwrap(), 0, true);
						Http2Response.this.trailers.setWritten();
					}
				}
				else {
					if(!Http2Response.this.headers.contains(Headers.NAME_CONTENT_LENGTH)) {
						Http2Response.this.headers.contentLength(Http2Response.this.transferredLength);
					}

					if(this.singleChunk == null) {
						if(Http2Response.this.trailers == null) {
							Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, true);
							Http2Response.this.headers.setWritten();
						}
						else {
							Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, false);
							Http2Response.this.headers.setWritten();
							Http2Response.this.connectionStream.writeHeaders(Http2Response.this.trailers.unwrap(), 0, true);
							Http2Response.this.trailers.setWritten();
						}
					}
					else {
						if(Http2Response.this.trailers == null) {
							Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, false);
							Http2Response.this.headers.setWritten();
							Http2Response.this.connectionStream.writeData(this.singleChunk, 0, true);
						}
						else {
							Http2Response.this.connectionStream.writeHeaders(Http2Response.this.headers.unwrap(), 0, false);
							Http2Response.this.headers.setWritten();
							Http2Response.this.connectionStream.writeData(this.singleChunk, 0, false);
							Http2Response.this.connectionStream.writeHeaders(Http2Response.this.trailers.unwrap(), 0, true);
							Http2Response.this.trailers.setWritten();
						}
					}
				}
			}
			Http2Response.this.connectionStream.onExchangeComplete();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			if(!Http2Response.this.connectionStream.isReset()) {
				Http2Response.this.connectionStream.onExchangeError(throwable);
			}
		}
	}
}
