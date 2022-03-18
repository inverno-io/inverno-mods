/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.form;

import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.Authentication;
import io.inverno.mod.security.http.LogoutSuccessHandler;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class RedirectLogoutSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> implements LogoutSuccessHandler<A, B, C>{

	public static final String DEFAULT_LOGOUT_SUCCESS_URI = "/";
	
	private final String logoutSuccessUri;

	public RedirectLogoutSuccessHandler() {
		this(DEFAULT_LOGOUT_SUCCESS_URI);
	}
	
	public RedirectLogoutSuccessHandler(String loginSuccessUri) {
		this.logoutSuccessUri = loginSuccessUri;
	}

	public String getLogoutSuccessUri() {
		return logoutSuccessUri;
	}
	
	@Override
	public Mono<Void> handleLogoutSuccess(C exchange, A authentication) {
		return Mono.fromRunnable(() -> {
			exchange.response()
				.headers(headers -> headers
					.status(Status.FOUND)
					.set(Headers.NAME_LOCATION, this.logoutSuccessUri)
				)
				.body().empty();
		});
	}
}
