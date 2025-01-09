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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
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

	/* Body data subscription */
	private boolean mono;
	private ByteBuf singleChunk;
	private boolean many;

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
				Publisher<ByteBuf> data = this.body.getData();
				this.mono = data instanceof Mono;
				data.subscribe(this);
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

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		this.disposable = this;
		subscription.request(this.mono ? 1 : Long.MAX_VALUE);
	}

	@Override
	protected void hookOnNext(ByteBuf value) {
		this.transferredLength += value.readableBytes();
		if(this.mono) {
			this.singleChunk = value;
		}
		else if(!this.connectionStream.isReset()) {
			if(!this.many && this.singleChunk == null) {
				this.singleChunk = value;
			}
			else {
				this.many = true;
				if (this.headers.isWritten()) {
					this.connectionStream.writeData(value, 0, false);
				} else {
					this.connectionStream.writeHeaders(this.headers.unwrap(), 0, false);
					this.headers.setWritten();
					this.connectionStream.writeData(Unpooled.wrappedBuffer(this.singleChunk, value), 0, false);
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
		if(!this.connectionStream.isReset()) {
			if(this.mono || !this.many) {
				if(!this.headers.contains(Headers.NAME_CONTENT_LENGTH)) {
					this.headers.contentLength(this.transferredLength);
				}

				if(this.singleChunk == null) {
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
				else {
					if(this.trailers == null) {
						this.connectionStream.writeHeaders(this.headers.unwrap(), 0, false);
						this.headers.setWritten();
						this.connectionStream.writeData(this.singleChunk, 0, true);
					}
					else {
						this.connectionStream.writeHeaders(this.headers.unwrap(), 0, false);
						this.headers.setWritten();
						this.connectionStream.writeData(this.singleChunk, 0, false);
						this.connectionStream.writeHeaders(this.trailers.unwrap(), 0, true);
						this.trailers.setWritten();
					}
					this.singleChunk = null;
				}
			}
			else {
				if(this.trailers == null) {
					this.connectionStream.writeData(Unpooled.EMPTY_BUFFER, 0, true);
				}
				else {
					this.connectionStream.writeHeaders(this.trailers.unwrap(), 0, true);
					this.trailers.setWritten();
				}
			}
		}
		this.connectionStream.onExchangeComplete();
	}

	@Override
	protected void hookOnCancel() {
		if(this.singleChunk != null) {
			this.singleChunk.release();
		}
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		if(!this.connectionStream.isReset()) {
			this.connectionStream.onExchangeError(throwable);
		}
	}
}
