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
package io.inverno.mod.security.http.form;

import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.login.LogoutSuccessHandler;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A logout success handler implementation that redirects the client (302) after a successful logout.
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
public class RedirectLogoutSuccessHandler<A extends Authentication, B extends Identity, C extends AccessController, D extends SecurityContext<B, C>, E extends Exchange<D>> implements LogoutSuccessHandler<A, B, C, D, E>{

	/**
	 * The default logout success URI: {@code /}.
	 */
	public static final String DEFAULT_LOGOUT_SUCCESS_URI = "/";
	
	/**
	 * The URI where the client is redirected after a successful logout.
	 */
	private final String logoutSuccessUri;

	/**
	 * <p>
	 * Creates a redirect logout success handler which redirects the client to the default logout success URI.
	 * </p>
	 */
	public RedirectLogoutSuccessHandler() {
		this(DEFAULT_LOGOUT_SUCCESS_URI);
	}
	
	/**
	 * <p>
	 * Creates a redirect logout success handler which redirects the client to the specified logout success URI.
	 * </p>
	 * 
	 * @param loginSuccessUri the URI where to redirect the client after a successful logout
	 */
	public RedirectLogoutSuccessHandler(String loginSuccessUri) {
		this.logoutSuccessUri = loginSuccessUri;
	}

	/**
	 * <p>
	 * Returns the URI where the client is redirected after a successful logout.
	 * </p>
	 * 
	 * @return the logout success URI
	 */
	public String getLogoutSuccessUri() {
		return logoutSuccessUri;
	}
	
	@Override
	public Mono<Void> handleLogoutSuccess(E exchange, A authentication) {
		return Mono.fromRunnable(() -> exchange.response()
			.headers(headers -> headers
				.status(Status.FOUND)
				.set(Headers.NAME_LOCATION, this.logoutSuccessUri)
			)
			.body().empty()
		);
	}
}
