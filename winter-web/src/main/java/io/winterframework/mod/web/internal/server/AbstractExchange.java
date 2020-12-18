/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.Status;
import io.winterframework.mod.web.WebException;
import reactor.core.publisher.BaseSubscriber;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractExchange extends BaseSubscriber<ByteBuf> implements Exchange<RequestBody, ResponseBody> {

	protected final ChannelHandlerContext context;
	protected final EventExecutor contextExecutor;
	
	protected final ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler;
	protected final ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler;
	
	protected final AbstractRequest request;
	protected AbstractResponse response;

	protected Handler handler;
	
	private int transferedLength;
	protected int chunkCount;
	private ByteBuf firstChunk;
	
	private ErrorSubscriber errorSubscriber;
	
	protected static final ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> LAST_RESORT_ERROR_HANDLER = exchange -> {
		if(exchange.response().isHeadersWritten()) {
			throw new IllegalStateException("Headers already written", exchange.getError());
		}
		if(exchange.getError() instanceof WebException) {
			exchange.response().headers(h -> h.status(((WebException)exchange.getError()).getStatusCode())).body().empty();
		}
		else {
			exchange.response().headers(h -> h.status(Status.INTERNAL_SERVER_ERROR)).body().empty();
		}
	};
	
	public AbstractExchange(ChannelHandlerContext context, ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler, AbstractRequest request, AbstractResponse response) {
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

	public ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> getRootHandler() {
		return this.rootHandler;
	}
	
	public ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> getErrorHandler() {
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
			ErrorExchange<ResponseBody, Throwable> errorExchange = this.createErrorExchange(throwable);
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
		this.response.data().subscribe(this);
	}
	
	protected void executeInEventLoop(Runnable runnable) {
		if(this.contextExecutor.inEventLoop()) {
			runnable.run();
			this.request(1);
		}
		else {
			this.contextExecutor.execute(() -> {
				try {
					runnable.run();
					this.request(1);
				}
				catch (Throwable throwable) {
					this.cancel();
					this.hookOnError(throwable);
				}
			});
		}
	}
	
	protected abstract ErrorExchange<ResponseBody, Throwable> createErrorExchange(Throwable error);
	
	@Override
	protected final void hookOnSubscribe(Subscription subscription) {
		this.onStart(subscription);
	}
	
	protected void onStart(Subscription subscription) {
//		subscription.request(Long.MAX_VALUE);
		subscription.request(2);
	}
	
	@Override
	protected final void hookOnNext(ByteBuf value) {
		this.chunkCount++;
		if(this.chunkCount == 1) {
			this.transferedLength = value.readableBytes();
			this.firstChunk = value;
		}
		else {
			this.transferedLength += value.readableBytes();
			if(this.firstChunk != null) {
				final ByteBuf previousChunk = this.firstChunk;
				this.firstChunk = null;
				this.executeInEventLoop(() -> {
					this.onNextMany(previousChunk);
				});
			}
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
		if(this.response.isHeadersWritten()) {
			this.executeInEventLoop(() -> { 
				this.onCompleteWithError(throwable);
			});
		}
		else {
			this.transferedLength = 0;
			this.chunkCount = 0;
			this.firstChunk = null;
			ErrorExchange<ResponseBody, Throwable> errorExchange = this.createErrorExchange(throwable);
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
		if(this.firstChunk != null) {
			// single chunk response
			if(this.response.getHeaders().getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
				this.response.getHeaders().contentLength(this.transferedLength);
			}
			this.executeInEventLoop(() -> this.onCompleteSingle(this.firstChunk));
		}
		else if(this.chunkCount == 0) {
			// empty response
			if(this.response.getHeaders().getCharSequence(Headers.NAME_CONTENT_LENGTH) == null) {
				this.response.getHeaders().contentLength(0);
			}
			this.executeInEventLoop(this::onCompleteEmpty);
		}
		else {
			this.executeInEventLoop(this::onCompleteMany);
		}
	}
	
	protected abstract void onCompleteEmpty();
	
	protected abstract void onCompleteSingle(ByteBuf value);
	
	protected abstract void onCompleteMany();
	
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
