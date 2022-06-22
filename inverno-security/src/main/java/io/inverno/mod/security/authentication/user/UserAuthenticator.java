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
package io.inverno.mod.security.authentication.user;

import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.AbstractPrincipalAuthenticator;
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.CredentialsMatcher;
import io.inverno.mod.security.authentication.CredentialsResolver;
import io.inverno.mod.security.authentication.PrincipalAuthentication;
import io.inverno.mod.security.authentication.PrincipalCredentials;
import io.inverno.mod.security.identity.Identity;

/**
 * <p>
 * An authenticator used to authenticate users with {@link PrincipalCredentials}.
 * </p>
 * 
 * <p>
 * The resulting {@link UserAuthentication} extends the {@link PrincipalAuthentication} to expose details about the authenticated user, such as its identity and the groups it belongs to. A
 * {@link RoleBasedAccessController} can then be obtained to secure access to protected services of resources.
 * </p>
 * 
 * <p>
 * This implementation typically uses a {@link UserRepository} to resolve users to authenticate but it also allows to use any compliant {@link CredentialsResolver} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of principal credentials to authenticate
 * @param <B> the identity type
 * @param <C> the user type
 */
public class UserAuthenticator<A extends PrincipalCredentials, B extends Identity, C extends User<B>> extends AbstractPrincipalAuthenticator<A, C, UserAuthentication<B>> {

	/**
	 * <p>
	 * Creates a user authenticator with the specified user credentials resolver and user credentials matcher.
	 * </p>
	 * 
	 * @param credentialsResolver a user credentials resolver
	 * @param credentialsMatcher  a user credentials matcher
	 */
	public UserAuthenticator(CredentialsResolver<? extends C> credentialsResolver, CredentialsMatcher<A, C> credentialsMatcher) {
		super(credentialsResolver, credentialsMatcher);
	}

	@Override
	protected UserAuthentication<B> createAuthentication(C resolvedCredentials) throws AuthenticationException {
		return UserAuthentication.of(resolvedCredentials);
	}
}
