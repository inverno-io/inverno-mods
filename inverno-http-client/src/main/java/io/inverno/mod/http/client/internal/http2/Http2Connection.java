/*
 * Copyright 2022 Jeremy Kuhn
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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.ResetStreamException;
import io.inverno.mod.http.client.internal.EndpointChannelConfigurer;
import io.inverno.mod.http.client.internal.EndpointExchange;
import io.inverno.mod.http.client.internal.HttpConnection;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.inverno.mod.http.client.internal.http1x.Http1xUpgradingExchange;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
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
import io.netty.util.concurrent.EventExecutor;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Http/2 {@link HttpConnection} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http2Connection extends Http2ConnectionHandler implements Http2FrameListener, io.netty.handler.codec.http2.Http2Connection.Listener, HttpConnection {

	private static final Logger LOGGER = LogManager.getLogger(HttpConnection.class);
	
	private final HttpClientConfiguration configuration;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	final IntObjectMap<Http2ConnectionStream> clientStreams;
	
	private ChannelHandlerContext channelContext;
	private Scheduler scheduler;
	private boolean tls;
	private Long maxConcurrentStreams;
	private HttpConnection.Handler handler;
	
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
	 * @param decoder            the HTTP/2 connection decoder
	 * @param encoder            the HTTP/2 connection encoder
	 * @param initialSettings    the HTTP/2 initial settings
	 * @param configuration      the HTTP client configurartion
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public Http2Connection(
			Http2ConnectionDecoder decoder, 
			Http2ConnectionEncoder encoder, 
			Http2Settings initialSettings,
			HttpClientConfiguration configuration, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter
		) {
		super(decoder, encoder, initialSettings);
		this.configuration = configuration;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.clientStreams = new IntObjectHashMap<>();
		this.maxConcurrentStreams = configuration.http2_max_concurrent_streams();
	}
	
	/**
	 * <p>
	 * Returns the event loop associated to the connection.
	 * </p>
	 * 
	 * @return the connection event loop
	 */
	public EventExecutor executor() {
		return this.channelContext.executor();
	}
	
	/**
	 * <p>
	 * Returns a new channel promise.
	 * </p>
	 * 
	 * @return a new channel promise
	 */
	public ChannelPromise newPromise() {
		return this.channelContext.newPromise();
	}
	
	/**
	 * <p>
	 * Returns the void promise.
	 * </p>
	 * 
	 * @return the void promise
	 */
	public ChannelPromise voidPromise() {
		return this.channelContext.voidPromise();
	}
	
	@Override
	public boolean isTls() {
		return this.tls;
	}

	@Override
	public HttpVersion getProtocol() {
		return HttpVersion.HTTP_2_0;
	}
	
//	@Override
	public SocketAddress getLocalAddress() {
		return this.channelContext.channel().localAddress();
	}

//	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return Optional.ofNullable(this.channelContext.pipeline().get(SslHandler.class))
			.map(handler -> handler.engine().getSession().getLocalCertificates())
			.filter(certificates -> certificates.length > 0);
	}

//	@Override
	public SocketAddress getRemoteAddress() {
		return this.channelContext.channel().remoteAddress();
	}

