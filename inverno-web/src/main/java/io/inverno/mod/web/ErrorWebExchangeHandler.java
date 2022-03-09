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
package io.inverno.mod.web;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.*;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An error exchange handler used to handle {@link ErrorWebExchange}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <A> the error type
 */
public interface ErrorWebExchangeHandler<A extends Throwable> extends ErrorExchangeHandler<A, ErrorWebExchange<A>> {

	/**
	 * <p>
	 * Wraps the specified Error Exchange handler into an Error Web Exchange handler.
	 * </p>
	 *
	 * @param handler the handler to wrap
	 * @param <A> the error type
	 *
	 * @return an Error Web Exchange handler
	 */
	static <A extends Throwable> ErrorWebExchangeHandler<A> wrap(ErrorExchangeHandler<A, ErrorWebExchange<A>> handler) {
		return new ErrorWebExchangeHandler<A>() {

			@Override
			public ReactiveExchangeHandler<ExchangeContext, ErrorWebExchange<A>> intercept(ExchangeInterceptor<ExchangeContext, ErrorWebExchange<A>> interceptor) {
				return handler.intercept(interceptor);
			}

			@Override
			public Mono<Void> defer(ErrorWebExchange<A> exchange) {
				return handler.defer(exchange);
			}

			@Override
			public void handle(ErrorWebExchange<A> exchange) throws HttpException {
				handler.handle(exchange);
			}
		};
	}
}
