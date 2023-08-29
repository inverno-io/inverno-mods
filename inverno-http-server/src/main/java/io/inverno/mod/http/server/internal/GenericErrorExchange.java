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
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.Response;
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

	private final HttpVersion protocol;
	private final Request request;
	private final AbstractResponse response;
	private Mono<Void> finalizer;
	private final Throwable error;
	private final ExchangeContext exchangeContext;
	
	/**
	 * <p>
	 * Creates an error exchange with the specified request, response and error.
	 * </p>
	 * 
	 * @param protocol        the exchange HTTP version
	 * @param request         the request
	 * @param response        the response
	 * @param finalizer       the exchange finalizer
	 * @param error           the error
	 * @param exchangeContext the exchange context attached to the failed exchange
	 */
	public GenericErrorExchange(HttpVersion protocol, AbstractRequest request, AbstractResponse response, Mono<Void> finalizer, Throwable error, ExchangeContext exchangeContext) {
		this.protocol = protocol;
		this.request = request;
		this.response = response;
		this.finalizer = finalizer;
		this.error = error;
		this.exchangeContext = exchangeContext;
	}

	@Override
	public HttpVersion getProtocol() {
		return this.protocol;
	}
	
	@Override
	public Request request() {
		return this.request;
	}

	@Override
	public Response response() {
		return this.response;
	}
	
	@Override
	public ExchangeContext context() {
		return exchangeContext;
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
