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
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.RequestTimeoutException;
import io.inverno.mod.http.client.internal.AbstractExchange;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base Http/2 {@link Exchange} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
abstract class AbstractHttp2Exchange<A extends ExchangeContext, B extends HttpConnectionRequest> extends AbstractExchange<A, B, Http2Response, Http2Headers> {

	protected final Http2ConnectionStream connectionStream;
	
	private ScheduledFuture<?> timeoutFuture;

	/**
	 * <p>
	 * Creates an Http/2 exchane.
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
		super(configuration, sink, headerService, parameterConverter, context, request);
		this.connectionStream = connectionStream;
	}
	
	@Override
	protected final void startTimeout() {
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
	
	@Override
	protected final void cancelTimeout() {
		if(this.timeoutFuture != null) {
			this.timeoutFuture.cancel(false);
			this.timeoutFuture = null;
		}
	}
	
	@Override
	protected final Http2Response createResponse(Http2Headers headers) {
		Http2Response response = new Http2Response(this.headerService, this.parameterConverter, headers);
		response.body().transform(data -> {
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
		return response;
	}
	
	@Override
	public HttpVersion getProtocol() {
		return HttpVersion.HTTP_2_0;
	}
	
	@Override
	public void doReset(long code) {
		// reseting the stream should dispose the stream
//		this.dispose(new HttpClientException("Exchange has been reset: " + code));
		this.connectionStream.resetStream(code);
	}
}
