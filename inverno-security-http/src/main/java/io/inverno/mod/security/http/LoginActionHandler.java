/*
 * Copyright 2022 Jeremy Kuhn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.security.http;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.UserCredentials;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
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
