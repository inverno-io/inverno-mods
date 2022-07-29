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

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.authentication.Authentication;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Handles failed authentication in a {@link LoginActionHandler}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see LoginActionHandler
 * 
 * @param <A> the context type
 * @param <B> the exchange type
 */
public interface LoginFailureHandler<A extends ExchangeContext, B extends Exchange<A>> {

	/**
	 * <p>
	 * Handles failed authentication.
	 * </p>
	 *
	 * @param exchange the exchange
	 * @param error    the security error
	 *
	 * @return a mono which completes when the failure has been handled
	 */
	Mono<Void> handleLoginFailure(B exchange, io.inverno.mod.security.SecurityException error);
	
	/**
	 * <p>
	 * Invokes this login failure handler and then invokes the specified login failure handler.
	 * </p>
	 * 
	 * @param after the handler to invoke after this handler
	 * 
	 * @return a composed login failure handler that invokes in sequence this handler followed by the specified handler
	 */
	default LoginFailureHandler<A, B> andThen(LoginFailureHandler<? super A, ? super B> after) {
		return (exchange, authentication) -> {
			return this.handleLoginFailure(exchange, authentication).then(after.handleLoginFailure(exchange, authentication));
		};
	}
	
	/**
	 * <p>
	 * Invokes the specified login failure handler and then invokes this login failure handler.
	 * </p>
	 * 
	 * @param before the handler to invoke before this handler
	 * 
	 * @return a composed login failure handler that invokes in sequence the specified handler followed by this handler
	 */
	default LoginFailureHandler<A, B> compose(LoginFailureHandler<? super A, ? super B> before) {
		return (exchange, authentication) -> {
			return before.handleLoginFailure(exchange, authentication).then(this.handleLoginFailure(exchange, authentication));
		};
	}
	
	/**
	 * <p>
	 * Returns a composed login failure handler that invokes the specified handlers in sequence.
	 * </p>
	 *
	 * @param <A> the context type
	 * @param <B> the exchange type
	 * @param handlers the list of handlers to invoke in sequence
	 *
	 * @return a composed login failure handler that invokes the specified handlers in sequence
	 */
	@SafeVarargs
    @SuppressWarnings("varargs")
	static <A extends ExchangeContext, B extends Exchange<A>> LoginFailureHandler<A, B> of(LoginFailureHandler<? super A, ? super B>... handlers) {
		return (exchange, authentication) -> {
			Mono<Void> handlerChain = null;
			for(LoginFailureHandler<? super A, ? super B> handler : handlers) {
				if(handlerChain == null) {
					handlerChain = handler.handleLoginFailure(exchange, authentication);
				}
				else {
					handlerChain = handlerChain.thenEmpty(handler.handleLoginFailure(exchange, authentication));
				}
			}
			return handlerChain;
		};
	}
}
