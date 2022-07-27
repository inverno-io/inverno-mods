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

import io.inverno.mod.security.SecurityException;
import io.inverno.mod.security.authentication.user.UserAuthenticator;

/**
 * <p>
 * A credentials matcher can be used during an authentication to match two credentials, typically the one provided by the entity to authenticated and the one resolved from a trusted authority or
 * repository.
 * </p>
 *
 * <p>
 * an {@link Authenticator} implemention can typically relies on a credentials matcher to determine whether the credentials provided by an entity are matching the credentials obtained from a trusted
 * source (i.e. a trusted repository, a trusted directory service...).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see PrincipalAuthenticator
 * @see UserAuthenticator
 * 
 * @param <A> the type of the credentials
 * @param <B> the type of the other credentials
 */
@FunctionalInterface
public interface CredentialsMatcher<A extends Credentials, B extends Credentials> {
	
	/**
	 * <p>
	 * Determines whether the two specified credentials are matching.
	 * </p>
	 *
	 * <p>
	 * This method must be:
	 * </p>
	 *
	 * <ul>
	 * <li>reflexive: {@code matches(credentials, credentials)} should return {@code true}</li>
	 * <li>symetric: if {@code matches(credentials1, credentials2)} returns {@code true} ({@code matches(credentials2, credentials1)}) should also return {@code true}</li>
	 * <li>transitive: if {@code matches(credentials1, credentials2)} returns {@code true} and {@code matches(credentials2, credentials3)} returns true then {@code matches(credentials1, credentials3)}
	 * should also return {@code true}</li>
	 * </ul>
	 *
	 * <p>
	 * However this method does not have to be consistent: multiple invocations of {@code matches(credentials1, credentials2)} are not guaranteed to always return the same result.
	 * </p>
	 *
	 * @param credentials      the credentials
	 * @param otherCredentials the other credentials
	 *
	 * @return true if the credentials matches the other credentials, false otherwise
	 *
	 * @throws SecurityException if there was an error matching credentials
	 */
	boolean matches(A credentials, B otherCredentials) throws SecurityException;
}
