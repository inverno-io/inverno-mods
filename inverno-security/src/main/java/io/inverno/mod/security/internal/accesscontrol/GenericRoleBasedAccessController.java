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
package io.inverno.mod.security.internal.accesscontrol;

import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import java.util.Collection;
import java.util.Set;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link RoleBasedAccessController} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericRoleBasedAccessController implements RoleBasedAccessController {

	/**
	 * The set of roles.
	 */
	private final Set<String> roles;

	/**
	 * <p>
	 * Creates a role-based access controller with the specified set of roles.
	 * </p>
	 * 
	 * @param roles a set of roles
	 */
	public GenericRoleBasedAccessController(Set<String> roles) {
		this.roles = roles;
	}

	@Override
	public Mono<Boolean> hasRole(String role) {
		return Mono.just(this.roles.contains(role));
	}

	@Override
	public Mono<Boolean> hasAnyRole(Collection<String> roles) {
		return Mono.just(roles.stream().anyMatch(this.roles::contains));
	}

	@Override
	public Mono<Boolean> hasAllRoles(Collection<String> roles) {
		return Mono.just(this.roles.containsAll(roles));
	}
}
