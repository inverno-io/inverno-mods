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

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationException;

/**
 * <p>
 * Generic {@link Authentication} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericAuthentication implements Authentication {

	/**
	 * The anonymous authentication (unauthenticated with no cause of authentication failure).
	 */
	public static final GenericAuthentication ANONYMOUS = new GenericAuthentication(false);
	
	/**
	 * The granted authentication.
	 */
	public static final GenericAuthentication GRANTED = new GenericAuthentication(true);

	/**
	 * The denied authentication.
	 */
	public static final GenericAuthentication DENIED = new GenericAuthentication(new AuthenticationException("Access denied"));

	/**
	 * Flag indicating whether the entity has been authenticated.
	 */
	@JsonIgnore
	private final boolean authenticated;
	
	/**
	 * The cause of the failed authentication that resulted in this authentication.
	 */
	@JsonIgnore
	private final io.inverno.mod.security.SecurityException cause;

	/**
	 * <p>
	 * Creates a generic authentication.
	 * </p>
	 * 
	 * @param authenticated true to create an authenticated authentication, false otherwise
	 */
	public GenericAuthentication(boolean authenticated) {
		this.authenticated = authenticated;
		this.cause = null;
	}

	/**
	 * <p>
	 * Creates an unauthenticated authentication with the specified cause of authentication failure.
	 * </p>
	 * 
	 * @param cause the cause of the failed authentication
	 */
	public GenericAuthentication(io.inverno.mod.security.SecurityException cause) {
		this.authenticated = false;
		this.cause = cause;
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.authenticated;
	}

	@Override
	public Optional<io.inverno.mod.security.SecurityException> getCause() {
		return Optional.ofNullable(this.cause);
	}

	@Override
	public int hashCode() {
		return Objects.hash(authenticated);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericAuthentication other = (GenericAuthentication) obj;
		return authenticated == other.authenticated;
	}
}
