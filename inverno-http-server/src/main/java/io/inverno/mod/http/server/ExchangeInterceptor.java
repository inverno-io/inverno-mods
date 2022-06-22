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
package io.inverno.mod.http.server;

import reactor.core.publisher.Mono;

/**
 * <p>
 * An exchange interceptor is used to intercept a server exchange before it is handled by a {@link ExchangeHandler}.
 * </p>
 *
 * <p>
 * Multiple exchange interceptors can be chained on an exchange handler invoking the
 * {@link ReactiveExchangeHandler#intercept(io.inverno.mod.http.server.ExchangeInterceptor)} method in order to form an exchange handling chain.
 * </p>
 *
 * <p>
 * An exchange interceptor can perform some processing or instrument the exchange prior to the actual exchange processing by the exchange handler.
 * </p>
 *
 * <p>
 * It can also process the exchange and stop the exhange handling chain by returning an empty Mono in which case the exchange handler is not invoked.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 *
 * @see ReactiveExchangeHandler
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
@FunctionalInterface
public interface ExchangeInterceptor<A extends ExchangeContext, B extends Exchange<A>> {

	/**
	 * <p>
	 * Intercepts the exchange before the exchange handler is invoked.
	 * </p>
	 *
	 * @param exchange the server exchange to handle
	 *
	 * @return a Mono emitting the exchange or an instrumented exchange to continue the exchange handling chain or an empty Mono to stop the exchange handling chain
	 */
	Mono<? extends B> intercept(B exchange);
}
