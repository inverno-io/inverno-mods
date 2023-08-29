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
package io.inverno.mod.http.server;

import io.inverno.mod.http.base.ExchangeContext;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A reactive server controller defines how exchanges and error exchanges must be handled within the HTTP server following reactive principles.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the controller
 * @param <C> the type of error exchange handled by the controller
 */
public interface ReactiveServerController<A extends ExchangeContext, B extends Exchange<A>, C extends ErrorExchange<A>> {

	/**
	 * <p>
	 * Returns a Mono that defers the processing of an exchange.
	 * </p>
	 *
	 * <p>
	 * The HTTP server subscribes to the returned Mono and, on completion, subscribes to the exchange response body data publisher to respond to the client.
	 * </p>
	 *
	 * @param exchange the exchange to process
	 *
	 * @see ReactiveExchangeHandler
	 *
	 * @return an empty mono that completes when the exchange has been processed
	 */
	Mono<Void> defer(B exchange);

	/**
	 * <p>
	 * Returns a Mono that defers the processing of an error exchange.
	 * </p>
	 *
	 * <p>
	 * In case of error, the HTTP server creates an error exchange from the original exchange, subscribes to the returned Mono and on completion, subscribes to the error exchange response body data
	 * publisher to respond to the client.
	 * </p>
	 *
	 * @param errorExchange the error exchange to process
	 *
	 * @see ReactiveExchangeHandler
	 *
	 * @return an empty mono that completes when the error exchange has been processed
	 */
	Mono<Void> defer(C errorExchange);
}
