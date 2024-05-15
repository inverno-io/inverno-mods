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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
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
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http1xResponse extends AbstractResponse<Http1xResponseHeaders, Http1xResponseBody, Http1xResponseTrailers, Http1xResponse> {
	
	private final HeadersValidator headersValidator;
	private final Http1xConnection connection;
	private final HttpVersion version;
	private final Http1xResponseBody body;
	
	private Http1xResponseTrailers trailers;
	
	private Disposable disposable;

	/**
	 * <p>
	 * Creates an Http/1.x response.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headersValidator   the headers validator
	 * @param connection         the Http/1.x connection
	 * @param version            the Http version
	 * @param head               true to indicate a {@code HEAD} request, false otherwise
	 * @param keepAlive          true to indicate the connection is keepAlive, false otherwise
	 */
	public Http1xResponse(
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			HeadersValidator headersValidator, 
			Http1xConnection connection, 
			HttpVersion version, 
			boolean head, 
			boolean keepAlive
		) {
		super(headerService, parameterConverter, head, new Http1xResponseHeaders(headerService, parameterConverter, headersValidator));
		this.headersValidator = headersValidator;
		this.connection = connection;
		this.version = version;
		
		if(version == io.netty.handler.codec.http.HttpVersion.HTTP_1_0) {
			if(keepAlive) {
				this.headers.set((CharSequence)Headers.NAME_CONNECTION, (CharSequence)Headers.VALUE_KEEP_ALIVE);
			}
		}
		else if(!keepAlive) {
			this.headers.set((CharSequence)Headers.NAME_CONNECTION, (CharSequence)Headers.VALUE_CLOSE);
		}
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
					this.body.getData().subscribe(this.body.getData() instanceof Mono ? new MonoBodyDataSubscriber() : new BodyDataSubscriber());
				}
				else {
					Flux.concat(this.body.getFileRegionData(), Flux.from(this.body.getData()).cast(FileRegion.class)).subscribe(new FileRegionBodyDataSubscriber());
				}
			}
			else {
				this.headers.remove((CharSequence)Headers.NAME_TRANSFER_ENCODING);

				final HttpResponseStatus httpStatus = HttpResponseStatus.valueOf(this.headers.getStatusCode());
				final LinkedHttpHeaders httpTrailers;
				if(httpStatus == HttpResponseStatus.NOT_MODIFIED || httpStatus == HttpResponseStatus.NO_CONTENT) {
					httpTrailers = null;
				}
				else {
					httpTrailers = this.trailers != null ? this.trailers.unwrap() : null;
					if(httpTrailers != null) {
						this.headers.set(Headers.NAME_TRAILER, httpTrailers.names().stream().collect(Collectors.joining(", ")));
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
	
	@Override
	public void dispose(Throwable cause) {
		if(this.disposable != null) {
			this.disposable.dispose();
		}
	}
	
	@Override
	public Http1xResponse sendContinue() {
		this.connection.writeHttpObject(new DefaultFullHttpResponse(this.version, HttpResponseStatus.CONTINUE));
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
	
	/**
	 * <p>
	 * The response body data publisher optimized for {@link Mono} publisher that writes a single response object to the connection.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class MonoBodyDataSubscriber extends BaseSubscriber<ByteBuf> {
		
		private ByteBuf data;
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http1xResponse.this.disposable = this;
			subscription.request(1);
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			Http1xResponse.this.transferedLength += value.readableBytes();
			this.data = value;
		}

		@Override
		protected void hookOnComplete() {
			final HttpResponseStatus httpStatus = HttpResponseStatus.valueOf(Http1xResponse.this.headers.getStatusCode());
			final HttpHeaders httpTrailers;
			if(httpStatus == HttpResponseStatus.NOT_MODIFIED || httpStatus == HttpResponseStatus.NO_CONTENT) {
				Http1xResponse.this.headers.remove((CharSequence)Headers.NAME_TRANSFER_ENCODING);
				httpTrailers = null;
			}
			else {
				if(!Http1xResponse.this.headers.contains((CharSequence)Headers.NAME_CONTENT_LENGTH)) {
					Http1xResponse.this.headers.set((CharSequence)Headers.NAME_CONTENT_LENGTH, "" + Http1xResponse.this.transferedLength);
				}
				
				if(Http1xResponse.this.trailers == null) {
					httpTrailers = EmptyHttpHeaders.INSTANCE;
				}
				else {
					httpTrailers = Http1xResponse.this.trailers.unwrap();
					Http1xResponse.this.headers.set(Headers.NAME_TRAILER, httpTrailers.names().stream().collect(Collectors.joining(", ")));
				}
			}
			
			if(this.data == null) {
				Http1xResponse.this.connection.writeHttpObject(new FlatFullHttpResponse(Http1xResponse.this.version, httpStatus, Http1xResponse.this.headers.unwrap(), Unpooled.EMPTY_BUFFER, httpTrailers));
			}
			else {
				Http1xResponse.this.connection.writeHttpObject(new FlatFullHttpResponse(Http1xResponse.this.version, httpStatus, Http1xResponse.this.headers.unwrap(), this.data, httpTrailers));
			}
			Http1xResponse.this.headers.setWritten();
			if(Http1xResponse.this.trailers != null) {
				Http1xResponse.this.trailers.setWritten();
			}
			
			// we need this to start the next exchange
			Http1xResponse.this.connection.onExchangeComplete();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xResponse.this.connection.onExchangeError(throwable);
		}
	}
	
	/**
	 * <p>
	 * The response body data subscriber that writes response objects to the connection.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class BodyDataSubscriber extends BaseSubscriber<ByteBuf> {
		
		private HttpResponseStatus httpStatus;
		private HttpHeaders httpTrailers;
		
		private ByteBuf singleChunk;
		private boolean many;
		private boolean sse;
		private Charset charset;
		
		private void sanitizeResponse() {
			this.httpStatus = HttpResponseStatus.valueOf(Http1xResponse.this.headers.getStatusCode());
			if(this.httpStatus == HttpResponseStatus.NOT_MODIFIED || this.httpStatus == HttpResponseStatus.NO_CONTENT) {
				Http1xResponse.this.headers.remove((CharSequence)Headers.NAME_TRANSFER_ENCODING);
				this.httpTrailers = null;
			}
			else {
				if(!Http1xResponse.this.headers.contains((CharSequence)Headers.NAME_CONTENT_LENGTH)) {
					List<String> transferEncodings = Http1xResponse.this.headers.getAll((CharSequence)Headers.NAME_TRANSFER_ENCODING);
					if(!this.many && (transferEncodings.isEmpty() || !transferEncodings.getLast().endsWith(Headers.VALUE_CHUNKED))) {
						Http1xResponse.this.headers.set((CharSequence)Headers.NAME_CONTENT_LENGTH, "" + Http1xResponse.this.transferedLength);
					}
					else {
						if(transferEncodings.isEmpty()) {
							Http1xResponse.this.headers.add((CharSequence)Headers.NAME_TRANSFER_ENCODING, (CharSequence)Headers.VALUE_CHUNKED);
						}
						Http1xResponse.this.headers.get((CharSequence)Headers.NAME_CONTENT_TYPE)
							.ifPresent(contentType -> this.sse = contentType.regionMatches(true, 0, MediaTypes.TEXT_EVENT_STREAM, 0, MediaTypes.TEXT_EVENT_STREAM.length()));
					}
				}
				
				if(Http1xResponse.this.trailers == null) {
					this.httpTrailers = EmptyHttpHeaders.INSTANCE;
				}
				else {
					this.httpTrailers = Http1xResponse.this.trailers.unwrap();
					Http1xResponse.this.headers().set(Headers.NAME_TRAILER, this.httpTrailers.names().stream().collect(Collectors.joining(", ")));
				}
			}
		}
		
		private Charset getCharset() {
			if(Http1xResponse.this.isHeadersWritten()) {
				this.charset = Http1xResponse.this.headers.<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
			}
			if(this.charset == null) {
				return Http1xResponse.this.headers.<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
			}
			return this.charset;
		}
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http1xResponse.this.disposable = this;
			subscription.request(Long.MAX_VALUE);
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			Http1xResponse.this.transferedLength += value.readableBytes();
			if(!this.many && this.singleChunk == null) {
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
			if(this.many) {
				if(Http1xResponse.this.trailers == null) {
					Http1xResponse.this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
				}
				else {
					Http1xResponse.this.connection.writeHttpObject(new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, this.httpTrailers));
					Http1xResponse.this.trailers.setWritten();
				}
			}
			else {
				this.sanitizeResponse();
				if(this.singleChunk == null) {
					Http1xResponse.this.connection.writeHttpObject(new FlatFullHttpResponse(Http1xResponse.this.version, this.httpStatus, Http1xResponse.this.headers.unwrap(), Unpooled.EMPTY_BUFFER, this.httpTrailers));
				}
				else {
					Http1xResponse.this.connection.writeHttpObject(new FlatFullHttpResponse(Http1xResponse.this.version, this.httpStatus, Http1xResponse.this.headers.unwrap(), this.singleChunk, this.httpTrailers));
					this.singleChunk = null;
				}
				Http1xResponse.this.headers.setWritten();
				if(Http1xResponse.this.trailers != null) {
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
	}
	
	/**
	 * <p>
	 * The file region response body data publisher that writes response file regions to the connection.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class FileRegionBodyDataSubscriber extends BaseSubscriber<FileRegion> {
		
		private HttpHeaders httpTrailers;
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			final HttpResponseStatus httpStatus = HttpResponseStatus.valueOf(Http1xResponse.this.headers.getStatusCode());			
			if(httpStatus == HttpResponseStatus.NOT_MODIFIED || httpStatus == HttpResponseStatus.NO_CONTENT) {
				Http1xResponse.this.headers.remove((CharSequence)Headers.NAME_TRANSFER_ENCODING);
				this.httpTrailers = null;
			}
			else if(Http1xResponse.this.trailers == null) {
				this.httpTrailers = EmptyHttpHeaders.INSTANCE;
			}
			else {
				this.httpTrailers = Http1xResponse.this.trailers.unwrap();
				Http1xResponse.this.headers.set(Headers.NAME_TRAILER, this.httpTrailers.names().stream().collect(Collectors.joining(", ")));
			}
			
			Http1xResponse.this.connection.writeHttpObject(new FlatHttpResponse(Http1xResponse.this.version, httpStatus, Http1xResponse.this.headers.unwrap(), Unpooled.EMPTY_BUFFER));
			Http1xResponse.this.headers.setWritten();
			subscription.request(1);
		}

		@Override
		protected void hookOnNext(FileRegion value) {
			Http1xResponse.this.transferedLength += value.count();
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
