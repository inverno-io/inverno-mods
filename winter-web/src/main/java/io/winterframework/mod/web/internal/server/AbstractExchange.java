/**
 * 
 */
package io.winterframework.mod.web.internal.server;

import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.Response;
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
	
	protected static final ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> LAST_RESORT_ERROR_HANDLER = exchange -> {
		if(exchange.response().isHeadersWritten()) {
			// TODO exchange interrupted exception?
			throw new RuntimeException(exchange.getError());
		}
		if(exchange.getError() instanceof WebException) {
			exchange.response().headers(h -> h.status(((WebException)exchange.getError()).getStatusCode())).body().empty();
		}
		else {
			exchange.response().headers(h -> h.status(Status.INTERNAL_SERVER_ERROR)).body().empty();
		}
	};
	
	protected final AbstractRequest request;
	protected final AbstractResponse response;
	
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
	
	protected ExchangeSubscriber exchangeSubscriber;
	
	public void start(ExchangeSubscriber exchangeSubscriber) {
		if(this.exchangeSubscriber != null) {
			throw new IllegalStateException("Exchange already started");
		}
		this.exchangeSubscriber = exchangeSubscriber;
		try {
			this.rootHandler.handle(this);
		}
		catch(Throwable t1) {
			GenericErrorExchange errorExchange = new GenericErrorExchange(t1);
			// TODO We could also set the flux on error and call the error handler
			try {
				this.errorHandler.handle(errorExchange);
			} 
			catch (Throwable t2) {
				LAST_RESORT_ERROR_HANDLER.handle(errorExchange);
			}
		}
		this.response.data().subscribe(this);
	}
	
	protected final void scheduleOnEventLoop(Runnable runnable) {
		if(this.contextExecutor.inEventLoop()) {
			runnable.run();
		}
		else {
			this.contextExecutor.schedule(() -> {
				runnable.run();
			}, 0, TimeUnit.MILLISECONDS);
		}
	}
	
	@Override
	protected final void hookOnNext(ByteBuf value) {
		this.scheduleOnEventLoop(() -> {
			this.doHookOnNext(value);
		});
	}
	
	protected abstract void doHookOnNext(ByteBuf value);
	
	protected final void hookOnError(Throwable throwable) {
		this.scheduleOnEventLoop(() -> {
			this.doHookOnError(throwable);
		});
	}
	
	protected abstract void doHookOnError(Throwable throwable);
	
	@Override
	protected final void hookOnComplete() {
		this.scheduleOnEventLoop(() -> {
			this.doHookOnComplete();
		});
	}
	
	protected abstract void doHookOnComplete();
	
	private class GenericErrorExchange implements ErrorExchange<ResponseBody, Throwable> {

		private Throwable error;
		
		public GenericErrorExchange(Throwable error) {
			this.error = error;
		}
		
		@Override
		public Request<Void> request() {
			return AbstractExchange.this.request.map(ign -> null);
		}

		@Override
		public Response<ResponseBody> response() {
			return AbstractExchange.this.response;
		}

		@Override
		public Throwable getError() {
			return this.error;
		}
	}
	
	public static interface ExchangeSubscriber {
		
		static ExchangeSubscriber DEFAULT = new ExchangeSubscriber() {};
		
		default void onExchangeSubscribe(Subscription s) {
			
		}

		default void onExchangeNext(ByteBuf t) {
			
		}

		default void onExchangeError(Throwable t) {
			this.onExchangeComplete();
		}

		default void onExchangeComplete() {
			
		}
	}
}
