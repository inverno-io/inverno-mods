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
package io.winterframework.mod.http.server.internal;

import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.Exchange;
import io.winterframework.mod.http.server.ExchangeHandler;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;

/**
 * @author jkuhn
 *
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
	
	protected void executeInEventLoop(Runnable runnable) {
		this.executeInEventLoop(runnable, 1);
	}
	
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
	
	protected abstract ErrorExchange<Throwable> createErrorExchange(Throwable error);
	
	@Override
	protected final void hookOnSubscribe(Subscription subscription) {
		this.onStart(subscription);
	}
	
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
			this.executeInEventLoop(() -> this.onNextMany(value));
		}
	}
	
	protected abstract void onNextMany(ByteBuf value);
	
	protected final void hookOnError(Throwable throwable) {
		// if headers are already written => close the connection nothing we can do
		// if headers are not already written => we should invoke the error handler
		// what we need is to continue processing
		// - create a new Response for the error
		// - reset this exchange => transferedLength, chunkCount must be reset
		// - invoke the error handler (potentially the fallback error handler) with a new ErrorExchange 
		
		throwable.printStackTrace();
		
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
			this.executeInEventLoop(() -> this.onCompleteSingle(this.singleChunk));
		}
		else {
			this.executeInEventLoop(this::onCompleteMany);
		}
	}
	
	protected abstract void onCompleteEmpty();
	
	protected abstract void onCompleteSingle(ByteBuf value);
	
	protected abstract void onCompleteMany();

	@Override
	protected void hookFinally(SignalType type) {
		this.request.dispose();
	}
	
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
