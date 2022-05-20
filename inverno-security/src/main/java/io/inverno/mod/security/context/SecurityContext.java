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
import java.util.Optional;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface SecurityContext {

	static SecurityContext of(Authentication authentication) {
		return new GenericSecurityContext(authentication);
	}

	static SecurityContext of(Authentication authentication, Identity identity) {
		GenericSecurityContext context = new GenericSecurityContext(authentication);
		context.setIdentity(identity);
		return context;
	}

	static SecurityContext of(Authentication authentication, AccessController accessController) {
		GenericSecurityContext context = new GenericSecurityContext(authentication);
		context.setAccessControl(accessController);
		return context;
	}

	static SecurityContext of(Authentication authentication, Identity identity, AccessController accessController) {
		GenericSecurityContext context = new GenericSecurityContext(authentication);
		context.setIdentity(identity);
		context.setAccessControl(accessController);
		return context;
	}

	static SecurityContext of(Authentication authentication, Optional<Identity> identity, Optional<AccessController> accessController) {
		GenericSecurityContext context = new GenericSecurityContext(authentication);
		context.setIdentity(identity);
		context.setAccessControl(accessController);
		return context;
	}

	default boolean isAuthenticated() {
		return this.getAuthentication().isAuthenticated();
	}

	default Authentication getAuthentication() { 
		return Authentication.anonymous();
	}

	default Optional<Identity> getIdentity() {
		return Optional.empty();
	}

	default Optional<AccessController> getAccessController() {
		return Optional.empty();
	}
}
