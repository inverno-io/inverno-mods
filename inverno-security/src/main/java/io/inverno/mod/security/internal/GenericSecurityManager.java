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
package io.inverno.mod.security.internal;

import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.Credentials;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link io.inverno.mod.security.SecurityManager SecurityManager} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of credentials to authenticate
 * @param <B> the identity type
 * @param <C> the access controller type
 */
public class GenericSecurityManager<A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController> implements io.inverno.mod.security.SecurityManager<A, C, D> {
	
	/**
	 * The authenticator.
	 */
	private final Authenticator<? super A, ? extends B> authenticator;
	
	/**
	 * The identity resolver.
	 */
	private final IdentityResolver<? super B, ? extends C> identityResolver;
	
	/**
	 * The access controller resolver.
	 */
	private final AccessControllerResolver<? super B, ? extends D> accessControllerResolver;

	/**
	 * <p>
	 * Creates a generic security manager with the specified authenticator.
	 * </p>
	 * 
	 * @param authenticator the authenticator
	 */
	public GenericSecurityManager(Authenticator<? super A, ? extends B> authenticator) {
		this(authenticator, null, null);
	}
	
	/**
	 * <p>
	 * Creates a generic security manager with the specified authenticator and identity resolver.
	 * </p>
	 *
	 * @param authenticator    the authenticator
	 * @param identityResolver the identity resolver
	 */
	public GenericSecurityManager(Authenticator<? super A, ? extends B> authenticator, IdentityResolver<? super B, ? extends C> identityResolver) {
		this(authenticator, identityResolver, null);
	}
	
	/**
	 * <p>
	 * Creates a generic security manager with the specified authenticator and access controller resolver.
	 * </p>
	 *
	 * @param authenticator            the authenticator
	 * @param accessControllerResolver the access controller resolver
	 */
	public GenericSecurityManager(Authenticator<? super A, ? extends B> authenticator, AccessControllerResolver<? super B, ? extends D> accessControllerResolver) {
		this(authenticator, null, accessControllerResolver);
	}

	/**
	 * <p>
	 * Creates a generic security manager with the specified authenticator, identity resolver and access controller resolver.
	 * </p>
	 *
	 * @param authenticator            the authenticator
	 * @param identityResolver         the identity resolver
	 * @param accessControllerResolver the access controller resolver
	 */
	public GenericSecurityManager(Authenticator<? super A, ? extends B> authenticator, IdentityResolver<? super B, ? extends C> identityResolver, AccessControllerResolver<? super B, ? extends D> accessControllerResolver) {
		this.authenticator = Objects.requireNonNull(authenticator);
		this.identityResolver = identityResolver;
		this.accessControllerResolver = accessControllerResolver;
	}

	@Override
	public Mono<SecurityContext<C, D>> authenticate(A credentials) {
		if(credentials == null) {
			// Returns an anonymous authentication when no credentials are specified
			return Mono.just(SecurityContext.of(Authentication.anonymous()));
		}

		// 1. Authenticate
		return this.authenticator.authenticate(credentials)
			.switchIfEmpty(Mono.error(() -> new AuthenticationException("Unable to authenticate")))
			// 2. Resolve identity and access control
			.flatMap(authentication -> {
				if(authentication.isAuthenticated()) {
					SecurityContext.Builder<C, D> securityContextBuilder = SecurityContext.builder(authentication);
					return Flux.merge(
							Mono.justOrEmpty(this.identityResolver)
								.flatMap(resolver -> resolver.resolveIdentity(authentication))
								.doOnNext(securityContextBuilder::identity),
							Mono.justOrEmpty(this.accessControllerResolver)
								.flatMap(resolver -> resolver.resolveAccessController(authentication))
								.doOnNext(securityContextBuilder::accessController)
						)
						.then(Mono.fromSupplier(securityContextBuilder::build));
				}
				else {
					return Mono.just(SecurityContext.<C, D>of(authentication));
				}
			})
			// Return a denied authentication in case of security error, let any other error propagate
			.onErrorResume(io.inverno.mod.security.SecurityException.class, error -> Mono.just(SecurityContext.of(Authentication.denied(error))));
	}
}
