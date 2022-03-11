package io.inverno.mod.security;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface IdentityResolver<A extends Authentication, B extends Identity> {

	Mono<B> resolveIdentity(A authentication);
}
