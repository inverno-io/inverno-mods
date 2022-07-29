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
package io.inverno.mod.security.http.internal;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.Credentials;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link SecurityInterceptor} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the credentials type
 * @param <B> the authentication type
 * @param <C> the identity type
 * @param <D> the access controller type
 * @param <E> the intercepting security context type
 * @param <F> the exchange type
 */
public class GenericSecurityInterceptor<A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController, E extends InterceptingSecurityContext<C, D>, F extends Exchange<E>> implements SecurityInterceptor<A, C, D, E, F> {

	private static final Logger LOGGER = LogManager.getLogger(GenericSecurityInterceptor.class);

	/**
	 * The credentials extractor.
	 */
	private final CredentialsExtractor<? extends A> credentialsExtractor;
	
	/**
	 * The authenticator.
	 */
	private final Authenticator<? super A, ? extends B> authenticator;

	/**
	 * The identity resolver.
	 */
	private final Optional<IdentityResolver<? super B, ? extends C>> identityResolver;
	
	/**
	 * The access controller resolver.
	 */
	private final Optional<AccessControllerResolver<? super B, ? extends D>> accessControllerResolver;

	/**
	 * <p>
	 * Creates a security interceptor with the specified credentials extractor and authenticator.
	 * </p>
	 *
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 */
	public GenericSecurityInterceptor(CredentialsExtractor<? extends A> credentialsExtractor, Authenticator<? super A, ? extends B> authenticator) {
		this(credentialsExtractor, authenticator, null, null);
	}
	
	/**
	 * <p>
	 * Creates a security interceptor with the specified credentials extractor, authenticator and identity resolver.
	 * </p>
	 *
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 * @param identityResolver     an identity resolver
	 */
	public GenericSecurityInterceptor(CredentialsExtractor<? extends A> credentialsExtractor, Authenticator<? super A, ? extends B> authenticator, IdentityResolver<? super B, ? extends C> identityResolver) {
		this(credentialsExtractor, authenticator, identityResolver, null);
	}
	
	/**
	 * <p>
	 * Creates a security interceptor with the specified credentials extractor, authenticator and access controller resolver.
	 * </p>
	 *
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 * @param accessControllerResolver     an access controller resolver
	 */
	public GenericSecurityInterceptor(CredentialsExtractor<? extends A> credentialsExtractor, Authenticator<? super A, ? extends B> authenticator, AccessControllerResolver<? super B, ? extends D> accessControllerResolver) {
		this(credentialsExtractor, authenticator, null, accessControllerResolver);
	}

	/**
	 * <p>
	 * Creates a security interceptor with the specified credentials extractor, authenticator, identity resolver and access controller resolver.
	 * </p>
	 *
	 * @param credentialsExtractor a credentials extractor
	 * @param authenticator        an authenticator
	 * @param identityResolver     an identity resolver
	 * @param accessControllerResolver     an access controller resolver
	 */
	public GenericSecurityInterceptor(CredentialsExtractor<? extends A> credentialsExtractor, Authenticator<? super A, ? extends B> authenticator, IdentityResolver<? super B, ? extends C> identityResolver, AccessControllerResolver<? super B, ? extends D> accessControllerResolver) {
		this.credentialsExtractor = Objects.requireNonNull(credentialsExtractor);
		this.authenticator = Objects.requireNonNull(authenticator);
		this.identityResolver = Optional.ofNullable(identityResolver);
		this.accessControllerResolver = Optional.ofNullable(accessControllerResolver);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<? extends F> intercept(F exchange) {
		// 1. Extract credentials
		return this.credentialsExtractor.extract(exchange)
			// No credentials => anonymous access
			.switchIfEmpty(Mono.fromRunnable(() -> exchange.context().setSecurityContext((SecurityContext<C, D>)SecurityContext.of(Authentication.anonymous()))))
			// 2. Authenticate
			.flatMap(credentials -> this.authenticator
				.authenticate(credentials)
				.switchIfEmpty(Mono.error(() -> new AuthenticationException("Unable to authenticate")))
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
					(B) tuple3.getT1(),
					(Optional<C>) tuple3.getT2(),
					(Optional<D>) tuple3.getT3()
				)
			))
			// Return a denied authentication in case of security error, let any other error propagate
			.onErrorResume(io.inverno.mod.security.SecurityException.class, error -> {
				exchange.context().setSecurityContext((SecurityContext<C, D>)SecurityContext.of(Authentication.denied(error)));
				return Mono.empty();
			})
			.thenReturn(exchange);
	}
}