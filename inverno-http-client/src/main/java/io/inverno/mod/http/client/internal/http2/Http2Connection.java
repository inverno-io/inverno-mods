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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.ConnectionResetException;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.RequestTimeoutException;
import io.inverno.mod.http.client.internal.AbstractExchange;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.inverno.mod.http.client.internal.EndpointChannelConfigurer;
import io.inverno.mod.http.client.internal.EndpointExchange;
import io.inverno.mod.http.client.internal.HttpConnection;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.inverno.mod.http.client.internal.http1x.Http1xUpgradingExchange;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
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
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * HTTP/2 {@link HttpConnection} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http2Connection extends Http2ConnectionHandler implements Http2FrameListener, io.netty.handler.codec.http2.Http2Connection.Listener, HttpConnection, AbstractExchange.Handler<AbstractRequest, Http2Response, AbstractHttp2Exchange> {
	
	private final HttpClientConfiguration configuration;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final IntObjectMap<AbstractHttp2Exchange> clientStreams;
	
	private ChannelHandlerContext context;
	private boolean tls;
	private Long maxConcurrentStreams;
	private HttpConnection.Handler handler;
	private final long requestTimeout;
	
	private Mono<Void> close;
	private boolean closing;
	private boolean closed;
	
	/**
	 * <p>
	 * Creates an HTTP/2 connection.
	 * </p>
	 *
	 * @param configuration      the HTTP client configurartion
	 * @param decoder            the HTTP/2 connection decoder
	 * @param encoder            the HTTP/2 connection encoder
	 * @param initialSettings    the HTTP/2 initial settings
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public Http2Connection(
			HttpClientConfiguration configuration, 
			Http2ConnectionDecoder decoder, 
			Http2ConnectionEncoder encoder, 
			Http2Settings initialSettings, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter) {
		super(decoder, encoder, initialSettings);
		this.configuration = configuration;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.clientStreams = new IntObjectHashMap<>();
		this.maxConcurrentStreams = this.configuration.http2_max_concurrent_streams();
		this.requestTimeout = this.configuration.request_timeout();
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
	public Long getMaxConcurrentRequests() {
		return this.maxConcurrentStreams;
	}

	@Override
	public void setHandler(Handler handler) {
		this.handler = handler;
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
	public void onHttpClientUpgrade(Http1xUpgradingExchange upgradingExchange) throws Http2Exception {
		super.onHttpClientUpgrade();
		Http2Stream upgradingStream = this.connection().stream(1);
		Http2UpgradedExchange upgradedExchange = new Http2UpgradedExchange(this.context, upgradingExchange.getUpgradedExchangeSink(), upgradingExchange.context(), upgradingExchange.request(), this.encoder(), upgradingStream);
		upgradedExchange.lastModified = upgradingExchange.getLastModified();
		upgradedExchange.start(this);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.connection().addListener(this);
		this.tls = ctx.pipeline().get(SslHandler.class) != null;
		this.close = Mono.<Void>create(sink -> {
			if(!this.closed && !this.closing) {
				this.closing = true;
				ChannelFuture closeFuture;
				if(ctx.channel().isActive()) {
					closeFuture = ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
						.addListener(ChannelFutureListener.CLOSE);
				}
				else {
					closeFuture = ctx.close();
				}
				closeFuture.addListener(future -> {
					if(future.isSuccess()) {
						sink.success();
					}
					else {
						sink.error(future.cause());
					}
				});
			}
			else {
				sink.success();
			}
		})
		.subscribeOn(Schedulers.fromExecutor(ctx.executor()));
		this.closed = false;
		this.context = ctx;
		super.handlerAdded(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx); 
		this.closed = true;
		if(this.handler != null) {
			this.handler.onClose();
		}
		this.clientStreams.values().stream().forEach(exchange -> exchange.dispose(new ConnectionResetException("Connection reset by peer")));
	}
	
	@Override
	public Mono<Void> close() {
		if(this.closing || this.closed) {
			return Mono.empty();
		}
		else {
			return this.close;
		}
		// Inflight exchanges are disposed in #channelInactive()
	}

	@Override
	protected void onConnectionError(ChannelHandlerContext ctx, boolean outbound, Throwable cause, Http2Exception http2Ex) {
		super.onConnectionError(ctx, outbound, cause, http2Ex);
		// We can already remove the connection from the pool
		this.clientStreams.values().stream().forEach(exchange -> exchange.dispose(cause));
		if(this.handler != null) {
			this.handler.onError(cause);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// This is ok when there was an error establishing the connection but we also have:
		// - errors while sending the request
		// - errors while receiving the response
		// - errors while processing the response
		//   - in that case we should handle this before and drain the remaining response chunks so we can still use the connection
		
		// In any case only network related error should get here anything else must be handled upstream
		
		// we must kill the connection
		this.close().subscribe();
	}

	@Override
	public <A extends ExchangeContext> Mono<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> send(EndpointExchange<A> endpointExchange) {
		return Mono.<HttpConnectionExchange<ExchangeContext, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>>create(exchangeSink -> {
			Http2Request http2Request = new Http2Request(this.context, this.tls, this.parameterConverter, this.headerService, endpointExchange.request());
			Http2Exchange http2Exchange = new Http2Exchange(context, exchangeSink, endpointExchange.context(), http2Request, this.connection().local(), this.encoder());
			
			// Make sure the exchange is started on the connection event loop
			// We can start directly it since HTTP/2 supports interleaving!
			EventLoop eventLoop = this.context.channel().eventLoop();
			if(eventLoop.inEventLoop()) {
				http2Exchange.lastModified = System.currentTimeMillis();
				http2Exchange.start(this);
			}
			else {
				eventLoop.submit(() -> {
					http2Exchange.lastModified = System.currentTimeMillis();
					http2Exchange.start(this);
				});
			}
		})
		.map(exchange -> (HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>)exchange);
	}
	
	@Override
	public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception {
		// Response begins
		AbstractHttp2Exchange clientExchange = this.clientStreams.get(streamId);
		if(clientExchange != null) {
			if(!clientExchange.getStream().isTrailersReceived()) {
				Http2Response response = new Http2Response(headers, this.headerService, this.parameterConverter);
				this.clientStreams.get(streamId).setResponse(response);
				if(endOfStream) {
					// empty response
					response.data().tryEmitComplete();
					clientExchange.notifyComplete();
				}
			}
			else {
				Http2Response response = clientExchange.response();
				response.setResponseTrailers(new Http2ResponseTrailers(headers, this.headerService, this.parameterConverter));
				response.data().tryEmitComplete();
				clientExchange.notifyComplete();
			}
		}
		else {
			// We clearly have a stream otherwise we wouldn't be here, but we can't process it since we are missing the exchange
			this.resetStream(ctx, streamId, Http2Error.REFUSED_STREAM.code(), ctx.voidPromise());
		}
	}

	@Override
	public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
		onHeadersRead(ctx, streamId, headers, padding, endOfStream);
	}

	@Override
	public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
		// TODO flow control?
		int processed = data.readableBytes() + padding;
		AbstractHttp2Exchange clientExchange = this.clientStreams.get(streamId);
		if(clientExchange != null) {
			Sinks.Many<ByteBuf> responseData = clientExchange.response().data();
			
			if(responseData != null) {
				data.retain();
				if(responseData.tryEmitNext(data) != Sinks.EmitResult.OK) {
					data.release();
				}
				if(endOfStream) {
					responseData.tryEmitComplete();
					clientExchange.notifyComplete();
				}
			}
			else {
				data.release();
			}
		}
		else {
			// We clearly have a stream otherwise we wouldn't be here
			// This should have been already handled in onHeadersRead() so let's report an error
			this.resetStream(ctx, streamId, Http2Error.INTERNAL_ERROR.code(), ctx.voidPromise());
		}
		return processed;
	}

	@Override
	public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception {
	}

	@Override
	public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
		AbstractHttp2Exchange clientStream = this.clientStreams.remove(streamId);
		if (clientStream != null) {
			clientStream.dispose(new IllegalStateException("Stream " + streamId +" was reset (" + errorCode + ")"));
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
			this.maxConcurrentStreams = this.configuration.http2_max_concurrent_streams()== null ? mcs : Math.min(mcs, this.configuration.http2_max_concurrent_streams());
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
		AbstractHttp2Exchange clientStream = this.clientStreams.remove(stream.id());
		if (clientStream != null) {
			clientStream.dispose(new IllegalStateException("Stream was closed"));
		}
	}

	@Override
	public void onStreamRemoved(Http2Stream stream) {
	}

	@Override
	public void onGoAwaySent(int lastStreamId, long errorCode, ByteBuf debugData) {
		if(this.handler != null) {
			this.handler.onError(new HttpClientException("Go away sent with code: " + errorCode));
		}
	}

	@Override
	public void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData) {
		if(this.handler != null) {
			this.handler.onError(new HttpClientException("Go away received with code: " + errorCode));
		}
	}
	
	private void startTimeout(AbstractHttp2Exchange exchange) {
		if(exchange.timeoutFuture != null) {
			return;
		}
		long nextTimeout = this.requestTimeout - (System.currentTimeMillis() - exchange.lastModified);
		if(nextTimeout <= 0) {
			// reset the stream
			Http2Stream stream = exchange.getStream();
			exchange.dispose(new RequestTimeoutException("Exceeded timeout " + this.requestTimeout + "ms"));
			if(stream != null) {
				int streamId = stream.id();
				this.resetStream(exchange.getChannelContext(), streamId, Http2Error.NO_ERROR.code(), exchange.getChannelContext().voidPromise());
				this.clientStreams.remove(streamId);
			}
			if(this.handler != null) {
				// Recycle connection
				this.handler.onExchangeTerminate(exchange);
			}
		}
		else {
			exchange.timeoutFuture = this.context.channel().eventLoop().schedule(
			() -> {
				exchange.timeoutFuture = null;
				this.startTimeout(exchange);
				return exchange;
			},
			nextTimeout, TimeUnit.MILLISECONDS);
		}
	}
	
	private void cancelTimeout(AbstractHttp2Exchange exchange) {
		if(exchange.timeoutFuture != null) {
			exchange.timeoutFuture.cancel(false);
			exchange.timeoutFuture = null;
		}
	}

	@Override
	public void exchangeStart(AbstractHttp2Exchange exchange) {
		Http2Stream stream = exchange.getStream();
		if(stream != null) {
			this.clientStreams.put(exchange.getStream().id(), exchange);
			this.startTimeout(exchange);
		}
		// a null stream here indicates that there was an error creating the stream 
		// the corresponding error is handled within the exchange during start()
	}

	@Override
	public void requestComplete(AbstractHttp2Exchange exchange) {
	}
	
	@Override
	public void exchangeError(AbstractHttp2Exchange exchange, Throwable t) {
		this.cancelTimeout(exchange);
		Http2Stream stream = exchange.getStream();
		exchange.dispose(t);
		if(stream != null) {
			int streamId = stream.id();
			Http2Connection.this.resetStream(exchange.getChannelContext(), streamId, Http2Error.INTERNAL_ERROR.code(), exchange.getChannelContext().voidPromise());
			Http2Connection.this.clientStreams.remove(streamId);
		}
		
		// we can recycle the connection since a request has been processed
		if(this.handler != null) {
			this.handler.onExchangeTerminate(exchange);
		}
	}

	@Override
	public void exchangeComplete(AbstractHttp2Exchange exchange) {
		this.cancelTimeout(exchange);
		exchange.dispose();
		Http2Connection.this.clientStreams.remove(exchange.getStream().id());
		exchange.getStream().closeRemoteSide();
		// we can recycle the connection since a request has been processed
		if(this.handler != null) {
			this.handler.onExchangeTerminate(exchange);
		}
	}
}
