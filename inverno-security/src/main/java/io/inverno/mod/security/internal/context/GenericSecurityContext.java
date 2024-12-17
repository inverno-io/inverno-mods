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
package io.inverno.mod.security.internal.context;

import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Generic {@link SecurityContext} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 * @param <B> the access controller type
 */
public class GenericSecurityContext<A extends Identity, B extends AccessController> implements SecurityContext<A, B> {

	/**
	 * The authentication.
	 */
	private final Authentication authentication;
	
	/**
	 * The optional identity.
	 */
	private A identity;
	
	/**
	 * The optional access controller.
	 */
	private B accessController;

	/**
	 * <p>
	 * Creates a generic security context with the specified authentication.
	 * </p>
	 * 
	 * @param authentication the authentication.
	 */
	public GenericSecurityContext(Authentication authentication) {
		Objects.requireNonNull(authentication);
		this.authentication = authentication;
		this.identity = null;
		this.accessController = null;
	}

	/**
	 * <p>
	 * Sets the identity of the authenticated entity.
	 * </p>
	 * 
	 * <p>
	 * Following security context contract, the identity is only sets when the context is authenticated (see {@link #isAuthenticated() }).
	 * </p>
	 * 
	 * @param identity the identity to set
	 */
	public void setIdentity(A identity) {
		this.identity = this.isAuthenticated() ? identity : null;
	}

	/**
	 * <p>
	 * Sets the access controller for the authenticated entity.
	 * </p>
	 * 
	 * <p>
	 * Following security context contract, the access controller is only sets when the context is authenticated (see {@link #isAuthenticated() }).
	 * </p>
	 * 
	 * @param accessController the access controller to set
	 */
	public void setAccessController(B accessController) {
		this.accessController = this.isAuthenticated() ? accessController : null;
	}

	@Override
	public Authentication getAuthentication() {
		return this.authentication;
	}

	@Override
	public Optional<A> getIdentity() {
		return Optional.ofNullable(this.identity);
	}

	@Override
	public Optional<B> getAccessController() {
		return Optional.ofNullable(this.accessController);
	}
}
