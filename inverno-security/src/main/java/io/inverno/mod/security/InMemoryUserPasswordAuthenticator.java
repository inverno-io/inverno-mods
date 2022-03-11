package io.inverno.mod.security;

import reactor.core.publisher.Mono;

import java.util.Map;

public class InMemoryUserPasswordAuthenticator implements Authenticator<UsernamePasswordCredentials, Authentication> {

	private final Map<String, String> userDb;

	public InMemoryUserPasswordAuthenticator(Map<String, String> userDb) {
		this.userDb = userDb;
	}

	@Override
	public Mono<Authentication> authenticate(UsernamePasswordCredentials credentials) {
		return Mono.fromSupplier(() -> credentials.getPassword().equals(this.userDb.get(credentials.getUser())) ? Authentication.granted(): Authentication.denied());
	}
}
