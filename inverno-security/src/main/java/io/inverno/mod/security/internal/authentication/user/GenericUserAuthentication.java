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
package io.inverno.mod.security.internal.authentication.user;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.inverno.mod.security.authentication.user.UserAuthentication;
import io.inverno.mod.security.identity.Identity;

/**
 * <p>
 * Generic {@link UserAuthentication} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of identity
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericUserAuthentication<A extends Identity> implements UserAuthentication<A> {
	
	/**
	 * The username.
	 */
	@JsonIgnore
	private final String username;

	/**
	 * The user's identity.
	 */
	@JsonIgnore
	private final A identity;
	
	/**
	 * The groups the user belongs to.
	 */
	@JsonIgnore
	private final Set<String> groups;
	
	/**
	 * Flag indicating whether the user has been authenticated.
	 */
	@JsonIgnore
	private boolean authenticated;
	
	/**
	 * The cause of the failed authentication that resulted in this authentication.
	 */
	@JsonIgnore
	private io.inverno.mod.security.SecurityException cause;
	
	/**
	 * <p>
	 * Creates a generic user authentication with the specified username, identity and set of groups.
	 * </p>
	 *
	 * @param username      a username
	 * @param identity      an identity
	 * @param groups        a set of groups
	 * @param authenticated true to create an authenticated authentication, false otherwise
	 */
	@JsonCreator
	public GenericUserAuthentication(@JsonProperty("username") String username, @JsonProperty("identity") A identity, @JsonProperty("groups") Set<String> groups, @JsonProperty("authenticated") boolean authenticated) {
		this.username = username;
		this.identity = identity;
		this.groups = groups != null ? Collections.unmodifiableSet(groups) : Set.of();
		this.authenticated = authenticated;
		this.cause = null;
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
		this.cause = cause;
	}

	@Override
	@JsonProperty( "username" )
	public String getUsername() {
		return username;
	}

	@Override
	@JsonProperty( "groups" )
	public Set<String> getGroups() {
		return this.groups;
	}
	
	@Override
	@JsonProperty( "identity" )
	public A getIdentity() {
		return this.identity;
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
		return Objects.hash(groups, identity, username);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericUserAuthentication other = (GenericUserAuthentication) obj;
		return Objects.equals(groups, other.groups) && Objects.equals(identity, other.identity) && Objects.equals(username, other.username);
	}
}
