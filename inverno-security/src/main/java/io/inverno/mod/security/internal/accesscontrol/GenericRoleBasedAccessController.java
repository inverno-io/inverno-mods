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
import java.util.Arrays;
import java.util.Set;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericRoleBasedAccessController implements RoleBasedAccessController {

	private final Set<String> roles;

	public GenericRoleBasedAccessController(Set<String> roles) {
		this.roles = roles;
	}
	
	@Override
	public boolean hasRole(String role) {
		return roles.contains(role);
	}

	@Override
	public boolean hasAnyRole(String... roles) {
		return Arrays.stream(roles).anyMatch(this.roles::contains);
	}

	@Override
	public boolean hasAllRole(String... roles) {
		return this.roles.containsAll(Arrays.asList(roles));
	}
}
