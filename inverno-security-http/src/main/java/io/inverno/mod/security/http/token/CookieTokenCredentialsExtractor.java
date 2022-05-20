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
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class CookieTokenCredentialsExtractor implements CredentialsExtractor<TokenCredentials> {

	public static final String DEFAULT_COOKIE_NAME = "auth-token";
	
	private final String cookieName;
	
	public CookieTokenCredentialsExtractor() {
		this(DEFAULT_COOKIE_NAME);
	}
	
	public CookieTokenCredentialsExtractor(String cookieName) {
		this.cookieName = cookieName;
	}

	public String getAuthTokenCookie() {
		return cookieName;
	}
	
	@Override
	public Mono<TokenCredentials> extract(Exchange<?> exchange) throws MalformedCredentialsException {
		return Mono.fromSupplier(() -> exchange.request().cookies()
			.get(this.cookieName)
			.map(cookie -> new TokenCredentials(cookie.asString()))
			.orElse(null)
		);
	}
}
