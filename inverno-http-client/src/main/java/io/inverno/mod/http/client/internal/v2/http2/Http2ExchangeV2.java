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
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class Http2ExchangeV2<A extends ExchangeContext> extends AbstractHttp2ExchangeV2<A, Http2RequestV2> {

	public Http2ExchangeV2(
			HttpClientConfiguration configuration, 
			Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			A context, 
			Http2ConnectionStreamV2 connectionStream, 
			EndpointRequest request) {
		super(configuration, sink, headerService, parameterConverter, context, connectionStream, new Http2RequestV2(headerService, parameterConverter, connectionStream, request, configuration.http2_validate_headers()));
	}
	
	@Override
	protected void doStart() {
		this.request.send();
	}
	
	@Override
	protected void doDispose(Throwable cause) {
		this.request.dispose(cause);
		if(this.response != null) {
			this.response.dispose(cause);
		}
		else if(this.sink != null) {
			this.sink.tryEmitError(cause != null ? cause : new HttpClientException("Exchange was disposed"));
		}
	}
}
