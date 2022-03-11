package io.inverno.mod.security.http;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.security.*;
import io.inverno.mod.security.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

public class AuthenticationInterceptor<A extends Exchange<InterceptingSecurityContext>, B extends Credentials, C extends Authentication, D extends Identity, E extends Authorizations> implements ExchangeInterceptor<InterceptingSecurityContext, A> {

	private final CredentialsExtractor<B> credentialsExtractor;
	private final Authenticator<B, C> authenticator;

	private Optional<IdentityResolver<C, D>> identityResolver;
	private Optional<AuthorizationsResolver<C, E>> authorizationsResolver;

	public AuthenticationInterceptor(CredentialsExtractor<B> credentialsExtractor, Authenticator<B, C> authenticator) {
		this(credentialsExtractor, authenticator, null, null);
	}

	public AuthenticationInterceptor(CredentialsExtractor<B> credentialsExtractor, Authenticator<B, C> authenticator, IdentityResolver<C, D> identityResolver, AuthorizationsResolver<C, E> authorizationsResolver) {
		this.credentialsExtractor = Objects.requireNonNull(credentialsExtractor);
		this.authenticator = Objects.requireNonNull(authenticator);
		this.identityResolver = Optional.ofNullable(identityResolver);
		this.authorizationsResolver = Optional.ofNullable(authorizationsResolver);
	}

	public void setIdentityResolver(IdentityResolver<C, D> identityResolver) {
		this.identityResolver = Optional.ofNullable(identityResolver);
	}

	public void setAuthorizationsResolver(AuthorizationsResolver<C, E> authorizationsResolver) {
		this.authorizationsResolver = Optional.ofNullable(authorizationsResolver);
	}

	@Override
	public Mono<? extends A> intercept(A exchange) {
		return this.credentialsExtractor.extract(exchange) // 1. Extract credentials
			.flatMap(this.authenticator::authenticate) // 2. Authenticate
			.flatMap(authentication -> Mono.zip( // 3. Resolve identity and authorizations
				Mono.just(authentication),
				this.identityResolver
					.map(resolver -> resolver.resolveIdentity(authentication).map(Optional::of))
					.orElse(Mono.just(Optional.empty())),
				this.authorizationsResolver
					.map(resolver -> resolver.resolveAuthorizations(authentication).map(Optional::of))
					.orElse(Mono.just(Optional.empty()))
			))
			.doOnNext(tuple3 -> exchange.context().setSecurityContext( // 4. Create and set SecurityContext
				SecurityContext.of(
					(Authentication) tuple3.getT1(),
					(Optional<Identity>) tuple3.getT2(),
					(Optional<Authorizations>) tuple3.getT3()
				)
			))
			.thenReturn(exchange);
	}
}
