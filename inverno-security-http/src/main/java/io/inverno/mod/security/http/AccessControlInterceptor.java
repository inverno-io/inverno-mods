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
package io.inverno.mod.security.http;

import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import java.util.function.Predicate;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An access control interceptor verifies that the access to a resource is authorized.
 * </p>
 *
 * <p>
 * This interceptor must be executed after the {@link SecurityInterceptor} once the {@link SecurityContext} has been created. It is basically used to verify that the requester has access to
 * the resource being intercepted based on the security context and more specifically the authentication, the identity and the access controller.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 * @param <B> the access controller type
 * @param <C> the exchange type
 */
public class AccessControlInterceptor<A extends Identity, B extends AccessController, C extends Exchange<SecurityContext<A, B>>> implements ExchangeInterceptor<SecurityContext<A, B>, C> {

	/**
	 * The access verifier.
	 */
	private final Predicate<SecurityContext<A, B>> accessVerifier;
	
	/**
	 * <p>
	 * Creates an access control interceptor with the specified access verifier.
	 * </p>
	 * 
	 * @param accessVerifier an access verifier
	 */
	private AccessControlInterceptor(Predicate<SecurityContext<A, B>> accessVerifier) {
		this.accessVerifier = accessVerifier;
	}
	
	/**
	 * <p>
	 * Returns an access control interceptor that verifies the requester is anonymous (i.e. not authenticated).
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param <B> the access controller type
	 * @param <C> the exchange type
	 * 
	 * @return an access control interceptor
	 */
	public static <A extends Identity, B extends AccessController, C extends Exchange<SecurityContext<A, B>>> AccessControlInterceptor<A, B, C> anonymous() {
		return new AccessControlInterceptor<>(context -> {
			Authentication authentication = context.getAuthentication();
			return authentication.isAuthenticated();
		});
	}
	
	/**
	 * <p>
	 * Returns an access control interceptor that verifies the requester is authenticated.
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param <B> the access controller type
	 * @param <C> the exchange type
	 * 
	 * @return an access control interceptor
	 */
	public static <A extends Identity, B extends AccessController, C extends Exchange<SecurityContext<A, B>>> AccessControlInterceptor<A, B, C> authenticated() {
		return new AccessControlInterceptor<>(context -> {
			Authentication authentication = context.getAuthentication();
			if(!authentication.isAuthenticated()) {
				authentication.getCause()
					.map(UnauthorizedException::new)
					.ifPresent(e -> {
						throw e;
					});
				return false;
			}
			return true;
		});
	}
	
	/**
	 * <p>
	 * Returns an access control interceptor that uses the specified access verifier to verify access.
	 * </p>
	 * 
	 * <p>
	 * The access verifier shall return false to deny the access to the resource resulting in a {@link UnauthorizedException} being thrown by the interceptor but it can also throw a
	 * {@link UnauthorizedException} directly to provide more details about the error (e.g. a message).
	 * </p>
	 * 
	 * @param <A>            the identity type
	 * @param <B>            the access controller type
	 * @param <C>            the exchange type
	 * @param accessVerifier an access verifier
	 * 
	 * @return an access control interceptor
	 */
	public static <A extends Identity, B extends AccessController, C extends Exchange<SecurityContext<A, B>>> AccessControlInterceptor<A, B, C> verify(Predicate<SecurityContext<A, B>> accessVerifier) {
		return new AccessControlInterceptor<>(accessVerifier);
	}
	
	@Override
	public Mono<? extends C> intercept(C exchange) {
		if(!this.accessVerifier.test(exchange.context())) {
			throw new UnauthorizedException("Access denied");
		}
		return Mono.just(exchange);
	}
}
