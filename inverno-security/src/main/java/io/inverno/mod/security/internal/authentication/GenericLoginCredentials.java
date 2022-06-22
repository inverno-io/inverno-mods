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
package io.inverno.mod.security.internal.authentication;

import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.password.Password;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Generic {@link LoginCredentials} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericLoginCredentials implements LoginCredentials {
	
	/**
	 * The username.
	 */
	private final String username;

	/**
	 * The password.
	 */
	private final Password<?, ?> password;
	
	/**
	 * Flag indicating whether credentials are locked.
	 */
	private boolean locked;

	/**
	 * <p>
	 * Creates generic credentials with the specified username and password.
	 * </p>
	 * 
	 * @param username the username
	 * @param password the password
	 * 
	 * @throws InvalidCredentialsException if the username is blank
	 */
	public GenericLoginCredentials(String username, Password<?, ?> password) throws InvalidCredentialsException {
		if(StringUtils.isBlank(username)) {
			throw new InvalidCredentialsException("Username is blank");
		}
		this.username = username;
		this.password = Objects.requireNonNull(password);
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public Password<?, ?> getPassword() {
		return password;
	}
	
	@Override
	public boolean isLocked() {
		return locked;
	}

	/**
	 * Locks/Unlocks the credentials.
	 * 
	 * @param locked true to lock the credentials, false otherwise
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}
