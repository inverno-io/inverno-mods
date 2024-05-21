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
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.HttpServerException;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ResetStreamException;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.HttpConnection;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DelegatingDecompressorFrameListener;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Http/2 connection.
 * </p>
 * 
 * <p>
 * A {@link Http2ConnectionStream} is created to handle exchange on each server streams, it basically proxies the connection and abstract the stream to the exchange.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class Http2Connection extends Http2ConnectionHandler implements HttpConnection, Http2FrameListener, io.netty.handler.codec.http2.Http2Connection.Listener {
	
	private static final Logger LOGGER = LogManager.getLogger(HttpConnection.class);

	private final HttpServerConfiguration configuration;
	private final ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	private final MultipartDecoder<Part> multipartBodyDecoder;
	private final Http2ContentEncodingResolver contentEncodingResolver;
	
	private final IntObjectMap<Http2ConnectionStream> serverStreams;
	
	private ChannelHandlerContext channelContext;
	private boolean tls;
	
	boolean read;
	
	private Sinks.One<Void> shutdownSink;
	private Mono<Void> shutdown;
	
	private Sinks.One<Void> gracefulShutdownSink;
	private Mono<Void> gracefulShutdown;
	
	private boolean closing;
	private boolean closed;
	
	/**
	 * <p>
	 * Creates an Http/2 connection.
	 * </p>
	 * 
	 * @param decoder                 Http/2 decoder
	 * @param encoder                 Http/2 encoder
	 * @param initialSettings         Http/2 settings
	 * @param configuration           the server configuration
	 * @param controller              the server controller
	 * @param headerService           the header service
	 * @param parameterConverter      the parameter converter
	 * @param urlEncodedBodyDecoder   the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder    the multipart/form-data body decoder
	 * @param contentEncodingResolver a content encoding resolver
	 */
	Http2Connection(
			Http2ConnectionDecoder decoder, 
			Http2ConnectionEncoder encoder, 
			Http2Settings initialSettings, 
			HttpServerConfiguration configuration, 
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder,
			Http2ContentEncodingResolver contentEncodingResolver
		) {
		super(decoder, encoder, initialSettings);
		
		this.configuration = configuration;
		this.controller = controller;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		this.contentEncodingResolver = contentEncodingResolver;
		
		this.serverStreams = new IntObjectHashMap<>();
	}

	@Override
	public boolean isTls() {
		return this.tls;
	}

	@Override
	public HttpVersion getProtocol() {
		return HttpVersion.HTTP_2_0;
	}
	
	@Override
	public SocketAddress getLocalAddress() {
		return this.channelContext.channel().localAddress();
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return Optional.ofNullable(this.channelContext.pipeline().get(SslHandler.class))
			.map(handler -> handler.engine().getSession().getLocalCertificates())
			.filter(certificates -> certificates.length > 0);
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.channelContext.channel().remoteAddress();
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return Optional.ofNullable(this.channelContext.pipeline().get(SslHandler.class))
			.map(handler -> {
				try {
					return handler.engine().getSession().getPeerCertificates();
				} 
				catch(SSLPeerUnverifiedException e) {
					return null;
				}
			})
			.filter(certificates -> certificates.length > 0);
	}
	
	@Override
	public synchronized Mono<Void> shutdown() {
		if(this.shutdownSink == null) {
			this.shutdownSink = Sinks.one();
			this.shutdown = this.shutdownSink.asMono()
				.doOnSubscribe(ign -> {
					if(!this.closing || this.gracefulShutdownSink != null && this.gracefulShutdownSink.currentSubscriberCount() > 0) {
						this.closing = true;
						this.goAway(this.channelContext, this.connection().remote().lastStreamCreated(), Http2Error.NO_ERROR.code(), Unpooled.EMPTY_BUFFER, this.channelContext.voidPromise());
						this.flush(channelContext);

						this.channelContext.close()
							.addListener(future -> {
								if(future.isSuccess()) {
									this.shutdownSink.tryEmitEmpty();
								}
								else {
									this.shutdownSink.tryEmitError(future.cause());
								}
							});
					}
				})
				.subscribeOn(Schedulers.fromExecutor(this.channelContext.executor()));
		}
		return this.shutdown;
	}

	@Override
	public synchronized Mono<Void> shutdownGracefully() {
		if(this.gracefulShutdownSink == null) {
			this.gracefulShutdownSink = Sinks.one();
			this.gracefulShutdown = this.gracefulShutdownSink.asMono()
				.doOnSubscribe(ign -> {
					if(!this.closing) {
						this.closing = true;
					}
					
					ChannelPromise closePromise = this.channelContext.newPromise()
						.addListener(future -> {
							if(future.isSuccess()) {
								this.gracefulShutdownSink.tryEmitEmpty();
							}
							else {
								this.gracefulShutdownSink.tryEmitError(future.cause());
							}
						});

					try {
						// shutdown gracefully
						this.close(this.channelContext, closePromise);
					} 
					catch(Exception e) {
						// I don't see when this actually happen but let's assume the connection is closed then
						this.gracefulShutdownSink.tryEmitError(e);
					}
				})
				.subscribeOn(Schedulers.fromExecutor(this.channelContext.executor()));
		}
		return this.gracefulShutdown;
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.channelContext = ctx;
		this.connection().addListener(this);
		super.handlerAdded(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.tls = ctx.pipeline().get(SslHandler.class) != null;
		super.channelActive(ctx);
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if(evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
			HttpServerUpgradeHandler.UpgradeEvent upgradeEvent = (HttpServerUpgradeHandler.UpgradeEvent) evt;

			FullHttpRequest request = upgradeEvent.upgradeRequest();
			String host = request.headers().get("host");
			request.headers()
				.remove("http2-settings")
				.remove("host");
			
			DefaultHttp2Headers headers = new DefaultHttp2Headers();
			headers.method(request.method().name())
				.path(request.uri())
				.scheme("http");
			
			if(host != null) {
				headers.authority(host);
			}
			request.headers().forEach(header -> headers.add(header.getKey().toLowerCase(), header.getValue()));
			
			boolean endStream = request.content() == null || !request.content().isReadable();
			
			this.onHeadersRead(ctx, 1, headers, 0, endStream);
			if(!endStream) {
				this.onDataRead(ctx, 1, request.content(), 0, true); 
			}
		}
		super.userEventTriggered(ctx, evt);
	}

	@Override
	public void onError(ChannelHandlerContext ctx, boolean outbound, Throwable cause) {
		super.onError(ctx, outbound, cause);
		LOGGER.debug("onError", cause);
	}

	@Override
	protected void onStreamError(ChannelHandlerContext ctx, boolean outbound, Throwable cause, Http2Exception.StreamException http2Ex) {
		super.onStreamError(ctx, outbound, cause, http2Ex);
		LOGGER.debug("onStreamError", cause);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		// each stream should be closed individually so disposal whould be handled in #onStreamClosed()
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		this.shutdown().subscribe();
		LOGGER.error("Connection error", cause);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		this.read = true;
		super.channelRead(ctx, msg);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		this.read = false;
		super.channelReadComplete(ctx);
	}
	
	@Override
	public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception {
		Http2ConnectionStream serverStream = this.serverStreams.get(streamId);
		if (serverStream == null) {
			serverStream = new Http2ConnectionStream(this, this.channelContext, this.connection().stream(streamId));
			this.serverStreams.put(streamId, serverStream);
			Http2Exchange exchange = new Http2Exchange(
				this.configuration, 
				this.controller, 
				this.headerService, 
				this.parameterConverter, 
				this.urlEncodedBodyDecoder, 
				this.multipartBodyDecoder, 
				serverStream,
				headers
			);
			if(this.configuration.compression_enabled()) {
				String acceptEncoding = headers.get(HttpHeaderNames.ACCEPT_ENCODING) != null ? headers.get(HttpHeaderNames.ACCEPT_ENCODING).toString() : null;
				if(acceptEncoding != null) {
					exchange.response().headers().set(Headers.NAME_CONTENT_ENCODING, this.contentEncodingResolver.resolve(acceptEncoding));
				}
			}
			if(endOfStream) {
				exchange.request().body().ifPresent(body -> body.getDataSink().tryEmitComplete());
			}
			exchange.start();
		}
		else {
			// Continuation frame
			serverStream.exchange.request().headers().unwrap().add(headers);
		}
	}

	@Override
	public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
		this.onHeadersRead(ctx, streamId, headers, padding, endOfStream);
	}

	@Override
	public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
		// TODO flow control?
		int processed = data.readableBytes() + padding;
		
		Http2ConnectionStream serverStream = this.serverStreams.get(streamId);
		if(serverStream != null) {
			Http2RequestBody requestBody = serverStream.exchange.request().getBody();
			if(requestBody != null) {
				data.retain();
				if(requestBody.getDataSink().tryEmitNext(data) != Sinks.EmitResult.OK) {
					data.release();
				}
				if (endOfStream) {
					requestBody.getDataSink().tryEmitComplete();
				}
			}
		}
		else {
			// TODO this should never happen?
			throw new IllegalStateException("Unable to push data to unmanaged stream " + streamId);
		}
		return processed;
	}
	
	@Override
	public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception {
	}

	@Override
	public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
		Http2ConnectionStream serverStream = this.serverStreams.remove(streamId);
		if(serverStream != null) {
			serverStream.exchange.dispose(new ResetStreamException(errorCode, "Stream " + streamId +" was reset (" + errorCode + ")"));
		}
	}

	@Override
	public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {
	}

	@Override
	public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
		if (this.configuration.decompression_enabled()) {
			this.decoder().frameListener(new DelegatingDecompressorFrameListener(decoder().connection(), this));
		} 
		else {
			this.decoder().frameListener(this);
		}
	}

	@Override
	public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
	}

	@Override
	public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
	}

	@Override
	public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding) throws Http2Exception {
	}

	@Override
	public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws Http2Exception {
	}

	@Override
	public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception {
	}

	@Override
	public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) throws Http2Exception {
	}

	@Override
	public void onStreamAdded(Http2Stream stream) {
	}

	@Override
	public void onStreamActive(Http2Stream stream) {
	}

	@Override
	public void onStreamHalfClosed(Http2Stream stream) {
	}

	@Override
	public void onStreamClosed(Http2Stream stream) {
		Http2ConnectionStream serverStream = this.serverStreams.remove(stream.id());
		if(serverStream != null) {
			Throwable cause = new HttpServerException("Stream " + stream.id() + " was closed");
			serverStream.exchange.dispose(cause);
		}
	}

	@Override
	public void onStreamRemoved(Http2Stream stream) {
	}

	@Override
	public void onGoAwaySent(int lastStreamId, long errorCode, ByteBuf debugData) {
	}

	@Override
	public void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData) {
		this.shutdownGracefully().subscribe();
	}
}
