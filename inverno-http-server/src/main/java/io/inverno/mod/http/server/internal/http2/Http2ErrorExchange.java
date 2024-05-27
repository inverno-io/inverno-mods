/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.server.internal.http2;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.internal.GenericErrorExchangeHandler;
import io.netty.handler.codec.http2.Http2Error;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;

/**
 * <p>
 * Http/2 {@link ErrorExchange} implementation.
 * </p>
 * 
 * <p>
 * An error exchange is used to handle exchange processing errors. It is created from the faulty exchange (see {@link Http2ExchangeV2#createErrorExchange(java.lang.Throwable) }) and processed by the
 * error exchange handler specified in the server controller. In case of error while processing the error exchange, another last resort error exchange is created from the error exchange (see 
 * {@link Http2ErrorExchangeV2#createErrorExchange(java.lang.Throwable) }) and processed by the {@link GenericErrorExchangeHandler}. If the error couldn't be handled without further error or when a 
 * partial response was sent before the error occurred, the connection is shutdown.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http2ErrorExchange extends AbstractHttp2Exchange implements ErrorExchange<ExchangeContext> {
	
	private static final Logger LOGGER = LogManager.getLogger(ErrorExchange.class);
	
	private final Http2Exchange exchange;
	private final Http2Response response;
	private final Throwable error;
	private boolean lastResort;
	
	private Disposable disposable;

	/**
	 * <p>
	 * Creates an Http2 error exchange.
	 * </p>
	 * 
	 * @param exchange the parent exchange
	 * @param response the error response
	 * @param error    the error
	 */
	public Http2ErrorExchange(Http2Exchange exchange, Http2Response response, Throwable error) {
		super(exchange);
		this.exchange = exchange;
		this.response = response;
		this.error = error;
	}
	
	@Override
	public void start() {
		this.connectionStream.exchange = this;
		try {
			ErrorExchangeHandlerSubscriber handlerSubscriber = new Http2ErrorExchange.ErrorExchangeHandlerSubscriber();
			if(this.lastResort) {
				GenericErrorExchangeHandler.INSTANCE.handle(this);
				handlerSubscriber.hookOnComplete();
			}
			else {
				LOGGER.log(
					this.error instanceof HttpException && ((HttpException)this.error).getStatusCategory() != Status.Category.SERVER_ERROR ? Level.WARN : Level.ERROR, 
					"Exchange processing error", 
					this.error
				);
				this.controller.defer(this).subscribe(handlerSubscriber);
			}
		}
		catch(Throwable throwable) {
			this.handleError(throwable);
		}
	}

	@Override
	public void handleError(Throwable throwable) {
		if(this.lastResort || this.response.headers().isWritten()) {
			throwable.addSuppressed(this.error);
			LOGGER.error("Fatal exchange processing error", throwable);
			this.dispose(throwable);
			this.connectionStream.resetStream(Http2Error.INTERNAL_ERROR.code());
		}
		else {
			LOGGER.error("Error handler error", throwable);
			this.createErrorExchange(throwable).start();
		}
	}
	
	@Override
	public Http2ErrorExchange createErrorExchange(Throwable throwable) {
		throwable.addSuppressed(this.error);
		Http2ErrorExchange errorExchange = this.exchange.createErrorExchange(throwable);
		errorExchange.lastResort = true;
		return errorExchange;
	}
	
	@Override
	protected void doDispose(Throwable cause) {
		if(this.disposable != null) {
			this.disposable.dispose();
		}
		this.exchange.request().dispose(cause);
		this.response.dispose(cause);
	}

	@Override
	public ExchangeContext context() {
		return this.exchange.context();
	}

	@Override
	public Http2Request request() {
		return this.exchange.request();
	}

	@Override
	public Http2Response response() {
		return this.response;
	}
	
	@Override
	public Throwable getError() {
		return this.error;
	}
	
	/**
	 * <p>
	 * The subscriber used to subscribe to the mono returned by the error exchange handler and that sends the response on complete.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class ErrorExchangeHandlerSubscriber extends BaseSubscriber<Void> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http2ErrorExchange.this.disposable = this;
			super.hookOnSubscribe(subscription);
		}

		@Override
		protected void hookOnComplete() {
			if(!Http2ErrorExchange.this.connectionStream.isReset()) {
				Http2ErrorExchange.this.response.send();
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			if(!Http2ErrorExchange.this.connectionStream.isReset()) {
				Http2ErrorExchange.this.connectionStream.onExchangeError(throwable);
			}
		}
	}
}
