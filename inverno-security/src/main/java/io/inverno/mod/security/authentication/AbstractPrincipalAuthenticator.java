/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.security.authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base principal authenticator implementation used to authenticate {@link PrincipalCredentials}.
 * </p>
 * 
 * <p>
 * This implementation relies on a {@link CredentialsResolver} to resolve trusted credentials from a trusted source (i.e. a trusted repository, a trusted directory service...) and a
 * {@link CredentialsMatcher} to match the provided credentials with the resolved trusted credentials.
 * </p>
 *
 * <p>
 * Implementors must implement the {@link #createAuthenticated(io.inverno.mod.security.authentication.Credentials) } and
 * {@link #createDenied(io.inverno.mod.security.authentication.PrincipalCredentials, io.inverno.mod.security.authentication.AuthenticationException) } methods which creates the resulting
 * authentication in case of successful or failed authentication.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of principal credentials to authenticate
 * @param <B> the type of credentials resolved by the credentials resolver
 * @param <C> the type of authentication
 */
public abstract class AbstractPrincipalAuthenticator<A extends PrincipalCredentials, B extends Credentials, C extends PrincipalAuthentication> implements Authenticator<A, C> {

	private static final Logger LOGGER = LogManager.getLogger(AbstractPrincipalAuthenticator.class);
	
	/**
	 * The credentials' resolver.
	 */
	protected final CredentialsResolver<? extends B> credentialsResolver;
	
	/**
	 * The credentials' matcher.
	 */
	protected final CredentialsMatcher<? super A, ? super B> credentialsMatcher;
	
	/**
	 * Indicates whether an empty Mono should be returned on {@link AuthenticationException}.
	 */
	private boolean terminal;
	
	/**
	 * <p>
	 * Creates a terminal principal authenticator with the specified credentials resolver and credentials matcher.
	 * </p>
	 * 
	 * <p>
	 * The resulting authenticator is terminal and returns denied authentication when the credentials resolver returns no matching credentials corresponding to the credentials to authenticate or when
	 * they do not match.
	 * </p>
	 * 
	 * @param credentialsResolver a credentials resolver
	 * @param credentialsMatcher  a credentials matcher
	 */
	protected AbstractPrincipalAuthenticator(CredentialsResolver<? extends B> credentialsResolver, CredentialsMatcher<? super A, ? super B> credentialsMatcher) {
		this.credentialsResolver = credentialsResolver;
		this.credentialsMatcher = credentialsMatcher;
		this.terminal = true;
	}

	/**
	 * <p>
	 * Sets whether the authenticator is terminal and should return denied authentication on failed authentication or no authentication to indicate it was not able to authenticate credentials.
	 * </p>
	 * 
	 * @param terminal true to terminate authentication, false otherwise
	 */
	public void setTerminal(boolean terminal) {
		this.terminal = terminal;
	}
	
	@Override
	public Mono<C> authenticate(A credentials) {
		return this.credentialsResolver
			.resolveCredentials(credentials.getUsername())
			.switchIfEmpty(Mono.error(() -> new CredentialsNotFoundException("Credentials not found")))
			.map(resolvedCredentials -> {
				if(!this.credentialsMatcher.matches(credentials, resolvedCredentials)) {
					throw new InvalidCredentialsException("Invalid credentials");
				}
				return this.createAuthenticated(resolvedCredentials);
			})
			.onErrorResume(AuthenticationException.class, e -> {
				if(!this.terminal) {
					LOGGER.error("Failed to authenticate", e);
					return Mono.empty();
				}
				return Mono.just(this.createDenied(credentials, e));
			});
	}
	
	/**
	 * <p>
	 * Creates an authenticated authentication resulting from a successful authentication using the resolved trusted credentials.
	 * </p>
	 * 
	 * @param resolvedCredentials the resolved trusted credentials
	 * 
	 * @return an authenticated authentication
	 * @throws AuthenticationException if there was an error generating the authentication
	 */
	protected abstract C createAuthenticated(B resolvedCredentials) throws AuthenticationException;
	
	/**
	 * <p>
	 * Creates a denied authentication resulting from a failed authentication.
	 * </p>
	 * 
	 * @param credentials the invalid credentials
	 * @param cause       the authentication error
	 * 
	 * @return a denied authentication
	 * @throws AuthenticationException if there was an error generating the authentication
	 */
	protected abstract C createDenied(A credentials, AuthenticationException cause) throws AuthenticationException;
}
