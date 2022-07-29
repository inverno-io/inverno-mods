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
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Handles successful logout in a {@link LogoutActionHandler}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see LogoutActionHandler
 * 
 * @param <A> the authentication type
 * @param <B> the identity type
 * @param <C> the access controller type
 * @param <D> the security context type
 * @param <E> the exchange type
 */
public interface LogoutSuccessHandler<A extends Authentication, B extends Identity, C extends AccessController, D extends SecurityContext<B, C>, E extends Exchange<D>> {

	/**
	 * <p>
	 * Handles successful logout.
	 * </p>
	 *
	 * @param exchange       the exchange
	 * @param authentication the authentication
	 *
	 * @return a mono which completes when the logout has been handled
	 */
	Mono<Void> handleLogoutSuccess(E exchange, A authentication);
	
	/**
	 * <p>
	 * Invokes this logout success handler and then invokes the specified logout success handler.
	 * </p>
	 * 
	 * @param after the handler to invoke after this handler
	 * 
	 * @return a composed logout success handler that invokes in sequence this handler followed by the specified handler
	 */
	default LogoutSuccessHandler<A, B, C, D, E> andThen(LogoutSuccessHandler<? super A, ? super B, ? super C,  ? super D, ? super E> after) {
		return (exchange, authentication) -> {
			return this.handleLogoutSuccess(exchange, authentication).then(after.handleLogoutSuccess(exchange, authentication));
		};
	}
	
	/**
	 * <p>
	 * Invokes the specified logout success handler and then invokes this logout success handler.
	 * </p>
	 * 
	 * @param before the handler to invoke before this handler
	 * 
	 * @return a composed logout success handler that invokes in sequence the specified handler followed by this handler
	 */
	default LogoutSuccessHandler<A, B, C, D, E> compose(LogoutSuccessHandler<? super A, ? super B, ? super C,  ? super D, ? super E> before) {
		return (exchange, authentication) -> {
			return before.handleLogoutSuccess(exchange, authentication).then(this.handleLogoutSuccess(exchange, authentication));
		};
	}
	
	/**
	 * <p>
	 * Returns a composed logout success handler that invokes the specified handlers in sequence.
	 * </p>
	 *
	 * @param <A> the authentication type
	 * @param <B> the identity type
	 * @param <C> the access controller type
	 * @param <D> the security context type
	 * @param <E> the exchange type
	 * @param handlers the list of handlers to invoke in sequence
	 *
	 * @return a composed logout success handler that invokes the specified handlers in sequence
	 */
	@SafeVarargs
    @SuppressWarnings("varargs")
	static <A extends Authentication, B extends Identity, C extends AccessController, D extends SecurityContext<B, C>, E extends Exchange<D>> LogoutSuccessHandler<A, B, C, D, E> of(LogoutSuccessHandler<? super A, ? super B, ? super C,  ? super D, ? super E>... handlers) {
		return (exchange, authentication) -> {
			Mono<Void> handlerChain = null;
			for(LogoutSuccessHandler<? super A, ? super B, ? super C,  ? super D, ? super E> handler : handlers) {
				if(handlerChain == null) {
					handlerChain = handler.handleLogoutSuccess(exchange, authentication);
				}
				else {
					handlerChain = handlerChain.thenEmpty(handler.handleLogoutSuccess(exchange, authentication));
				}
			}
			return handlerChain;
		};
	}
}
