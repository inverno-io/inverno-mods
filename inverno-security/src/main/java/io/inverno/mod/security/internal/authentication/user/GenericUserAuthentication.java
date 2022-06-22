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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.inverno.mod.security.authentication.UserAuthentication;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericUserAuthentication implements UserAuthentication {
	
	@JsonIgnore
	private final String username;
	
	@JsonIgnore
	private final Set<String> groups;
	
	@JsonIgnore
	private final boolean authenticated;
	
	private Optional<io.inverno.mod.security.SecurityException> cause;
	
	@JsonCreator
	public GenericUserAuthentication(@JsonProperty("username") String username, @JsonProperty("groups") Set<String> groups, @JsonProperty("authenticated") boolean authenticated) {
		this.username = username;
		this.groups = groups != null ? Collections.unmodifiableSet(groups) : Set.of();
		this.authenticated = authenticated;
		this.cause = Optional.empty();
	}
	
	public void setCause(io.inverno.mod.security.SecurityException cause) {
		this.cause = Optional.ofNullable(cause);
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
		GenericUserAuthentication other = (GenericUserAuthentication) obj;
		return authenticated == other.authenticated && Objects.equals(username, other.username);
	}
}
