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
package io.inverno.mod.http.server;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An exchange handler is used to handle server exchanges.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see ErrorExchange
 * @see ReactiveExchangeHandler
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
@FunctionalInterface
public interface ExchangeHandler<A extends ExchangeContext, B extends Exchange<A>> extends ReactiveExchangeHandler<A, B> {

	/**
	 * <p>
	 * By default, returns a Mono that defers the execution of {@link #handle(io.inverno.mod.http.server.Exchange) }.
	 * </p>
	 */
	@Override
	default Mono<Void> defer(B exchange) throws HttpException {
		return Mono.fromRunnable(() -> this.handle(exchange));
	}
	
	/**
	 * <p>
	 * Processes the specified server exchange.
	 * </p>
	 *
	 * <p>
	 * This method is more convenient than {@link #defer(io.inverno.mod.http.server.Exchange) } when the handling logic does not need to be reactive.
	 * </p>
	 *
	 * @param exchange the exchange to process
	 *
	 * @throws HttpException if an error occurs during the processing of the exchange
	 */
	void handle(B exchange) throws HttpException;
}
