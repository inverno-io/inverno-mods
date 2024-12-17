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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import java.util.Optional;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base {@link HttpConnectionExchange} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the request type
 * @param <B> the response type
 * @param <C> the exchange type
 * @param <D> the originating response type
 */
public abstract class AbstractExchange<A extends ExchangeContext, B extends HttpConnectionRequest, C extends HttpConnectionResponse, D> implements HttpConnectionExchange<A, B, C> {

	private static final HttpClientException EXCHANGE_DISPOSED_ERROR = new StacklessHttpClientException("Exchange was disposed");

	private final Object state;

	/**
	 * The HTTP client configuration.
	 */
	protected final HttpClientConfiguration configuration;
	private final Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink;
	/**
	 * The header service.
	 */
	protected final HeaderService headerService;
	/**
	 * The parameter converter.
	 */
	protected final ObjectConverter<String> parameterConverter;
	private final A context;
	
	/**
	 * The HTTP request.
	 */
	protected final B request;
	/**
	 * The HTTP response.
	 */
	protected C response;
	
	private Throwable cancelCause;
	
	/**
	 * <p>
	 * Creates a base exchange.
	 * </p>
	 *
	 * @param state              the state
	 * @param configuration      the HTTP client configuration
	 * @param sink               the exchange sink
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param context            the exchange context
	 * @param request            the HTTP request
	 */
	public AbstractExchange(
			Object state,
			HttpClientConfiguration configuration, 
			Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			A context,
			B request) {
		this.state = state;
		this.configuration = configuration;
		this.sink = sink;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.context = context;
		this.request = request;
	}

	/**
	 * <p>
	 * Returns the state to pass back when recycling the connection.
	 * </p>
	 *
	 * @return the state
	 */
	public Object getState() {
		return state;
	}

	/**
	 * <p>
	 * Starts the request timeout.
	 * </p>
	 */
	protected abstract void startTimeout();

	/**
	 * <p>
	 * Cancels the request timeout.
	 * </p>
	 */
	protected abstract void cancelTimeout();
	
	/**
	 * <p>
	 * Initializes the exchange.
	 * </p>
	 * 
	 * <p>
	 * This basically starts the request timeout.
	 * </p>
	 */
	public final void init() {
		this.startTimeout();
	}
	
	/**
	 * <p>
	 * Starts the processing of the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method shall sends the request in order to start the exchange.
	 * </p>
	 */
	public abstract void start();
	
	/**
	 * <p>
	 * Emits the response.
	 * </p>
	 * 
	 * <p>
	 * This is invoked by the connection when the exchange response is received, the request timeout is cancelled and the exchange is emitted on the exchange sink to subsequently emit the response.
	 * </p>
	 * 
	 * @param originatingResponse the HTTP response received on the connection
	 */
	public final void emitResponse(D originatingResponse) {
		this.response = this.createResponse(originatingResponse);
		if(this.response != null) {
			this.cancelTimeout();
			if(this.sink != null) {
				this.sink.tryEmitValue(this);
			}
		}
	}
	
	/**
	 * <p>
	 * Creates the HTTP response from the originating response.
	 * </p>
	 * 
	 * @param originatingResponse the originating response
	 * 
	 * @return the HTTP response
	 */
	protected abstract C createResponse(D originatingResponse);
	
	/**
	 * <p>
	 * Disposes the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method cancels the request timeout, sets the cancel cause, invokes the exchange disposal logic implemented in {@link #doDispose(java.lang.Throwable) } and finally emits an error on the 
	 * exchange sink in case the response is not present.
	 * </p>
	 * 
	 * <p>
	 * It also makes sure the exchange disposal logic implemented in {@link #doDispose(java.lang.Throwable) } is invoked once in case of error.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 * 
	 * @see #doDispose(java.lang.Throwable) 
	 */
	public final void dispose(Throwable cause) {
		this.cancelTimeout();
		if(this.cancelCause == null) {
			this.cancelCause = cause != null ? cause : EXCHANGE_DISPOSED_ERROR;
			this.doDispose(cause);
			if(this.response == null && this.sink != null) {
				this.sink.tryEmitError(this.cancelCause);
			}
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
	public A context() {
		return this.context;
	}

	@Override
	public B request() {
		return this.request;
	}

	@Override
	public C response() {
		return this.response;
	}
	
	@Override
	public final Optional<Throwable> getCancelCause() {
		return Optional.ofNullable(this.cancelCause);
	}
}
