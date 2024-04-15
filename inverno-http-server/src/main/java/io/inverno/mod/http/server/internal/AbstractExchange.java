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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.ServerController;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.util.Strings;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * <p>
 * Base {@link Exchange} implementation.
 * </p>
 *
 * <p>
 * This class also implements the subscriber used to subscribe to exchange response data publisher.
 * </p>
 *
 * <p>
 * Implementors must provide the implementation of methods that actually send response data to the client.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractExchange extends BaseSubscriber<ByteBuf> implements Exchange<ExchangeContext> {

	private static final Logger LOGGER = LogManager.getLogger(Exchange.class);
	private static final Marker MARKER_ERROR = MarkerManager.getMarker("HTTP_ERROR");
	private static final Marker MARKER_ACCESS = MarkerManager.getMarker("HTTP_ACCESS");
	
	protected static final ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> LAST_RESORT_ERROR_CONTROLLER = exchange -> {};
	
	protected final HttpServerConfiguration configuration;
	protected final ChannelHandlerContext context;
	protected final EventExecutor contextExecutor;
	
	protected final ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller;
	protected final ExchangeContext exchangeContext;
	protected final AbstractRequest request;

	private ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> currentController;
	protected AbstractResponse response;
	protected Mono<Void> finalizer;
	private boolean finalizing;
	private boolean finalized;
	
	protected AbstractExchange.Handler handler;
	protected int transferedLength;
	private boolean reset;
	protected boolean single;
	protected boolean many;
	private ByteBuf singleChunk;
	private Throwable disposeError;
	
	/**
	 * the current subscriber:
	 * 
	 * <ul>
	 * <li>{@link ServerControllerSubscriber} subscribes on the handle returned by {@link ServerController#defer(io.inverno.mod.http.server.Exchange) }.</li>
	 * <li>{@code this} subscribes on response data stream.</li>
	 * <li>{@link ErrorHandlerSubscriber} subscribes on the handle returned by {@link ServerController#defer(io.inverno.mod.http.server.ErrorExchange)} which is invoked on errors while consuming 
	 * response data stream.</li>
	 * </ul>
	 */
	protected BaseSubscriber<?> subscriber;
	
	/**
	 * <p>
	 * Creates a server exchange with the specified channel handler context, root exchange handler, error exchange handler, request and response.
	 * </p>
	 *
	 * @param configuration the server configuration
	 * @param context       the channel handler context
	 * @param controller    the server controller
	 * @param request       the exchange request
	 * @param response      the exchange response
	 */
	public AbstractExchange(
			HttpServerConfiguration configuration,
			ChannelHandlerContext context, 
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller, 
			AbstractRequest request, 
			AbstractResponse response
		) {
		this.configuration = configuration;
		this.context = context;
		this.contextExecutor = this.context.executor();
		this.currentController = this.controller = controller;
		this.request = request;
		this.response = response;
		this.exchangeContext = controller.createContext();
		if(this.exchangeContext != null) {
			this.exchangeContext.init();
		}
	}
	
	@Override
	public AbstractRequest request() {
		return this.request;
	}

	@Override
	public AbstractResponse response() {
		return this.response;
	}
	
	@Override
	public ExchangeContext context() {
		return this.exchangeContext;
	}
	
	@Override
	public void finalizer(Mono<Void> finalizer) {
		if(this.finalizer != null) {
			this.finalizer = this.finalizer.then(finalizer);
		}
		else {
			this.finalizer = finalizer;
		}
	}
	
	/**
	 * <p>
	 * Finalizes the exchange by invoking the finalizer and the postFinalize when the final promise completes when a finalizer has been provided, otherwise the postFinalize runnable is invoked
	 * immediately.
	 * </p>
	 *
	 * <p>
	 * When using a finalizer, we have to wait for the final write operation to complete before invoking the finalizer, this basically breaks HTTP pipelining but this is mandatory to get a chance to
	 * reset shared resources used to process multiple exchanges (eg. Bytebuf).
	 * </p>
	 *
	 * @param finalPromise a promise that completes with the final exchange operation
	 * @param postFinalize a post finalize operation or null
	 *
	 * @return the promise
	 */
	public ChannelFuture finalizeExchange(ChannelPromise finalPromise, Runnable postFinalize) {
		if(this.finalizing || this.finalized) {
			return this.context.newFailedFuture(new IllegalStateException("Exchange already finalized"));
		}
		this.finalizing = true;
		ChannelPromise promise = this.context.newPromise();
		if(this.finalizer != null) {
			finalPromise.addListener(future -> {
				Mono<Void> actualFinalizer = this.finalizer;
				
				if(postFinalize != null) {
					actualFinalizer = Flux.concatDelayError(actualFinalizer, Mono.fromRunnable(postFinalize)).then();
				}
				
				actualFinalizer
					.doOnError(promise::tryFailure)
					.doOnSuccess(ign -> promise.trySuccess())
					.doOnCancel(() -> promise.tryFailure(new IllegalStateException("Finalizer was cancel")))
					.doFinally(ign -> {
//						LOGGER.debug(() -> "Exchange finalized");
						this.finalized = true;
						this.finalizing = false;
					})
					.subscribe();
			});
		}
		else if(postFinalize != null) {
			try {
				postFinalize.run();
				promise.trySuccess();
			}
			catch(Throwable t) {
				promise.tryFailure(t);
			}
			finally {
				this.finalized = true;
				this.finalizing = false;
			}
		}
		return promise;
	}
	
	/**
	 * <p>
	 * Returns the server controller.
	 * </p>
	 * 
	 * @return the server handler
	 */
	public ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> getController() {
		return this.controller;
	}
	
	/**
	 * <p>
	 * Returns the current transfered content length.
	 * </p>
	 * 
	 * @return the current transfered content length
	 */
	public long getTransferedLength() {
		return this.transferedLength;
	}
	
	/**
	 * <p>
	 * Disposes the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method delegates to {@link #dispose(java.lang.Throwable) } with a null error.
	 * </p>
	 */
	@Override
	public void dispose() {
		this.dispose(null);
	}
	
	/**
	 * <p>
	 * Disposes the exchange with the specified error.
	 * </p>
	 * 
	 * <p>
	 * This method cleans up exchange outstanding resources, it especially disposes the request which in turns drains received data if needed.
	 * </p>
	 * 
	 * <p>
	 * A non-null error indicates that the exchange did not complete successfully and that the error should be emitted when possible (e.g. in the request data publisher).
	 * </p>
	 * 
	 * @param error an error or null
	 * 
	 * @see AbstractRequest#dispose(java.lang.Throwable) 
	 */
	public void dispose(Throwable error) {
		this.disposeError = error;
		this.request.dispose(error);
		if(this.subscriber == this) {
			super.dispose();
		}
		else if(this.subscriber != null) {
			this.subscriber.dispose();
		}
	}
	
	@Override
	public boolean isDisposed() {
		if(this.handler == null) {
			return false;
		}
		if(this.subscriber != null) {
			if(this.subscriber == this) {
				return super.isDisposed();
			}
			return this.subscriber.isDisposed();
		}
		return true;
	}

	/**
	 * <p>
	 * Starts the processing of the exchange with the specified callback handler.
	 * </p>
	 *
	 * <p>
	 * This methods invokes the server root handler on the exchange and subscribe to the response data publisher.
	 * </p>
	 *
	 * @param handler an exchange callback handler
	 */
	public void start(Handler handler) {
		if(this.handler != null) {
			throw new IllegalStateException("Exchange already started");
		}
		this.handler = handler;
		this.handler.exchangeStart(this.context, this);
		try {
			Mono<Void> deferHandle = this.currentController.defer(this);
			ServerControllerSubscriber exchangeSubscriber = this.createServerControllerSubscriber();
			this.subscriber = exchangeSubscriber;
			deferHandle.subscribe(exchangeSubscriber);
		}
		catch(Throwable exchangeError) {
			this.hookOnError(exchangeError);
		}
	}
	
	/**
	 * <p>
	 * Creates the server controller subscriber used to consume the exchange deferred handle Mono supplied by {@link ServerController#defer(Exchange)}.
	 * </p>
	 * 
	 * @return the server controller subscriber
	 */
	protected ServerControllerSubscriber createServerControllerSubscriber() {
		return new ServerControllerSubscriber();
	}
	
	/**
	 * <p>
	 * Executes the specified task in the event loop.
	 * </p>
	 *
	 * <p>
	 * The tasks is executed immediately when the current thread is in the event loop, otherwise it is scheduled in the event loop.
	 * </p>
	 *
	 * @param runnable the task to execute
	 * 
	 * @return a future which completes once the task completes
	 */
	protected Future<?> executeInEventLoop(Runnable runnable) {
		if(this.contextExecutor.inEventLoop()) {
			try {
				runnable.run();
				return this.contextExecutor.newSucceededFuture(null);
			}
			catch(Throwable e) {
				return this.contextExecutor.newFailedFuture(e);
			}
		}
		else {
			return this.contextExecutor.submit(runnable);
		}
	}
	
	/**
	 * <p>
	 * Executes the specified task in the event loop.
	 * </p>
	 *
	 * <p>
	 * The tasks is executed immediately when the current thread is in the event loop, otherwise it is scheduled in the event loop.
	 * </p>
	 *
	 * @param <T> that task result type
	 * @param callable the task to execute
	 * 
	 * @return a future which completes once the task completes and returns the task result
	 */
	protected <T> Future<T> executeInEventLoop(Callable<T> callable) {
		if(this.contextExecutor.inEventLoop()) {
			try {
				return this.contextExecutor.newSucceededFuture(callable.call());
			}
			catch(Throwable t) {
				return this.contextExecutor.newFailedFuture(t);
			}
		}
		else {
			return this.contextExecutor.submit(callable);
		}
	}
	
	/**
	 * <p>
	 * Creates an error exchange handler from the exchange with the specified error.
	 * </p>
	 * 
	 * @param error the error
	 * 
	 * @return a new error exchange based on the exchange
	 */
	protected abstract ErrorExchange<ExchangeContext> createErrorExchange(Throwable error);
	
	@Override
	protected final void hookOnSubscribe(Subscription subscription) {
		this.onStart(subscription);
	}
	
	/**
	 * <p>
	 * Invokes when the exchange is started.
	 * </p>
	 *
	 * <p>
	 * The default implementation basically request an unbounded amount of events to the subscription.
	 * </p>
	 *
	 * @param subscription the subscription to the response data publisher
	 */
	protected void onStart(Subscription subscription) {
		subscription.request(1);
//		LOGGER.debug(() -> "Exchange started");
	}
	
	@Override
	protected final void hookOnNext(ByteBuf value) {
		this.transferedLength += value.readableBytes();
		if( (this.single || !this.many) && this.singleChunk == null) {
			// either we know we have a mono or we don't know yet if we have many
			this.singleChunk = value;
			this.subscriber.request(1);
		}
		else {
			// We don't have a mono and we know we have multiple chunks
			this.many = true;
			final ByteBuf firstValue = this.singleChunk;
			this.singleChunk = null;
			this.executeInEventLoop(() -> {
				ChannelPromise nextPromise = this.context.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						this.subscriber.request(1);
					}
					else if(!this.subscriber.isDisposed()) {
						this.subscriber.cancel();
						this.hookOnError(future.cause());
					}
				});
				if(firstValue != null) {
					this.onNextMany(firstValue, this.context.voidPromise());
				}
				this.onNextMany(value, nextPromise);
			})
			.addListener(future -> {
				if(!future.isSuccess() && !this.subscriber.isDisposed()) {
					this.subscriber.cancel();
					this.hookOnError(future.cause());
				}
			});
		}
	}
	
	/**
	 * <p>
	 * Invokes on an event when the response data publisher emits more than one event.
	 * </p>
	 * 
	 * <p>
	 * The specified promise will request next events on success.
	 * </p>
	 * 
	 * @param value       the event data
	 * @param nextPromise the promise used to request next event
	 */
	protected abstract void onNextMany(ByteBuf value, ChannelPromise nextPromise);
	
	/**
	 * <p>
	 * Invokes when the response data publisher completes with an error
	 * </p>
	 */
	@Override
	protected final void hookOnError(Throwable exchangeError) {
		// if headers are already written => close the connection nothing we can do
		// if headers are not already written => we should invoke the error handler
		// what we need is to continue processing
		// - create a new Response for the error
		// - reset this exchange => transferedLength, chunkCount must be reset
		// - invoke the error handler (potentially the fallback error handler) with a new ErrorExchange 
		if(this.response.isHeadersWritten()) {
			this.executeInEventLoop(() -> { 
				this.onCompleteWithError(exchangeError);
				this.logError("Fatal exchange processing error", exchangeError);
			});
		}
		else {
			this.transferedLength = 0;
			ErrorExchange<ExchangeContext> errorExchange = this.createErrorExchange(exchangeError);
			this.response = (AbstractResponse) errorExchange.response();
			try {
				Mono<Void> deferHandle = this.currentController.defer(errorExchange);
				this.executeInEventLoop(() -> {
					ErrorHandlerSubscriber errorSubscriber = new ErrorHandlerSubscriber(exchangeError);
					this.subscriber = errorSubscriber;
					deferHandle.subscribe(errorSubscriber);
				});
			} 
			catch (Throwable errorHandlerError) {
				this.logError("Error handler error", errorHandlerError);
				if(this.currentController != LAST_RESORT_ERROR_CONTROLLER) {
					this.currentController = LAST_RESORT_ERROR_CONTROLLER;
					this.hookOnError(exchangeError);
				}
				else {
					// the last resort error controller failed, there's nothing we can do anymore
					this.executeInEventLoop(() -> { 
						this.onCompleteWithError(exchangeError);
						this.logError("Fatal exchange processing error", exchangeError);
					});
				}
			}
		}
	}
	
	/**
	 * <p>
	 * Invokes on errors when there's nothing left to be done to handle the error.
	 * </p>
	 * 
	 * <p>
	 * This bascially means that there was an unrecoverable error, for instance response headers were sent but there was an error processing the response body. 
	 * </p>
	 * 
	 * @param throwable the error
	 */
	protected abstract void onCompleteWithError(Throwable throwable);
	
	@Override
	protected final void hookOnComplete() {
		if(this.transferedLength == 0) {
			if(this.response.headers().getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
				this.response.headers().contentLength(0);
			}
			this.executeInEventLoop(() -> {
				this.onCompleteEmpty();
				this.logAccess();
			});
		}
		else if(this.singleChunk != null) {
			// single chunk response
			if(this.response.headers().getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
				this.response.headers().contentLength(this.transferedLength);
			}
			if(this.request.getMethod().equals(Method.HEAD)) {
				this.executeInEventLoop(() -> {
					this.onCompleteEmpty();
					this.logAccess();
				});
			}
			else {
				this.executeInEventLoop(() -> {
					this.onCompleteSingle(this.singleChunk);
					this.logAccess();
				});
			}
		}
		else {
			this.executeInEventLoop(() -> {
				this.onCompleteMany();
				this.logAccess();
			});
		}
//		LOGGER.debug(() -> "Exchange completed");
	}
	
	/**
	 * <p>
	 * Invoked when the response data publisher completes with no data.
	 * </p>
	 */
	protected abstract void onCompleteEmpty();
	
	/**
	 * <p>
	 * Invoked when the response data publisher completes with a single data.
	 * </p>
	 * 
	 * @param value the single byte buffer
	 */
	protected abstract void onCompleteSingle(ByteBuf value);
	
	/**
	 * <p>
	 * Invoked when the response data publisher completes with many data.
	 * </p>
	 */
	protected abstract void onCompleteMany();
	
	@Override
	public void reset(long code) {
		this.executeInEventLoop(() -> {
			if(!this.reset) {
				this.reset = true;
				if(this.singleChunk != null) {
					this.singleChunk.release();
					this.singleChunk = null;
				}
				this.onReset(code);
				this.logAccess();
//				LOGGER.debug(() -> "Exchange reset");
			}
		});
	}
	
	/**
	 * <p>
	 * Invoked when the exchange has been reset leading to the response data publisher subscription being canceled.
	 * </p>
	 */
	protected abstract void onReset(long code);

	@Override
	public Optional<Throwable> getCancelCause() {
		return Optional.ofNullable(this.disposeError);
	}

	/**
	 * <p>
	 * Logs an access log message.
	 * </p>
	 */
	protected void logAccess() {
		LOGGER.info(MARKER_ACCESS, () -> new AccessLogMessage());
	}
	
	/**
	 * <p>
	 * Access log message
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.1.1
	 */
	private class AccessLogMessage implements MultiformatMessage {

		private static final long serialVersionUID = -8367544116216876788L;
		
		private static final String JSON_FORMAT = "JSON";
		
		@Override
		public String getFormat() {
			return Strings.EMPTY;
		}

		@Override
		public Object[] getParameters() {
			return new Object[] {
				this.getRemoteAddress(),
				this.getRequest(),
				this.getStatus(),
				this.getTransferedBytes(),
				this.getReferer(),
				this.getUserAgent()
			};
		}

		@Override
		public Throwable getThrowable() {
			return null;
		}
		
		@Override
		public String getFormattedMessage() {
			return this.asString();
		}

		@Override
		public String getFormattedMessage(String[] formats) {
			for(String format : formats) {
				if(format.equalsIgnoreCase(JSON_FORMAT)) {
					return this.asJson();
				}
			}
			return this.asString();
		}

		@Override
		public String[] getFormats() {
			return new String[] { "JSON" };
		}
		
		private String getRemoteAddress() {
			return ((InetSocketAddress)AbstractExchange.this.request.getRemoteAddress()).getAddress().getHostAddress();
		}
		
		private String getRequest() {
			return new StringBuilder().append(AbstractExchange.this.request.getMethod().name()).append(" ").append(AbstractExchange.this.request.getPath()).toString();
		}
		
		private int getStatus() {
			return AbstractExchange.this.response.headers().getStatusCode();
		}
		
		private int getTransferedBytes() {
			return AbstractExchange.this.transferedLength;
		}
		
		private String getReferer() {
			return AbstractExchange.this.request.headers().get(Headers.NAME_REFERER).orElse("");
		}
		
		private String getUserAgent() {
			return AbstractExchange.this.request.headers().get(Headers.NAME_USER_AGENT).orElse("");
		}
		
		private String asString() {
			StringBuilder message = new StringBuilder();
			message.append(((InetSocketAddress)AbstractExchange.this.request.getRemoteAddress()).getAddress().getHostName()).append(" ");
			message.append("\"").append(AbstractExchange.this.request.getMethod().name()).append(" ").append(AbstractExchange.this.request.getPath()).append("\" ");
			message.append(AbstractExchange.this.response.headers().getStatusCode()).append(" ");
			message.append(AbstractExchange.this.transferedLength).append(" ");
			message.append("\"").append(AbstractExchange.this.request.headers().get(Headers.NAME_REFERER).orElse("")).append("\" ");
			message.append("\"").append(AbstractExchange.this.request.headers().get(Headers.NAME_USER_AGENT).orElse("")).append("\" ");
			
			return message.toString();
		}
		
		private String asJson() {
			StringBuilder message = new StringBuilder();
			message.append("{");
			message.append("\"remoteAddress\":\"").append(this.getRemoteAddress()).append("\",");
			message.append("\"request\":\"").append(StringEscapeUtils.escapeJson(this.getRequest())).append("\",");
			message.append("\"status\":").append(this.getStatus()).append(",");
			message.append("\"bytes\":").append(this.getTransferedBytes()).append(",");
			message.append("\"referer\":\"").append(StringEscapeUtils.escapeJson(this.getReferer())).append("\",");
			message.append("\"userAgent\":\"").append(StringEscapeUtils.escapeJson(this.getUserAgent())).append("\"");
			message.append("}");
			
			return message.toString();
		}
	}
	
	/**
	 * <p>
	 * Logs an error.
	 * </p>
	 * 
	 * @param message the message
	 * @param throwable the error
	 */
	private void logError(String message, Throwable throwable) {
		if(throwable instanceof HttpException) {
			// HTTP error: typically recoverable HTTP exceptions returning a proper error to the client
			LOGGER.error(MARKER_ERROR, message, throwable);
		}
		else {
			// non HTTP error: typically unrecoverable unchecked exceptions 
			LOGGER.error(message, throwable);
		}
	}
	
	@Override
	protected void hookFinally(SignalType type) {
		this.request.dispose();
	}
	
	/**
	 * <p>
	 * Exchange callbacks handler.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see AbstractExchange#start(Handler)
	 */
	public static interface Handler {
		
		/**
		 * Default handler.
		 */
		static Handler DEFAULT = new Handler() {};
		
		/**
		 * <p>
		 * Notifies that the exchange has started.
		 * </p>
		 * 
		 * @param ctx      the channel handler context
		 * @param exchange the exchange
		 */
		default void exchangeStart(ChannelHandlerContext ctx, AbstractExchange exchange) {
			
		}

		/**
		 * <p>
		 * Notifies request data was received.
		 * </p>
		 * 
		 * @param ctx the channel handler context.
		 * @param t   the received data
		 */
		default void exchangeNext(ChannelHandlerContext ctx, ByteBuf t) {
			
		}

		/**
		 * <p>
		 * Notifies that an error was raised during the processing of the exchange.
		 * </p>
		 * 
		 * @param ctx the chanel handler context
		 * @param t   an error
		 */
		default void exchangeError(ChannelHandlerContext ctx, Throwable t) {
			this.exchangeComplete(ctx);
		}

		/**
		 * <p>
		 * Notifies that the exchange has completed.
		 * </p>
		 * 
		 * <p>
		 * This means the response has been fully sent.
		 * </p>
		 * 
		 * @param ctx the channel handler context
		 */
		default void exchangeComplete(ChannelHandlerContext ctx) {
			
		}
		
		/**
		 * <p>
		 * Notifies that the exchange has been reset.
		 * </p>
		 * 
		 * <p>
		 * This means the request might not be fully consumed and/or the response not fully sent or that indefinite response stream must be terminated leading to either the connection being closed
		 * (HTTP/1.x) or the stream to be reset (HTTP/2).
		 * </p>
		 * 
		 * @param ctx  the channel handler context
		 * @param code a code
		 */
		default void exchangeReset(ChannelHandlerContext ctx, long code) {

		}
	}
	
	/**
	 * <p>
	 * An error subscriber which is created to subscribe to the response data publisher of the error exchange created when the response data publisher completes with an error.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private final class ErrorSubscriber extends BaseSubscriber<ByteBuf> {
		
		private final Throwable exchangeError;
		
		public ErrorSubscriber(Throwable exchangeError) {
			this.exchangeError = exchangeError;
		}

		@Override
		protected void hookOnNext(ByteBuf value) {
			AbstractExchange.this.hookOnNext(value);
		}
		
		@Override
		protected void hookOnError(Throwable errorHandlerError) {
			AbstractExchange.this.logError("Error handler error", errorHandlerError);
			if(AbstractExchange.this.currentController != LAST_RESORT_ERROR_CONTROLLER) {
				AbstractExchange.this.currentController = LAST_RESORT_ERROR_CONTROLLER;
				// We should log intermediate errors but propagate the original error all the way.
				AbstractExchange.this.hookOnError(this.exchangeError);
			}
			else {
				AbstractExchange.this.executeInEventLoop(() -> { 
					AbstractExchange.this.onCompleteWithError(this.exchangeError);
					AbstractExchange.this.logError("Fatal exchange processing error", this.exchangeError);
				});
			}
		}
		
		@Override
		protected void hookOnComplete() {
			AbstractExchange.this.hookOnComplete();
			AbstractExchange.this.logError("Exchange processing error", this.exchangeError);
			
		}
	}
	
	/**
	 * <p>
	 * A subscriber to consume the exchange deferred handle Mono supplied by {@link ServerController#defer(Exchange)}. On complete it subscribes to the exchange response data publisher.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.3
	 */
	protected class ServerControllerSubscriber extends BaseSubscriber<Void> {

		@Override
		protected void hookOnError(Throwable t) {
			if(!AbstractExchange.this.reset) {
				AbstractExchange.this.hookOnError(t);
			}
		}

		@Override
		protected void hookOnComplete() {
			if(!AbstractExchange.this.reset) {
				if(AbstractExchange.this.request.getMethod().equals(Method.HEAD)) {
					AbstractExchange.this.executeInEventLoop(AbstractExchange.this::onCompleteEmpty);
				}
				else {
					AbstractExchange.this.single = AbstractExchange.this.response.isSingle();
					AbstractExchange.this.subscriber = AbstractExchange.this;
					AbstractExchange.this.response.dataSubscribe(AbstractExchange.this);
				}
			}
		}
	}
	
	/**
	 * <p>
	 * A subscriber to consume the error exchange deferred handle Mono supplied by {@link ErrorExchangeHandler#defer(Exchange)}. On complete it uses the {@link AbstractExchange#ErrorSubscriber} to
	 * subscribe to the error exchange response data publisher.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.3
	 */
	private final class ErrorHandlerSubscriber extends BaseSubscriber<Void> {

		private final Throwable exchangeError;
		
		public ErrorHandlerSubscriber(Throwable exchangeError) {
			this.exchangeError = exchangeError;
		}
		
		@Override
		protected void hookOnError(Throwable errorHandlerError) {
			AbstractExchange.this.logError("Error handler error", errorHandlerError);
			if(AbstractExchange.this.currentController != LAST_RESORT_ERROR_CONTROLLER) {
				AbstractExchange.this.currentController = LAST_RESORT_ERROR_CONTROLLER;
				AbstractExchange.this.hookOnError(this.exchangeError);
			}
			else {
				AbstractExchange.this.executeInEventLoop(() -> { 
					AbstractExchange.this.onCompleteWithError(this.exchangeError);
					AbstractExchange.this.logError("Fatal exchange processing error", this.exchangeError);
				});
			}
		}

		@Override
		protected void hookOnComplete() {
			if(!AbstractExchange.this.reset) {
				AbstractExchange.this.single = AbstractExchange.this.response.isSingle();
				ErrorSubscriber subscriber = new ErrorSubscriber(this.exchangeError);
				AbstractExchange.this.subscriber = subscriber;
				AbstractExchange.this.response.dataSubscribe(subscriber);
			}
		}
	}
}
