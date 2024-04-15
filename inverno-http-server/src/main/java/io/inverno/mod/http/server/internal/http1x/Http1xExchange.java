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
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.inverno.mod.http.base.internal.netty.FlatFullHttpResponse;
import io.inverno.mod.http.base.internal.netty.FlatHttpResponse;
import io.inverno.mod.http.base.internal.netty.FlatLastHttpContent;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketFrame;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketMessage;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.AbstractExchange;
import io.inverno.mod.http.server.internal.GenericErrorExchange;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
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
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

/**
 * <p>
 * HTTP1.x {@link Exchange} implementation.
 * </p>
 *
 * <p>
 * This implementation provides the logic to send HTTP1.x response data to the client.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see AbstractExchange
 */
class Http1xExchange extends AbstractExchange {

	private final Http1xConnectionEncoder encoder;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final GenericWebSocketFrame.GenericFactory webSocketFrameFactory;
	private final GenericWebSocketMessage.GenericFactory webSocketMessageFactory;
	private final HeadersValidator headersValidator;
	
	private boolean manageChunked;
	private Charset charset;

	final HttpVersion version;	
	Http1xExchange next;
	boolean keepAlive;
	boolean acceptTrailers;
	
	private Http1xWebSocket webSocket;
	
	/**
	 * <p>
	 * Creates a HTTP1.x server exchange.
	 * </p>
	 *
	 * @param configuration         the server configuration
	 * @param context               the channel handler context
	 * @param version               the HTTP version
	 * @param httpRequest           the underlying HTTP request
	 * @param encoder               the HTTP1.x connection encoder
	 * @param headerService         the header service
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param controller            the server controller
	 * @param headersValidator      a headers validator or null
	 */
	public Http1xExchange(
			HttpServerConfiguration configuration,
			ChannelHandlerContext context, 
			HttpVersion version,
			HttpRequest httpRequest,
			Http1xConnectionEncoder encoder,
			HeaderService headerService,
			ObjectConverter<String> parameterConverter,
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder,
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller,
			GenericWebSocketFrame.GenericFactory webSocketFrameFactory,
			GenericWebSocketMessage.GenericFactory webSocketMessageFactory,
			HeadersValidator headersValidator) {
		super(configuration, context, controller, new Http1xRequest(context, httpRequest, new Http1xRequestHeaders(httpRequest, headerService, parameterConverter), parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder), new Http1xResponse(version, context, headerService, parameterConverter, headersValidator));
		this.encoder = encoder;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.version = version;

		HttpHeaders headers = httpRequest.headers();
		this.keepAlive = !headers.containsValue(Headers.NAME_CONNECTION, Headers.VALUE_CLOSE, true) && 
			(this.version.isKeepAliveDefault() || headers.containsValue(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE, true));

		String te = httpRequest.headers().get(Headers.NAME_TE);
		this.acceptTrailers = te != null && te.contains(Headers.VALUE_TRAILERS);
		
		this.webSocketFrameFactory = webSocketFrameFactory;
		this.webSocketMessageFactory = webSocketMessageFactory;
		this.headersValidator = headersValidator;
	}

	@Override
	public io.inverno.mod.http.base.HttpVersion getProtocol() {
		return this.version == HttpVersion.HTTP_1_0 ? io.inverno.mod.http.base.HttpVersion.HTTP_1_0 : io.inverno.mod.http.base.HttpVersion.HTTP_1_1;
	}
	
	@Override
	public Optional<? extends WebSocket<ExchangeContext, ? extends WebSocketExchange<ExchangeContext>>> webSocket(String... subProtocols) {
		this.webSocket = new Http1xWebSocket(this.configuration, this.context, this, this.webSocketFrameFactory, this.webSocketMessageFactory, subProtocols);
		return Optional.of(this.webSocket);
	}
	
	@Override
	protected AbstractExchange.ServerControllerSubscriber createServerControllerSubscriber() {
		return new Http1xServerControllerSubscriber();
	}
	
	public void dispose(boolean deep) {
		this.dispose(null);
	}
	
