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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketFrame;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketMessage;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.HttpServerException;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.HttpConnection;
import io.inverno.mod.http.server.internal.http1x.ws.GenericWebSocketExchange;
import io.inverno.mod.http.server.internal.http1x.ws.WebSocketConnection;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.TooLongHttpHeaderException;
import io.netty.handler.codec.http.TooLongHttpLineException;
import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameServerExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateServerExtensionHandshaker;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Http/1.x connection.
 * </p>
 * 
 * <p>
 * Http pipelining is implemented by linking {@link Http1xExchange} one after the other. At any point in time there is at most one {@link #requestingExchange} and one {@link #respondingExchange} that
 * can be the same as the requesting exchange.
 * </p>
 * 
 * <p>
 * When receiving a request, a requesting exchange is created and started right away to become the responding exchange when there's no responding exchange, otherwise it is chained to the existing
 * requesting exchange.
 * </p>
 * 
 * <p>
 * When the responding exchange completes, the connection starts the next exchange if any which becomes the responding exchange, otherwise there's no more pending request and both the requesting and
 * responding exchanges which should then be the same are set to {@code null}.
 * </p>
 * 
 * <p>
 * The connection is shutdown on decoder error after any pending non-faulty requests have been processed.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class Http1xConnection extends ChannelDuplexHandler implements HttpConnection {
	
	private static final Logger LOGGER = LogManager.getLogger(HttpConnection.class);

	private final HttpServerConfiguration configuration;
	private final ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartDecoder<Parameter> urlEncodedBodyDecoder; 
	private final MultipartDecoder<Part> multipartBodyDecoder;
	private final GenericWebSocketFrame.GenericFactory webSocketFrameFactory;
	private final GenericWebSocketMessage.GenericFactory webSocketMessageFactory;
	private final HeadersValidator headersValidator;
	
	private ChannelHandlerContext channelContext;
	private boolean tls;
	private boolean supportsFileRegion;
	
	private Http1xExchange requestingExchange;
	/**
	 * The responding exchange which can be replaced by an {@link Http1xErrorExchange} in case of error while processing the exchange.
	 */
	AbstractHttp1xExchange respondingExchange;
	
	private Throwable decoderError;
	private boolean read;
	private boolean flush;
	
	private Sinks.One<Void> shutdownSink;
	private Mono<Void> shutdown;
	
	private Sinks.One<Void> gracefulShutdownSink;
	private Mono<Void> gracefulShutdown;
	private ScheduledFuture<?> gracefulShutdownTimeout;
	private ChannelPromise gracefulShutdownClosePromise;
	
	private boolean closing;
	private boolean closed;
	
	/**
	 * <p>
	 * Creates an Http/1.x connection.
	 * </p>
	 *
	 * @param configuration           the server configuration
	 * @param controller              the server controller
	 * @param headerService           the header service
	 * @param parameterConverter      the parameter converter
	 * @param urlEncodedBodyDecoder   the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder    the multipart/form-data body decoder
	 * @param webSocketFrameFactory   the WebSocket frame factory
	 * @param webSocketMessageFactory the WebSocket message factory
	 * @param headersValidator        the header validator
	 */
	Http1xConnection(
			HttpServerConfiguration configuration, 
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder, 
			GenericWebSocketFrame.GenericFactory webSocketFrameFactory, 
			GenericWebSocketMessage.GenericFactory webSocketMessageFactory, 
			HeadersValidator headersValidator
		) {
		this.configuration = configuration;
		this.controller = controller;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		this.webSocketFrameFactory = webSocketFrameFactory;
		this.webSocketMessageFactory = webSocketMessageFactory;
		this.headersValidator = headersValidator;
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
	
	/**
	 * <p>
	 * Indicates whether the connection supports file region.
	 * </p>
	 * 
	 * @return true if file region is supported, false otherwise
	 */
	public boolean supportsFileRegion() {
		return this.supportsFileRegion;
	}

	@Override
	public HttpVersion getProtocol() {
		return io.inverno.mod.http.base.HttpVersion.HTTP_1_1;
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
					if(this.gracefulShutdownTimeout != null) {
						this.gracefulShutdownTimeout.cancel(false);
						this.gracefulShutdownTimeout = null;
						this.closing = false;
					}
					if(!this.closing) {
						this.closing = true;
						ChannelPromise closePromise = this.channelContext.newPromise().addListener(future -> {
							this.closed = true;
							if(future.isSuccess()) {
								this.shutdownSink.tryEmitEmpty();
								if(this.gracefulShutdownSink != null) {
									this.gracefulShutdownSink.tryEmitEmpty();
								}
							}
							else {
								this.shutdownSink.tryEmitError(future.cause());
								if(this.gracefulShutdownSink != null) {
									this.gracefulShutdownSink.tryEmitError(future.cause());
								}
							}
						});
						this.close(this.channelContext, closePromise);
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
						this.gracefulShutdownClosePromise = this.channelContext.newPromise().addListener(future -> {
							this.closed = true;
							if(future.isSuccess()) {
								this.gracefulShutdownSink.tryEmitEmpty();
							}
							else {
								this.gracefulShutdownSink.tryEmitError(future.cause());
							}
						});
						
						if(this.respondingExchange == null) {
							this.close(this.channelContext, this.gracefulShutdownClosePromise);
						}
						else {
							this.gracefulShutdownTimeout = this.channelContext.executor()
								.schedule(() -> {
									this.close(this.channelContext, this.gracefulShutdownClosePromise);
								}, this.configuration.graceful_shutdown_timeout(), TimeUnit.MILLISECONDS);
						}
					}
				})
				.subscribeOn(Schedulers.fromExecutor(this.channelContext.executor()));
		}
		return this.gracefulShutdown;
	}
	
	/**
	 * <p>
	 * Tries to shutdown the connection if a graceful shutdown is under way.
	 * </p>
	 * 
	 * <p>
	 * This should be invoked when an exchange completes, the connection is shutdown when there's no more pending exchanges.
	 * </p>
	 */
	private void tryShutdown() {
		if(this.gracefulShutdownTimeout != null && this.respondingExchange == null) {
			this.gracefulShutdownTimeout.cancel(false);
			this.close(this.channelContext, this.gracefulShutdownClosePromise);
		}
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}
	
	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
		if(this.channelContext.channel().isActive()) {
			ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
				.addListener(future -> ctx.close(promise));
		}
		else {
			ctx.close(promise);
		}
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.channelContext = ctx;
		super.handlerAdded(ctx);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.tls = ctx.pipeline().get(SslHandler.class) != null;
		this.supportsFileRegion = !this.tls && ctx.pipeline().get(HttpContentCompressor.class) == null;
		super.channelActive(ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		try {
			super.userEventTriggered(ctx, evt);
		}
		finally {
			if(evt instanceof IdleStateEvent) {
				this.exceptionCaught(ctx, new HttpServerException("Idle timeout: " + ((IdleStateEvent)evt).state()));
			}
		}
	}
	
	/**
	 * <p>
	 * Disposes all queued exchanges.
	 * </p>
	 * 
	 * <p>
	 * This is typically invoked right before the connection is shutdown OR when the connection becomes inactive (i.e. reset by peer) in order to stop processing and free resources.
	 * </p>
	 * 
	 * @param throwable an error or null if disposal does not result from an error (e.g. shutdown)
	 */
	private void dispose(Throwable throwable) {
		AbstractHttp1xExchange current = this.respondingExchange;
		while(current != null) {
			AbstractHttp1xExchange next = current.next;
			current.next = null;
			current.dispose(throwable);
			current = next;
		}
		this.respondingExchange = null;
		this.requestingExchange = null;
	}
	
	/**
	 * <p>
	 * Disposes all queued exchanges and shutdown the connection.
	 * </p>
	 * 
	 * <p>
	 * This is typically invoked after an error which put the connection in an illegal state (e.g. partial request has been sent).
	 * </p>
	 * 
	 * @param throwable an error or null if disposal does not result from an error (e.g. shutdown)
	 */
	private void disposeAndShutdown(Throwable throwable) {
		this.dispose(throwable);
		this.shutdown().subscribe();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// This shall dispose all pending exchanges
		// dispose the exchange queue
		this.dispose(new HttpServerException("Connection was closed"));
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(cause instanceof WebSocketHandshakeException || cause instanceof CorruptedWebSocketFrameException) {
			// Delegate this to the WebSocket protocol handler
			super.exceptionCaught(ctx, cause);
		}
		else {
			this.disposeAndShutdown(cause);
			LOGGER.error("Connection error", cause);
		}
	}
	
	/**
	 * <p>
	 * Callback method invoked when a decoder error is raised while reading the channel.
	 * </p>
	 * 
	 * @param cause the decoder error
	 */
	public void decoderError(Throwable cause) {
		if(this.requestingExchange == null) {
			this.disposeAndShutdown(cause);
			LOGGER.warn("Invalid object received", cause);
		}
		else if(this.requestingExchange == this.respondingExchange.unwrap()) {
			if(!this.respondingExchange.response().headers().isWritten()) {
				HttpResponseStatus status;
				if(cause instanceof TooLongHttpLineException) {
					status = HttpResponseStatus.REQUEST_URI_TOO_LONG;
				}
				else if(cause instanceof TooLongHttpHeaderException) {
					status = HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE;
				} 
				else {
					status = HttpResponseStatus.BAD_REQUEST;
				}
				this.channelContext.write(new DefaultFullHttpResponse(this.respondingExchange.version, status));
			}
			this.disposeAndShutdown(cause);
			LOGGER.warn("Invalid object received", cause);
		}
		else {
			this.decoderError = cause;
		}
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(this.closing || this.decoderError != null) {
			if(msg instanceof ReferenceCounted) {
				((ReferenceCounted)msg).release();
			}
			return;
		}
		
		if(msg == LastHttpContent.EMPTY_LAST_CONTENT) {
			if(this.requestingExchange != null && this.requestingExchange.request().getBody() != null) {
				this.requestingExchange.request().getBody().getDataSink().tryEmitComplete();
			}
		}
		else if(msg instanceof DefaultHttpRequest) {
			this.read = true;

			HttpRequest httpRequest = (HttpRequest)msg;
			Http1xExchange exchange = new Http1xExchange(
				this.configuration, 
				this.controller, 
				this.headerService, 
				this.parameterConverter, 
				this.urlEncodedBodyDecoder, 
				this.multipartBodyDecoder, 
				this.headersValidator, 
				this, 
				httpRequest
			);
			
			if(msg instanceof LastHttpContent) {
				exchange.request().body().ifPresent(body -> body.getDataSink().tryEmitComplete());
			}

			if(this.requestingExchange == null) {
				this.respondingExchange = this.requestingExchange = exchange;
				if(httpRequest.decoderResult().isFailure()) {
					this.decoderError(httpRequest.decoderResult().cause());
					return;
				}
				this.respondingExchange.start();
			}
			else {
				this.requestingExchange = this.requestingExchange.next = exchange;
				if(httpRequest.decoderResult().isFailure()) {
					this.decoderError(httpRequest.decoderResult().cause());
				}
			}
		}
		else if(msg instanceof DefaultHttpContent || msg instanceof HttpContent) {
			this.read = true;
			HttpContent content = (HttpContent)msg;
			if(content.decoderResult().isFailure()) {
				content.release();
				this.decoderError(content.decoderResult().cause());
				return;
			}
			
			if(this.requestingExchange != null) {
				Http1xRequestBody body = this.requestingExchange.request().getBody();
				if(body != null) {
					if(body.getDataSink().tryEmitNext(content.content()) != Sinks.EmitResult.OK) {
						content.release();
					}

					if(content instanceof LastHttpContent) {
						body.getDataSink().tryEmitComplete();
					}
				}
				else {
					content.release();
				}
			}
			else {
				content.release();
			}
		}
		else {
			// This is required for WebSocket
			super.channelRead(ctx, msg);
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		if(this.read) {
			this.read = false;
			if(this.flush) {
				ctx.flush();
				this.flush = false;
			}
		}
		super.channelReadComplete(ctx);
	}
	
	/**
	 * <p>
	 * Requests to write an Http object.
	 * </p>
	 * 
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param object the object to write
	 */
	public void writeHttpObject(HttpObject object) {
		this.writeHttpObject(object, this.channelContext.voidPromise());
	}
	
	/**
	 * <p>
	 * Requests to write an Http object.
	 * </p>
	 * 
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param object  the object to write
	 * @param promise a promise
	 */
	public void writeHttpObject(HttpObject object, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			if(this.read) {
				this.flush = true;
				this.channelContext.write(object, promise);
			}
			else {
				this.channelContext.writeAndFlush(object, promise);
			}
		}
		else {
			this.channelContext.executor().execute(() -> writeHttpObject(object, promise));
		}
	}
	
	/**
	 * <p>
	 * Requests to write a file region.
	 * </p>
	 * 
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param fileRegion the file region to write
	 */
	public void writeFileRegion(FileRegion fileRegion) {
		this.writeFileRegion(fileRegion, this.channelContext.voidPromise());
	}
	
	/**
	 * <p>
	 * Requests to write a file region.
	 * </p>
	 * 
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param fileRegion the file region to write
	 * @param promise    a promise
	 */
	public void writeFileRegion(FileRegion fileRegion, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			if(this.read) {
				this.flush = true;
				this.channelContext.write(fileRegion, promise);
			}
			else {
				this.channelContext.writeAndFlush(fileRegion, promise);
			}
		}
		else {
			this.channelContext.executor().execute(() -> writeFileRegion(fileRegion, promise));
		}
	}
	
	/**
	 * <p>
	 * Setups the channel pipeline to handle WebSocket and sends opening handshake.
	 * </p>
	 *
	 * <p>
	 * The resulting mono completes successfully and emits the resulting WebSocket exchange when the opening handshake succeeds or with an error when it fails in which case the channel pipeline is
	 * restored to its original state.
	 * </p>
	 * 
	 * @return a Mono emitting the WebSocket exchange on successful the opening handshake
	 */
	public Mono<GenericWebSocketExchange> writeWebSocketHandshake(String[] subProtocols) {
		return Mono.defer(() -> {
			WebSocketServerProtocolConfig.Builder webSocketConfigBuilder = WebSocketServerProtocolConfig.newBuilder()
				.websocketPath(this.respondingExchange.request().getPath())
				.subprotocols(subProtocols != null && subProtocols.length > 0 ? Arrays.stream(subProtocols).collect(Collectors.joining(",")) : null)
				.checkStartsWith(false)
				.handleCloseFrames(false)
				.dropPongFrames(true)
				.expectMaskedFrames(true)
				.closeOnProtocolViolation(false)
				.allowMaskMismatch(this.configuration.ws_allow_mask_mismatch())
				.forceCloseTimeoutMillis(this.configuration.ws_close_timeout())
				.maxFramePayloadLength(this.configuration.ws_max_frame_size())
				.allowExtensions(this.configuration.ws_frame_compression_enabled() || this.configuration.ws_message_compression_enabled());

			if(configuration.ws_handshake_timeout() > 0) {
				webSocketConfigBuilder.handshakeTimeoutMillis(configuration.ws_handshake_timeout());
			}

			WebSocketConnection webSocketConnection = new WebSocketConnection(
				this.configuration, 
				webSocketConfigBuilder.build(), 
				this.respondingExchange, 
				this.webSocketFrameFactory, 
				this.webSocketMessageFactory
			);
			
			ChannelPipeline pipeline = this.channelContext.pipeline();
			Map<String, ChannelHandler> initialChannelHandlers = pipeline.toMap();

			List<WebSocketServerExtensionHandshaker> extensionHandshakers = new LinkedList<>();
			if(this.configuration.ws_frame_compression_enabled()) {
				extensionHandshakers.add(new DeflateFrameServerExtensionHandshaker(this.configuration.ws_frame_compression_level()));
			}
			if(this.configuration.ws_message_compression_enabled()) {
				extensionHandshakers.add(new PerMessageDeflateServerExtensionHandshaker(
					this.configuration.ws_message_compression_level(),
					this.configuration.ws_message_allow_server_window_size(),
					this.configuration.ws_message_prefered_client_window_size(),
					this.configuration.ws_message_allow_server_no_context(),
					this.configuration.ws_message_preferred_client_no_context()
				));
			}
			if(!extensionHandshakers.isEmpty()) {
				pipeline.addLast(new WebSocketServerExtensionHandler(extensionHandshakers.toArray(WebSocketServerExtensionHandshaker[]::new)));
			}
			pipeline.addLast(webSocketConnection);

			this.channelContext.fireChannelRead(this.respondingExchange.request().unwrap());
		
			return webSocketConnection.getWebSocketExchange()
				.doOnError(t -> {
					for(String currentHandlerName : pipeline.toMap().keySet()) {
						if(!initialChannelHandlers.containsKey(currentHandlerName)) {
							pipeline.remove(currentHandlerName);
						}
					}

					Map<String, ChannelHandler> currentChannelHandlers = pipeline.toMap();
					if(currentChannelHandlers.size() != initialChannelHandlers.size()) {
						// We may have missing handlers
						Iterator<String> currentHandlerNamesIterator = currentChannelHandlers.keySet().iterator();
						if(currentHandlerNamesIterator.hasNext()) {
							String currentHandlerName = currentHandlerNamesIterator.next();
							for(Map.Entry<String, ChannelHandler> currentInitialHandler : initialChannelHandlers.entrySet()) {
								if(!currentInitialHandler.getKey().equals(currentHandlerName)) {
									pipeline.addBefore(currentHandlerName, currentInitialHandler.getKey(), currentInitialHandler.getValue());
								}
								else {
									if(!currentHandlerNamesIterator.hasNext()) {
										break;
									}
									currentHandlerName = currentHandlerNamesIterator.next();
								}
							}
						}
					}
				});
		});
	}
	
	/**
	 * <p>
	 * Callback method invoked when the processing of the current responding exchange completes.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop, it shutdowns the connection if the keepAlive flag is false, otherwise it starts the next exchange in the chain if any.
	 * </p>
	 */
	public void onExchangeComplete() {
		if(this.channelContext.executor().inEventLoop()) {
			if(!this.respondingExchange.keepAlive) {
				this.shutdown().subscribe();
			}
			else {
				// This is required if the request body publisher hasn't been consumed in order to free resources
				this.respondingExchange.dispose(null);
				this.respondingExchange = this.respondingExchange.next;
				if(this.respondingExchange != null) {
					if(this.respondingExchange.next != null || this.decoderError == null) {
						this.respondingExchange.start();
					}
					else {
						this.decoderError(this.decoderError);
					}
				}
				else {
					this.requestingExchange = null;
					this.tryShutdown();
				}
			}
		}
		else {
			this.channelContext.executor().execute(this::onExchangeComplete);
		}
	}
	
	/**
	 * <p>
	 * Callback method invoked when an error is raised during the processing of the current responding exchange.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop and delegates the error handling to the responding exchange (see {@link AbstractHttp1xExchange#handleError(java.lang.Throwable) }).
	 * </p>
	 * 
	 * @param throwable the error
	 */
	public void onExchangeError(Throwable throwable) {
		if(this.channelContext.executor().inEventLoop()) {
			this.respondingExchange.handleError(throwable);
		}
		else {
			this.channelContext.executor().execute(() -> this.onExchangeError(throwable));
		}
	}
}
