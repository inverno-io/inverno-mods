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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.ErrorWebExchange;
import io.inverno.mod.web.WebRequest;
import io.inverno.mod.web.WebResponse;
import java.util.function.Function;
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

	private final GenericWebRequest request;
	
	private final GenericWebResponse response;
	
	private final Throwable error;
	
	private final ExchangeContext context;
	
	private final Function<Mono<Void>, Exchange<ExchangeContext>> finalizerConsumer;
	
	public GenericErrorWebExchange(GenericWebRequest request, GenericWebResponse response, Throwable error, ExchangeContext context, Function<Mono<Void>, Exchange<ExchangeContext>> finalizerConsumer) {
		this.request = request;
		this.response = response;
		this.error = error;
		this.context = context;
		this.finalizerConsumer = finalizerConsumer;
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
	public Throwable getError() {
		return this.error;
	}
	
	@Override
	public ExchangeContext context() {
		return this.context;
	}
	
	@Override
	public GenericErrorWebExchange finalizer(Mono<Void> finalizer) {
		this.finalizerConsumer.apply(finalizer);
		return this;
	}
}
