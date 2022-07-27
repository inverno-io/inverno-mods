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

/**
 * <p>
 * A login credentials matcher is used to verify that two login credentials are matching.
 * </p>
 * 
 * <p>
 * Two login credentials are matching if an only if they are defined for the same username and their password are matching.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of the first login credentials
 * @param <B> the type of the seconde login credentials
 */
public class LoginCredentialsMatcher<A extends LoginCredentials, B extends LoginCredentials> implements CredentialsMatcher<A, B> {

	@Override
	public boolean matches(A credentials, B otherCredentials) throws SecurityException {
		return credentials.getUsername().equals(otherCredentials.getUsername()) && credentials.getPassword().matches(otherCredentials.getPassword());
	}
}
