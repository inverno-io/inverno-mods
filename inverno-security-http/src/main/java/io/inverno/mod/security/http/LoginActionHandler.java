/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.security.Authentication;
import io.inverno.mod.security.AuthenticationException;
import io.inverno.mod.security.Authenticator;
import io.inverno.mod.security.UserCredentials;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class LoginActionHandler<A extends UserCredentials, B extends Authentication, C extends ExchangeContext, D extends Exchange<C>> implements ExchangeHandler<C, D> {
	
	private final CredentialsExtractor<A> credentialsExtractor;
	
	private final Authenticator<A, B> authenticator;
	
	private final LoginSuccessHandler<B, C, D> loginSuccessHandler;
	
	private final LoginFailureHandler<C, D> loginFailureHandler;
	
	public LoginActionHandler(CredentialsExtractor<A> credentialsExtractor, Authenticator<A, B> authenticator) {
		this(credentialsExtractor, authenticator, null, null);
	}
	
	public LoginActionHandler(CredentialsExtractor<A> credentialsExtractor, Authenticator<A, B> authenticator, LoginSuccessHandler<B, C, D> loginSuccessHandler, LoginFailureHandler<C, D> loginFailureHandler) {
		this.credentialsExtractor = credentialsExtractor;
		this.authenticator = authenticator;
		this.loginSuccessHandler = loginSuccessHandler;
		this.loginFailureHandler = loginFailureHandler;
	}

	public CredentialsExtractor<A> getCredentialsExtractor() {
		return credentialsExtractor;
	}

	public Authenticator<A, B> getAuthenticator() {
		return authenticator;
	}

	public LoginSuccessHandler<B, C, D> getLoginSuccessHandler() {
		return loginSuccessHandler;
	}

	public LoginFailureHandler<C, D> getLoginFailureHandler() {
		return loginFailureHandler;
	}
	
	@Override
	public Mono<Void> defer(D exchange) {
		// 1. Extract credentials
		Mono<Void> result = this.credentialsExtractor.extract(exchange)
			// 2. Authenticate
			.flatMap(this.authenticator::authenticate)
			// 3. Handle login success and propagate login errors
			.flatMap(authentication -> {
				if(authentication.isAuthenticated()) {
					if(this.loginSuccessHandler != null) {
						return this.loginSuccessHandler.handleLoginSuccess(exchange, authentication);
					}
				}
				// not authenticated let's propagate the error if any, otherwise, throw an AuthenticationException
				if(authentication.getCause().isPresent()) {
					throw authentication.getCause().get();
				}
				else {
					throw new AuthenticationException();
				}
			});
		
		// 4. Handle login failure
		if(this.loginFailureHandler != null) {
			result = result.onErrorResume(io.inverno.mod.security.SecurityException.class, error -> this.loginFailureHandler.handleLoginFailure(exchange, error));
		}
		return result;
	}

	@Override
	public void handle(D exchange) throws HttpException {
		throw new UnsupportedOperationException();
	}
}
