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
import io.inverno.mod.security.http.login.LoginActionHandler;
import io.inverno.mod.security.http.login.LoginSuccessHandler;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A login success handler that sets a token cookie in the response using the token value specified in the token authentication resulting from the login authentication.
 * </p>
 * 
 * <p>
 * The secure flag is set on the token cookie when the server is configured with TLS (HTTPS). An application deployed behind an SSL offloader is likely to be configured without TLS and as a result it
 * is up to the SSL offloader to set the secure flag on the authentication token cookie.
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
public class CookieTokenLoginSuccessHandler<A extends TokenAuthentication, B extends ExchangeContext, C extends Exchange<B>> implements LoginSuccessHandler<A, B, C> {

	/**
	 * The default token cookie path: {@code /}.
	 * 
	 * <p>
	 * This constant is also used in {@link CookieTokenLogoutSuccessHandler}.
	 * </p>
	 */
	public static final String DEFAULT_PATH = "/";
	
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
	 * Creates a cookie token login success handler with default path and token cookie name.
	 * </p>
	 */
	public CookieTokenLoginSuccessHandler() {
		this(DEFAULT_PATH, CookieTokenCredentialsExtractor.DEFAULT_COOKIE_NAME);
	}
	
	/**
	 * <p>
	 * Creates a cookie token login success handler with the specified path and the default token cookie name.
	 * </p>
	 * 
	 * @param path the token cookie path
	 */
	public CookieTokenLoginSuccessHandler(String path) {
		this(path, CookieTokenCredentialsExtractor.DEFAULT_COOKIE_NAME);
	}
	
	/**
	 * <p>
	 * Creates a cookie token login success handler with specified path and token cookie name.
	 * </p>
	 *
	 * @param path        the token cookie path
	 * @param tokenCookie the token cookie name
	 */
	public CookieTokenLoginSuccessHandler(String path, String tokenCookie) {
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
	public Mono<Void> handleLoginSuccess(C exchange, A authentication) {
		return Mono.fromRunnable(() -> exchange.response().cookies(cookies -> cookies
			.addCookie(cookie -> cookie
				// TODO should be secured when we are in https...
				// We can determine this: exchange.request().getScheme()
				// But we can also be behind an ssl offloader
				.path(this.path)
				.secure("https".equals(exchange.request().getScheme()))
				.httpOnly(true)
				.name(this.tokenCookie)
				.value(authentication.getToken())
			)));
	}
}
