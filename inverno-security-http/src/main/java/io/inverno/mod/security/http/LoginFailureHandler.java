/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.security.http;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public interface LoginFailureHandler<A extends ExchangeContext, B extends Exchange<A>> {

	Mono<Void> handleLoginFailure(B exchange, io.inverno.mod.security.SecurityException error);
	
	default LoginFailureHandler<A, B> andThen(LoginFailureHandler<? super A, ? super B> after) {
		return (exchange, authentication) -> {
			return this.handleLoginFailure(exchange, authentication).then(after.handleLoginFailure(exchange, authentication));
		};
	}
	
	default LoginFailureHandler<A, B> compose(LoginFailureHandler<? super A, ? super B> before) {
		return (exchange, authentication) -> {
			return before.handleLoginFailure(exchange, authentication).then(this.handleLoginFailure(exchange, authentication));
		};
	}
}