//	@Override
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
	public Long getMaxConcurrentRequests() {
		return this.maxConcurrentStreams;
	}

	@Override
	public void setHandler(HttpConnection.Handler handler) {
		this.handler = handler;
	}
	
	@Override
	public <A extends ExchangeContext> Mono<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> send(EndpointExchange<A> endpointExchange) {
		if(this.closed || this.closing) {
			return Mono.error(new HttpClientException("Connection was closed"));
		}
		Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink = Sinks.one();
		Http2ConnectionStream clientStream = new Http2ConnectionStream(this, this.channelContext, this.connection().local());
		clientStream.exchange = new Http2Exchange<>(
			this.configuration, 
			sink,
			this.headerService,
			this.parameterConverter,
			endpointExchange.context(),
			clientStream,
			endpointExchange.request()
		);
		
		return sink.asMono()
			.doOnSubscribe(ign -> {
				clientStream.exchange.start();
			})
			.subscribeOn(this.scheduler);
	}

	@Override
	public Mono<Void> shutdown() {
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
				.subscribeOn(this.scheduler);
		}
		return this.shutdown;
	}

	@Override
	public Mono<Void> shutdownGracefully() {
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
				.subscribeOn(this.scheduler);
		}
		return this.gracefulShutdown;
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.connection().addListener(this);
		this.channelContext = ctx;
		this.scheduler = Schedulers.fromExecutor(ctx.executor());
		super.handlerAdded(ctx);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.tls = ctx.pipeline().get(SslHandler.class) != null;
		super.channelActive(ctx);
		this.closed = false;
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
		this.closed = true;
		// each stream should be closed individually so disposal whould be handled in #onStreamClosed()
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		this.shutdown().subscribe();
		LOGGER.error("Connection error", cause);
	}
	
	/**
	 * <p>
	 * Invoked by {@link EndpointChannelConfigurer} when finalizing the H2C upgrade to start the upgraded exchange.
	 * </p>
	 * 
	 * @param upgradingExchange the HTTP/1.x upgrading exchange
	 * 
	 * @throws Http2Exception if there was an error during the client upgrade
	 */
	public <A extends ExchangeContext> void onHttpClientUpgrade(Http1xUpgradingExchange<A> upgradingExchange) throws Http2Exception {
		super.onHttpClientUpgrade();
		Http2ConnectionStream clientStream = new Http2ConnectionStream(this, this.channelContext, this.connection().stream(1));
		this.clientStreams.put(1, clientStream);
		clientStream.exchange = new Http2UpgradedExchange<>(this.configuration, upgradingExchange.getUpgradedSink(), this.headerService, this.parameterConverter, upgradingExchange.context(), clientStream, upgradingExchange.request());
		clientStream.exchange.start();
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
		Http2ConnectionStream clientStream = this.clientStreams.get(streamId);
		if(clientStream != null) {
			if(!clientStream.getOrCreateStream().isTrailersReceived()) { // TODO test trailers
				clientStream.exchange.emitResponse(headers);
				if(endOfStream) {
					// empty response
					clientStream.exchange.response().body().getDataSink().tryEmitComplete();
					clientStream.onResponseComplete();
				}
			}
			else {
				clientStream.exchange.response().setTrailers(headers);
				clientStream.exchange.response().body().getDataSink().tryEmitComplete();
				clientStream.onResponseComplete();
			}
		}
		else {
			// We clearly have a stream otherwise we wouldn't be here, but we can't process it since we are missing the exchange
			this.resetStream(ctx, streamId, Http2Error.REFUSED_STREAM.code(), ctx.voidPromise());
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
		Http2ConnectionStream clientStream = this.clientStreams.get(streamId);
		if(clientStream != null) {
			data.retain();
			if(clientStream.exchange.response().body().getDataSink().tryEmitNext(data) != Sinks.EmitResult.OK) {
				data.release();
			}
			
			if(endOfStream) {
				clientStream.exchange.response().body().getDataSink().tryEmitComplete();
				clientStream.onResponseComplete();
			}
		}
		else {
			// We clearly have a stream otherwise we wouldn't be here, but we can't process it since we are missing the exchange
			this.resetStream(ctx, streamId, Http2Error.REFUSED_STREAM.code(), ctx.voidPromise());
		}
		return processed;
	}

	@Override
	public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception {
	}

	@Override
	public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
		Http2ConnectionStream clientStream = this.clientStreams.remove(streamId);
		if(clientStream != null) {
			try {
				clientStream.exchange.dispose(new ResetStreamException(errorCode, "Stream " + streamId +" was reset (" + errorCode + ")"));
			}
			finally {
				if(this.handler != null) {
					this.handler.recycle();
				}
			}
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
		
		Long mcs = settings.maxConcurrentStreams();
		if(mcs != null) {
			this.maxConcurrentStreams = this.configuration.http2_max_concurrent_streams() == null ? mcs : Math.min(mcs, this.configuration.http2_max_concurrent_streams());
			if(this.handler != null) {
				this.handler.onSettingsChange(this.maxConcurrentStreams);
			}
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
		Http2ConnectionStream clientStream = this.clientStreams.remove(stream.id());
		if(clientStream != null) {
			try {
				clientStream.exchange.dispose(new HttpClientException("Stream was closed"));
			}
			finally {
				if(this.handler != null) {
					this.handler.recycle();
				}
			}
		}
	}

	@Override
	public void onStreamRemoved(Http2Stream stream) {
	}

	@Override
	public void onGoAwaySent(int lastStreamId, long errorCode, ByteBuf debugData) {
		if(this.handler != null) {
			this.handler.close();
		}
	}

	@Override
	public void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData) {
		if(this.handler != null) {
			this.handler.close();
		}
		this.shutdownGracefully().subscribe();
	}
}
