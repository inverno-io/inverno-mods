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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.inverno.mod.http.base.internal.netty.FlatFullHttpResponse;
import io.inverno.mod.http.base.internal.netty.FlatHttpResponse;
import io.inverno.mod.http.base.internal.netty.FlatLastHttpContent;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.internal.AbstractResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import java.nio.charset.Charset;
import java.util.List;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Http/1.x {@link Response} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http1xResponse extends AbstractResponse<Http1xResponseHeaders, Http1xResponseBody, Http1xResponseTrailers, Http1xResponse> {
	
	private final HeadersValidator headersValidator;
	private final Http1xConnection connection;
	private final HttpVersion version;
	private final Http1xResponseBody body;
	
	private Http1xResponseTrailers trailers;
	
	private Disposable disposable;

	/* Body data subscription */
	private boolean mono;
	private HttpResponseStatus httpStatus;
	private HttpHeaders httpTrailers;
	private ByteBuf singleChunk;
	private boolean many;
	private boolean sse;
	private Charset charset;

	/**
	 * <p>
	 * Creates an Http/1.x response.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headersValidator   the headers validator
	 * @param connection         the Http/1.x connection
	 * @param version            the HTTP version
	 * @param head               true to indicate a {@code HEAD} request, false otherwise
	 */
	public Http1xResponse(
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			HeadersValidator headersValidator, 
			Http1xConnection connection, 
			HttpVersion version, 
			boolean head
		) {
		super(headerService, parameterConverter, head, new Http1xResponseHeaders(headerService, parameterConverter, headersValidator));
		this.headersValidator = headersValidator;
		this.connection = connection;
		this.version = version;
		this.body = new Http1xResponseBody(this.headers, connection.supportsFileRegion());
	}
	
	/**
	 * <p>
	 * Sends the response.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop, it subscribes to the response body file region publisher when present and to the response body data publisher otherwise in order to generate 
	 * and send the response body. In case of an {@code HEAD} request, an empty response with headers only is sent.
	 * </p>
	 */
	@Override
	public void send() {
		if(this.connection.executor().inEventLoop()) {
			if(!this.head) {
				if(this.body.getFileRegionData() == null) {
					Publisher<ByteBuf> data = this.body.getData();
					this.mono = data instanceof Mono;
					data.subscribe(this);
				}
				else {
					Flux.concat(this.body.getFileRegionData(), Flux.from(this.body.getData()).cast(FileRegion.class)).subscribe(new FileRegionBodyDataSubscriber());
				}
			}
			else {
				this.headers.remove(HttpHeaderNames.TRANSFER_ENCODING);

				final HttpResponseStatus httpStatus = HttpResponseStatus.valueOf(this.headers.getStatusCode());
				final LinkedHttpHeaders httpTrailers;
				if(httpStatus == HttpResponseStatus.NOT_MODIFIED || httpStatus == HttpResponseStatus.NO_CONTENT) {
					httpTrailers = null;
				}
				else {
					httpTrailers = this.trailers != null ? this.trailers.unwrap() : null;
					if(httpTrailers != null) {
						this.headers.set(HttpHeaderNames.TRAILER, String.join(", ", httpTrailers.names()));
					}
				}

				this.connection.writeHttpObject(new FlatFullHttpResponse(this.version, httpStatus, this.headers.unwrap(), Unpooled.EMPTY_BUFFER, httpTrailers));
				if(httpTrailers != null) {
					this.trailers.setWritten();
				}
				this.connection.onExchangeComplete();
			}
		}
		else {
			this.connection.executor().execute(this::send);
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
	public Http1xResponse sendContinue() throws IllegalStateException {
		if(this.isHeadersWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		if(this.connection.executor().inEventLoop()) {
			this.connection.writeHttpObject(new DefaultFullHttpResponse(this.version, HttpResponseStatus.CONTINUE));
		}
		else {
			this.connection.executor().execute(this::sendContinue);
		}
		return this;
	}

	@Override
	public Http1xResponseBody body() {
		return this.body;
	}
	
	@Override
	public Http1xResponseTrailers trailers() {
		if(this.trailers == null) {
			this.trailers = new Http1xResponseTrailers(this.headerService, this.parameterConverter, this.headersValidator);
		}
		return this.trailers;
	}

	private void sanitizeResponse() {
		HttpHeaders httpTrailers = EmptyHttpHeaders.INSTANCE;
		switch(Http1xResponse.this.headers.getStatusCode()) {
			case 204:
			case 304: {
				Http1xResponse.this.headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
				break;
			}
			default: {
				if(!Http1xResponse.this.headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
					List<String> transferEncodings = Http1xResponse.this.headers.getAll(HttpHeaderNames.TRANSFER_ENCODING);
					if(!this.many && (transferEncodings.isEmpty() || !transferEncodings.getLast().endsWith(Headers.VALUE_CHUNKED))) {
						Http1xResponse.this.headers.set(HttpHeaderNames.CONTENT_LENGTH, "" + Http1xResponse.this.transferredLength);
					}
					else {
						if(transferEncodings.isEmpty()) {
							Http1xResponse.this.headers.add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
						}
						Http1xResponse.this.headers.get(HttpHeaderNames.CONTENT_TYPE)
							.ifPresent(contentType -> this.sse = contentType.regionMatches(true, 0, MediaTypes.TEXT_EVENT_STREAM, 0, MediaTypes.TEXT_EVENT_STREAM.length()));
					}
				}

				if(Http1xResponse.this.trailers != null) {
					httpTrailers = Http1xResponse.this.trailers.unwrap();
					Http1xResponse.this.headers.set(HttpHeaderNames.TRAILER, String.join(", ", httpTrailers.names()));
				}
			}
		}
		this.httpTrailers = httpTrailers;
		this.httpStatus = HttpResponseStatus.valueOf(Http1xResponse.this.headers.getStatusCode());
	}

	private Charset getCharset() {
		if(Http1xResponse.this.isHeadersWritten()) {
			this.charset = Http1xResponse.this.headers.<Headers.ContentType>getHeader(HttpHeaderNames.CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		if(this.charset == null) {
			return Http1xResponse.this.headers.<Headers.ContentType>getHeader(HttpHeaderNames.CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		return this.charset;
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		Http1xResponse.this.disposable = this;
		subscription.request(this.mono ? 1 : Long.MAX_VALUE);
	}

	@Override
	protected void hookOnNext(ByteBuf value) {
		Http1xResponse.this.transferredLength += value.readableBytes();
		if(this.mono || (!this.many && this.singleChunk == null)) {
			this.singleChunk = value;
		}
		else {
			this.many = true;
			if(!Http1xResponse.this.headers.isWritten()) {
				this.sanitizeResponse();
				if(this.sse) {
					ByteBuf chunked_header = Unpooled.copiedBuffer(Integer.toHexString(this.singleChunk.readableBytes()) + "\r\n", Charsets.orDefault(this.getCharset()));
					ByteBuf chunked_trailer = Unpooled.copiedBuffer("\r\n", Charsets.orDefault(this.getCharset()));
					Http1xResponse.this.connection.writeHttpObject(new FlatHttpResponse(Http1xResponse.this.version, this.httpStatus, Http1xResponse.this.headers.unwrap(), Unpooled.wrappedBuffer(chunked_header, this.singleChunk, chunked_trailer)));
				}
				else {
					Http1xResponse.this.connection.writeHttpObject(new FlatHttpResponse(Http1xResponse.this.version, this.httpStatus, Http1xResponse.this.headers.unwrap(), this.singleChunk));
				}
				this.singleChunk = null;
				Http1xResponse.this.headers.setWritten();
			}

			if(this.sse) {
				ByteBuf chunked_header = Unpooled.copiedBuffer(Integer.toHexString(value.readableBytes()) + "\r\n", Charsets.orDefault(this.getCharset()));
				ByteBuf chunked_trailer = Unpooled.copiedBuffer("\r\n", Charsets.orDefault(this.getCharset()));
				Http1xResponse.this.connection.writeHttpObject(new DefaultHttpContent(Unpooled.wrappedBuffer(chunked_header, value, chunked_trailer)));
			}
			else {
				Http1xResponse.this.connection.writeHttpObject(new DefaultHttpContent(value));
			}
		}
	}

	@Override
	protected void hookOnComplete() {
		if(this.mono || !this.many) {
			this.sanitizeResponse();
			Http1xResponse.this.connection.writeHttpObject(new FlatFullHttpResponse(Http1xResponse.this.version, this.httpStatus, Http1xResponse.this.headers.unwrap(), this.singleChunk != null ? this.singleChunk : Unpooled.EMPTY_BUFFER, httpTrailers));
			Http1xResponse.this.headers.setWritten();
			if(Http1xResponse.this.trailers != null) {
				Http1xResponse.this.trailers.setWritten();
			}
		}
		else {
			if(Http1xResponse.this.trailers == null) {
				Http1xResponse.this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
			}
			else {
				Http1xResponse.this.connection.writeHttpObject(new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, this.httpTrailers));
				Http1xResponse.this.trailers.setWritten();
			}
		}
		Http1xResponse.this.connection.onExchangeComplete();
	}

	@Override
	protected void hookOnCancel() {
		if(this.singleChunk != null) {
			this.singleChunk.release();
		}
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		Http1xResponse.this.connection.onExchangeError(throwable);
	}

	/**
	 * <p>
	 * The file region response body data publisher that writes response file regions to the connection.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class FileRegionBodyDataSubscriber extends BaseSubscriber<FileRegion> {
		
		private HttpHeaders httpTrailers;
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			final HttpResponseStatus httpStatus = HttpResponseStatus.valueOf(Http1xResponse.this.headers.getStatusCode());			
			if(httpStatus == HttpResponseStatus.NOT_MODIFIED || httpStatus == HttpResponseStatus.NO_CONTENT) {
				Http1xResponse.this.headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
				this.httpTrailers = null;
			}
			else if(Http1xResponse.this.trailers == null) {
				this.httpTrailers = EmptyHttpHeaders.INSTANCE;
			}
			else {
				this.httpTrailers = Http1xResponse.this.trailers.unwrap();
				Http1xResponse.this.headers.set(HttpHeaderNames.TRAILER, String.join(", ", this.httpTrailers.names()));
			}
			
			Http1xResponse.this.connection.writeHttpObject(new FlatHttpResponse(Http1xResponse.this.version, httpStatus, Http1xResponse.this.headers.unwrap(), Unpooled.EMPTY_BUFFER));
			Http1xResponse.this.headers.setWritten();
			subscription.request(1);
		}

		@Override
		protected void hookOnNext(FileRegion value) {
			Http1xResponse.this.transferredLength += (int)value.count();
			Http1xResponse.this.connection.writeFileRegion(value, Http1xResponse.this.connection.newPromise().addListener(future -> {
				if(future.isSuccess()) {
					this.request(1);
				}
				else {
					Http1xResponse.this.connection.onExchangeError(future.cause());
					// this should result in the connection to be shutdown since we have sent headers with a partial body
				}
			}));
		}
		
		@Override
		protected void hookOnComplete() {
			if(Http1xResponse.this.trailers == null) {
				Http1xResponse.this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
			}
			else {
				Http1xResponse.this.connection.writeHttpObject(new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, this.httpTrailers));
				Http1xResponse.this.trailers.setWritten();
			}
			
			Http1xResponse.this.connection.onExchangeComplete();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xResponse.this.connection.onExchangeError(throwable);
		}
	}
}
