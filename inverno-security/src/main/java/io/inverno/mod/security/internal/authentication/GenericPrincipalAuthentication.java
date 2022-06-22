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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.authentication.PrincipalAuthentication;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Generic {@link PrincipalAuthentication} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericPrincipalAuthentication implements PrincipalAuthentication {
	
	/**
	 * The username.
	 */
	@JsonIgnore
	private final String username;
	
	/**
	 * Flag indicating whether the prinipal has been authenticated.
	 */
	@JsonIgnore
	private boolean authenticated;
	
	/**
	 * The cause of the failed authentication that resulted in this authentication.
	 */
	@JsonIgnore
	private Optional<io.inverno.mod.security.SecurityException> cause;
	
	/**
	 * <p>
	 * Creates a generic principal authentication with the specified username.
	 * </p>
	 * 
	 * @param username the username
	 * @param authenticated true to create an authenticated authentication, false otherwise
	 */
	@JsonCreator
	public GenericPrincipalAuthentication(@JsonProperty("username") String username, @JsonProperty("authenticated") boolean authenticated) {
		this.username = username;
		this.authenticated = authenticated;
		this.cause = Optional.empty();
	}
	
	/**
	 * <p>
	 * Sets the cause of the failed authentication.
	 * </p>
	 * 
	 * <p>
	 * This also sets the authenticated flag to false when the specified cause is not null.
	 * </p>
	 * 
	 * @param cause a security exception resulting from the failed authentication
	 */
	public void setCause(io.inverno.mod.security.SecurityException cause) {
		if(cause != null) {
			this.authenticated = false;
		}
		this.cause = Optional.ofNullable(cause);
	}

	@Override
	@JsonProperty( "username" )
	public String getUsername() {
		return username;
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.authenticated;
	}

	@Override
	public Optional<io.inverno.mod.security.SecurityException> getCause() {
		return this.cause;
	}

	@Override
	public int hashCode() {
		return Objects.hash(authenticated, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericPrincipalAuthentication other = (GenericPrincipalAuthentication) obj;
		return authenticated == other.authenticated && Objects.equals(username, other.username);
	}
}
