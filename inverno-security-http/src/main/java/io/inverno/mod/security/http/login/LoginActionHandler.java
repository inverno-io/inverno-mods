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
package io.inverno.mod.security.http.login;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.http.CredentialsExtractor;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An exchange handler that authenticates login credentials and delegates further processing to success and failure handlers.
 * </p>
 * 
 * <p>
 * A login action handler is typically used in a form login authentication to authenticate the credentials sent by a user in a POST request.
 * </p>
 * 
 * <p>
 * It relies on a {@link CredentialsExtractor} to extract login credentials from the request, an {@link Authenticator} to authenticate them, a {@link LoginSuccessHandler} and a
 * {@link LoginFailureHandler} to respectively handle successful authentications and failed authentications.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the login credentials type
 * @param <B> the authentication type
 * @param <C> the context type
 * @param <D> the exchange type
 */
public class LoginActionHandler<A extends LoginCredentials, B extends Authentication, C extends ExchangeContext, D extends Exchange<C>> implements ExchangeHandler<C, D> {
	
	/**
	 * The credentials' extractor.
	 */
	private final CredentialsExtractor<A> credentialsExtractor;
	
	/**
	 * The authenticator.
	 */
	private final Authenticator<A, B> authenticator;
	
	/**
	 * The login success handler.
	 */
	private final LoginSuccessHandler<B, C, D> loginSuccessHandler;
	
	/**
	 * The login failure handler.
	 */
	private final LoginFailureHandler<C, D> loginFailureHandler;
	
	/**
	 * <p>
	 * Creates a login action handler with the specified credentials extractor and authenticator.
	 * </p>
	 *
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 */
	public LoginActionHandler(CredentialsExtractor<A> credentialsExtractor, Authenticator<A, B> authenticator) {
		this(credentialsExtractor, authenticator, null, null);
	}
	
	/**
	 * <p>
	 * Creates a login action handler with the specified credentials extractor, authenticator, login success handler and login failure handler.
	 * </p>
	 *
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 * @param loginSuccessHandler  a login success handler
	 * @param loginFailureHandler  a login failure handler
	 */
	public LoginActionHandler(CredentialsExtractor<A> credentialsExtractor, Authenticator<A, B> authenticator, LoginSuccessHandler<B, C, D> loginSuccessHandler, LoginFailureHandler<C, D> loginFailureHandler) {
		this.credentialsExtractor = credentialsExtractor;
		this.authenticator = authenticator;
		this.loginSuccessHandler = loginSuccessHandler;
		this.loginFailureHandler = loginFailureHandler;
	}

	/**
	 * <p>
	 * Returns the credentials extractor.
	 * </p>
	 * 
	 * @return the credentials extractor
	 */
	public CredentialsExtractor<A> getCredentialsExtractor() {
		return credentialsExtractor;
	}

	/**
	 * <p>
	 * Returns the authenticator.
	 * </p>
	 * 
	 * @return the authenticator
	 */
	public Authenticator<A, B> getAuthenticator() {
		return authenticator;
	}

	/**
	 * <p>
	 * Returns the login success handler.
	 * </p>
	 * 
	 * @return the login success handler
	 */
	public LoginSuccessHandler<B, C, D> getLoginSuccessHandler() {
		return loginSuccessHandler;
	}

	/**
	 * <p>
	 * Returns the login failure handler.
	 * </p>
	 * 
	 * @return the login failure handler
	 */
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
					else {
						// Return an empty response
						return Mono.fromRunnable(() -> exchange.response().body().empty());
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
