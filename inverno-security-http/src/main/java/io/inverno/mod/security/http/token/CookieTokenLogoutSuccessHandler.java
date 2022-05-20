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
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.LogoutSuccessHandler;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class CookieTokenLogoutSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> implements LogoutSuccessHandler<A, B, C> {

	public static final String DEFAULT_TOKEN_COOKIE = "auth-token";
	
	public static final String DEFAULT_PATH = "/";
	
	private final String path;
	private final String cookieName;
	
	public CookieTokenLogoutSuccessHandler() {
		this(DEFAULT_PATH, DEFAULT_TOKEN_COOKIE);
	}
	
	public CookieTokenLogoutSuccessHandler(String path) {
		this(path, DEFAULT_TOKEN_COOKIE);
	}
	
	public CookieTokenLogoutSuccessHandler(String path, String cookieName) {
		this.path = path;
		this.cookieName = cookieName;
	}

	public String getCookieName() {
		return cookieName;
	}

	@Override
	public Mono<Void> handleLogoutSuccess(C exchange, A authentication) {
		return Mono.fromRunnable(() -> exchange.response().cookies(cookies -> cookies
			.addCookie(cookie -> cookie
				.path(this.path)
				.name(this.cookieName)
				.value("")
				.maxAge(0)
			)
		));
	}
}
