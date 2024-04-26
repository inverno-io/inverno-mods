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
import io.inverno.mod.security.authentication.TokenCredentials;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A credentials extractor that extracts a token credentials stored in an HTTP cookie.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class CookieTokenCredentialsExtractor implements CredentialsExtractor<TokenCredentials> {

	/**
	 * The default token cookie name: {@code AUTH-TOKEN}.
	 * 
	 * <p>
	 * This constant is also used in {@link CookieTokenLoginSuccessHandler} and {@link CookieTokenLogoutSuccessHandler}.
	 * </p>
	 */
	public static final String DEFAULT_COOKIE_NAME = "AUTH-TOKEN";
	
	/**
	 * The token cookie name.
	 */
	private final String tokenCookie;
	
	/**
	 * <p>
	 * Creates a cookie token credentials extractor with the default token cookie name.
	 * </p>
	 */
	public CookieTokenCredentialsExtractor() {
		this(DEFAULT_COOKIE_NAME);
	}
	
	/**
	 * <p>
	 * Creates a cookie token credentials extractor with the specified token cookie name.
	 * </p>
	 * 
	 * @param tokenCookie the token cookie name
	 */
	public CookieTokenCredentialsExtractor(String tokenCookie) {
		this.tokenCookie = tokenCookie;
	}

	/**
	 * <p>
	 * Returns the token credentials cookie name.
	 * </p>
	 * 
	 * @return the token cookie name
	 */
	public String getTokenCookie() {
		return tokenCookie;
	}
	
	@Override
	public Mono<TokenCredentials> extract(Exchange<?> exchange) throws MalformedCredentialsException {
		return Mono.fromSupplier(() -> exchange.request().headers().cookies()
			.get(this.tokenCookie)
			.map(cookie -> new TokenCredentials(cookie.asString()))
			.orElse(null)
		);
	}
}