	public void dispose(Throwable error, boolean deep) {
		super.dispose(error);
		if(deep && this.next != null) {
			this.next.dispose(error, deep);
		}
	}
	
	@Override
	protected ErrorExchange<ExchangeContext> createErrorExchange(Throwable error) {
		return new GenericErrorExchange(this, new Http1xResponse(this.version, this.context, this.headerService, this.parameterConverter, this.headersValidator), error, this.finalizer);
	}
	
	private Charset getCharset() {
		if(this.response.isHeadersWritten()) {
			return this.response().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		if(this.charset == null) {
			this.charset = this.response().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).map(Headers.ContentType::getCharset).orElse(Charsets.DEFAULT);
		}
		return this.charset;
	}
	
	private void preProcessResponseInternals(Http1xResponseHeaders headers, Http1xResponseTrailers trailers) {
		if(!this.keepAlive) {
			headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		}
		if(headers.getStatusCode() == Status.NOT_MODIFIED.getCode()) {
			headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
			headers.remove(HttpHeaderNames.CONTENT_LENGTH);
			this.acceptTrailers = false;
		}
		else {
			if(headers.getContentLength() != null) {
				if(headers.isChunkedTransferEncoding()) {
					headers.remove(HttpHeaderNames.CONTENT_LENGTH);
				}
				else {
					this.acceptTrailers = false;
				}
			}
			else if(!headers.isChunkedTransferEncoding()) {
				headers.add(Headers.NAME_TRANSFER_ENCODING, Headers.VALUE_CHUNKED);
				headers.get(Headers.NAME_CONTENT_TYPE).ifPresent(contentType -> this.manageChunked = contentType.regionMatches(true, 0, MediaTypes.TEXT_EVENT_STREAM, 0, MediaTypes.TEXT_EVENT_STREAM.length()));
			}
			else {
				this.acceptTrailers = false;
			}
		}
		if(this.acceptTrailers && trailers != null) {
			headers.set(Headers.NAME_TRAILER, trailers.getNames().stream().collect(Collectors.joining(", ")));
		}
	}
	
	private HttpResponse createHttpResponse(Http1xResponseHeaders headers, Http1xResponseTrailers trailers) {
		this.preProcessResponseInternals(headers, trailers);
		HttpResponseStatus status = HttpResponseStatus.valueOf(headers.getStatusCode());
		HttpHeaders httpHeaders = headers.getUnderlyingHeaders();
		return new FlatHttpResponse(this.version, status, httpHeaders);
	}
	
	private HttpResponse createFullHttpResponse(Http1xResponseHeaders headers, ByteBuf content, Http1xResponseTrailers trailers) {
		this.preProcessResponseInternals(headers, trailers);
		HttpResponseStatus status = HttpResponseStatus.valueOf(headers.getStatusCode());
		HttpHeaders httpHeaders = headers.getUnderlyingHeaders();
		if(this.acceptTrailers && trailers != null) {
			return new FlatFullHttpResponse(this.version, status, httpHeaders, content, trailers.getUnderlyingTrailers());
		}
		else {
			return new FlatFullHttpResponse(this.version, status, httpHeaders, content, EmptyHttpHeaders.INSTANCE);
		}
	}
	
