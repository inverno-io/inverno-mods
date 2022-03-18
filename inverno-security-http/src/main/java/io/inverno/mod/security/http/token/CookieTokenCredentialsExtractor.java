/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.token;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.TokenCredentials;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class CookieTokenCredentialsExtractor implements CredentialsExtractor<TokenCredentials> {

	private static final String DEFAULT_TOKEN_COOKIE = "auth-token";
	
	private final String tokenCookie;
	
	public CookieTokenCredentialsExtractor() {
		this(DEFAULT_TOKEN_COOKIE);
	}
	
	public CookieTokenCredentialsExtractor(String tokenCookie) {
		this.tokenCookie = tokenCookie;
	}

	public String getAuthTokenCookie() {
		return tokenCookie;
	}
	
	@Override
	public Mono<TokenCredentials> extract(Exchange<?> exchange) throws MalformedCredentialsException {
		return Mono.fromSupplier(() -> exchange.request().cookies()
			.get(this.tokenCookie)
			.map(cookie -> new TokenCredentials(cookie.asString()))
			.orElse(null)
		);
	}
}
