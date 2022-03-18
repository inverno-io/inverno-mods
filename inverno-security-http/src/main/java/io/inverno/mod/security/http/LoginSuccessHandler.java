/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.security.http;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.Authentication;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
@FunctionalInterface
public interface LoginSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> {

	Mono<Void> handleLoginSuccess(C exchange, A authentication);

	default LoginSuccessHandler<A, B, C> andThen(LoginSuccessHandler<? super A, ? super B, ?super C> after) {
		return (exchange, authentication) -> {
			return this.handleLoginSuccess(exchange, authentication).then(after.handleLoginSuccess(exchange, authentication));
		};
	}
	
	default LoginSuccessHandler<A, B, C> compose(LoginSuccessHandler<? super A, ? super B, ?super C> before) {
		return (exchange, authentication) -> {
			return before.handleLoginSuccess(exchange, authentication).then(this.handleLoginSuccess(exchange, authentication));
		};
	}
}
