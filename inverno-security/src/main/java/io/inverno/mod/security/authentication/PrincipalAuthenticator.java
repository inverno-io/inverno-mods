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

import io.inverno.mod.security.authentication.user.UserAuthenticator;

/**
 * <p>
 * An authenticator used to authenticate principal credentials.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see UserAuthenticator
 * 
 * @param <A> the type of principal credentials to authenticate
 * @param <B> the type of principal credentials resolved by the credentials resolver
 */
public class PrincipalAuthenticator<A extends PrincipalCredentials, B extends PrincipalCredentials> extends AbstractPrincipalAuthenticator<A, B, PrincipalAuthentication> {
	
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
	public PrincipalAuthenticator(CredentialsResolver<? extends B> credentialsResolver, CredentialsMatcher<? super A, ? super B> credentialsMatcher) {
		super(credentialsResolver, credentialsMatcher);
	}

	@Override
	protected PrincipalAuthentication createAuthenticated(B resolvedCredentials) throws AuthenticationException {
		return PrincipalAuthentication.of(resolvedCredentials);
	}

	@Override
	protected PrincipalAuthentication createDenied(A credentials, AuthenticationException cause) throws AuthenticationException {
		return PrincipalAuthentication.of(credentials.getUsername(), cause);
	}
}
