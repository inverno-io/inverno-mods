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
package io.inverno.mod.security.http.token;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.authentication.TokenAuthentication;
import io.inverno.mod.security.http.LoginSuccessHandler;
import reactor.core.publisher.Mono;

/**
 * <p>
 * The secure flag is set when the server is configured with TLS (HTTPS). An application deployed behind an SSL offloader is likely to be configured without TLS and as a result it is up to the SSL
 * offloader to set the secure flag on the authentication token cookie.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class CookieTokenLoginSuccessHandler<A extends TokenAuthentication, B extends ExchangeContext, C extends Exchange<B>> implements LoginSuccessHandler<A, B, C> {

	public static final String DEFAULT_COOKIE_NAME = "auth-token";
	
	public static final String DEFAULT_PATH = "/";
	
	private final String path;
	private final String cookieName;
	
	public CookieTokenLoginSuccessHandler() {
		this(DEFAULT_PATH, DEFAULT_COOKIE_NAME);
	}
	
	public CookieTokenLoginSuccessHandler(String path) {
		this(path, DEFAULT_COOKIE_NAME);
	}
	
	public CookieTokenLoginSuccessHandler(String path, String cookieName) {
		this.path = path;
		this.cookieName = cookieName;
	}

	public String getCookieName() {
		return cookieName;
	}
	
	@Override
	public Mono<Void> handleLoginSuccess(C exchange, A authentication) {
		return Mono.fromRunnable(() -> exchange.response().cookies(cookies -> cookies
			.addCookie(cookie -> cookie
				// TODO should be secured when we are in https...
				// We can determine this: exchange.request().getScheme()
				// But we can also be behind an ssl offloader
				.path(this.path)
				.secure("https".equals(exchange.request().getScheme()))
				.httpOnly(true)
				.name(DEFAULT_COOKIE_NAME)
				.value(authentication.getToken())
			)));
	}
}
