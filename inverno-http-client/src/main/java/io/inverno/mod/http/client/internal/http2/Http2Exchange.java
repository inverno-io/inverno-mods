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
import io.inverno.mod.http.client.ResetStreamException;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.handler.codec.http2.Http2Headers;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Http/2 {@link Exchange} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> The exchange context type
 */
public class Http2Exchange<A extends ExchangeContext> extends AbstractHttp2Exchange<A, Http2Request> {

	/**
	 * <p>
	 * Creates an Http/2 exchange.
	 * </p>
	 *
	 * @param state              the state
	 * @param configuration      the HTTP client configuration
	 * @param sink               the exchange sink
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param context            the exchange context
	 * @param connectionStream   the Http/2 connection stream
	 * @param request            the Http/2 request
	 */
	public Http2Exchange(
			Object state,
			HttpClientConfiguration configuration, 
			Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			A context, 
			Http2ConnectionStream connectionStream, 
			EndpointRequest request) {
		super(state, configuration, sink, headerService, parameterConverter, context, connectionStream, new Http2Request(headerService, parameterConverter, connectionStream, request, configuration.http2_validate_headers()));
	}
	
	@Override
	public void start() {
		this.request.send();
	}

	@Override
	protected Http2Response createResponse(Http2Headers headers) {
		if(headers.getInt(Http2Headers.PseudoHeaderName.STATUS.value()) == 100) {
			this.request.sendBody();
			return null;
		}
		return super.createResponse(headers);
	}
	
	@Override
	protected void doDispose(Throwable cause) {
		if(cause instanceof ResetStreamException && this.connectionStream.getStream().isResetSent()) {
			// We sent the reset
			cause = null;
		}
		this.request.dispose(cause);
		if(this.response != null) {
			this.response.dispose(cause);
		}
	}
}
