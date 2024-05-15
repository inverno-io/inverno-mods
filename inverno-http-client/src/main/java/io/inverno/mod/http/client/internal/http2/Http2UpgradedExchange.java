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
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.inverno.mod.http.client.internal.http1x.Http1xUpgradingExchange;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * HTTP/2 {@link Exchange} implementation representing an upgraded H2C exchange.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see Http1xUpgradingExchange
 * 
 * @param <A> The exchange context type
 */
public class Http2UpgradedExchange<A extends ExchangeContext> extends AbstractHttp2Exchange<A, HttpConnectionRequest> {

	/**
	 * <p>
	 * Creates an upgraded HTTP/2 exchange.
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
	public Http2UpgradedExchange(
			HttpClientConfiguration configuration, 
			Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			A context, 
			Http2ConnectionStream connectionStream, 
			HttpConnectionRequest request
		) {
		super(configuration, sink, headerService, parameterConverter, context, connectionStream, request);
	}

	@Override
	protected void doStart() {
		// Does nothing since the request has already been sent
	}

	@Override
	protected void doDispose(Throwable cause) {
		if(this.response != null) {
			this.response.dispose(cause);
		}
		else if(this.sink != null) {
			this.sink.tryEmitError(cause != null ? cause : new HttpClientException("Exchange was disposed"));
		}
	}
}
