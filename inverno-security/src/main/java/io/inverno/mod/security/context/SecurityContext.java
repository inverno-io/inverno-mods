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
package io.inverno.mod.security.context;

import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.internal.context.GenericSecurityContext;
import java.util.Optional;

/**
 * <p>
 * The security context represents the central security component in an application.
 * </p>
 * 
 * <p>
 * It provides information and services to properly secure an application. It is basically composed of three components:
 * </p>
 * 
 * <dl>
 * <dt>{@link Authentication}</dt>
 * <dd>It proves that an entity has been authenticated, in other words that the credentials of that entity has been authenticated.</dd>
 * <dt>{@link Identity}</dt>
 * <dd>When specified it provides the identity of the authenticated entity.</dd>
 * <dt>{@link AccessController}</dt>
 * <dd>It provides services to control the access to protected services or resources.</dd>
 * </dl>
 * 
 * <p>
 * This makes it explicit that application security comes down to a process which starts by authenticating a request, or more specifically the entity that issued the request (and not a user). From
 * there, access control can be achieved using various approaches such as role-based access control or permission-based access control. However, although access control is related to the resulting
 * authentication, it is decorrelated from the authentication process: an entity can be authenticated without being able to apply access control afterwards.
 * </p>
 * 
 * <p>
 * Finally, the identity which is also related to the authentication provides information about the identity of the authenticated entity. As for the access control, this is decorrelated from the
 * authentication process: an entity can be authenticated, and yet its identity might remain unknown. A typical example would be OAuth2 where authorizations (scopes) are granted to an authenticated
 * entity but no identity is ever provided.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 * @param <B> the access controller type
 */
public interface SecurityContext<A extends Identity, B extends AccessController> {

	/**
	 * <p>
	 * Creates a security context with the specified authentication.
	 * </p>
	 *
	 * <p>
	 * The resulting context has no identity and no access controller.
	 * </p>
	 *
	 * @param <A>            the identity type
	 * @param <B>            the access controller type
	 * @param authentication an authentication
	 *
	 * @return a new security context
	 */
	static <A extends Identity, B extends AccessController> SecurityContext<A, B> of(Authentication authentication) {
		return new GenericSecurityContext<>(authentication);
	}

	/**
	 * <p>
	 * Creates a security context with the specified authentication and identity.
	 * </p>
	 *
	 * <p>
	 * The resulting context has no access controller.
	 * </p>
	 *
	 * @param <A>            the identity type
	 * @param <B>            the access controller type
	 * @param authentication an authentication
	 * @param identity       an identity
	 *
	 * @return a new security context
	 */
	static <A extends Identity, B extends AccessController> SecurityContext<A, B> of(Authentication authentication, A identity) {
		GenericSecurityContext<A, B> context = new GenericSecurityContext<>(authentication);
		context.setIdentity(identity);
		return context;
	}

	/**
	 * <p>
	 * Creates a security context with the specified authentication and access controller.
	 * </p>
	 * 
	 * <p>
	 * The resulting context has no identity.
	 * </p>
	 *
	 * @param <A>              the identity type
	 * @param <B>              the access controller type
	 * @param authentication   an authentication
	 * @param accessController an access controller
	 *
	 * @return a new security context
	 */
	static <A extends Identity, B extends AccessController> SecurityContext<A, B> of(Authentication authentication, B accessController) {
		GenericSecurityContext<A, B> context = new GenericSecurityContext<>(authentication);
		context.setAccessController(accessController);
		return context;
	}

	/**
	 * <p>
	 * Creates a security context with the specified authentication, identity and access controller.
	 * </p>
	 *
	 * @param <A>              the identity type
	 * @param <B>              the access controller type
	 * @param authentication   an authentication
	 * @param identity         an optional identity
	 * @param accessController an optional access controller
	 *
	 * @return a new security context
	 */
	static <A extends Identity, B extends AccessController> SecurityContext<A, B> of(Authentication authentication, A identity, B accessController) {
		GenericSecurityContext<A, B> context = new GenericSecurityContext<>(authentication);
		context.setIdentity(identity);
		context.setAccessController(accessController);
		return context;
	}

