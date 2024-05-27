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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.client.ConnectionResetException;
import io.inverno.mod.http.client.ConnectionTimeoutException;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.RequestTimeoutException;
import io.inverno.mod.http.client.internal.EndpointExchange;
import io.inverno.mod.http.client.internal.HttpConnection;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Http/1.x {@link HttpConnection} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http1xConnection extends ChannelDuplexHandler implements HttpConnection {
	
	private static final Logger LOGGER = LogManager.getLogger(HttpConnection.class);

	protected final HttpClientConfiguration configuration;
	protected final HttpVersion protocol;
	protected final HeaderService headerService;
	protected final ObjectConverter<String> parameterConverter;
	protected final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	protected final MultipartEncoder<Part<?>> multipartBodyEncoder;
	protected final Part.Factory partFactory;

	private final io.netty.handler.codec.http.HttpVersion version;
	private final HeadersValidator headersValidator;
	private final Long maxConcurrentRequests;
	
	protected ChannelHandlerContext channelContext;
	private Scheduler scheduler;
	private boolean tls;
	private boolean supportsFileRegion;
	protected HttpConnection.Handler handler;

	private Http1xExchange<?> respondingExchange;
	private Http1xExchange<?> requestingExchange;
	private Http1xExchange<?> queuedExchange;
	
	private Throwable requestError;
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
	 * @param configuration         the HTTP client configurartion
	 * @param httpVersion           the HTTP/1.x protocol version
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyEncoder the URL encoded body encoder
	 * @param multipartBodyEncoder  the multipart body encoder
	 * @param partFactory           the part factory
	 */
	Http1xConnection(
			HttpClientConfiguration configuration, 
			HttpVersion protocol, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartEncoder<Parameter> urlEncodedBodyEncoder, 
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory
		) {
		this.configuration = configuration;
		this.protocol = protocol;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
		
		switch(protocol) {
			case HTTP_1_0: this.version = io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
				break;
			case HTTP_1_1: this.version = io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
				break;
			default: throw new IllegalStateException("Invalid protocol version: " + protocol);
		}
		
		this.headersValidator = configuration.http1x_validate_headers() ? HeadersValidator.DEFAULT_HTTP1X_HEADERS_VALIDATOR : null;
		this.maxConcurrentRequests = this.configuration.http1_max_concurrent_requests();
	}
	
	/**
	 * <p>
	 * Return the {@link ByteBufAllocator} assigned to the channel.
	 * </p>
	 * 
	 * @return the ByteBuf allocator
	 */
	public ByteBufAllocator alloc() {
		return this.channelContext.alloc();
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
		return this.protocol;
	}
	
	/**
	 * <p>
	 * Returns Netty's Http protocol version.
	 * </p>
	 * 
	 * @return the http version
	 */
	public io.netty.handler.codec.http.HttpVersion getVersion() {
		return this.version;
	}
	
	@Override
	public SocketAddress getLocalAddress() {
		return this.channelContext.channel().localAddress();
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return Optional.ofNullable(this.channelContext.pipeline().get(SslHandler.class))
			.map(sslHandler -> sslHandler.engine().getSession().getLocalCertificates())
			.filter(certificates -> certificates.length > 0);
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.channelContext.channel().remoteAddress();
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return Optional.ofNullable(this.channelContext.pipeline().get(SslHandler.class))
			.map(sslHandler -> {
				try {
					return sslHandler.engine().getSession().getPeerCertificates();
				} 
				catch(SSLPeerUnverifiedException e) {
					LOGGER.debug("Could not verify identity of the client", e);
					return null;
				}
			})
			.filter(certificates -> certificates.length > 0);
	}

	@Override
	public Long getMaxConcurrentRequests() {
		return this.maxConcurrentRequests;
	}

	@Override
	public void setHandler(HttpConnection.Handler handler) {
		this.handler = handler;
	}
	
	/**
	 * <p>
	 * Registers the exchange in the connection.
	 * </p>
	 * 
	 * <p>
	 * A registered exchange is added to the Http/1.x connection's exchange queue, exchanges are started and corresponding requests sent in sequence to the remote endpoint.
	 * </p>
	 * 
	 * @param exchange the exchange to register
	 */
	private void registerExchange(Http1xExchange exchange) {
		if(this.closed || this.closing || this.requestError != null) {
			throw new HttpClientException("Connection was closed");
		}
		
		if(this.queuedExchange == null) {
			this.queuedExchange = exchange;
		}
		else {
			this.queuedExchange.next = exchange;
			this.queuedExchange = exchange;
		}

		if(this.requestingExchange == null) {
			this.requestingExchange = exchange;
			if(this.respondingExchange == null) {
				this.respondingExchange = exchange;
			}
			this.requestingExchange.start();
		}
	}
	
	/**
	 * <p>
	 * Creates the Http/1.x exchange
	 * </p>
	 * 
	 * @param <A> the type of the exchange context
	 * @param sink the exchange sink
	 * @param endpointExchange the endpoint exchange
	 * 
	 * @return a new Http/1.x exchange
	 */
	<A extends ExchangeContext> Http1xExchange createExchange(Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink, EndpointExchange<A> endpointExchange) {
		return new Http1xExchange<>(this.configuration, sink, this.headerService, this.parameterConverter, endpointExchange.context(), this, endpointExchange.request());
	}

	@Override
	public <A extends ExchangeContext> Mono<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> send(EndpointExchange<A> endpointExchange) {
		if(this.closed || this.closing || this.requestError != null) {
			return Mono.error(new HttpClientException("Connection was closed"));
		}
		
		endpointExchange.request().getHeaders().getUnderlyingHeaders().setValidator(this.headersValidator);
		
		Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink = Sinks.one();
		Http1xExchange exchange = this.createExchange(sink, endpointExchange);
		exchange.init();
		return sink.asMono()
			.doOnSubscribe(ign -> this.registerExchange(exchange))
			.subscribeOn(this.scheduler);
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
						if(this.handler != null) {
							this.handler.close();
						}
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
				.subscribeOn(this.scheduler);
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
						if(this.handler != null) {
							this.handler.close();
						}
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
						
						if(this.queuedExchange == null && this.respondingExchange == null) {
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
				.subscribeOn(this.scheduler);
		}
		return this.gracefulShutdown;
	}
	
	/**
	 * <p>
	 * Tries to shutdown the connection if a graceful shutdown is under way.
	 * </p>
	 * 
	 * <p>
	 * This is invoked everytime an exchange completes, the connection is shutdown when there's no more inflight exchanges.
	 * </p>
	 */
	private void tryShutdown() {
		if(this.gracefulShutdownTimeout != null && this.queuedExchange == null && this.respondingExchange == null) {
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
		this.scheduler = Schedulers.fromExecutor(ctx.executor());
		super.handlerAdded(ctx);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.tls = ctx.pipeline().get(SslHandler.class) != null;
		this.supportsFileRegion = !this.tls && ctx.pipeline().get(HttpContentCompressor.class) == null;
		super.channelActive(ctx);
		this.closed = false;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		try {
			super.userEventTriggered(ctx, evt);
		}
		finally {
			if(evt instanceof IdleStateEvent && this.respondingExchange != null) {
				this.exceptionCaught(ctx, new ConnectionTimeoutException("Idle timeout: " + ((IdleStateEvent)evt).state()));
			}
		}
	}
	
	/**
	 * <p>
	 * Disposes all queued exchanges.
	 * </p>
	 * 
	 * <p>
	 * The is typically invoked right before the connection is shutdown OR when the connection becomes inactive (i.e. reset by peer) in order to stop processing and free resources.
	 * </p>
	 * 
	 * @param throwable an error or null if disposal does not result from an error (e.g. shutdown)
	 */
	private void dispose(Throwable throwable) {
		Http1xExchange<?> current = this.respondingExchange;
		while(current != null) {
			Http1xExchange<?> next = current.next;
			current.next = null;
			current.dispose(throwable);
			current = next;
		}
		this.respondingExchange = this.requestingExchange = this.queuedExchange = null;
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
		if(this.handler != null) {
			this.handler.close();
		}
		this.dispose(new ConnectionResetException(this.closing || this.closed ? "Connection was closed" : "Connection reset by peer"));
		this.closed = true;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(this.handler != null) {
			this.handler.close();
		}
		this.disposeAndShutdown(cause);
		LOGGER.error("Connection error", cause);
	}
	
	/**
	 * <p>
	 * Callback method invoked when a decoder error is raised while reading the channel.
	 * </p>
	 * 
	 * @param cause the decoder error
	 */
	public void decoderError(Throwable cause) {
		this.disposeAndShutdown(cause);
		LOGGER.warn("Invalid object received", cause);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(this.respondingExchange == null) {
			if(msg instanceof ReferenceCounted) {
				((ReferenceCounted)msg).release();
			}
			return;
		}
		
		if(msg == LastHttpContent.EMPTY_LAST_CONTENT) {
			if(this.respondingExchange.response() != null) {
				this.respondingExchange.response().body().getDataSink().tryEmitComplete();
				this.onResponseComplete();
			}
		}
		else if(msg instanceof DefaultHttpResponse) {
			this.read = true;
			HttpResponse httpResponse = (HttpResponse)msg;
			if(httpResponse.decoderResult().isFailure()) {
				this.decoderError(httpResponse.decoderResult().cause());
			}
			else {
				this.respondingExchange.emitResponse(httpResponse);
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
			
			if(this.respondingExchange.response().body().getDataSink().tryEmitNext(content.content()) != Sinks.EmitResult.OK) {
				content.release();
			}
			if(content instanceof LastHttpContent) {
				HttpHeaders trailingHeaders = ((LastHttpContent)content).trailingHeaders();
				if(trailingHeaders != null) {
					this.respondingExchange.response().setTrailers((LinkedHttpHeaders)trailingHeaders);
				}
				this.respondingExchange.response().body().getDataSink().tryEmitComplete();
				this.onResponseComplete();
			}
		}
		else {
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
	 * Callback method invoked when the requesting exchange request has been sent to the server.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop, it starts the next pending exchange if any.
	 * </p>
	 */
	public void onRequestSent() {
		if(this.channelContext.executor().inEventLoop()) {
			this.requestingExchange = this.requestingExchange.next;
			if(this.requestingExchange != null) {
				this.requestingExchange.start();
			}
		}
		else {
			this.channelContext.executor().execute(this::onRequestSent);
		}
	}
	
	/**
	 * <p>
	 * Callback method invoked when an error is raised while sending the requesting exchange request to the server or when the request times out.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop, when an error is raised while sending a request, the connection is shutdown after all inflight exchanges have completed if the request was
	 * partially sent to the server (i.e. headers were written), otherwise the connection is recycled, the faulty exchange removed from the queue and the next exchange, if any, is started.
	 * </p>
	 * 
	 * <p>
	 * It also handles request timeout errors. Unlike above request errors, timeout errors are not specific to the requesting exchange and any request can time out.
	 * </p>
	 * 
	 * <p>
	 * In general we should look at the exchange queue as follows and consider its properties:
	 * </p>
	 * 
	 * <pre>{@code
	 * 
	 *          [respondingExchange] -----(requestedExchange*)-----> [requestingExchange] -----[...]-----> [queuedExchange]}</pre>
	 * 
	 * <ul>
	 * <li>Since the request timeout is constant exchanges time out in sequence as a result we have {@code timedOutExchange == respondingExchange.next}.</li>
	 * </ul>
	 * 
	 * <p>
	 * The following actions are taken when any of above exchange times out:
	 * </p>
	 * 
	 * <ul>
	 *   <li>{@link #respondingExchange}</li>
	 *   <ul>
	 *     <li>Before headers have been sent:</li>
	 *     <ul>
	 *       <li>This happens when a request took too much time to be sent in which case we have {@code timedOutExchange == respondingExchange == requestingExchange}.</li>
	 *       <li>The responding exchange is disposed to stop the processing and the next exchange, if any, is started.</li>
	 *     </ul>
	 *     <li>After headers have been sent:</li>
	 *     <ul>
	 *       <li>This happens when the request has been sent but the server took too much time to respond.</li>
	 *       <li>The connection is shutdown right away.</li>
	 *     </ul>
	 *   </ul>
	 *   <li>{@code requestedExchange}</li>
	 *   <ul>
	 *     <li>This happens when the request has been sent but the responding exchange is taking too much time to complete in which case we have {@code timedOutExchange == respondingExchange.next} as 
	 * per above properties.</li>
	 *     <li>The requested exchange is disposed as well as following exchanges and the connection is shutdown after the responding exchange completes.</li>
	 *   </ul>
	 *   <li>{@link #requestingExchange}</li>
	 *   <ul>
	 *     <li>Before headers have been sent:</li>
	 *       <li>This happens when a request took too much time to be sent in which case we have {@code timedOutExchange == requestingExchange == respondingExchange.next} as per above properties.</li>
	 *       <li>The requesting exchange is disposed, the next exchange, if any, is started </li>
	 *     <li>After headers have been sent:</li>
	 *       <li>This happens when the request has been partially sent and no response and no response was received because the responding exchange is taking too much time to complete, we are 
	 * considering {@code timedOutExchange == requestingExchange == respondingExchange.next} as per above properties, see above for {@code respondingExchange == requestingExchange} case.</li>
	 *       <li>The requested exchange is disposed as well as following exchanges and the connection is shutdown after the responding exchange completes.</li>
	 *   </ul>
	 *   <li>{@link #queuedExchange}</li>
	 *   <ul>
	 *     <li>This happens when the responding exchange request hasn't been fully sent, a response has been but the exchange is not complete in which case we have 
	 * {@code respondingExchange == requestingExchange} and {@code timedOutExchange == respondingExchange.next} as per above properties.</li>
	 *     <li>The first queued exchange is disposed and removed from the queue</li>
	 *   </ul>
	 * </ul>
	 * 
	 * @param throwable the error
	 */
	public void onRequestError(Throwable throwable) {
		if(this.channelContext.executor().inEventLoop()) {
			Throwable adjustedThrowable = throwable;
			if(throwable instanceof RequestTimeoutException) {
				adjustedThrowable = new ConnectionResetException("Connection closed after previous request timed out", throwable);
				// either the responding exchange or the next one
				Http1xExchange<?> timedOutExchange;
				if(this.respondingExchange.response() == null) {
					timedOutExchange = this.respondingExchange;
				}
				else {
					timedOutExchange = this.respondingExchange.next;
				}
				
				timedOutExchange.dispose(throwable);
				if(timedOutExchange != this.requestingExchange) {
					if(timedOutExchange.request().isHeadersWritten()) {
						if(this.handler != null) {
							this.handler.close();
						}
						
						adjustedThrowable = new ConnectionResetException("Connection closed after previous request timed out", throwable);
						if(timedOutExchange == this.respondingExchange) {
							this.disposeAndShutdown(adjustedThrowable);
						}
						else {
							this.requestError = throwable;
							// we must wait for the responding exchange to complete before closing the connection
							Http1xExchange<?> current = timedOutExchange.next;
							while(current != null) {
								Http1xExchange<?> next = current.next;
								current.next = null;
								current.dispose(adjustedThrowable);
								current = next;
							}
							this.requestingExchange = this.queuedExchange = null;
						}
					}
					else {
						// Queued exchange
						if(this.handler != null) {
							this.handler.recycle();
						}
						this.respondingExchange.next = timedOutExchange.next;
					}
					return;
				}
			}
			
			if(this.requestingExchange.request().isHeadersWritten()) {
				// make sure no new exchange are submitted
				if(this.handler != null) {
					this.handler.close();
				}
				
				if(this.respondingExchange == this.requestingExchange) {
					this.disposeAndShutdown(adjustedThrowable);
				}
				else {
					this.requestError = adjustedThrowable;
					// we must wait for previous exchanges to complete before closing the connection
					Http1xExchange<?> current = this.requestingExchange;
					while(current != null) {
						Http1xExchange<?> next = current.next;
						current.next = null;
						current.dispose(adjustedThrowable);
						current = next;
					}
					this.requestingExchange = this.queuedExchange = null;
				}
			}
			else {
				// We haven't sent anything: just dispose the exchange and start the next one
				this.requestingExchange.dispose(adjustedThrowable);
				if(this.handler != null) {
					this.handler.recycle();
				}
				if(this.requestingExchange.next != null) {
					if(this.requestingExchange == this.respondingExchange) {
						this.respondingExchange = this.requestingExchange.next;
					}
					else {
						Http1xExchange<?> lastRequestedExchange = this.respondingExchange;
						while(lastRequestedExchange.next != this.requestingExchange) {
							lastRequestedExchange = lastRequestedExchange.next;
						}
						lastRequestedExchange.next = this.requestingExchange.next;
					}
					this.requestingExchange = this.requestingExchange.next;
					this.requestingExchange.start();
				}
				else {
					if(this.requestingExchange == this.respondingExchange) {
						this.respondingExchange = this.requestingExchange = this.queuedExchange = null;
					}
					else {
						Http1xExchange<?> lastRequestedExchange = this.respondingExchange;
						while(lastRequestedExchange.next != this.requestingExchange) {
							lastRequestedExchange = lastRequestedExchange.next;
						}
						
						lastRequestedExchange.next = this.requestingExchange = null;
						this.queuedExchange = lastRequestedExchange;
					}
				}
			}
		}
		else {
			this.channelContext.executor().execute(() -> this.onRequestError(throwable));
		}
	}
	
	/**
	 * <p>
	 * Callback method invoked when the responding exchange response has been fully received.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop, it shutdowns the connection when the server responded with {@code connection: close} header, otherwise responding exchange is disposed and the
	 * connection recycled.
	 * </p>
	 */
	public void onResponseComplete() {
		if(this.channelContext.executor().inEventLoop()) {
			if(this.respondingExchange.response().headers().contains(Headers.NAME_CONNECTION, Headers.VALUE_CLOSE)) {
				this.shutdown().subscribe();
			}
			else {
				this.respondingExchange.dispose(null);
				if(this.handler != null) {
					this.handler.recycle();
				}
				
				Http1xExchange<?> completedExchange = this.respondingExchange;
				this.respondingExchange = this.respondingExchange.next;
				if(this.respondingExchange == null) {
					this.respondingExchange = this.requestingExchange = this.queuedExchange = null;
					this.tryShutdown();
				}
				else if(this.requestError != null && this.respondingExchange.next == null) {
					this.respondingExchange = this.requestingExchange = this.queuedExchange = null;
					this.shutdown().subscribe();
				}
				else if(completedExchange == this.requestingExchange) {
					this.requestingExchange = this.respondingExchange;
					this.requestingExchange.start();
				}
			}
		}
		else {
			this.channelContext.executor().execute(this::onResponseComplete);
		}
	}
}
