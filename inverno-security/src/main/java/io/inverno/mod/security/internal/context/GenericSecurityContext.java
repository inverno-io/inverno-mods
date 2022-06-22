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
import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
class GenericSecurityContext implements SecurityContext {

	private final Authentication authentication;
	private Optional<Identity> identity;
	private Optional<AccessController> accessController;

	public GenericSecurityContext(Authentication authentication) {
		Objects.requireNonNull(authentication);
		this.authentication = authentication;
	}

	public void setIdentity(Identity identity) {
		this.identity = Optional.ofNullable(identity);
	}

	public void setIdentity(Optional<Identity> identity) {
		this.identity = identity;
	}

	public void setAccessControl(AccessController accessController) {
		this.accessController = Optional.ofNullable(accessController);
	}

	public void setAccessControl(Optional<AccessController> accessController) {
		this.accessController = accessController;
	}

	@Override
	public boolean isAuthenticated() {
		return this.authentication.isAuthenticated();
	}

	@Override
	public Authentication getAuthentication() {
		return this.authentication;
	}

	@Override
	public Optional<Identity> getIdentity() {
		return this.identity;
	}

	@Override
	public Optional<AccessController> getAccessController() {
		return this.accessController;
	}
}
