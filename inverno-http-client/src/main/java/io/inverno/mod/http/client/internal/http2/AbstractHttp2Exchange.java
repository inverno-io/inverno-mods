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
package io.inverno.mod.http.client.internal.http2;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.RequestTimeoutException;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base HTTP/2 {@link Exchange} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
abstract class AbstractHttp2Exchange<A extends ExchangeContext, B extends HttpConnectionRequest> implements HttpConnectionExchange<A, B, Http2Response> {

	protected final HttpClientConfiguration configuration;
	protected final Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink;
	protected final HeaderService headerService;
	protected final ObjectConverter<String> parameterConverter;
	protected final A context;
	protected final Http2ConnectionStream connectionStream;
	
	protected final B request;
	protected Http2Response response;
	
	private ScheduledFuture<?> timeoutFuture;
	
	private Throwable cancelCause;

	/**
	 * <p>
	 * Creates an HTTP/2 exchane.
	 * </p>
	 * 
	 * @param configuration      the HTTP client configurartion
	 * @param sink               the exchange sink
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param context            the exchange context
	 * @param connectionStream   the Http/2 connection stream
	 * @param request            the Http/2 request
	 */
	public AbstractHttp2Exchange(
			HttpClientConfiguration configuration, 
			Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			A context, 
			Http2ConnectionStream connectionStream, 
			B request
		) {
		this.configuration = configuration;
		this.sink = sink;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.context = context;
		this.connectionStream = connectionStream;
		this.request = request;
	}
	
	/**
	 * <p>
	 * Starts the request timeout task.
	 * </p>
	 */
	private void startTimeout() {
		if(this.configuration.request_timeout() > 0) {
			this.timeoutFuture = this.connectionStream.executor().schedule(
				() -> {
					this.timeoutFuture = null;
					this.dispose(new RequestTimeoutException("Exceeded timeout " + this.configuration.request_timeout() + "ms"));
					this.connectionStream.resetStream(Http2Error.CANCEL.code());
				}, 
				this.configuration.request_timeout(), 
				TimeUnit.MILLISECONDS
			);
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
	 * This method invokes the start logic implementd in {@link #doStart()} and starts the request timeout task.
	 * </p>
	 * 
	 * @see #doStart() 
	 */
	final void start() {
		this.doStart();
		this.startTimeout();
	}
	
	/**
	 * <p>
	 * Starts the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method shall implement the specific exchange start logic, typically send the request.
	 * </p>
	 */
	protected abstract void doStart();
	
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
	final void emitResponse(Http2Headers headers) {
		this.cancelTimeout();
		this.response = new Http2Response(this.headerService, this.parameterConverter, headers);
		this.response.body().transform(data -> {
			if(data instanceof Mono) {
				return Mono.from(data)
					.doOnCancel(() -> this.reset(Http2Error.CANCEL.code()))
					.doOnSuccess(ign -> this.dispose(null));
			}
			else {
				return Flux.from(data)
					.doOnCancel(() -> this.reset(Http2Error.CANCEL.code()))
					.doOnComplete(() -> this.dispose(null));
			}
		});
		
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
	 * This method cancels the request timeout and sets the cancel cause and makes sure the exchange disposal logic implemented in {@link #doDispose(java.lang.Throwable) } is invoked once.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 * 
	 * @see #doDispose(java.lang.Throwable) 
	 */
	final void dispose(Throwable cause) {
		this.cancelTimeout();
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
	public HttpVersion getProtocol() {
		return HttpVersion.HTTP_2_0;
	}

	@Override
	public A context() {
		return this.context;
	}

	@Override
	public B request() {
		return this.request;
	}

	@Override
	public Http2Response response() {
		return this.response;
	}
	
	@Override
	public void reset(long code) {
		// reseting the stream should dispose the stream
		this.connectionStream.resetStream(code);
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return Optional.ofNullable(this.cancelCause);
	}
}
