/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security;

import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class UserAuthenticator implements Authenticator<UserCredentials, Authentication> {

	private final CredentialsResolver<UserCredentials> credentialsResolver;
	
	public UserAuthenticator(CredentialsResolver<UserCredentials> credentialsResolver) {
		this.credentialsResolver = credentialsResolver;
	}

	@Override
	public Mono<Authentication> authenticate(UserCredentials credentials) throws AuthenticationException {
		return this.credentialsResolver
			.resolveCredentials(credentials.getUsername())
			.filter(resolvedCredentials -> resolvedCredentials.getPassword().equals(credentials.getPassword()))
			.map(ign -> Authentication.granted())
			.switchIfEmpty(Mono.error(() -> new InvalidCredentialsException("Invalid password")));
	}
}
