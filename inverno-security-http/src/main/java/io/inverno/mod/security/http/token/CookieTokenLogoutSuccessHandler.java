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
package io.inverno.mod.security.http.token;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.login.LogoutSuccessHandler;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A logout success handler that removes the token cookie.
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
public class CookieTokenLogoutSuccessHandler<A extends Authentication, B extends Identity, C extends AccessController, D extends SecurityContext<B, C>, E extends Exchange<D>> implements LogoutSuccessHandler<A, B, C, D, E> {

	/**
	 * The token cookie path.
	 */
	private final String path;
	
	/**
	 * The token cookie name.
	 */
	private final String tokenCookie;
	
	/**
	 * <p>
	 * Creates a cookie token logout success handler with default path and token cookie name.
	 * </p>
	 */
	public CookieTokenLogoutSuccessHandler() {
		this(CookieTokenLoginSuccessHandler.DEFAULT_PATH, CookieTokenCredentialsExtractor.DEFAULT_COOKIE_NAME);
	}
	
	/**
	 * <p>
	 * Creates a cookie token logout success handler with the specified path and the default token cookie name.
	 * </p>
	 * 
	 * @param path the token cookie path
	 */
	public CookieTokenLogoutSuccessHandler(String path) {
		this(path, CookieTokenCredentialsExtractor.DEFAULT_COOKIE_NAME);
	}
	
	/**
	 * <p>
	 * Creates a cookie token logout success handler with specified path and token cookie name.
	 * </p>
	 *
	 * @param path        the token cookie path
	 * @param tokenCookie the token cookie name
	 */
	public CookieTokenLogoutSuccessHandler(String path, String tokenCookie) {
		this.path = path;
		this.tokenCookie = tokenCookie;
	}

	/**
	 * <p>
	 * Returns the token cookie path.
	 * </p>
	 * 
	 * @return the token cookie path
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * <p>
	 * Returns the token cookie name.
	 * </p>
	 * 
	 * @return the token cookie name
	 */
	public String getTokenCookie() {
		return tokenCookie;
	}

	@Override
	public Mono<Void> handleLogoutSuccess(E exchange, A authentication) {
		return Mono.fromRunnable(() -> exchange.response().cookies(cookies -> cookies
			.addCookie(cookie -> cookie
				.path(this.path)
				.name(this.tokenCookie)
				.value("")
				.maxAge(0)
			)
		));
	}
}
