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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.password.Password;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * An application user that can be authenticated in an application using password credentials.
 * </p>
 * 
 * <p>
 * A user may belong to one or more groups of users, it is typically authenticated in a {@link UserAuthenticator} resulting in a {@link UserAuthentication} which exposes the user groups. A
 * {@link GroupsRoleBasedAccessControllerResolver} can then be used obtain a role-based access controller.
 * </p>
 * 
 * <p>
 * A user may also have an identity, also exposed in the resulting {@link UserAuthentication}, an {@link UserIdentityResolver} can then be used to extract it.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User<A extends Identity> implements LoginCredentials {

	/**
	 * The username which uniquely identifies the user.
	 */
	@JsonIgnore
	protected final String username;
	
	/**
	 * The identity of the user (can be null).
	 */
	@JsonIgnore
	private A identity;
	
	/**
	 * The user password.
	 */
	@JsonIgnore
	private Password<?, ?> password;
	
	/**
	 * The user groups.
	 */
	@JsonIgnore
	private Set<String> groups;
	
	/**
	 * Flag indicating whether the user is locked.
	 */
	@JsonIgnore
	private boolean locked;

	/**
	 * <p>
	 * Creates a new user with the specified username, password and groups.
	 * </p>
	 * 
	 * @param username a username which uniquely identifies the user
	 * @param password a password
	 * @param groups   a set of groups
	 */
	public User(String username, Password<?, ?> password, String... groups) {
		this(username, null, password, Set.of(groups), false);
	}
	
	/**
	 * <p>
	 * Creates a new user with the specified username, identity, password and groups.
	 * </p>
	 * 
	 * @param username a username which uniquely identifies the user
	 * @param identity an identity
	 * @param password a password
	 * @param groups   a set of groups
	 */
	public User(String username, A identity, Password<?, ?> password, String... groups) {
		this(username, identity, password, Set.of(groups), false);
	}
	
	/**
	 * <p>
	 * Creates a new user with the specified username, identity, password, groups and lock flag.
	 * </p>
	 * 
	 * @param username a username which uniquely identifies the user
	 * @param identity an identity
	 * @param password a password
	 * @param groups   a set of groups
	 * @param locked   true to create a locked user, false otherwise
	 */
	@JsonCreator
	public User(@JsonProperty(value = "username", required = true) String username, @JsonProperty(value = "identity", required = false) A identity, @JsonProperty(value = "password", required = false) Password<?, ?> password, @JsonProperty(value = "groups") Set<String> groups, @JsonProperty(value = "locked", defaultValue = "false") boolean locked) {
		this.username = Objects.requireNonNull(username);
		this.identity = identity;
		this.password = Objects.requireNonNull(password);
		this.groups = groups != null ? Collections.unmodifiableSet(groups) : Set.of();
		this.locked = locked;
	}
	
	/**
	 * <p>
	 * Returns a user builder with the specified username.
	 * </p>
	 *
	 * @param <A>      the identity type
	 * @param username a username which uniquely identifies the user
	 *
	 * @return a user builder
	 */
	public static <A extends Identity> User.Builder<A> of(String username) {
		return new User.Builder<>(username);
	}

	/**
	 * <p>
	 * Returns the username which uniquely identifies the user.
	 * </p>
	 * 
	 * @return the username
	 */
	@Override
	@JsonProperty("username")
	public String getUsername() {
		return username;
	}

	/**
	 * <p>
	 * Returns the identity of the user.
	 * </p>
	 * 
	 * @return the user identity or null
	 */
	@JsonProperty("identity")
	public A getIdentity() {
		return identity;
	}
	
	/**
	 * <p>
	 * Sets the user identity.
	 * </p>
	 * 
	 * @param identity an identity
	 */
	@JsonIgnore
	protected void setIdentity(A identity) {
		this.identity = identity;
	}
	
	/**
	 * <p>
	 * Returns the user password.
	 * </p>
	 * 
	 * @return the user password
	 */
	@Override
	@JsonProperty("password")
	public Password<?, ?> getPassword() {
		return password;
	}

	/**
	 * <p>
	 * Sets the user password
	 * </p>
	 * 
	 * @param password the password to set
	 */
	@JsonIgnore
	protected void setPassword(Password<?, ?> password) {
		this.password = Objects.requireNonNull(password);
	}
	
	/**
	 * <p>
	 * Returns the user groups.
	 * </p>
	 * 
	 * @return a set of groups
	 */
	@JsonProperty("groups")
	public Set<String> getGroups() {
		return groups;
	}

	/**
	 * <p>
	 * Sets the user groups.
	 * </p>
	 * 
	 * @param groups a set of groups
	 */
	@JsonIgnore
	protected void setGroups(Set<String> groups) {
		this.groups = groups != null ? Collections.unmodifiableSet(groups) : Set.of();
	}
	
	/**
	 * <p>
	 * Determines whether the user is locked.
	 * </p>
	 * 
	 * @return true if the user is locked, false otherwise
	 */
	@JsonProperty("locked")
	@Override
	public boolean isLocked() {
		return locked;
	}

	/**
	 * <p>
	 * Locks/unlocks the user.
	 * </p>
	 * 
	 * @param locked true to lock the user, false otherwise
	 */
	@JsonIgnore
	protected void setLocked(boolean locked) {
		this.locked = locked;
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		return Objects.equals(username, other.username);
	}

	/**
	 * <p>
	 * A builder used to build users.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @param <A> the identity type
	 */
	public static class Builder<A extends Identity> {
		
		/**
		 * The username which uniquely identifies the user.
		 */
		private final String username;

		/**
		 * The identity of the user.
		 */
		private A identity;

		/**
		 * The user password.
		 */
		private Password<?, ?> password;

		/**
		 * The user groups.
		 */
		private Set<String> groups;

		/**
		 * The lock flag.
		 */
		private boolean locked;
		
		/**
		 * <p>
		 * Creates a builder with the specifies username.
		 * </p>
		 * 
		 * @param username a username
		 */
		private Builder(String username) {
			this.username = Objects.requireNonNull(username);
		}
		
		/**
		 * <p>
		 * Specifies the identity of the user.
		 * </p>
		 *
		 * @param <B>      the identity type
		 * @param identity the user identity
		 *
		 * @return this builder
		 */
		public <B extends Identity> Builder<B> identity(B identity) {
			User.Builder<B> builder = new User.Builder<>(this.username);
			builder.identity = identity;
			builder.password = this.password;
			builder.groups = this.groups;
			builder.locked = this.locked;
			return builder;
		}
		
		/**
		 * <p>
		 * Specifies the user password.
		 * </p>
		 * 
		 * @param password the user password
		 * 
		 * @return this builder
		 */
		public Builder<A> password(Password<?,?> password) {
			this.password = Objects.requireNonNull(password);
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the user groups.
		 * </p>
		 *
		 * @param groups an array of groups
		 *
		 * @return this builder
		 */
		public Builder<A> groups(String... groups) {
			this.groups = groups != null ? Arrays.stream(groups).collect(Collectors.toSet()) : null;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the user groups.
		 * </p>
		 *
		 * @param groups a collection of groups
		 *
		 * @return this builder
		 */
		public Builder<A> groups(Collection<String> groups) {
			this.groups = groups != null ? new HashSet<>(groups) : null;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies whether to create a locked user.
		 * </p>
		 * 
		 * @param locked true to create a locked user, false otherwise
		 * 
		 * @return this builder
		 */
		public Builder<A> locked(boolean locked) {
			this.locked = locked;
			return this;
		}
		
		/**
		 * <p>
		 * Builds the user.
		 * </p>
		 * 
		 * @return a new user
		 */
		public User<A> build() {
			return new User<>(this.username, this.identity, this.password, this.groups, this.locked);
		}
	}
}
