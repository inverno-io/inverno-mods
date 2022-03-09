/*
 * Copyright 2022 Jeremy KUHN
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

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;

/**
 * <p>
 * An exchange interceptor used to intercept {@link ErrorWebExchange}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the error type
 */
public interface ErrorWebExchangeInterceptor<A extends Throwable> extends ExchangeInterceptor<ExchangeContext, ErrorWebExchange<A>> {

	/**
	 * <p>
	 * Wraps the specified Error Exchange interceptor into an Error Web Exchange interceptor.
	 * </p>
	 *
	 * @param interceptor the interceptor to wrap
	 * @param <A> the error type
	 *
	 * @return an Error Web Exchange interceptor
	 */
	static <A extends Throwable> ErrorWebExchangeInterceptor<A> wrap(ExchangeInterceptor<ExchangeContext, ErrorWebExchange<A>> interceptor) {
		return exchange -> {
			return interceptor.intercept(exchange);
		};
	}
}
