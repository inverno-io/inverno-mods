/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;

/**
 *
 * @author jkuhn
 */
public abstract class HttpAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends AuthenticationErrorInterceptor<A, B> {

	@Override
	protected void interceptUnauthorized(B exchange) throws HttpException {
		final String challenge;
		if(exchange.getError().getCause() != null && exchange.getError().getCause() instanceof io.inverno.mod.security.SecurityException) {
			challenge = this.createChallenge((io.inverno.mod.security.SecurityException)exchange.getError().getCause());
		}
		else {
			challenge = this.createChallenge(null);
		}
		
		exchange.response()
			.headers(headers -> headers
				.status(Status.UNAUTHORIZED)
				.add(Headers.NAME_WWW_AUTHENTICATE, challenge)
			)
			.body().empty();
	}
	
	protected abstract String createChallenge(io.inverno.mod.security.SecurityException cause);
	
}
