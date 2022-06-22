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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class InMemoryUserCredentialsResolver implements CredentialsResolver<UserCredentials> {

	private final Map<String, UserCredentials> credentials;

	public InMemoryUserCredentialsResolver() {
		this.credentials = new ConcurrentHashMap<>();
	}
	
	public InMemoryUserCredentialsResolver(List<UserCredentials> credentials) {
		this.credentials = new ConcurrentHashMap<>();
		if(credentials != null) {
			credentials.stream().forEach(c -> this.credentials.put(c.getUsername(), c));
		}
	}
	
	public InMemoryUserCredentialsResolver(Map<String, String> credentials) {
		this.credentials = new ConcurrentHashMap<>();
		if(credentials != null) {
			credentials.entrySet().stream().forEach(e -> this.credentials.put(e.getKey(), new UserCredentials(e.getKey(), e.getValue())));
		}
	}
	
	public void put(String username, String password) {
		this.credentials.put(username, new UserCredentials(username, password));
	}
	
	public void put(UserCredentials credentials) {
		this.credentials.put(credentials.getUsername(), credentials);
	}
	
	public void remove(String username) {
		this.credentials.remove(username);
	}
	
	@Override
	public Mono<UserCredentials> resolveCredentials(String id) throws CredentialsNotFoundException {
		return Mono.fromSupplier(() -> this.credentials.get(id)).switchIfEmpty(Mono.error(() -> new CredentialsNotFoundException(id)));
	}
}
