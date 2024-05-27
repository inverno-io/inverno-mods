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
import io.inverno.mod.http.client.internal.AbstractExchange;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.ScheduledFuture;
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
class Http1xExchange<A extends ExchangeContext> extends AbstractExchange<A, Http1xRequest, Http1xResponse, HttpResponse> {
	
	protected final Http1xConnection connection;

	private ScheduledFuture<?> timeoutFuture;
	
	Http1xExchange<?> next;
	
	private boolean reset;
	
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
		super(configuration, sink, headerService, parameterConverter, context, new Http1xRequest(parameterConverter, connection, endpointRequest));
		this.connection = connection;
	}

	@Override
	protected void startTimeout() {
		if(this.configuration.request_timeout() > 0) {
			this.timeoutFuture = this.connection.executor().schedule(
				() -> {
					this.timeoutFuture = null;
					// we are supposed to have sent the request so we can close the connection
					this.connection.onRequestError(new RequestTimeoutException("Exceeded timeout " + this.configuration.request_timeout() + "ms"));
				}, 
				this.configuration.request_timeout(), 
				TimeUnit.MILLISECONDS
			);
		}
	}
	
	@Override
	protected void cancelTimeout() {
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
	@Override
	public void start() {
		this.request.send();
	}
	
	@Override
	protected final Http1xResponse createResponse(HttpResponse response) {
		if(response.status() == HttpResponseStatus.CONTINUE) {
			this.request.sendBody();
			return null;
		}
		return new Http1xResponse(this.headerService, this.parameterConverter, response);
	}
	
	@Override
	protected void doDispose(Throwable cause) {
		this.request.dispose(cause);
		if(this.response != null) {
			this.response.dispose(cause);
		}
	}

	@Override
	public HttpVersion getProtocol() {
		return this.connection.getProtocol();
	}
	
	@Override
	public final void reset(long code) {
		if(this.connection.executor().inEventLoop()) {
			if(!this.reset) {
				this.reset = true;
				// exchange has to be the responding exchange because the exchange is only emitted when the response is received
				this.dispose(new HttpClientException("Exchange has been reset: " + code));
				this.connection.shutdown().subscribe();
			}
		}
		else {
			this.connection.executor().execute(() -> this.reset(code));
		}
	}
}
