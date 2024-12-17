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

import io.inverno.mod.security.authentication.password.Password;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A simple login credentials resolver that stores credentials in memory.
 * </p>
 * 
 * <p>
 * This is a convenient implementation that should not be used to create properly secured applications.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class InMemoryLoginCredentialsResolver implements CredentialsResolver<LoginCredentials> {

	/**
	 * The map of credentials.
	 */
	private final Map<String, LoginCredentials> credentials;

	/**
	 * <p>
	 * Creates an empty login credentials resolver.
	 * </p>
	 */
	public InMemoryLoginCredentialsResolver() {
		this.credentials = new ConcurrentHashMap<>();
	}
	
	/**
	 * <p>
	 * Creates a login credentials resolver initialized with the specified list of credentials.
	 * </p>
	 * 
	 * @param credentials a list of credentials
	 */
	public InMemoryLoginCredentialsResolver(List<LoginCredentials> credentials) {
		this.credentials = new ConcurrentHashMap<>();
		if(credentials != null) {
			credentials.forEach(c -> this.credentials.put(c.getUsername(), c));
		}
	}
	
	/**
	 * <p>
	 * Adds/Sets the login credentials identified by the specified username.
	 * </p>
	 * 
	 * @param username a username
	 * @param password a password
	 */
	public void put(String username, Password<?, ?> password) {
		this.credentials.put(username, LoginCredentials.of(username, password));
	}
	
	/**
	 * <p>
	 * Adds/Sets the specified login credentials.
	 * </p>
	 * 
	 * @param credentials login credentials
	 */
	public void put(LoginCredentials credentials) {
		this.credentials.put(credentials.getUsername(), credentials);
	}
	
	/**
	 * <p>
	 * Removes the login credentials identified by the specified username.
	 * </p>
	 * 
	 * @param username a username
	 * 
	 * @return the removed credentials or null if no credentials were removed
	 */
	public LoginCredentials remove(String username) {
		return this.credentials.remove(username);
	}
	
	@Override
	public Mono<LoginCredentials> resolveCredentials(String id) throws AuthenticationException {
		return Mono.fromSupplier(() -> this.credentials.get(id));
	}
}
