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
public interface LogoutSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> {

	Mono<Void> handleLogoutSuccess(C exchange, A authentication);
	
	default LogoutSuccessHandler<A, B, C> andThen(LogoutSuccessHandler<? super A, ? super B, ?super C> after) {
		return (exchange, authentication) -> {
			return this.handleLogoutSuccess(exchange, authentication).then(after.handleLogoutSuccess(exchange, authentication));
		};
	}
	
	default LogoutSuccessHandler<A, B, C> compose(LogoutSuccessHandler<? super A, ? super B, ?super C> before) {
		return (exchange, authentication) -> {
			return before.handleLogoutSuccess(exchange, authentication).then(this.handleLogoutSuccess(exchange, authentication));
		};
	}
}
