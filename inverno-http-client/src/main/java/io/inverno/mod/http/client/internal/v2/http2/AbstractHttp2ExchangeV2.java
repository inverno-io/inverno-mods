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
package io.inverno.mod.http.client.internal.v2.http2;

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
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
abstract class AbstractHttp2ExchangeV2<A extends ExchangeContext, B extends HttpConnectionRequest> implements HttpConnectionExchange<A, B, Http2ResponseV2> {

	protected final HttpClientConfiguration configuration;
	protected final Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink;
	protected final HeaderService headerService;
	protected final ObjectConverter<String> parameterConverter;
	protected final A context;
	protected final Http2ConnectionStreamV2 connectionStream;
	
	protected final B request;
	protected Http2ResponseV2 response;
	
	private ScheduledFuture<?> timeoutFuture;
	
	private Throwable cancelCause;

	public AbstractHttp2ExchangeV2(
			HttpClientConfiguration configuration, 
			Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			A context, 
			Http2ConnectionStreamV2 connectionStream, 
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
	
	private void cancelTimeout() {
		if(this.timeoutFuture != null) {
			this.timeoutFuture.cancel(false);
			this.timeoutFuture = null;
		}
	}
	
	final void start() {
		this.doStart();
		this.startTimeout();
	}
	
	protected abstract void doStart();
	
	final void emitResponse(Http2Headers headers) {
		this.cancelTimeout();
		this.response = new Http2ResponseV2(this.headerService, this.parameterConverter, headers);
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
	
	final void dispose(Throwable cause) {
		this.cancelTimeout();
		if(this.cancelCause == null) {
			this.cancelCause = cause;
		}
		this.doDispose(cause);
	}
	
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
	public Http2ResponseV2 response() {
		return this.response;
	}
	
	@Override
	public void reset(long code) {
		// reseting the stream should dispose the stream
//		this.dispose(new HttpClientException("Exchange has been reset: " + code));
		this.connectionStream.resetStream(code);
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return Optional.ofNullable(this.cancelCause);
	}
}
