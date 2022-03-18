/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.form;

import io.inverno.mod.security.http.LoginSuccessHandler;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.Authentication;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class RedirectLoginSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> implements LoginSuccessHandler<A, B, C> {

	public static final String DEFAULT_LOGIN_SUCCESS_URI = "/";
	
	private final String loginSuccessUri;

	public RedirectLoginSuccessHandler() {
		this(DEFAULT_LOGIN_SUCCESS_URI);
	}
	
	public RedirectLoginSuccessHandler(String loginSuccessUri) {
		this.loginSuccessUri = loginSuccessUri;
	}

	public String getLoginSuccessUri() {
		return loginSuccessUri;
	}

	@Override
	public Mono<Void> handleLoginSuccess(C exchange, A authentication) {
		return exchange.request().body().get().urlEncoded().collectMap()
			.flatMap(parameterMap -> { 
				exchange.response()
					.headers(headers -> headers
						.status(Status.FOUND)
						.set(Headers.NAME_LOCATION, Optional.ofNullable(parameterMap.get(FormLoginPageHandler.PARAMETER_REDIRECT_URI)).map(Parameter::asString).orElse(this.loginSuccessUri))
					)
					.body().empty();
				
				return Mono.empty();
			});
	}
}
