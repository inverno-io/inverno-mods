/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.security.http.login;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.authentication.Authentication;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Handles successful authentication in a {@link LoginActionHandler}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see LoginActionHandler
 * 
 * @param <A> the authentication type
 * @param <B> the context type
 * @param <C> the exchange type
 */
@FunctionalInterface
public interface LoginSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> {

	/**
	 * <p>
	 * Handles successful authentication.
	 * </p>
	 *
	 * @param exchange       the exchange
	 * @param authentication the authentication resulting from the authentication process
	 *
	 * @return a mono which completes when the authentication has been handled
	 */
	Mono<Void> handleLoginSuccess(C exchange, A authentication);

	/**
	 * <p>
	 * Invokes this login success handler and then invokes the specified login success handler.
	 * </p>
	 * 
	 * @param after the handler to invoke after this handler
	 * 
	 * @return a composed login success handler that invokes in sequence this handler followed by the specified handler
	 */
	default LoginSuccessHandler<A, B, C> andThen(LoginSuccessHandler<? super A, ? super B, ?super C> after) {
		return (exchange, authentication) -> this.handleLoginSuccess(exchange, authentication).then(after.handleLoginSuccess(exchange, authentication));
	}
	
	/**
	 * <p>
	 * Invokes the specified login success handler and then invokes this login success handler.
	 * </p>
	 * 
	 * @param before the handler to invoke before this handler
	 * 
	 * @return a composed login success handler that invokes in sequence the specified handler followed by this handler
	 */
	default LoginSuccessHandler<A, B, C> compose(LoginSuccessHandler<? super A, ? super B, ?super C> before) {
		return (exchange, authentication) -> before.handleLoginSuccess(exchange, authentication).then(this.handleLoginSuccess(exchange, authentication));
	}
	
	/**
	 * <p>
	 * Returns a composed login success handler that invokes the specified handlers in sequence.
	 * </p>
	 *
	 * @param <A> the authentication type
	 * @param <B> the context type
	 * @param <C> the exchange type
	 * @param handlers the list of handlers to invoke in sequence
	 *
	 * @return a composed login success handler that invokes the specified handlers in sequence
	 */
	@SafeVarargs
    @SuppressWarnings("varargs")
	static <A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> LoginSuccessHandler<A, B, C> of(LoginSuccessHandler<? super A, ? super B, ?super C>... handlers) {
		return (exchange, authentication) -> {
			Mono<Void> handlerChain = null;
			for(LoginSuccessHandler<? super A, ? super B, ?super C> handler : handlers) {
				if(handlerChain == null) {
					handlerChain = handler.handleLoginSuccess(exchange, authentication);
				}
				else {
					handlerChain = handlerChain.thenEmpty(handler.handleLoginSuccess(exchange, authentication));
				}
			}
			return handlerChain;
		};
	}
}
