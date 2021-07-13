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

import java.net.InetSocketAddress;
import java.util.function.Supplier;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.util.Strings;
import org.reactivestreams.Subscription;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * <p>
 * Base {@link Exchange} implementation.
 * </p>
 * 
 * <p>
 * This class also implements the subscriber used to subscribe to exchange
 * response data publisher.
 * </p>
 * 
 * <p>
 * Implementors must basic provide the implementation of methods that actually
 * send response data to the client.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractExchange extends BaseSubscriber<ByteBuf> implements Exchange {

	private static final Logger LOGGER = LogManager.getLogger(AbstractExchange.class);
	private static final Marker MARKER_ERROR = MarkerManager.getMarker("HTTP_ERROR");
	private static final Marker MARKER_ACCESS = MarkerManager.getMarker("HTTP_ACCESS");
	
	protected final ChannelHandlerContext context;
	protected final EventExecutor contextExecutor;
	
	protected final ExchangeHandler<Exchange> rootHandler;
	protected final ExchangeHandler<ErrorExchange<Throwable>> errorHandler;
	
	protected final AbstractRequest request;
	protected AbstractResponse response;
	
	protected Mono<Void> finalizer;
	
	protected Handler handler;
	
	protected int transferedLength;

	protected boolean single;
	private ByteBuf singleChunk;
	
	private ErrorSubscriber errorSubscriber;
	
	protected static final ExchangeHandler<ErrorExchange<Throwable>> LAST_RESORT_ERROR_HANDLER = new GenericErrorHandler();
	
	/**
	 * <p>
	 * Creates an exchange with the specified channel handler context, root exchange
	 * handler, error exchange handler, request and response.
	 * </p>
	 * 
	 * @param context      the channel handler context
	 * @param rootHandler  the server root exchange handler
	 * @param errorHandler the server error exchange handler
	 * @param request      the exchange request
	 * @param response     the exchange response
	 */
	public AbstractExchange(ChannelHandlerContext context, ExchangeHandler<Exchange> rootHandler, ExchangeHandler<ErrorExchange<Throwable>> errorHandler, AbstractRequest request, AbstractResponse response) {
		this.context = context;
		this.contextExecutor = this.context.executor();
		this.rootHandler = rootHandler;
		this.errorHandler = errorHandler;
		this.request = request;
		this.response = response;
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
	public AbstractExchange finalizer(Mono<Void> finalizer) {
		if(this.finalizer != null) {
			this.finalizer = this.finalizer.then(finalizer);
		}
		else {
			this.finalizer = finalizer;
		}
		return this;
	}
	
	/**
	 * <p>
	 * Creates a new finalize promise which finalizes the exchange as defined by
	 * {{@link #finalize(Runnable, Runnable)}.
	 * </p>
	 * 
	 * @param preFinalize  a non-null pre finalizer
	 * @param postFinalize a non-null post finalizer
	 * 
	 * @return a new exchange finalizer promise
	 */
	protected ChannelPromise newFinalizePromise(Runnable preFinalize, Runnable postFinalize) {
		return this.context.newPromise().addListener(future -> this.finalize(preFinalize, postFinalize));
	}
	
	/**
	 * <p>
	 * Creates a new finalize promise which finalizes the exchange as defined by
	 * {{@link #finalize(Runnable)}.
	 * </p>
	 * 
	 * @param postFinalize a non-null post finalizer
	 * 
	 * @return a new exchange finalizer promise
	 */
	protected ChannelPromise newFinalizePromise(Runnable postFinalize) {
		return this.context.newPromise().addListener(future -> this.finalize(postFinalize));
	}
	
	/**
	 * <p>
	 * Finalizes the exchange by invoking the preFinalize runnable before the
	 * subscribing to the exchange finalizer when defined and finally invoking the
	 * postFinalize runnable
	 * </p>
	 * 
	 * @param preFinalize  a non-null pre finalizer
	 * @param postFinalize a non-null post finalizer
	 */
	protected void finalize(Runnable preFinalize, Runnable postFinalize) {
		if(this.finalizer == null) {
			preFinalize.run();
			postFinalize.run();
		}
		else {
			preFinalize.run();
			// TODO here we do nothing if the finalizer fails, maybe we should consider a debug or trace log
			// Note that when creating the finalizer the user can actually catch the error: doOnError(...)
			Mono<Void> actualFinalizer = this.finalizer;
			if(postFinalize != null) {
				actualFinalizer = actualFinalizer.doFinally(ign -> postFinalize.run());
			}
			actualFinalizer.subscribe();
		}
	}
	
	/**
	 * <p>
	 * Finalizes the exchange by subscribing to the exchange finalizer when defined
	 * and finally invoking the postFinalize runnable
	 * </p>
	 * 
	 * @param postFinalize a non-null post finalizer
	 */
	protected void finalize(Runnable postFinalize) {
		if(this.finalizer == null) {
			postFinalize.run();
		}
		else {
			// TODO here we do nothing if the finalizer fails, maybe we should consider a debug or trace log
			// Note that when creating the finalizer the user can actually catch the error: doOnError(...)
			this.finalizer.doOnTerminate(postFinalize).subscribe();
		}
	}
	
	/**
	 * <p>
	 * Creates a new promise that shall be used to finalize the exchange.
	 * </p>
	 * 
	 * <p>
	 * If a finalizer has been requested on the exchange (see
	 * {@link Exchange#finalizer()}), a listener terminating the finalizer sink is
	 * added before the specified listener in the returned promise.
	 * </p>
	 * 
	 * @param listener
	 * @return
	 */
	/*protected ChannelPromise newFinalizerPromise(GenericFutureListener<? extends Future<? super Void>> listener) {
		if(this.finalizer == null) {
			return this.context.newPromise().addListener(listener);
		}
		
		return this.context.newPromise()
			.addListener(future -> {
				if(future.isSuccess()) {
					this.finalizer.tryEmitEmpty();
				}
				else {
					this.finalizer.tryEmitError(future.cause());
				}
			})
			.addListener(listener);
	}*/
	
	/**
	 * <p>
	 * Completes the exchange finalizer if it was requested (see
	 * {@link Exchange#finalizer()})
	 * </p>
	 */
	/*protected void finalizerComplete() {
		if(this.finalizer != null) {
			this.finalizer.tryEmitEmpty();
		}
	}*/

	/**
	 * <p>
	 * Fails the exchange finalizer if it was requested (see
	 * {@link Exchange#finalizer()})
	 * </p>
	 */
	/*protected void finalizerFails(Throwable error) {
		if(this.finalizer != null) {
			this.finalizer.tryEmitError(error);
		}
	}*/
	
	/**
	 * <p>
	 * Returns the root handler.
	 * </p>
	 * 
	 * @return the root handler
	 */
	public ExchangeHandler<Exchange> getRootHandler() {
		return this.rootHandler;
	}
	
	/**
	 * <p>
	 * Returns the error handler.
	 * </p>
	 * 
	 * @return the error handler
	 */
	public ExchangeHandler<ErrorExchange<Throwable>> getErrorHandler() {
		return this.errorHandler;
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
	
	@Override
	public void dispose() {
		if(this.errorSubscriber != null) {
			this.errorSubscriber.dispose();
		}
		else {
			super.dispose();
		}
		this.request.dispose();
	}
	
	@Override
	public boolean isDisposed() {
		return this.errorSubscriber != null ? this.errorSubscriber.isDisposed() : super.isDisposed();
	}
	
	/**
	 * <p>
	 * Starts the processing of the exchange with the specified callback handler.
	 * </p>
	 * 
	 * <p>
	 * This methods invokes the server root handler on the exchange and subscribe to
	 * the response data publisher.
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
			this.rootHandler.handle(this);
		}
		catch(Throwable throwable) {
			this.logError(() -> "Exchange handler error", throwable);
			// We need to create a new error exchange each time we try to handle the error in order to have a fresh response 
			ErrorExchange<Throwable> errorExchange = this.createErrorExchange(throwable);
			try {
				this.errorHandler.handle(errorExchange);
			} 
			catch (Throwable t) {
				this.logError(() -> "ErrorExchange handler error", t);
				errorExchange = this.createErrorExchange(throwable);
				// TODO This may fail as well what do we do in such situations?
				LAST_RESORT_ERROR_HANDLER.handle(errorExchange);
			}
			finally {
				this.response = (AbstractResponse) errorExchange.response();
			}
		}
		this.single = this.response.isSingle();
		this.response.data().subscribe(this);
	}
	
	/**
	 * <p>
	 * Executes the specified task in the event loop.
	 * </p>
	 * 
	 * <p>
	 * The tasks is executed immediately when the current thread is in the event
	 * loop, otherwise it is scheduled in the event loop.
	 * </p>
	 * 
	 * <p>
	 * After the execution of the task, one event is requested to the response data
	 * subscriber.
	 * </p>
	 * 
	 * @param runnable the task to execute
	 */
	protected void executeInEventLoop(Runnable runnable) {
		this.executeInEventLoop(runnable, 1);
	}
	
	/**
	 * <p>
	 * Executes the specified task in the event loop.
	 * </p>
	 * 
	 * <p>
	 * The tasks is executed immediately when the current thread is in the event
	 * loop, otherwise it is scheduled in the event loop.
	 * </p>
	 * 
	 * <p>
	 * After the execution of the task, the specified number of events is requested
	 * to the response data subscriber.
	 * </p>
	 * 
	 * @param runnable the task to execute
	 * @param request  the number of events to request to the response data
	 *                 subscriber after the task completes
	 */
	protected void executeInEventLoop(Runnable runnable, int request) {
		if(this.contextExecutor.inEventLoop()) {
			runnable.run();
			this.request(request);
		}
		else {
			this.contextExecutor.execute(() -> {
				try {
					runnable.run();
					this.request(request);
				}
				catch (Throwable throwable) {
					this.cancel();
					this.hookOnError(throwable);
				}
			});
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
	protected abstract ErrorExchange<Throwable> createErrorExchange(Throwable error);
	
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
	 * The default implementation basically request an unbounded amount of events to
	 * the subscription.
	 * </p>
	 * 
	 * @param subscription the subscription to the response data publisher
	 */
	protected void onStart(Subscription subscription) {
		subscription.request(Long.MAX_VALUE);
	}
	
	@Override
	protected final void hookOnNext(ByteBuf value) {
		this.transferedLength += value.readableBytes();
		if(this.single && this.singleChunk == null) {
			if(this.single && this.response.headers().getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
				this.response.headers().contentLength(this.transferedLength);
			}
			this.singleChunk = value;
		}
		else {
			if(this.request.getMethod().equals(Method.HEAD)) {
				value.release();
				this.executeInEventLoop(this::onCompleteEmpty);
				this.dispose();
			}
			else {
				this.executeInEventLoop(() -> this.onNextMany(value));
			}
		}
	}
	
	/**
	 * <p>
	 * Invokes on an event when the response data publisher emits more than one
	 * event.
	 * </p>
	 * 
	 * @param value the event data
	 */
	protected abstract void onNextMany(ByteBuf value);
	
	/**
	 * <p>
	 * Invokes when the response data publisher completes with an error
	 * </p>
	 */
	protected final void hookOnError(Throwable throwable) {
		// if headers are already written => close the connection nothing we can do
		// if headers are not already written => we should invoke the error handler
		// what we need is to continue processing
		// - create a new Response for the error
		// - reset this exchange => transferedLength, chunkCount must be reset
		// - invoke the error handler (potentially the fallback error handler) with a new ErrorExchange 
		if(this.response.isHeadersWritten()) {
			this.executeInEventLoop(() -> { 
				this.onCompleteWithError(throwable);
				this.logError(() -> "Exchange processing error", throwable);
			});
		}
		else {
			this.transferedLength = 0;
			ErrorExchange<Throwable> errorExchange = this.createErrorExchange(throwable);
			try {
				this.errorHandler.handle(errorExchange);
			} 
			catch (Throwable t) {
				this.logError(() -> "ErrorExchange handler error", t);
				errorExchange = this.createErrorExchange(throwable);
				// TODO This may fail as well what do we do in such situations?
				LAST_RESORT_ERROR_HANDLER.handle(errorExchange);
			}
			finally {
				this.response = (AbstractResponse) errorExchange.response();
			}
	
			this.executeInEventLoop(() -> {
				this.errorSubscriber = new ErrorSubscriber(throwable);
				this.response.data().subscribe(this.errorSubscriber);
			});
		}
	}
	
	/**
	 * <p>
	 * Invokes when the response data stream completes with error.
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
	}
	
	/**
	 * <p>
	 * Logs an access log message.
	 * </p>
	 */
	private void logAccess() {
		// HTTP access
		/*LOGGER.info(MARKER_ACCESS, () -> {
			InetSocketAddress inetRemoteAddress = (InetSocketAddress)this.request.getRemoteAddress();
//			InetSocketAddress inetLocalAddress = (InetSocketAddress)this.request.getLocalAddress();
			String method = this.request.getMethod().name();
			StringMapMessage accessLogMessage = new StringMapMessage()
//				.with("remoteAddress", inetRemoteAddress.getAddress().getHostAddress()) // %a - Remote IP address
				.with("remoteHost", inetRemoteAddress.getAddress().getHostName()) // %h - Remote host name
//				.with("localAddress", inetLocalAddress.getAddress().getHostAddress()) // %A - Local IP address
//				.with("localPort", Integer.toString(inetLocalAddress.getPort())) // %p - Local port
				.with("bytes", Integer.toString(this.transferedLength)) // %B - Bytes sent, excluding HTTP headers
//				.with("protocol", this.request.getProtocol()) // %H - Request protocol
//				.with("remoteLogicalUsername", "-") // %l - Remote logical username from identd (always returns '-')
//				.with("method", method) // %m - Request method
//				.with("query", this.request.getQuery()) // %q - Query string (prepended with a '?' if it exists, otherwise an empty string
				.with("request", method + " " + this.request.getPath()) // %r - First line of the request
				.with("status", Integer.toString(this.response.headers().getStatusCode())) // %s - HTTP status code of the response
//				.with("path", this.request.getPathAbsolute()) // %U - Requested URL path
//				.with("authority", this.request.getAuthority()) // %v - Local server name
				.with("referer", this.request.headers().get(Headers.NAME_REFERER).orElse("")) // %{Referer}i - referer
				.with("userAgent", this.request.headers().get(Headers.NAME_USER_AGENT).orElse("")); // %{User-agent}i - user agent
			
			return accessLogMessage;
		});*/
		
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
	 * @param messageSupplier the message supplier
	 * @param throwable the error
	 */
	private void logError(Supplier<?> messageSupplier, Throwable throwable) {
		if(throwable instanceof HttpException) {
			// HTTP error: typically recoverable HTTP exceptions returning a proper error to the client
			LOGGER.error(MARKER_ERROR, messageSupplier, throwable);
		}
		else {
			// non HTTP error: typically unrecoverable unchecked exceptions 
			LOGGER.error(() -> "Exchange handler error", throwable);
		}
	}
	
	/**
	 * <p>
	 * Invokes when the response data publisher completes with no data.
	 * </p>
	 */
	protected abstract void onCompleteEmpty();
	
	/**
	 * <p>
	 * Invokes when the response data publisher completes with a single data.
	 * </p>
	 */
	protected abstract void onCompleteSingle(ByteBuf value);
	
	/**
	 * <p>
	 * Invokes when the response data publisher completes with many data.
	 * </p>
	 */
	protected abstract void onCompleteMany();

	@Override
	protected void hookFinally(SignalType type) {
		this.request.dispose();
	}
	
	/**
	 * <p>
	 * Exchange callbacks handler
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see AbstractExchange#start(Handler)
	 */
	public static interface Handler {
		
		static Handler DEFAULT = new Handler() {};
		
		default void exchangeStart(ChannelHandlerContext ctx, AbstractExchange exchange) {
			
		}

		default void exchangeNext(ChannelHandlerContext ctx, ByteBuf t) {
			
		}

		default void exchangeError(ChannelHandlerContext ctx, Throwable t) {
			this.exchangeComplete(ctx);
		}

		default void exchangeComplete(ChannelHandlerContext ctx) {
			
		}
	}
	
	/**
	 * <p>
	 * An error subscriber which is created to subscribe to the response data
	 * publisher of the error exchange created when the response data publisher
	 * completes with an error.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	private final class ErrorSubscriber extends BaseSubscriber<ByteBuf> {
		
		private Throwable originalError;
		
		public ErrorSubscriber(Throwable originalError) {
			this.originalError = originalError;
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			AbstractExchange.this.hookOnNext(value);
		}
		
		@Override
		protected void hookOnError(Throwable throwable) {
			// If we get there it means we can no longer process anything
			AbstractExchange.this.onCompleteWithError(this.originalError);
			AbstractExchange.this.logError(() -> "Exchange processing error", this.originalError);
			AbstractExchange.this.logError(() -> "ErrorExchange processing error", throwable);
		}
		
		@Override
		protected void hookOnComplete() {
			AbstractExchange.this.hookOnComplete();
			AbstractExchange.this.logError(() -> "Exchange processing error", this.originalError);
			AbstractExchange.this.logAccess();
		}
	}
}
