package io.inverno.mod.security.http;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.Credentials;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface CredentialsExtractor<A extends Credentials> {

	Mono<A> extract(Exchange<?> exchange) throws MalformedCredentialsException;
}
