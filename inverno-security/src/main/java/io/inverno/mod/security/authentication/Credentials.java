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

import io.inverno.mod.security.authentication.user.UserRepository;

/**
 * <p>
 * Credentials represents the data required by an entity to get access to protected services or resources.
 * </p>
 * 
 * <p>
 * Credentials must be provided to an {@link Authenticator} by an entity that wants to access protected services or resources. Authenticators can then authenticate these credentials either by matching
 * them with credentials stored in a secured repository (see {@link UserRepository}), by using cryptographic methods or by using other authentication services (e.g. LDAP, Active Directory...).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface Credentials {
	
	/**
	 * <p>
	 * Determines whether a credentials is locked.
	 * </p>
	 * 
	 * <p>
	 * Locked credentials should be considered invalid by authenticators when matching provided credentials with stored entity credentials.
	 * </p>
	 * 
	 * @return true if credentials are locked, false otherwise
	 */
	default boolean isLocked() {
		return false;
	}
}
