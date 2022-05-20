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
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericAuthentication implements Authentication {

	public static final GenericAuthentication ANONYMOUS = new GenericAuthentication(false);
	
	public static final GenericAuthentication GRANTED = new GenericAuthentication(true);

	public static final GenericAuthentication DENIED = new GenericAuthentication(new AuthenticationException());

	@JsonIgnore
	private final boolean authenticated;
	
	@JsonIgnore
	private final Optional<io.inverno.mod.security.SecurityException> cause;

	public GenericAuthentication(boolean authenticated) {
		this.authenticated = authenticated;
		this.cause = Optional.empty();
	}

	public GenericAuthentication(io.inverno.mod.security.SecurityException cause) {
		this.authenticated = false;
		this.cause = Optional.ofNullable(cause);
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