	@Override
	protected void onNextMany(ByteBuf value, ChannelPromise nextPromise) {
		try {
			Http1xResponseHeaders headers = (Http1xResponseHeaders)this.response.headers();
			if(!headers.isWritten()) {
				this.encoder.writeFrame(this.context, this.createHttpResponse(headers, (Http1xResponseTrailers)this.response.trailers()), nextPromise);
				headers.setWritten(true);
			}
			if(this.manageChunked) {
				// We must handle chunked transfer encoding
				ByteBuf chunked_header = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(Integer.toHexString(value.readableBytes()) + "\r\n", Charsets.orDefault(this.getCharset())));
				ByteBuf chunked_trailer = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("\r\n", Charsets.orDefault(this.getCharset())));
				this.encoder.writeFrame(this.context, new DefaultHttpContent(Unpooled.wrappedBuffer(chunked_header, value, chunked_trailer)), nextPromise);
			}
			else {
				this.encoder.writeFrame(this.context, new DefaultHttpContent(value), nextPromise);
			}
		}
		finally {
			this.handler.exchangeNext(this.context, value);
		}
	}
	
	@Override
	protected void onCompleteWithError(Throwable throwable) {
		// This closes the connection and enventually finalizes the exchange
		this.handler.exchangeError(this.context, throwable);
	}
	
	@Override
	protected void onCompleteEmpty() {
		Http1xResponse http1xResponse = (Http1xResponse)this.response;
		Http1xResponseHeaders headers = http1xResponse.headers();

		if(this.request.getMethod().equals(Method.HEAD)) {
			// TODO at least for resources we could provide the actual content length
			if(headers.getContentLength() == null && !headers.contains(Headers.NAME_TRANSFER_ENCODING)) {
				headers.set(Headers.NAME_TRANSFER_ENCODING, Headers.VALUE_CHUNKED);
			}
			ChannelPromise finalizePromise = this.context.newPromise();
			this.encoder.writeFrame(this.context, this.createFullHttpResponse(headers, Unpooled.buffer(0), (Http1xResponseTrailers)this.response.trailers()), finalizePromise);
			headers.setWritten(true);
			this.finalizeExchange(finalizePromise, () -> this.handler.exchangeComplete(this.context));
		}
		else {
			// empty response or file region
			Publisher<FileRegion> fileRegionData = http1xResponse.body().getFileRegionData();
			if(fileRegionData == null) {
				// just write headers in a fullHttpResponse
				// Headers are not written here since we have an empty response
				ChannelPromise finalizePromise = this.context.newPromise();
				this.encoder.writeFrame(this.context, this.createFullHttpResponse(headers, Unpooled.buffer(0), (Http1xResponseTrailers)this.response.trailers()), finalizePromise);
				headers.setWritten(true);
				this.finalizeExchange(finalizePromise, () -> this.handler.exchangeComplete(this.context));
			}
			else {
				// Headers are not written here since we have an empty response
				this.encoder.writeFrame(this.context, this.createHttpResponse(headers, http1xResponse.trailers()), this.context.voidPromise());
				headers.setWritten(true);
				FileRegionDataSubscriber fileRegionSubscriber = new FileRegionDataSubscriber();
				this.subscriber = fileRegionSubscriber;
				fileRegionData.subscribe(fileRegionSubscriber);
			}
		}
	}
	
	@Override
	protected void onCompleteSingle(ByteBuf value) {
		// Response has one chunk => send a FullHttpResponse
		Http1xResponseHeaders headers = (Http1xResponseHeaders)this.response.headers();
		
		ChannelPromise finalizePromise = this.context.newPromise();
		this.encoder.writeFrame(this.context, this.createFullHttpResponse(headers, value, (Http1xResponseTrailers)this.response.trailers()), finalizePromise);
		headers.setWritten(true);
		this.handler.exchangeNext(this.context, value);
		this.finalizeExchange(finalizePromise, () -> this.handler.exchangeComplete(this.context));
	}
	
	@Override
	protected void onCompleteMany() {
		Http1xResponseTrailers responseTrailers = (Http1xResponseTrailers)this.response.trailers();
		ChannelPromise finalizePromise = this.context.newPromise();
		if(this.acceptTrailers && responseTrailers != null) {
			this.encoder.writeFrame(this.context, new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, responseTrailers.getUnderlyingTrailers()), finalizePromise);
			responseTrailers.setWritten(true);
		}
		else {
			this.encoder.writeFrame(this.context, LastHttpContent.EMPTY_LAST_CONTENT, finalizePromise);
		}
		this.finalizeExchange(finalizePromise, () -> this.handler.exchangeComplete(this.context));
	}

	@Override
	protected void onReset(long code) {
		Http1xResponseHeaders headers = (Http1xResponseHeaders)this.response.headers();
		if(headers.isWritten()) {
			// This closes the connection and enventually finalizes the exchange
			this.handler.exchangeReset(this.context, code);
		}
		else {
			// we can just respond with no content
			headers.status(Status.NO_CONTENT); 
			
			ChannelPromise finalizePromise = this.context.newPromise();
			this.encoder.writeFrame(this.context, this.createFullHttpResponse(headers, Unpooled.buffer(0), (Http1xResponseTrailers)this.response.trailers()), finalizePromise);
			headers.setWritten(true);
			this.finalizeExchange(finalizePromise, () -> this.handler.exchangeComplete(this.context));
		}
	}
	
	/**
	 * <p>
	 * File region subscriber.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private class FileRegionDataSubscriber extends BaseSubscriber<FileRegion> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			this.request(1);
		}

		@Override
		protected void hookOnNext(FileRegion fileRegion) {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xExchange.this.transferedLength += fileRegion.count();
				Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, fileRegion, Http1xExchange.this.context.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						// TODO here we put null as next value because we don't have access to the actual buffer, can we do better?
						Http1xExchange.this.handler.exchangeNext(Http1xExchange.this.context, null);
						this.request(1);
					}
					else {
						Http1xExchange.this.handler.exchangeError(Http1xExchange.this.context, future.cause());
						this.cancel();
						Http1xExchange.this.onCompleteWithError(future.cause());
					}
				}));
			});
		}
		
		@Override
		protected void hookOnComplete() {
			Http1xExchange.this.executeInEventLoop(() -> {
				Http1xResponseTrailers responseTrailers = (Http1xResponseTrailers)Http1xExchange.this.response.trailers();
				ChannelPromise finalizePromise = Http1xExchange.this.context.newPromise();
				if(Http1xExchange.this.acceptTrailers && responseTrailers != null) {
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, new FlatLastHttpContent(Unpooled.EMPTY_BUFFER, responseTrailers.getUnderlyingTrailers()), finalizePromise);
				}
				else {
					Http1xExchange.this.encoder.writeFrame(Http1xExchange.this.context, LastHttpContent.EMPTY_LAST_CONTENT, finalizePromise);
				}
				Http1xExchange.this.finalizeExchange(finalizePromise, () -> Http1xExchange.this.handler.exchangeComplete(Http1xExchange.this.context));
			});
		}
	}
	
	/**
	 * <p>
	 * Extends the base server controller subscriber to support WebSocket upgrade over HTTP/1.x.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	protected class Http1xServerControllerSubscriber extends AbstractExchange.ServerControllerSubscriber {

		@Override
		protected void hookOnError(Throwable t) {
			super.hookOnError(t);
		}

		/**
		 * <p>
		 * If {@link #webSocket(java.lang.String...) } has been invoked in the exchange handler, tries a WebSocket upgrade handshake by adding proper channel handlers in the pipeline. In case the
		 * handshake succeeds, finalizes the upgrade, otherwise restores the pipeline and invokes the fallback handler or report the error if it is missing.
		 * </p>
		 */
		@Override
		protected void hookOnComplete() {
			if(Http1xExchange.this.webSocket != null) {
				// Handshake
				Http1xExchange.this.webSocket.handshake().subscribe(
					ign -> {},
					cause -> {
						if(Http1xExchange.this.webSocket.getFallback() != null) {
							// restore the pipeline and invoke the fallback 
							Http1xExchange.this.webSocket.restorePipeline();
							
							AbstractExchange.ServerControllerSubscriber serverControllerSubscriber = Http1xExchange.super.createServerControllerSubscriber();
							Http1xExchange.this.subscriber = serverControllerSubscriber;
							Http1xExchange.this.webSocket.getFallback().subscribe(serverControllerSubscriber);
						}
						else {
							Http1xExchange.this.hookOnError(cause);
						}
					},
					() -> {
						// We finished the upgrade, we can log the result
						Http1xExchange.this.keepAlive = false;
						Http1xExchange.this.response.headers().status(Status.SWITCHING_PROTOCOLS);
						Http1xExchange.this.logAccess();
						
						// Cancel the exchange and next requests (if any) once the WebSocket upgrade completes
						Http1xExchange.this.dispose(true);
					}
				);
			}
			else {
				super.hookOnComplete();
			}
		}
	}
}