/*
 * Copyright 2025 Jeremy KUHN
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
package io.inverno.mod.session.http;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.session.Session;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session injector injects or removes the session identifier from an exchange.
 * </p>
 *
 * <p>
 * It is used by the {@link SessionInterceptor} to inject the session identifier in the exchange when a new session is created or if the session identifier has been refreshed and to remove the session
 * identifier from the exchange when the session has been invalidated.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the session type
 * @param <C> the exchange context type
 * @param <D> the exchange type
 */
@FunctionalInterface
public interface SessionInjector<A, B extends Session<A>, C extends ExchangeContext, D extends Exchange<C>> {

	/**
	 * <p>
	 * Injects the specified session into the exchange.
	 * </p>
	 *
	 * @param exchange the exchange
	 * @param session  the session
	 *
	 * @return a mono which completes when the session has been injected
	 */
	Mono<Void> inject(D exchange, B session);

	/**
	 * <p>
	 * Removes the session from the exchange.
	 * </p>
	 *
	 * @param exchange the exchange
	 *
	 * @return a mono which completes when the session has been removed
	 */
	default Mono<Void> remove(D exchange) {
		return Mono.empty();
	}

	/**
	 * <p>
	 * Invokes this session injector and then invokes the specified session injector.
	 * </p>
	 *
	 * @param after the injector to invoke after this injector
	 *
	 * @return a composed session injector that invokes in sequence this injector followed by the specified injector
	 */
	default SessionInjector<A, B, C, D> andThen(SessionInjector<? super A, ? super B, ? super C, ? super D> after) {
		return new SessionInjector<A, B, C, D>() {
			@Override
			public Mono<Void> inject(D exchange, B session) {
				return SessionInjector.this.inject(exchange, session).then(after.inject(exchange, session));
			}

			@Override
			public Mono<Void> remove(D exchange) {
				return SessionInjector.this.remove(exchange).then(after.remove(exchange));
			}
		};
	}

	/**
	 * <p>
	 * Invokes the specified session injector and then invokes this session injector.
	 * </p>
	 *
	 * @param before the injector to invoke before this injector
	 *
	 * @return a composed session injector that invokes in sequence the specified injector followed by this injector
	 */
	default SessionInjector<A, B, C, D> compose(SessionInjector<? super A, ? super B, ? super C, ? super D> before) {
		return new SessionInjector<A, B, C, D>() {
			@Override
			public Mono<Void> inject(D exchange, B session) {
				return before.inject(exchange, session).then(SessionInjector.this.inject(exchange, session));
			}

			@Override
			public Mono<Void> remove(D exchange) {
				return before.remove(exchange).then(SessionInjector.this.remove(exchange));
			}
		};
	}

	/**
	 * <p>
	 * Returns a composed session injector that invokes the specified injectors in sequence.
	 * </p>
	 *
	 * @param <A> the session data type
	 * @param <B> the session type
	 * @param <C> the exchange context type
	 * @param <D> the exchange type
	 * @param injectors the list of injectors to invoke in sequence
	 *
	 * @return a composed session injector that invokes the specified injectors in sequence
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	static <A, B extends Session<A>, C extends ExchangeContext, D extends Exchange<C>> SessionInjector<A, B, C, D> of(SessionInjector<? super A, ? super B, ? super C, ? super D>... injectors) {
		return new SessionInjector<A, B, C, D>() {
			@Override
			public Mono<Void> inject(D exchange, B session) {
				return Flux.fromArray(injectors).concatMap(injector -> injector.inject(exchange, session)).then();
			}

			@Override
			public Mono<Void> remove(D exchange) {
				return Flux.fromArray(injectors).concatMap(injector -> injector.remove(exchange)).then();
			}
		};
	}
}
