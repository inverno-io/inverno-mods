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

import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;

/**
 * <p>
 * An exchange interceptor used to intercept {@link WebExchange}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the type of the exchange context
 */
public interface WebExchangeInterceptor<A extends ExchangeContext> extends ExchangeInterceptor<A, WebExchange<A>> {

	/**
	 * <p>
	 * Wraps the specified Exchange interceptor into a Web Exchange interceptor.
	 * </p>
	 *
	 * @param interceptor the interceptor to wrap
	 * @param <A> the type of the exchange context
	 *
	 * @return a Web Exchange interceptor
	 */
	static <A extends ExchangeContext> WebExchangeInterceptor<A> wrap(ExchangeInterceptor<A, WebExchange<A>> interceptor) {
		return exchange -> {
			return interceptor.intercept(exchange);
		};
	}
}
