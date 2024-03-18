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
package io.inverno.mod.http.client;

import io.inverno.mod.http.base.ExchangeContext;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An client exchange interceptor is used to intercept a client exchange before a request is actually sent to the server.
 * </p>
 * 
 * <p>
 * A client exchange interceptor operates on a {@link InterceptableExchange} which allows to perform some processing or instrumentation that will be applied to the actual {@link Exchange}, to the request first
 * before it is sent to the endpoint and to the response when it is received from the endpoint.
 * </p>
 * 
 * <p>
 * Multiple exchange interceptors can be chained on an {@link HttpClient.EndpointBuilder}.
 * </p>
 * 
 * <p>
 * An interceptor can also prevent a request from being sent to the endpoint by returning an empty Mono, in which case the interceptable exchange is returned instead of an actual exchange.
 * </p>
 * 
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of the interceptable exchange
 */
@FunctionalInterface
public interface ExchangeInterceptor<A extends ExchangeContext, B extends InterceptableExchange<A>> {

	/**
	 * <p>
	 * Intercepts the exchange before the request is sent.
	 * </p>
	 *
	 * @param exchange the interceptable exchange to handle
	 *
	 * @return a Mono emitting the exchange or an instrumented exchange to continue the exchange processing or an empty Mono to stop the exchange processing and prevent the request from being sent to
	 *         the endpoint in which case the interceptable exchange shall be returned by the sent operation.
	 */
	Mono<B> intercept(B exchange);
	
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
	 * @param <B> the type of the interceptable exchange
	 * @param interceptors the interceptors to chain
	 *
	 * @return a composed interceptor
	 */
	@SafeVarargs
    @SuppressWarnings("varargs")
	static <A extends ExchangeContext, B extends InterceptableExchange<A>> ExchangeInterceptor<A, B> of(ExchangeInterceptor<? super A, B>... interceptors) {
		return exchange -> {
			Mono<B> interceptorChain = null;
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
