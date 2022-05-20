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
package io.inverno.mod.security.accesscontrol;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import io.inverno.mod.security.internal.accesscontrol.GenericRoleBasedAccessController;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface RoleBasedAccessController extends AccessController {
	
	boolean hasRole(String role);
	
	boolean hasAnyRole(String... roles);
	
	boolean hasAllRole(String... roles);
	
	public static RoleBasedAccessController of(String... roles) {
		return new GenericRoleBasedAccessController(Arrays.stream(roles).filter(Objects::nonNull).collect(Collectors.toSet()));
	}
	
	public static RoleBasedAccessController of(Collection<String> roles) {
		return new GenericRoleBasedAccessController(roles.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
	}
}
