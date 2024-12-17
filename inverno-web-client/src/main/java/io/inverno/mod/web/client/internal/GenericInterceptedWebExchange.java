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
package io.inverno.mod.web.client.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.InterceptedExchange;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.client.InterceptedWebExchange;
import io.inverno.mod.web.client.WebResponse;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link InterceptedWebExchange} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericInterceptedWebExchange<A extends ExchangeContext> implements InterceptedWebExchange<A> {

	private final InterceptedExchange<A> exchange;
	private final GenericInterceptedWebRequest request;
	private final GenericInterceptedWebResponse response;

	private Function<WebResponse, Mono<Void>> errorMapper;

	/**
	 * <p>
	 * Creates a generic intercepted Web exchange.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param uri                   the service URI
	 * @param exchange              the originating intercepted exchange
	 */
	public GenericInterceptedWebExchange(DataConversionService dataConversionService, URI uri, InterceptedExchange<A> exchange) {
		this.exchange = exchange;

		this.request = new GenericInterceptedWebRequest(uri, exchange.request());
		this.response = new GenericInterceptedWebResponse(dataConversionService, exchange.response());
	}

	@Override
	public GenericInterceptedWebExchange<A> failOnErrorStatus(Function<WebResponse, Mono<Void>> errorMapper) {
		this.errorMapper = errorMapper;
		return this;
	}
	
	@Override
	public GenericInterceptedWebRequest request() {
		return this.request;
	}

	@Override
	public GenericInterceptedWebResponse response() {
		return this.response;
	}

	@Override
	public HttpVersion getProtocol() {
		return this.exchange.getProtocol();
	}

	@Override
	public A context() {
		return this.exchange.context();
	}

	@Override
	public void reset(long code) {
		this.exchange.reset();
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return this.exchange.getCancelCause();
	}

	public Function<WebResponse, Mono<Void>> getErrorMapper() {
		return errorMapper;
	}
}
