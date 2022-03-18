/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
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
