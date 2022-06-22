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

import io.inverno.mod.security.authentication.password.Password;
import io.inverno.mod.security.internal.authentication.GenericLoginCredentials;

/**
 * <p>
 * Login credentials composed of a username and a password.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface LoginCredentials extends PrincipalCredentials {

	/**
	 * <p>
	 * Creates a login credentials with the specified username and password.
	 * </p>
	 * 
	 * @param username a username
	 * @param password a password
	 * 
	 * @return a new login credentials
	 * 
	 * @throws InvalidCredentialsException if the combination of user and password is invalid
	 */
	static LoginCredentials of(String username, Password<?, ?> password) throws InvalidCredentialsException {
		return new GenericLoginCredentials(username, password);
	}
	
	/**
	 * <p>
	 * Returns the password.
	 * </p>
	 * 
	 * @return a password
	 */
	Password<?, ?> getPassword();
	
	/*
	 * TODO We might also include the realm here as well in order to be able to use multiple CredentialsResolver per realm.
	 * This might be interesting however, considering HTTP Basic authentication scheme the realm is sent as a challenge to the client which do not send it back with the authorization header
	 * the information is basically lost
	 * this is not the case for the digest scheme
	 */
}
