/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.Response;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base {@link ErrorExchange} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericErrorExchange implements ErrorExchange<ExchangeContext> {

	private final Exchange<ExchangeContext> exchange;
	private final AbstractResponse response;
	private final Throwable error;
	private Mono<Void> finalizer;
	
	/**
	 * <p>
	 * Creates an error exchange with the specified request, response and error.
	 * </p>
	 *
	 * @param exchange              the original failed exchange 
	 * @param response              the response
	 * @param error                 the error
	 * @param finalizer             the exchange finalizer
	 */
	public GenericErrorExchange(Exchange<ExchangeContext> exchange, AbstractResponse response, Throwable error, Mono<Void> finalizer) {
		this.exchange = exchange;
		this.response = response;
		this.error = error;
		this.finalizer = finalizer;
	}

	@Override
	public HttpVersion getProtocol() {
		return this.exchange.getProtocol();
	}
	
	@Override
	public ExchangeContext context() {
		return this.exchange.context();
	}
	
	@Override
	public Request request() {
		return this.exchange.request();
	}

	@Override
	public Response response() {
		return this.response;
	}

	@Override
	public void reset(long code) {
		this.exchange.reset(code);
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return this.exchange.getCancelCause();
	}
	
	@Override
	public void finalizer(Mono<Void> finalizer) {
		if(this.finalizer != null) {
			this.finalizer = this.finalizer.then(finalizer);
		}
		else {
			this.finalizer = finalizer;
		}
	}

	@Override
	public Throwable getError() {
		return this.error;
	}
}
