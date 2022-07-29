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

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationReleaser;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An exchange handler that logs out a logged in entity and delegates further processing to a success handler.
 * </p>
 * 
 * <p>
 * A logout action handler is used whenever there is a need to explicitly invalidate an authentication (e.g. invalidate a token credentials) or free resources locked by that authentication (e.g.
 * remove a session). It uses an {@link AuthenticationReleaser} to release the authentication and a {@link LogoutSuccessHandler} to handle successful logouts.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the authentication type
 * @param <B> the identity type
 * @param <C> the access controller type
 * @param <D> the security context type
 * @param <E> the exchange type
 */
public class LogoutActionHandler<A extends Authentication, B extends Identity, C extends AccessController, D extends SecurityContext<B, C>, E extends Exchange<D>> implements ExchangeHandler<D, E> {

	/**
	 * The authentication releaser used to release an authentication.
	 */
	private final AuthenticationReleaser<A> authenticationReleaser;
	
	/**
	 * The logout success handler invoked after a successful logout.
	 */
	private final LogoutSuccessHandler<A, B, C, D, E> logoutSuccessHandler;
	
	/**
	 * <p>
	 * Creates a logout action handler with the specified authentication releaser.
	 * </p>
	 * 
	 * @param authenticationReleaser an authentication releaser
	 */
	public LogoutActionHandler(AuthenticationReleaser<A> authenticationReleaser) {
		this(authenticationReleaser, null);
	}
	
	/**
	 * <p>
	 * Creates a logout action handler with the specified authentication releaser and logout success handler.
	 * </p>
	 *
	 * @param authenticationReleaser an authentication releaser
	 * @param logoutSuccessHandler   a logout success handler
	 */
	public LogoutActionHandler(AuthenticationReleaser<A> authenticationReleaser, LogoutSuccessHandler<A, B, C, D, E> logoutSuccessHandler) {
		this.authenticationReleaser = authenticationReleaser;
		this.logoutSuccessHandler = logoutSuccessHandler;
	}

	/**
	 * <p>
	 * Returns the authentication releaser.
	 * </p>
	 * 
	 * @return the authentication releaser
	 */
	public AuthenticationReleaser<A> getAuthenticationReleaser() {
		return authenticationReleaser;
	}

	/**
	 * <p>
	 * Returns the logout success handler.
	 * </p>
	 * 
	 * @return the logout success handler
	 */
	public LogoutSuccessHandler<A, B, C, D, E> getLogoutSuccessHandler() {
		return logoutSuccessHandler;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> defer(E exchange) {
		Authentication authentication = exchange.context().getAuthentication();
		/* 
		 * We can have ClassCastException here but at some point we can't make everything generic, this risk should be limited assuming everything is 
		 * properly configured. Since everything related to configuration is generic, it should be ok and if not, the error will be raised at runtime 
		 * instead of compile time.
		 */
		Mono<Void> result = this.authenticationReleaser.release((A)authentication);
		if(this.logoutSuccessHandler != null) {
			result = result.then(this.logoutSuccessHandler.handleLogoutSuccess(exchange, (A)authentication));
		}
		return result;
	}
	
	@Override
	public void handle(E exchange) throws HttpException {
		throw new UnsupportedOperationException();
	}
}
