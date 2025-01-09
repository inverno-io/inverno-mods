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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.internal.GenericErrorExchangeHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;

/**
 * <p>
 * Http/1.x {@link ErrorExchange} implementation.
 * </p>
 * 
 * <p>
 * An error exchange is used to handle exchange processing errors. It is created from the faulty exchange (see {@link Http1xExchange#createErrorExchange(java.lang.Throwable) }) and processed by the
 * error exchange handler specified in the server controller. In case of error while processing the error exchange, another last resort error exchange is created from the error exchange (see 
 * {@link Http1xErrorExchange#createErrorExchange(java.lang.Throwable) }) and processed by the {@link GenericErrorExchangeHandler}. If the error couldn't be handled without further error or when a
 * partial response was sent before the error occurred, the connection is shutdown.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http1xErrorExchange extends AbstractHttp1xExchange implements ErrorExchange<ExchangeContext> {
	
	private static final Logger LOGGER = LogManager.getLogger(ErrorExchange.class);
	
	private final Http1xExchange exchange;
	private final Http1xResponse response;
	private final Throwable error;
	private boolean lastResort;
	
	private Disposable disposable;

	/**
	 * <p>
	 * Creates an Http/1.x error exchange.
	 * </p>
	 * 
	 * @param exchange the parent exchange
	 * @param response the error response
	 * @param error    the error
	 */
	public Http1xErrorExchange(Http1xExchange exchange, Http1xResponse response, Throwable error) {
		super(exchange);
		this.exchange = exchange;
		this.response = response;
		this.error = error;
		this.lastResort = false;
	}

	@Override
	Http1xExchange unwrap() {
		return this.exchange;
	}

	@Override
	boolean isKeepAlive() {
		return this.exchange.isKeepAlive();
	}

	@Override
	public void start() {
		this.connection.respondingExchange = this;
		try {
			if(this.lastResort) {
				GenericErrorExchangeHandler.INSTANCE.handle(this);
				this.hookOnComplete();
			}
			else {
				LOGGER.log(
					this.error instanceof HttpException && ((HttpException)this.error).getStatusCategory() != Status.Category.SERVER_ERROR ? Level.WARN : Level.ERROR, 
					"Exchange processing error", 
					this.error
				);
				this.controller.defer(this).subscribe(this);
			}
		}
		catch(Throwable throwable) {
			this.handleError(throwable);
		}
	}

	@Override
	public void handleError(Throwable throwable) {
		if(this.lastResort || this.response.headers().isWritten()) {
			if(throwable != this.error) {
				throwable.addSuppressed(this.error);
			}
			LOGGER.error("Fatal exchange processing error", throwable);
			this.dispose(throwable);
			this.connection.shutdown().subscribe();
		}
		else {
			LOGGER.error("Error handler error", throwable);
			this.createErrorExchange(throwable).start();
		}
	}
	
	@Override
	public Http1xErrorExchange createErrorExchange(Throwable throwable) {
		if(throwable != this.error) {
			throwable.addSuppressed(this.error);
		}
		Http1xErrorExchange errorExchange = this.exchange.createErrorExchange(throwable);
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
	public Throwable getError() {
		return this.error;
	}

	@Override
	public ExchangeContext context() {
		return this.exchange.context();
	}

	@Override
	public Http1xRequest request() {
		return this.exchange.request();
	}

	@Override
	public Http1xResponse response() {
		return this.response;
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		this.disposable = this;
		subscription.request(1);
	}

	@Override
	protected void hookOnComplete() {
		if(!this.reset) {
			this.response.send();
		}
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		if(!this.reset) {
			this.connection.onExchangeError(throwable);
		}
	}
}
