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
package io.inverno.mod.web;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An exchange handler used to handle {@link WebExchange}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <A> the type of web exchange context
 */
public interface WebExchangeHandler<A extends ExchangeContext> extends ExchangeHandler<A, WebExchange<A>> {

	/**
	 * <p>
	 * Wraps the specified Exchange handler into a Web Exchange handler.
	 * </p>
	 *
	 * @param handler the handler to wrap
	 * @param <A> the type of the exchange context
	 *
	 * @return a Web Exchange handler
	 */
	static <A extends ExchangeContext> WebExchangeHandler<A> wrap(ExchangeHandler<A, WebExchange<A>> handler) {
		return new WebExchangeHandler<A>() {

			@Override
			public ReactiveExchangeHandler<A, WebExchange<A>> intercept(ExchangeInterceptor<A, WebExchange<A>> interceptor) {
				return handler.intercept(interceptor);
			}

			@Override
			public Mono<Void> defer(WebExchange<A> exchange) {
				return handler.defer(exchange);
			}

			@Override
			public void handle(WebExchange<A> exchange) throws HttpException {
				handler.handle(exchange);
			}
		};
	}
}
