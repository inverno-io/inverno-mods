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
 * A reactive exchange handler is used to handle server exchanges following reactive principles.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ErrorExchange
 * @see ExchangeHandler
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
@FunctionalInterface
public interface ReactiveExchangeHandler<A extends ExchangeContext, B extends Exchange<A>> {

	/**
	 * <p>
	 * Returns a composed exchange handler that first applies the interceptor to transform the exchange and then invoke the 
	 * {@link #defer(io.inverno.mod.http.server.Exchange)}.
	 * </p>
	 *
	 * @param interceptor the interceptor
	 *
	 * @return a composed exchange handler
	 */
	default ReactiveExchangeHandler<A, B> intercept(ExchangeInterceptor<A, B> interceptor) {
		return (B exchange) -> interceptor.intercept(exchange).flatMap(ReactiveExchangeHandler.this::defer);
	}
	
	/**
	 * <p>
	 * Returns a Mono that defers the processing of the exchange.
	 * </p>
	 *
	 * <p>
	 * The HTTP server subscribes to the returned Mono and after completion, subscribes to the exchange response body data stream to respond to the
	 * client.
	 * </p>
	 *
	 * @param exchange the exchange to process
	 *
	 * @return an empty mono that completes when the exchange has been processed
	 */
	Mono<Void> defer(B exchange);
}
