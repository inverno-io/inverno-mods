/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.form;

import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.http.AuthenticationErrorInterceptor;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class FormAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends AuthenticationErrorInterceptor<A, B> {

	public static final String DEFAULT_LOGIN_PAGE_URI = "/login";
	
	private final String loginUri;
	
	public FormAuthenticationErrorInterceptor() {
		this(DEFAULT_LOGIN_PAGE_URI);
	}

	public FormAuthenticationErrorInterceptor(String loginUri) {
		super(true);
		this.loginUri = loginUri;
	}

	public String getLoginUri() {
		return loginUri;
	}

	@Override
	public Mono<? extends B> intercept(B exchange) {
		if(exchange.request().getMethod().equals(Method.GET)) {
			return super.intercept(exchange);
		}
		return Mono.just(exchange);
	}

	@Override
	protected void interceptUnauthorized(B exchange) throws HttpException {
		exchange.response()
			.headers(headers -> headers
				.status(Status.FOUND)
				.set(Headers.NAME_LOCATION, URIs.uri(this.loginUri).queryParameter(FormLoginPageHandler.PARAMETER_REDIRECT_URI, exchange.request().getPath()).buildString())
			);
	}
}
