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

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.internal.GenericErrorExchangeHandler;
import java.util.Objects;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * <p>
 * A server controller defines how server exchanges and server error exchanges are handled, within the HTTP server.
 * </p>
 *
 * <p>
 * When receiving a client request, the HTTP server creates an {@link Exchange} and invokes the server controller to actually process that request and
 * provide a response to the client. In case of error during that process, it creates an {@link ErrorExchange} from the original exchange and invokes
 * the controller again to handle the error and provide an error response to the client.
 * </p>
 * 
 * <p>
 * The HTTP server shall only rely on the {@link #defer(io.inverno.mod.http.server.Exchange)} and {@link #defer(io.inverno.mod.http.server.ErrorExchange)
 * } methods in order to remain reactive, the server controller only exposes non-reactive handling methods to facilitate the definition of the
 * controller using lambdas.
 * </p>
 * 
 * <p>
 * The {@link #createContext() } method is used by the server to create the exchange context associated to an Exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see ExchangeHandler
 * @see ErrorExchangeHandler
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the controller
 * @param <C> the type of error exchange handled by the controller
 */
@FunctionalInterface
public interface ServerController<A extends ExchangeContext, B extends Exchange<A>, C extends ErrorExchange<A>> extends ReactiveServerController<A, B, C> {

	/**
	 * <p>
	 * By default, returns a Mono that defers the execution of {@link #handle(io.inverno.mod.http.server.Exchange) }.
	 * </p>
	 */
	@Override
	default Mono<Void> defer(B exchange) {
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

	/**
	 * <p>
	 * By default, returns a Mono that defers the execution of {@link #handle(io.inverno.mod.http.server.ErrorExchange) }.
	 * </p>
	 */
	@Override
	default Mono<Void> defer(C errorExchange) {
		return Mono.fromRunnable(() -> this.handle(errorExchange));
	}

	/**
	 * <p>
	 * Processes the specified server error exchange.
	 * </p>
	 *
	 * <p>
	 * The purpose of this method is to eventually inject a {@link ResponseBody} in the response which basically completes the exchange.
	 * </p>
	 *
	 * @param errorExchange the exchange to process
	 *
	 * @throws HttpException if an error occurs during the processing of the exchange
	 */
	@SuppressWarnings("unchecked")
	default void handle(C errorExchange) throws HttpException {
		GenericErrorExchangeHandler.INSTANCE.handle((ErrorExchange<ExchangeContext>) errorExchange);
	}

	/**
	 * <p>
	 * Creates the context that is eventually attached to the exchange.
	 * </p>
	 * 
	 * <p>
	 * This method returns null by default.
	 * </p>
	 * 
	 * @return 
	 */
	default A createContext() {
		return null;
	}
	
	/**
	 * <p>
	 * Returns a server controller that delegates to the specified exchange handler.
	 * </p>
	 *
	 * @param <U>          the type of the exchange context
	 * @param <V>          the type of exchange handled by the controller
	 * @param <W>          the type of error exchange handled by the controller
	 * @param handler      an exchange handler
	 *
	 * @return a server controller
	 */
	static <U extends ExchangeContext, V extends Exchange<U>, W extends ErrorExchange<U>> ServerController<U, V, W> from(ExchangeHandler<U, V> handler) {
		Objects.requireNonNull(handler);
		return new ServerController<U, V, W>() {

			@Override
			public Mono<Void> defer(V exchange) {
				return handler.defer(exchange);
			}

			@Override
			public void handle(V exchange) {
				handler.handle(exchange);
			}

			@Override
			@SuppressWarnings("unchecked")
			public Mono<Void> defer(W errorExchange) {
				return GenericErrorExchangeHandler.INSTANCE.defer((ErrorExchange<ExchangeContext>) errorExchange);
			}

			@Override
			@SuppressWarnings("unchecked")
			public void handle(W errorExchange) {
				GenericErrorExchangeHandler.INSTANCE.handle((ErrorExchange<ExchangeContext>) errorExchange);
			}
		};
	}

	/**
	 * <p>
	 * Returns a server controller that delegates to the specified exchange handler and error exchange handler.
	 * </p>
	 *
	 * @param <U>          the type of the exchange context
	 * @param <V>          the type of exchange handled by the controller
	 * @param <W>          the type of error exchange handled by the controller
	 * @param handler      an exchange handler
	 * @param errorHandler an error exchange handler
	 *
	 * @return a server controller
	 */
	static <U extends ExchangeContext, V extends Exchange<U>, W extends ErrorExchange<U>> ServerController<U, V, W> from(ExchangeHandler<U, V> handler, ErrorExchangeHandler<U, W> errorHandler) {
		Objects.requireNonNull(handler);
		Objects.requireNonNull(errorHandler);
		return new ServerController<U, V, W>() {

			@Override
			public Mono<Void> defer(V exchange) {
				return handler.defer(exchange);
			}

			@Override
			public void handle(V exchange) {
				handler.handle(exchange);
			}

			@Override
			public Mono<Void> defer(W errorExchange) {
				return errorHandler.defer(errorExchange);
			}

			@Override
			public void handle(W errorExchange) {
				errorHandler.handle(errorExchange);
			}
		};
	}

	/**
	 * <p>
	 * Returns a server controller that delegates to the specified exchange handler and error exchange handler and uses the specified context supplier
	 * to create exchange contexts.
	 * </p>
	 *
	 * @param <U>             the type of the exchange context
	 * @param <V>             the type of exchange handled by the controller
	 * @param <W>             the type of error exchange handled by the controller
	 * @param handler         an exchange handler
	 * @param errorHandler    an error exchange handler
	 * @param contextSupplier an exchange context supplier
	 *
	 * @return a server controller
	 */
	static <U extends ExchangeContext, V extends Exchange<U>, W extends ErrorExchange<U>> ServerController<U, V, W> from(ExchangeHandler<U, V> handler, ErrorExchangeHandler<U, W> errorHandler, Supplier<U> contextSupplier) {
		Objects.requireNonNull(handler);
		Objects.requireNonNull(errorHandler);
		Objects.requireNonNull(contextSupplier);
		return new ServerController<U, V, W>() {

			@Override
			public Mono<Void> defer(V exchange) {
				return handler.defer(exchange);
			}

			@Override
			public void handle(V exchange) {
				handler.handle(exchange);
			}

			@Override
			public Mono<Void> defer(W errorExchange) {
				return errorHandler.defer(errorExchange);
			}

			@Override
			public void handle(W errorExchange) {
				errorHandler.handle(errorExchange);
			}

			@Override
			public U createContext() {
				return contextSupplier.get();
			}
		};
	}
}
