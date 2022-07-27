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
package io.inverno.mod.security;

import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.Credentials;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.security.internal.GenericSecurityManager;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A security manager authenticate the credentials of an entity and obtain the {@link SecurityContext} used to protect services and resources in the application.
 * </p>
 * 
 * <p>
 * A security manager implementation shall rely on an {@link Authenticator} to authenticate credentials, an {@link IdentityResolver} to resolve the identity of the authenticated entity and an
 * {@link AccessControllerResolver} to get the access controller for the authenticated entity in order to create the {@link SecurityContext}.
 * </p>
 * 
 * <p>
 * The resulting security context can then be used to protect sensitive services or resources.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of credentials to authenticate
 * @param <B> the identity type
 * @param <C> the access controller type
 */
public interface SecurityManager<A extends Credentials, B extends Identity, C extends AccessController> {
	
	/**
	 * <p>
	 * Creates a security manager with the specified authenticator.
	 * </p>
	 * 
	 * <p>
	 * The security contexts created using the resulting security manager will have no identity and no access controller since corresponding resolvers are missing.
	 * </p>
	 * 
	 * @param <A> credentials type to authenticate
	 * @param <B> the authentication resulting from a successful authentication and used to resolve identity and access controller
	 * @param <C> the identity type
	 * @param <D> the access controller type
	 * @param authenticator the authenticator
	 * 
	 * @return a new security manager
	 */
	static <A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController> SecurityManager<A, C, D> of(Authenticator<? super A, ? extends B> authenticator) {
		return new GenericSecurityManager<>(authenticator);
	}
	
	/**
	 * <p>
	 * Creates a security manager with the specified authenticator and identity resolver.
	 * </p>
	 * 
	 * <p>
	 * The security contexts created using the resulting security manager will have no access controller since corresponding resolver is missing.
	 * </p>
	 * 
	 * @param <A> credentials type to authenticate
	 * @param <B> the authentication resulting from a successful authentication and used to resolve identity and access controller
	 * @param <C> the identity type
	 * @param <D> the access controller type
	 * @param authenticator the authenticator
	 * @param identityResolver the identity resolver
	 * 
	 * @return a new security manager
	 */
	static <A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController> SecurityManager<A, C, D> of(Authenticator<? super A, ? extends B> authenticator, IdentityResolver<? super B, ? extends C> identityResolver) {
		return new GenericSecurityManager<>(authenticator, identityResolver);
	}
	
	/**
	 * <p>
	 * Creates a security manager with the specified authenticator and access controller resolver.
	 * </p>
	 * 
	 * <p>
	 * The security contexts created using the resulting security manager will have no identity since corresponding resolver is missing.
	 * </p>
	 * 
	 * @param <A> credentials type to authenticate
	 * @param <B> the authentication resulting from a successful authentication and used to resolve identity and access controller
	 * @param <C> the identity type
	 * @param <D> the access controller type
	 * @param authenticator the authenticator
	 * @param accessControllerResolver the access controller resolver
	 * 
	 * @return a new security manager
	 */
	static <A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController> SecurityManager<A, C, D> of(Authenticator<? super A, ? extends B> authenticator, AccessControllerResolver<? super B, ? extends D> accessControllerResolver) {
		return new GenericSecurityManager<>(authenticator, accessControllerResolver);
	}
	
	/**
	 * <p>
	 * Creates a security manager with the specified authenticator, identity resolver and access controller resolver.
	 * </p>
	 * 
	 * @param <A> credentials type to authenticate
	 * @param <B> the authentication resulting from a successful authentication and used to resolve identity and access controller
	 * @param <C> the identity type
	 * @param <D> the access controller type
	 * @param authenticator the authenticator
	 * @param identityResolver the identity resolver
	 * @param accessControllerResolver the access controller resolver
	 * 
	 * @return a new security manager
	 */
	static <A extends Credentials, B extends Authentication, C extends Identity, D extends AccessController> SecurityManager<A, C, D> of(Authenticator<? super A, ? extends B> authenticator, IdentityResolver<? super B, ? extends C> identityResolver, AccessControllerResolver<? super B, ? extends D> accessControllerResolver) {
		return new GenericSecurityManager<>(authenticator, identityResolver, accessControllerResolver);
	}
	
	/**
	 * <p>
	 * Authenticates the specified credentials and returns the corresponding security context.
	 * </p>
	 * 
	 * <p>
	 * This method authenticates the credentials, then resolve the identiy and the access controller and finally create the resulting security context which can be:
	 * </p>
	 * 
	 * <ul>
	 * <li><b>anonymous</b> (i.e. not authenticated with no cause) when specifying null credentials.</li>
	 * <li><b>denied</b> when an error was raised during the authentication process or identity and access controller resolutions.</li>
	 * <li><b>granted</b> when the security manager was able to authenticated credentials, resolve identity and access controller.</li>
	 * </ul>
	 * 
	 * @param credentials the credentials to authenticate or null to get an anonymous security context
	 * 
	 * @return a mono emitting the resulting security context
	 */
	Mono<SecurityContext<B, C>> authenticate(A credentials);
}
