/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.WebRequest;
import io.inverno.mod.web.server.WebResponse;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link ErrorWebExchange} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebResponse
 */
class GenericErrorWebExchange implements ErrorWebExchange<ExchangeContext> {

	private final ErrorExchange<ExchangeContext> errorExchange;
	private final GenericWebRequest request;
	private final GenericWebResponse response;
	
	public GenericErrorWebExchange(ErrorExchange<ExchangeContext> errorExchange, GenericWebRequest request, GenericWebResponse response) {
		this.errorExchange = errorExchange;
		this.request = request;
		this.response = response;
	}

	@Override
	public HttpVersion getProtocol() {
		return this.errorExchange.getProtocol();
	}
	
	@Override
	public ExchangeContext context() {
		return this.errorExchange.context();
	}
	
	@Override
	public WebRequest request() {
		return this.request;
	}
	
	@Override
	public WebResponse response() {
		return this.response;
	}

	@Override
	public void reset(long code) {
		this.errorExchange.reset(code);
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return this.errorExchange.getCancelCause();
	}
	
	@Override
	public Throwable getError() {
		return this.errorExchange.getError();
	}
	
	@Override
	public void finalizer(Mono<Void> finalizer) {
		this.errorExchange.finalizer(finalizer);
	}
}
