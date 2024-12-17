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
package io.inverno.mod.security.ldap.internal.authentication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.SecurityException;
import io.inverno.mod.security.ldap.authentication.LDAPAuthentication;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Generic LDAP authentication implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericLDAPAuthentication implements LDAPAuthentication {

	/**
	 * The uid.
	 */
	@JsonIgnore
	private final String username;
	
	/**
	 * The user DN.
	 */
	@JsonIgnore
	private final String dn;
	
	/**
	 * The groups the user belongs to.
	 */
	@JsonIgnore
	private final Set<String> groups;
	
	/**
	 * Indicates whether the user is authenticated (i.e. authentication succeeds)
	 */
	@JsonIgnore
	private boolean authenticated;
	
	/**
	 * The authentication error.
	 */
	@JsonIgnore
	private io.inverno.mod.security.SecurityException cause;
	
	/**
	 * <p>
	 * Creates a generic LDAP authentication.
	 * </p>
	 * 
	 * @param username      the uid
	 * @param authenticated whether the user is authenticated
	 * @param dn            the user DN
	 * @param groups        the groups the user belongs to
	 */
	@JsonCreator
	public GenericLDAPAuthentication(@JsonProperty("username") String username, @JsonProperty("dn") String dn, @JsonProperty("groups") Set<String> groups, @JsonProperty("authenticated") boolean authenticated) {
		this.username = username;
		this.dn = dn;
		this.groups = groups != null ? Collections.unmodifiableSet(groups) : Set.of();
		this.authenticated = authenticated;
		this.cause = null;
	}
	
	/**
	 * <p>
	 * Sets the authentication error.
	 * </p>
	 * 
	 * <p>
	 * Set the authenticated flag to false, if the specified cause is not null.
	 * </p>
	 * 
	 * @param cause an authentication error
	 */
	public void setCause(io.inverno.mod.security.SecurityException cause) {
		if(cause != null) {
			this.authenticated = false;
		}
		this.cause = cause;
	}
	
	@Override
	public String getUsername() {
		return this.username;
	}

	@Override
	public boolean isAuthenticated() {
		return this.authenticated;
	}

	@Override
	public Optional<SecurityException> getCause() {
		return Optional.ofNullable(this.cause);
	}

	@Override
	public String getDN() {
		return this.dn;
	}

	@Override
	public Set<String> getGroups() {
		return this.groups;
	}

	@Override
	public int hashCode() {
		return Objects.hash(authenticated, dn, groups, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericLDAPAuthentication other = (GenericLDAPAuthentication) obj;
		return authenticated == other.authenticated && Objects.equals(dn, other.dn) && Objects.equals(groups, other.groups) && Objects.equals(username, other.username);
	}
}
