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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.inverno.mod.security.internal.authentication.GenericPrincipalAuthentication;

/**
 * <p>
 * An authentication resulting from the authentication of a principal entity uniquely identified by a username.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@JsonDeserialize( as = GenericPrincipalAuthentication.class )
public interface PrincipalAuthentication extends Authentication {
	
	/**
	 * <p>
	 * Returns the unique username of the authenticated entity.
	 * </p>
	 * 
	 * @return a username
	 */
	@JsonProperty( "username" )
	String getUsername();
	
	/**
	 * <p>
	 * Returns a new authenticated principal authentication for the specified username.
	 * </p>
	 * 
	 * <p>
	 * This is a conveninence method that should be used with care and only used after a successful authentication to generate the resulting authentication.
	 * </p>
	 * 
	 * @param username a username
	 * 
	 * @return a new principal authentication
	 */
	static PrincipalAuthentication of(String username) {
		return new GenericPrincipalAuthentication(username, true);
	}
	
	/**
	 * <p>
	 * Returns a new principal authentication from the specified credentials.
	 * </p>
	 *
	 * <p>
	 * This is a conveninence method that should be used with care. In order to respect the {@link Authentication} contract it is important to make sure that the specified credentials have been
	 * previously authenticated by an {@link Authenticator}.
	 * </p>
	 *
	 * <p>
	 * The resulting authentication is authenticated if the specified credentials are not locked.
	 * </p>
	 *
	 * @param credentials authenticated credentials
	 *
	 * @return a new principal authentication
	 */
	static PrincipalAuthentication of(PrincipalCredentials credentials) {
		return new GenericPrincipalAuthentication(credentials.getUsername(), !credentials.isLocked());
	}
}
