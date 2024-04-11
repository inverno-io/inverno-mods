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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.ConnectionResetException;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.RequestTimeoutException;
import io.inverno.mod.http.client.internal.AbstractExchange;
import io.inverno.mod.http.client.internal.EndpointExchange;
import io.inverno.mod.http.client.internal.HttpConnection;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * HTTP/1.x {@link HttpConnection} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http1xConnection extends ChannelDuplexHandler implements HttpConnection, Http1xConnectionEncoder, AbstractExchange.Handler<Http1xRequest, Http1xResponse, Http1xExchange> {
	
//	private static final Logger LOGGER = LogManager.getLogger(Http1xConnection.class);

	protected final HttpClientConfiguration configuration;
	protected final HttpVersion httpVersion;
	protected final HeaderService headerService;
	protected final ObjectConverter<String> parameterConverter;
	protected final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	protected final MultipartEncoder<Part<?>> multipartBodyEncoder;
	protected final Part.Factory partFactory;
	
	protected ChannelHandlerContext context;
	protected boolean tls;
	protected boolean supportsFileRegion;
	protected HttpConnection.Handler handler;
	
	private final Long maxConcurrentRequests;
	private final long requestTimeout;
	
	private boolean read;
	private boolean flush;

	private Runnable gracefulShutdownNotifier;
	private boolean closing;
	private boolean closed;
	
	private ScheduledFuture<?> timeoutFuture;
	
	// Corresponds to the current request being responded (the tail of the queue)
	private Http1xExchange respondingExchange;
	
	// The last requested exchange
	private Http1xExchange lastRequestedExchange;
	
	// Corresponds to the current request being requested
	private Http1xExchange requestingExchange;
	
	// New request should be chain to the head
	private Http1xExchange exchangeQueue;
	
	/**
	 * <p>
	 * Creates an HTTP/1.x connection.
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
	public Http1xConnection(
			HttpClientConfiguration configuration, 
			HttpVersion httpVersion, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartEncoder<Parameter> urlEncodedBodyEncoder, 
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory) {
		this.configuration = configuration;
		this.httpVersion = httpVersion;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
		
		this.maxConcurrentRequests = this.configuration.http1_max_concurrent_requests();
		this.requestTimeout = this.configuration.request_timeout();
	}
	
	@Override
	public boolean isTls() {
		return this.tls;
	}

	@Override
	public HttpVersion getProtocol() {
		return this.httpVersion;
	}

	@Override
	public Long getMaxConcurrentRequests() {
		return this.maxConcurrentRequests;
	}

	@Override
	public void setHandler(HttpConnection.Handler handler) {
		this.handler = handler;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.tls = ctx.pipeline().get(SslHandler.class) != null;
		this.supportsFileRegion = !this.tls && ctx.pipeline().get(HttpContentCompressor.class) == null;
		this.closed = false;
		this.context = ctx;
		super.handlerAdded(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//		LOGGER.debug("Channel inactive");
		// we must notify all pending exchanges that the connection was reset
		// this includes:
		// - the responding exchange: 
		//   - if response was set tryEmitError() on the response data publisher 
		//   - if response was not set tryEmitError() on the response sink
		//   - all this should be done in the dispose() method since the exchange knows its state
		// - the requesting exchange
		//   - dispose the request data publisher, this is also done in the dispose() method
		// - the rest of the queue: dispose()
		if(this.handler != null) {
			this.handler.onClose();
		}
		if(this.respondingExchange != null) {
			this.respondingExchange.dispose(new ConnectionResetException(this.closing || this.closed ? "Connection closed" : "Connection reset by peer"), true);
		}
		this.closed = true;
	}

	@Override
	public Mono<Void> shutdown() {
		if(this.closing || this.closed) {
			return Mono.fromRunnable(this::tryNotifyGracefulShutdown);
		}
		return Mono.<Void>create(sink -> {
			if(!this.closed && !this.closing) {
//				LOGGER.debug("Shutdown");
				this.closing = true;
				ChannelFuture closeFuture;
				if(this.context.channel().isActive()) {
					closeFuture = this.context.writeAndFlush(Unpooled.EMPTY_BUFFER)
						.addListener(ChannelFutureListener.CLOSE);
				}
				else {
					closeFuture = this.context.close();
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
		.subscribeOn(Schedulers.fromExecutor(this.context.executor()));
	}

	@Override
	public Mono<Void> shutdownGracefully() {
		if(this.closing || this.closed) {
			return Mono.empty();
		}
		return Mono.<Void>create(sink -> {
			if(!this.closed && !this.closing) {
//				LOGGER.debug("Shutdown gracefully");
				this.closing = true;

				ChannelPromise closePromise = this.context.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						sink.success();
					}
					else {
						sink.error(future.cause());
					}
				});
				if(this.exchangeQueue == null && this.respondingExchange == null) {
					if(this.context.channel().isActive()) {
						this.context.writeAndFlush(Unpooled.EMPTY_BUFFER)
							.addListener(future -> this.context.close(closePromise));
					}
					else {
						this.context.close(closePromise);
					}
				}
				else {
					// wait up to timeout for the exchange queue to be empty
					ScheduledFuture<?> gracefulTimeout = this.context.executor()
						.schedule(() -> {
							this.context.close(closePromise);
						}, this.configuration.graceful_shutdown_timeout(), TimeUnit.MILLISECONDS);
					this.gracefulShutdownNotifier = () -> {
						if(gracefulTimeout != null) {
							gracefulTimeout.cancel(false);
						}
						this.context.writeAndFlush(Unpooled.EMPTY_BUFFER)
							.addListener(future -> this.context.close(closePromise));
					};
				}
			}
			else {
				sink.success();
			}
		})
		.subscribeOn(Schedulers.fromExecutor(this.context.executor()));
	}
	
	private void tryNotifyGracefulShutdown() {
		if(this.gracefulShutdownNotifier != null && this.exchangeQueue == null && this.respondingExchange == null) {
			this.gracefulShutdownNotifier.run();
		}
	}

	@Override
	public boolean isClosed() {
		return this.closed;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if(msg instanceof HttpObject) {
				this.read = true;
				if(msg instanceof HttpResponse) {
					HttpResponse httpResponse = (HttpResponse)msg;
					if(!this.validateHttpObject(ctx, httpResponse)) {
						return;
					}
					this.respondingExchange.setResponse(new Http1xResponse(httpResponse, this.headerService, this.parameterConverter));
					// Remove the to-be-closed connection as soon as we know
					if(this.respondingExchange.isClose()) {
						if(this.handler != null) {
							this.handler.onClose();
						}
					}
				}
				else {
					Sinks.Many<ByteBuf> responseData = this.respondingExchange.response().data();

					if(msg == LastHttpContent.EMPTY_LAST_CONTENT) {
						if(responseData != null) {
							responseData.tryEmitComplete();
							this.respondingExchange.notifyComplete();
						}
					}
					else {
						HttpContent httpContent = (HttpContent)msg;
						if(!this.validateHttpObject(ctx, httpContent)) {
							return;
						}

						if(responseData != null) {
							if(responseData.tryEmitNext(httpContent.content()) != Sinks.EmitResult.OK) {
								// TODO we must make sure that after data are drained the next exchange is actually started
								httpContent.release();
							}

							if(httpContent instanceof LastHttpContent) {
								HttpHeaders trailingHeaders = ((LastHttpContent)httpContent).trailingHeaders();
								if(trailingHeaders != null) {
									this.respondingExchange.response().setResponseTrailers(new Http1xResponseTrailers(trailingHeaders, this.headerService, this.parameterConverter));
								}
								responseData.tryEmitComplete();
								this.respondingExchange.notifyComplete();
							}
						}
						else {
							httpContent.release();
						}
					}
				}
			}
			else {
				// WebSocket
				super.channelRead(ctx, msg);
			}
		}
		finally {
			if(this.respondingExchange != null) {
				this.respondingExchange.lastModified = System.currentTimeMillis();
			}
		}
	}
	
	/**
	 * <p>
	 * Validates the HTTP object.
	 * </p>
	 * 
	 * @param ctx the channel context
	 * @param httpObject the HTTP object to validate
	 * 
	 * @return true if the object is valid, false otherwise
	 * @throws Exception if {@link #exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)} throws an exception
	 */
	private boolean validateHttpObject(ChannelHandlerContext ctx, HttpObject httpObject) throws Exception {
		DecoderResult result = httpObject.decoderResult();
		if(result.isFailure()) {
			this.exceptionCaught(ctx, result.cause());
			return false;
		}
		else if(httpObject instanceof HttpResponse) {
			io.netty.handler.codec.http.HttpVersion version = ((HttpResponse) httpObject).protocolVersion();
			if (version != io.netty.handler.codec.http.HttpVersion.HTTP_1_0 && version != io.netty.handler.codec.http.HttpVersion.HTTP_1_1) {
				this.exceptionCaught(ctx, new IllegalStateException("Unsupported protocol: " + version));
				return false;
			}
		}
		return true;
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
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//		LOGGER.debug("Exception caught", cause);
		// In any case only network related error should get here anything else must be handled upstream
		this.cancelTimeout();
		// Evict the faulty connection
		if(this.handler != null) {
			this.handler.onError(cause);
		}

		// Dispose all pending exchanges including inflight exchanges
		if(this.respondingExchange != null) {
			this.respondingExchange.dispose(cause, true);
		}
		this.exchangeQueue = null;
		this.requestingExchange = null;
		this.respondingExchange = null;
		
		// close the faulty connection
		this.shutdown().subscribe();
	}
	
	@Override
	public ChannelFuture writeFrame(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		if(this.read) {
			this.flush = true;
			return ctx.write(msg, promise);
		}
		else {
			return ctx.writeAndFlush(msg, promise);
		}
		// Servers might not send response chunk while the request is being received (missing flush) this might be an issue for timeouts but also in general
	}

	@Override
	public <A extends ExchangeContext> Mono<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> send(EndpointExchange<A> endpointExchange) {
		if(this.closed || this.closing) {
			return Mono.error(new HttpClientException("Connection closed"));
		}
		return Mono.<HttpConnectionExchange<ExchangeContext, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>>create(exchangeSink -> {
			Http1xRequest http1xRequest = new Http1xRequest(this.context, this.tls, this.supportsFileRegion, this.parameterConverter, endpointExchange.request());
			
			try {
				// This must be thread safe as multiple threads can change the exchange queue
				EventLoop eventLoop = this.context.channel().eventLoop();
				if(eventLoop.inEventLoop()) {
					this.createAndRegisterExchange(this.context, exchangeSink, endpointExchange.context(), http1xRequest);
				}
				else {
					eventLoop.submit(() -> {
						this.createAndRegisterExchange(this.context, exchangeSink, endpointExchange.context(), http1xRequest);
					});
				}
			}
			catch(Throwable t) {
				exchangeSink.error(t);
			}
		})
		.map(exchange -> (HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>)exchange);
	}
	
	/**
	 * <p>
	 * Creates the HTTP/1.x exchange and registers it in the connection.
	 * </p>
	 *
	 * <p>
	 * This method is invoked when sending the request to create and register the exchange in the connection. A registered exchange is added to the HTTP/1.x connection's exchange queue, corresponding
	 * requests are sent in sequence to the remote endpoint.
	 * </p>
	 *
	 * @param context         the channel context
	 * @param exchangeSink    the exchange sink
	 * @param exchangeContext the exchange context
	 * @param request         the HTTP/1.x request
	 */
	private void createAndRegisterExchange(ChannelHandlerContext context, MonoSink<HttpConnectionExchange<ExchangeContext, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> exchangeSink, ExchangeContext exchangeContext, Http1xRequest request) {
		Http1xExchange exchange = this.createExchange(context, exchangeSink, exchangeContext, request);
		exchange.lastModified = System.currentTimeMillis();
		if(this.exchangeQueue == null) {
			this.exchangeQueue = exchange;
			this.exchangeQueue.start(this);
		}
		else {
			this.exchangeQueue.next = exchange;
			this.exchangeQueue = exchange;
			if(this.requestingExchange == null) {
				this.exchangeQueue.start(this);
			}
		}
	}
	
	/**
	 * <p>
	 * Creates the HTTP/1.x exchange.
	 * </p>
	 * 
	 * <p>
	 * This method shall be overridden to provide specific exchange implementation for protocol upgrade.
	 * </p>
	 * 
	 * @param context                 the channel context
	 * @param exchangeSink            the exchange sink
	 * @param exchangeContext         the exchange context
	 * @param request                 the HTTP/1.x request
	 * 
	 * @return a new HTTP/1.x exchange
	 */
	protected Http1xExchange createExchange(ChannelHandlerContext context, MonoSink<HttpConnectionExchange<ExchangeContext, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> exchangeSink, ExchangeContext exchangeContext, Http1xRequest request) {
		return new Http1xExchange(context, exchangeSink, exchangeContext, this.httpVersion, request, this);
	}

	/**
	 * <p>
	 * Starts the request timeout.
	 * </p>
	 */
	private void startTimeout() {
		if(this.timeoutFuture != null) {
			return;
		}
		long nextTimeout = -1;
		if(this.respondingExchange != null) {
			long currentTimestamp = System.currentTimeMillis();
			Http1xExchange previous = null;
			Http1xExchange current = this.respondingExchange;
			do {
				long currentTimeout = this.requestTimeout - (currentTimestamp - current.lastModified);
				if(currentTimeout <= 0) {
					// if the exchange request has not been sent we can simply remove it otherwise we can already kill the connection
					// can this happen here?
					// not when this is invoked in exchangeStart
					// when invoked in exchangeComplete it might, well no: because we always consider the nearest timeout timestamp so it should have already been triggered
					// nonetheless we can have overlap so let's blow properly
					RequestTimeoutException timeoutError = new RequestTimeoutException("Exceeded timeout " + this.requestTimeout + "ms");
					if(current.request().isHeadersWritten()) {
						// We have an inflight request that has timed out we must report the error and close the whole connection 
						// Evict the faulty connection
						if(this.handler != null) {
							this.handler.onError(timeoutError);
						}
						// Dispose all pending exchanges including inflight exchanges
						if(this.respondingExchange != null) {
							this.respondingExchange.dispose(timeoutError, true);
						}
						this.requestingExchange = null;
						this.respondingExchange = null;
						
						// close the faulty connection
						Http1xConnection.this.shutdown().subscribe();

						// Do not start the timeout since connection is closed
						return;
					}
					else if(previous != null) {
						// Remove the timed out exchange which is not inflight
						current.dispose(timeoutError, false);
						// we can recycle the connection since a request has been processed
						if(this.handler != null) {
							this.handler.onExchangeTerminate(current);
						}
						previous.next = current.next;
					}
					else {
						// The timed out exchange is the responding exchange which has not been requested yet (i.e. it is also the requesting exchange)
						// This can only happen when invoked in exchangeStart() in case the second System.currentTyimeMillis() is after the time out
						throw timeoutError;
					}
				}
				nextTimeout = nextTimeout == -1 ? currentTimeout : Math.min(currentTimeout, nextTimeout);
				current = current.next;
			} while(current != null);
		}
		
		if(nextTimeout >= 0) {
			this.timeoutFuture = this.context.channel().eventLoop().schedule(
				() -> {
					this.timeoutFuture = null;
					this.startTimeout();
				}, nextTimeout, TimeUnit.MILLISECONDS);
		}
	}
	
	/**
	 * <p>
	 * Cancels the request timeout.
	 * </p>
	 */
	private void cancelTimeout() {
		if(this.timeoutFuture != null) {
			this.timeoutFuture.cancel(false);
			this.timeoutFuture = null;
		}
	}
	
	@Override
	public void exchangeStart(Http1xExchange exchange) {
//		LOGGER.debug("Exchange start");
		// This method MUST be invoked once for a given exchange
		this.requestingExchange = exchange;
		if(System.currentTimeMillis() - exchange.lastModified > this.requestTimeout) {
			throw new RequestTimeoutException("Exceeded timeout " + this.requestTimeout + "ms");
		}
		if(this.respondingExchange == null) {
			this.respondingExchange = exchange;
			this.startTimeout();
		}
	}
	
	@Override
	public void requestComplete(Http1xExchange exchange) {
//		LOGGER.debug("Exchange request complete");
		if(exchange == this.requestingExchange) {
			this.lastRequestedExchange = this.requestingExchange;
			this.requestingExchange = null;
			this.requestingExchange = null;
			if(this.lastRequestedExchange.next != null) {
				this.lastRequestedExchange.next.start(this);
			}
		}
	}
	
	@Override
	public void exchangeError(Http1xExchange exchange, Throwable t) {
//		LOGGER.debug("Exchange error", t);
		if(exchange == this.requestingExchange) {
			// The faulty exchange is the current requesting exchange
			if(this.requestingExchange.request().isHeadersWritten()) {
				// We have sent a partial request, we can't know the connection state
				try {
					this.exceptionCaught(this.context, t);
				}
				catch(Exception e) {
					this.context.fireExceptionCaught(e);
				}
			}
			else {
				// We don't have initiated anything on the server so we can just ignore the exchange
				this.requestingExchange.dispose(t);
				// we can recycle the connection since the exchange has been processed
				if(this.handler != null) {
					this.handler.onExchangeTerminate(this.requestingExchange);
				}
				
				if(exchange == this.respondingExchange) {
					this.cancelTimeout();
					this.respondingExchange = null;
				}
				if(this.lastRequestedExchange != null) {
					this.lastRequestedExchange = this.requestingExchange.next;
				}

				Http1xExchange nextRequestingExchange = this.requestingExchange.next;
				this.requestingExchange = null;
				if(nextRequestingExchange != null) {
					nextRequestingExchange.start(this);
				}
			}
		}
		else if(exchange == this.respondingExchange) {
			// The faulty exchange is the current responding exchange
			// just try to dispose it with the error, exchangeComplete() should be invoked eventually
			this.respondingExchange.dispose(t);
		}
	}

	@Override
	public void exchangeComplete(Http1xExchange exchange) {
//		LOGGER.debug("Exchange complete");
		// we have only one responding exchange at a given time
		// checking that it matches the exchange protects against multiple invocations
		if(this.respondingExchange == exchange) {
			this.cancelTimeout();
			
			// we can recycle the connection since a request has been processed
			if(this.respondingExchange.isClose()) {
				// We must close the connection
				this.respondingExchange.dispose(true);
				// Connection should already have been removed from the pool, make sure connection is closed
				this.shutdown().subscribe();
			}
			else {
				// We must dispose the exchange in order to free the response data publisher if it wasn't subscribed
				// We received the complete response we can dispose the exchange
				this.respondingExchange.dispose();
				// we can recycle the connection since a request has been processed
				if(this.handler != null) {
					this.handler.onExchangeTerminate(this.respondingExchange);
				}
				
				if(this.respondingExchange.next != null) {
					this.respondingExchange = respondingExchange.next;
					this.startTimeout();
				}
				else {
					this.respondingExchange = null;
					this.exchangeQueue = null;
					this.tryNotifyGracefulShutdown();
				}
			}
		}
	}

	@Override
	public void exchangeReset(Http1xExchange exchange, long code) {
//		LOGGER.debug("Exchange reset: code={}", code);
		this.exchangeError(exchange, new HttpClientException("Exchange has been reset: " + code));
	}
}
