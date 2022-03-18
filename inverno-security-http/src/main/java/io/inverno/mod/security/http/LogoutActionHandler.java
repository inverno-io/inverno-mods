/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.security.Authentication;
import io.inverno.mod.security.AuthenticationReleasor;
import io.inverno.mod.security.http.context.SecurityContext;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class LogoutActionHandler<A extends Authentication, B extends SecurityContext, C extends Exchange<B>> implements ExchangeHandler<B, C> {

	private final AuthenticationReleasor<A> authenticationReleasor;
	
	private final LogoutSuccessHandler<A, B, C> logoutSuccessHandler;
	
	public LogoutActionHandler(AuthenticationReleasor<A> authenticationReleasor) {
		this(authenticationReleasor, null);
	}
	
	public LogoutActionHandler(AuthenticationReleasor<A> authenticationReleasor, LogoutSuccessHandler<A, B, C> logoutSuccessHandler) {
		this.authenticationReleasor = authenticationReleasor;
		this.logoutSuccessHandler = logoutSuccessHandler;
	}

	public AuthenticationReleasor<A> getAuthenticationReleasor() {
		return authenticationReleasor;
	}

	public LogoutSuccessHandler<A, B, C> getLogoutSuccessHandler() {
		return logoutSuccessHandler;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> defer(C exchange) {
		Authentication authentication = exchange.context().getAuthentication();
		/* 
		 * We can have ClassCastException here but at some point we can't make everything generic, this risk should be limited assuming everything is 
		 * properly configured. Since everything related to configuration is generic, it should be ok and if not, the error will be raised at runtime 
		 * instead of compile time.
		 */
		Mono<Void> result = this.authenticationReleasor.release((A)authentication);
		if(this.logoutSuccessHandler != null) {
			result = result.then(this.logoutSuccessHandler.handleLogoutSuccess(exchange, (A)authentication));
		}
		return result;
	}
	
	@Override
	public void handle(C exchange) throws HttpException {
		throw new UnsupportedOperationException();
	}
}
