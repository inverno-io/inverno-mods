/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.security.http;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.Credentials;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class AuthenticationInterceptor<A extends Exchange<InterceptingSecurityContext>, B extends Credentials, C extends Authentication, D extends Identity, E extends AccessController> implements ExchangeInterceptor<InterceptingSecurityContext, A> {

	private static final Logger LOGGER = LogManager.getLogger(AuthenticationInterceptor.class);
	
	private final CredentialsExtractor<B> credentialsExtractor;
	private final Authenticator<B, C> authenticator;

	private Optional<IdentityResolver<C, D>> identityResolver;
	private Optional<AccessControllerResolver<C, E>> accessControllerResolver;

	public AuthenticationInterceptor(CredentialsExtractor<B> credentialsExtractor, Authenticator<B, C> authenticator) {
		this(credentialsExtractor, authenticator, null, null);
	}
	
	public AuthenticationInterceptor(CredentialsExtractor<B> credentialsExtractor, Authenticator<B, C> authenticator, IdentityResolver<C, D> identityResolver) {
		this(credentialsExtractor, authenticator, identityResolver, null);
	}
	
	public AuthenticationInterceptor(CredentialsExtractor<B> credentialsExtractor, Authenticator<B, C> authenticator, AccessControllerResolver<C, E> accessControllerResolver) {
		this(credentialsExtractor, authenticator, null, accessControllerResolver);
	}

	public AuthenticationInterceptor(CredentialsExtractor<B> credentialsExtractor, Authenticator<B, C> authenticator, IdentityResolver<C, D> identityResolver, AccessControllerResolver<C, E> accessControllerResolver) {
		this.credentialsExtractor = Objects.requireNonNull(credentialsExtractor);
		this.authenticator = Objects.requireNonNull(authenticator);
		this.identityResolver = Optional.ofNullable(identityResolver);
		this.accessControllerResolver = Optional.ofNullable(accessControllerResolver);
	}

	public void setIdentityResolver(IdentityResolver<C, D> identityResolver) {
		this.identityResolver = Optional.ofNullable(identityResolver);
	}

	public void setAccessControllerResolver(AccessControllerResolver<C, E> accessControllerResolver) {
		this.accessControllerResolver = Optional.ofNullable(accessControllerResolver);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<? extends A> intercept(A exchange) {
		// 1. Extract credentials
		return this.credentialsExtractor.extract(exchange)
			// 2. Authenticate
			.flatMap(credentials -> this.authenticator
				.authenticate(credentials)
				.doOnError(io.inverno.mod.security.SecurityException.class, error -> LOGGER.debug("Failed to authenticate", error))
			)
			// 3. Resolve identity and access control
			.flatMap(authentication -> Mono.zip(
					Mono.just(authentication),
					this.identityResolver
						.filter(ign -> authentication.isAuthenticated())
						.map(resolver -> resolver.resolveIdentity(authentication)
							.doOnError(io.inverno.mod.security.SecurityException.class, error -> LOGGER.debug("Failed to resolve identity", error))
							.map(Optional::of)
						)
						.orElse(Mono.just(Optional.empty())),
					this.accessControllerResolver
						.filter(ign -> authentication.isAuthenticated())
						.map(resolver -> resolver.resolveAccessController(authentication)
							.doOnError(io.inverno.mod.security.SecurityException.class, error -> LOGGER.debug("Failed to resolve authorizations", error))
							.map(Optional::of)
						)
						.orElse(Mono.just(Optional.empty()))
				)
			)
			// 4. Create and set SecurityContext
			.doOnNext(tuple3 -> exchange.context().setSecurityContext( 
				SecurityContext.of(
					(Authentication) tuple3.getT1(),
					(Optional<Identity>) tuple3.getT2(),
					(Optional<AccessController>) tuple3.getT3()
				)
			))
			// Return a denied authentication in case of security error, let any other error propagate
			.onErrorResume(io.inverno.mod.security.SecurityException.class, error -> {
				exchange.context().setSecurityContext(SecurityContext.of(Authentication.denied(error)));
				return Mono.empty();
			})
			.thenReturn(exchange);
		/*
		// 1. Extract credentials
		return this.credentialsExtractor.extract(exchange)
			// 2. Authenticate
			.flatMap(this.authenticator::authenticate)
			// 3. Resolve identity and authorizations
			.onErrorContinue(io.inverno.mod.security.SecurityException.class, (error, ign) -> {
				exchange.context().setSecurityContext(SecurityContext.of(Authentication.denied((io.inverno.mod.security.SecurityException)error)));
			})
			.flatMap(authentication -> Mono.zip(
				Mono.just(authentication),
				this.identityResolver
					.filter(ign -> authentication.isAuthenticated())
					.map(resolver -> resolver.resolveIdentity(authentication).map(Optional::of))
					.orElse(Mono.just(Optional.empty())),
				this.authorizationsResolver
					.filter(ign -> authentication.isAuthenticated())
					.map(resolver -> resolver.resolveAuthorizations(authentication).map(Optional::of))
					.orElse(Mono.just(Optional.empty()))
			))
			// 4. Create and set SecurityContext
			.doOnNext(tuple3 -> exchange.context().setSecurityContext( 
				SecurityContext.of(
					(Authentication) tuple3.getT1(),
					(Optional<Identity>) tuple3.getT2(),
					(Optional<Authorizations>) tuple3.getT3()
				)
			))
			// 4'. Create and set failed SecurityContext
			.doOnError(io.inverno.mod.security.SecurityException.class, error -> exchange.context().setSecurityContext(SecurityContext.of(Authentication.denied(error))))
			.onErrorResume(io.inverno.mod.security.SecurityException.class, error -> Mono.empty())
			.thenReturn(exchange);*/
	}
}