	/**
	 * <p>
	 * Creates a security context builder with the specified authentication.
	 * </p>
	 *
	 * @param <A>              the identity type
	 * @param <B>              the access controller type
	 * @param authentication an authentication
	 *
	 * @return a security context builder
	 */
	static <A extends Identity, B extends AccessController> SecurityContext.Builder<A, B> builder(Authentication authentication) {
		return new SecurityContext.Builder<>(authentication);
	}

	/**
	 * <p>
	 * Determines whether an entity has been authenticated.
	 * </p>
	 * 
	 * <p>
	 * This method basically delegates to {@link Authentication#isAuthenticated()}.
	 * </p>
	 * 
	 * @return true if an entity has been authenticated, false otherwise
	 */
	default boolean isAuthenticated() {
		return this.getAuthentication().isAuthenticated();
	}
	
	/**
	 * <p>
	 * Determines whether this context is anonymous.
	 * </p>
	 * 
	 * <p>
	 * This method basically delegates to {@link Authentication#isAnonymous()}.
	 * </p>
	 * 
	 * @return true if the context represents an anonymous access, false otherwise
	 */
	default boolean isAnonymous() {
		return this.getAuthentication().isAnonymous();
	}

	/**
	 * <p>
	 * Returns the authentication.
	 * </p>
	 * 
	 * <p>
	 * A security context always returns an authentication which can be authenticated or unauthenticated following a failed authentication or for anonymous access.
	 * </p>
	 * 
	 * @return an authentication
	 * 
	 * @see Authentication
	 */
	default Authentication getAuthentication() {
		return Authentication.anonymous();
	}
	
	/**
	 * <p>
	 * Returns the identity of the authenticated entity.
	 * </p>
	 * 
	 * <p>
	 * The identity is always empty for an unauthenticated context and may be empty for an authenticated context when the identity of the authenticated entity is unknown.
	 * </p>
	 * 
	 * @return an optional returning the identity or an empty optional
	 */
	default Optional<A> getIdentity() {
		return Optional.empty();
	}

	/**
	 * <p>
	 * Returns the access controller that control access to protected services and resources for the authenticated entity.
	 * </p>
	 * 
	 * <p>
	 * The access controller is always empty for an unauthenticated context and may be empty for an authenticated context when access control is unsupported or unavailable for the authenticated
	 * entity.
	 * </p>
	 * 
	 * @return an optional returning the access controller or an empty optional
	 */
	default Optional<B> getAccessController() {
		return Optional.empty();
	}

	/**
	 * <p>
	 * A security context builder.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the identity type
	 * @param <B> the access controller type
	 */
	class Builder<A extends Identity, B extends AccessController> {

		private final Authentication authentication;

		private A identity;
		private B accessController;

		/**
		 * <p>
		 * Creates a security context builder
		 * </p>
		 *
		 * @param authentication an authentication
		 */
		private Builder(Authentication authentication) {
			this.authentication = authentication;
		}

		/**
		 * <p>
		 * Specifies an identity.
		 * </p>
		 *
		 * @param identity an identity
		 *
		 * @return the builder
		 */
		public Builder<A, B> identity(A identity) {
			this.identity = identity;
			return this;
		}

		/**
		 * <p>
		 * Specifies an access controller.
		 * </p>
		 *
		 * @param accessController an access controller
		 *
		 * @return the builder
		 */
		public Builder<A, B> accessController(B accessController) {
			this.accessController = accessController;
			return this;
		}

		/**
		 * <p>
		 * Builds the security context.
		 * </p>
		 *
		 * @return a security context
		 */
		public SecurityContext<A, B> build() {
			return SecurityContext.of(this.authentication, this.identity, this.accessController);
		}
	}
}
