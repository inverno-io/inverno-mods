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

import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import reactor.core.publisher.BaseSubscriber;
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

	protected final ChannelHandlerContext context;
	protected final EventExecutor contextExecutor;
	
	protected final ExchangeHandler<Exchange> rootHandler;
	protected final ExchangeHandler<ErrorExchange<Throwable>> errorHandler;
	
	protected final AbstractRequest request;
	protected AbstractResponse response;

	protected Handler handler;
	
	private int transferedLength;
	
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

	public ExchangeHandler<Exchange> getRootHandler() {
		return this.rootHandler;
	}
	
	public ExchangeHandler<ErrorExchange<Throwable>> getErrorHandler() {
		return this.errorHandler;
	}
	
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
			// We need to create a new error exchange each time we try to handle the error in order to have a fresh response 
			ErrorExchange<Throwable> errorExchange = this.createErrorExchange(throwable);
			try {
				this.errorHandler.handle(errorExchange);
			} 
			catch (Throwable t) {
				// TODO we should probably log the error handler error
				errorExchange = this.createErrorExchange(throwable);
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
			});
		}
		else {
			this.transferedLength = 0;
			ErrorExchange<Throwable> errorExchange = this.createErrorExchange(throwable);
			try {
				this.errorHandler.handle(errorExchange);
			} 
			catch (Throwable t) {
				// TODO we should probably log the error handler error
				errorExchange = this.createErrorExchange(throwable);
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
			this.executeInEventLoop(this::onCompleteEmpty);
		}
		else if(this.singleChunk != null) {
			// single chunk response
			if(this.request.getMethod().equals(Method.HEAD)) {
				this.executeInEventLoop(this::onCompleteEmpty);
			}
			else {
				this.executeInEventLoop(() -> {
					this.onCompleteSingle(this.singleChunk);
				});
			}
		}
		else {
			this.executeInEventLoop(this::onCompleteMany);
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
			// TODO we should probably log the error handler error
			AbstractExchange.this.onCompleteWithError(this.originalError);
		}
		
		@Override
		protected void hookOnComplete() {
			AbstractExchange.this.hookOnComplete();
		}
	}
}
