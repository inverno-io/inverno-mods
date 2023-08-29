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

import io.inverno.mod.http.base.ExchangeContext;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An exchange interceptor is used to intercept a server exchange before it is handled by a {@link ExchangeHandler}.
 * </p>
 *
 * <p>
 * Multiple exchange interceptors can be chained on an exchange handler invoking {@link ReactiveExchangeHandler#intercept(io.inverno.mod.http.server.ExchangeInterceptor)} method multiple times in
 * order to form an exchange handling chain.
 * </p>
 *
 * <p>
 * An exchange interceptor is meant to perform extra processing or instrumentation on the exchange prior to the actual exchange processing by the exchange handler. But it can also fully process the
 * exchange and stop the exchange handling chain by returning an empty Mono in which case the exchange handler will not be invoked.
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
	
	/**
	 * <p>
	 * Returns a composed interceptor that invokes this interceptor first and then invokes the specified interceptor.
	 * </p>
	 * 
	 * @param after the interceptor to invoke after this interceptor
	 * 
	 * @return a composed interceptor that invokes in sequence this interceptor followed by the specified interceptor
	 */
	default ExchangeInterceptor<A, B> andThen(ExchangeInterceptor<? super A, B> after) {
		return exchange -> this.intercept(exchange).flatMap(after::intercept);
	}
	
	/**
	 * <p>
	 * Returns a composed interceptor that invokes the specified interceptor first and then invokes this interceptor.
	 * </p>
	 * 
	 * @param before the interceptor to invoke before this interceptor
	 * 
	 * @return a composed interceptor that invokes in sequence this interceptor followed by the specified interceptor
	 */
	default ExchangeInterceptor<A, B> compose(ExchangeInterceptor<? super A, B> before) {
		return exchange -> before.intercept(exchange).flatMap(this::intercept);
	}
	
	/**
	 * <p>
	 * Returns an interceptor resulting from chaining the specified interceptors in sequence.
	 * </p>
	 *
	 * @param <A> the type of the exchange context
	 * @param <B> the type of exchange handled by the handler
	 * @param interceptors the interceptors to chain
	 *
	 * @return a composed interceptor
	 */
	@SafeVarargs
    @SuppressWarnings("varargs")
	static <A extends ExchangeContext, B extends Exchange<A>> ExchangeInterceptor<A, B> of(ExchangeInterceptor<? super A, B>... interceptors) {
		return exchange -> {
			Mono<? extends B> interceptorChain = null;
			for(ExchangeInterceptor<? super A, B> interceptor : interceptors) {
				if(interceptorChain == null) {
					interceptorChain = interceptor.intercept(exchange);
				}
				else {
					interceptorChain = interceptorChain.flatMap(interceptor::intercept);
				}
			}
			return interceptorChain;
		};
	}
}
