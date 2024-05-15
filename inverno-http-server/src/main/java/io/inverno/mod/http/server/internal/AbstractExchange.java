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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.ServerController;
import java.util.Optional;

/**
 * <p>
 * Base {@link Exchange} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <A> the request type
 * @param <B> the response type
 * @param <C> the error exchange type
 */
public abstract class AbstractExchange<A extends Request, B extends Response, C extends ErrorExchange<ExchangeContext>> implements Exchange<ExchangeContext> {
	
	/**
	 * The server configuration.
	 */
	protected final HttpServerConfiguration configuration;
	/**
	 * The server controller.
	 */
	protected final ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller;
	/**
	 * Flag indicating whether the request is a {@link Method#HEAD} request.
	 */
	protected final boolean head;
	
	private Throwable cancelCause;
	/**
	 * Flag indicating whether the exchange was reset.
	 */
	protected boolean reset;

	/**
	 * <p>
	 * Creates a base exchange.
	 * </p>
	 *
	 * @param configuration the server configuration
	 * @param controller    the server controller
	 * @param head          true to indicate a {@code HEAD} request, false otherwise
	 */
	public AbstractExchange(HttpServerConfiguration configuration, ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller, boolean head) {
		this.configuration = configuration;
		this.controller = controller;
		this.head = head;
	}
	
	/**
	 * <p>
	 * Creates a base exchange from the specified parent exchange.
	 * </p>
	 * 
	 * <p>
	 * This is typically used to implement error exchanges that must inherit from the originating exchange.
	 * </p>
	 * 
	 * @param parentExchange the parent exchange
	 */
	protected AbstractExchange(AbstractExchange<A, B, C> parentExchange) {
		this.configuration = parentExchange.configuration;
		this.controller = parentExchange.controller;
		this.head = parentExchange.head;
		this.reset = parentExchange.reset;
	}
	
	/**
	 * <p>
	 * Starts the processing of the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method shall invoke an exchange handler and proceed until the processing of the exchange is complete.
	 * </p>
	 */
	public abstract void start();
	
	/**
	 * <p>
	 * Handles error raised during the processing of the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method shall assess whether an error response can still be sent to the client in which case an error exchange shall be created, otherwise the connection shall be shutdown.
	 * </p>
	 * 
	 * @param throwable the error raised during the processing of the exchange
	 */
	public abstract void handleError(Throwable throwable);
	
	/**
	 * <p>
	 * Creates an error exchange from the exchange.
	 * </p>
	 * 
	 * @param throwable an error
	 * 
	 * @return a new error exchange
	 */
	public abstract C createErrorExchange(Throwable throwable);
	
	/**
	 * <p>
	 * Disposes the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method sets the cancel cause and makes sure the exchange disposal logic implemented in {@link #doDispose(java.lang.Throwable) } is invoked once.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 * 
	 * @see #doDispose(java.lang.Throwable) 
	 */
	public final void dispose(Throwable cause) {
		// prevent dispose from being invoked multiple times
		if(this.cancelCause == null) {
			this.cancelCause = cause;
			this.doDispose(cause);
		}
	}
	
	/**
	 * <p>
	 * Disposes the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method shall implement the specific exchange disposal logic.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	protected abstract void doDispose(Throwable cause);
	
	@Override
	public abstract A request();

	@Override
	public abstract B response();
	
	@Override
	public Optional<Throwable> getCancelCause() {
		return Optional.ofNullable(this.cancelCause);
	}
	
	@Override
	public final void reset(long code) {
		if(!this.reset) {
			this.reset = true;
			this.doReset(code);
		}
	}
	
	/**
	 * <p>
	 * Resets the exchange with the specified code.
	 * </p>
	 * 
	 * @param code a code
	 */
	protected abstract void doReset(long code);
}
