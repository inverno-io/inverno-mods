package io.inverno.mod.security;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface Authenticator<A extends Credentials, B extends Authentication> {

	Mono<B> authenticate(A credentials) throws AuthenticationException;

}
