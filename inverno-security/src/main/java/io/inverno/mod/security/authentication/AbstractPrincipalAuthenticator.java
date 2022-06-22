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

import java.util.Objects;
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
 * Implementators must implement the {@link #createAuthentication(io.inverno.mod.security.authentication.Credentials) } method which creates the resulting authentication in case authentication was
 * successful.
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

	/**
	 * The credentials resolver.
	 */
	protected final CredentialsResolver<? extends B> credentialsResolver;
	
	/**
	 * The credentials matcher.
	 */
	protected final CredentialsMatcher<A, B> credentialsMatcher;
	
	/**
	 * <p>
	 * Creates a principal authenticator with the specified credentials resolver and credentials matcher.
	 * </p>
	 * 
	 * @param credentialsResolver a credentials resolver
	 * @param credentialsMatcher  a credentials matcher
	 */
	protected AbstractPrincipalAuthenticator(CredentialsResolver<? extends B> credentialsResolver, CredentialsMatcher<A, B> credentialsMatcher) {
		this.credentialsResolver = Objects.requireNonNull(credentialsResolver);
		this.credentialsMatcher = Objects.requireNonNull(credentialsMatcher);
	}

	@Override
	public Mono<C> authenticate(A credentials) throws AuthenticationException {
		return this.credentialsResolver
			.resolveCredentials(credentials.getUsername())
			.filter(resolvedCredentials -> this.credentialsMatcher.matches(credentials, resolvedCredentials))
			.map(this::createAuthentication)
			.switchIfEmpty(Mono.error(() -> new InvalidCredentialsException("Invalid credentials")));
	}
	
	/**
	 * <p>
	 * Creates the authentication resulting from a successful authentication using the resolved trusted credentials.
	 * </p>
	 * 
	 * @param resolvedCredentials the resolved trusted credentials
	 * 
	 * @return an authentication
	 * @throws AuthenticationException if there was an error generating the authentication
	 */
	protected abstract C createAuthentication(B resolvedCredentials) throws AuthenticationException;
}
