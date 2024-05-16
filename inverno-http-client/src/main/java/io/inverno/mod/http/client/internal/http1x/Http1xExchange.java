/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.RequestTimeoutException;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Http/1.x {@link Exchange} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> The exchange context type
 */
class Http1xExchange<A extends ExchangeContext> implements HttpConnectionExchange<A, Http1xRequest, Http1xResponse> {
	
	private final HttpClientConfiguration configuration;
	private final Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final A context;
	protected final Http1xConnection connection;
	
	private final Http1xRequest request;
	private Http1xResponse response;

	Http1xExchange<?> next;
	
	private final long creationTime;
	private ScheduledFuture<?> timeoutFuture;
	
	private Throwable cancelCause;
	
	/**
	 * Flag indicating whether the exchange was reset.
	 */
	protected boolean reset;

	/**
	 * <p>
	 * Creates an Http/1.x exchange.
	 * </p>
	 * 
	 * @param configuration      the HTTP client configurartion
	 * @param sink               the exchange sink
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param context            the exchange context
	 * @param connection         the Http/1.x connection
	 * @param endpointRequest    the endpoint request
	 */
	public Http1xExchange(
			HttpClientConfiguration configuration,
			Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink,
			HeaderService headerService,
			ObjectConverter<String> parameterConverter, 
			A context, 
			Http1xConnection connection, 
			EndpointRequest endpointRequest
		) {
		this.configuration = configuration;
		this.sink = sink;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.context = context;
		this.connection = connection;
		
		this.request = new Http1xRequest(parameterConverter, connection, endpointRequest);
		
		this.creationTime = System.currentTimeMillis();
	}
	
	/**
	 * <p>
	 * Starts the request timeout task.
	 * </p>
	 */
	private void startTimeout() {
		if(this.configuration.request_timeout() > 0) {
			long requestTimeout = this.configuration.request_timeout() - (System.currentTimeMillis() - this.creationTime);
			if(requestTimeout > 0) {
				this.timeoutFuture = this.connection.executor().schedule(
					() -> {
						this.timeoutFuture = null;
						// we are supposed to have sent the request so we can close the connection
						this.dispose(new RequestTimeoutException("Exceeded timeout " + this.configuration.request_timeout() + "ms"));
						this.connection.shutdown().subscribe();
					}, 
					requestTimeout, 
					TimeUnit.MILLISECONDS
				);
			}
			else {
				this.dispose(new RequestTimeoutException("Exceeded timeout " + this.configuration.request_timeout() + "ms"));
				this.connection.shutdown().subscribe();
			}
		}
	}
	
	/**
	 * <p>
	 * Cancels the request timeout task.
	 * </p>
	 */
	private void cancelTimeout() {
		if(this.timeoutFuture != null) {
			this.timeoutFuture.cancel(false);
			this.timeoutFuture = null;
		}
	}
	
	/**
	 * <p>
	 * Starts the processing of the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method sends the request and start the request timeout task.
	 * </p>
	 */
	void start() {
		// TODO we can have a timeout before sending the request, in which case we can timeout right away and maybe save the connection
		// That would mean: previous exchange took time to send request body
		// maybe we should start the timeout when creating the exchange i.e. when it's registered, then we'll have to handle the headers not written case
		this.request.send();
		this.startTimeout();
	}
	
	/**
	 * <p>
	 * Emits the response.
	 * </p>
	 * 
	 * <p>
	 * This is invoked by the connection when the exchange response is received, the request timeout is cancelled and the exchange is emitted on the exchange sink to make the response available.
	 * </p>
	 * 
	 * @param response the Http response received on the connection
	 */
	void emitResponse(HttpResponse response) {
		this.cancelTimeout();
		this.response = new Http1xResponse(this.headerService, this.parameterConverter, response);
		if(this.sink != null) {
			this.sink.tryEmitValue(this);
		}
	}
	
	/**
	 * <p>
	 * Disposes the exchange.
	 * </p>
	 * 
	 * <p>
	 * This methods cancels the request timeout, sets the cancel cause and release exchange resources.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	void dispose(Throwable cause) {
		this.cancelTimeout();
		if(this.cancelCause == null) {
			this.cancelCause = cause;
		}
		
		this.request.dispose(cause);
		if(this.response != null) {
			this.response.dispose(cause);
		}
		else if(this.sink != null) {
			this.sink.tryEmitError(cause != null ? cause : new HttpClientException("Exchange was disposed"));
		}
	}

	@Override
	public HttpVersion getProtocol() {
		return this.connection.getProtocol();
	}

	@Override
	public A context() {
		return this.context;
	}

	@Override
	public Http1xRequest request() {
		return this.request;
	}

	@Override
	public Http1xResponse response() {
		return this.response;
	}
	
	@Override
	public void reset(long code) {
		// exchange has to be the responding exchange because the exchange is only emitted when the response is received
		this.reset = true;
		if(this.connection.executor().inEventLoop()) {
			this.dispose(new HttpClientException("Exchange has been reset: " + code));
			this.connection.shutdown().subscribe();
		}
		else {
			this.connection.executor().execute(() -> this.reset(code));
		}
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return Optional.ofNullable(this.cancelCause);
	}
}
