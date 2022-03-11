package io.inverno.mod.security;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface AuthorizationsResolver<A extends Authentication, B extends Authorizations> {

	Mono<B> resolveAuthorizations(A authentication);
}
