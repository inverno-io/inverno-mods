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

import io.inverno.mod.security.internal.accesscontrol.GenericRoleBasedAccessController;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An access controller that uses roles to control the access to services or resources based on the permissions that were granted to an authenticated entity.
 * </p>
 * 
 * <p>
 * This basically follows the <a href="https://en.wikipedia.org/wiki/Role-based_access_control">Role-based access control</a> approach which control the access to services or resources based on roles
 * assigned to the authenticated entity.
 * </p>
 * 
 * <p>
 * Access to a service or a resource is granted when the authenticated entity has a particular role. This can be checked as follows:
 * </p>
 * 
 * <pre>{@code
 *     RoleBasedAccessController accessController = ...
 *     accessController.hasRole("admin").doOnNext(granted -> {...})...    }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface RoleBasedAccessController extends AccessController {
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has the specified role.
	 * </p>
	 * 
	 * @param role the role to evaluate
	 * 
	 * @return a mono emitting true if access is granted, false otherwise
	 */
	Mono<Boolean> hasRole(String role);
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified roles.
	 * </p>
	 * 
	 * @param roles the array of role to evaluate
	 * 
	 * @return a mono emitting true if access is granted, false otherwise
	 */
	default Mono<Boolean> hasAnyRole(String... roles) {
		return this.hasAnyRole(roles != null ? Arrays.asList(roles) : List.of());
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has any of the specified roles.
	 * </p>
	 * 
	 * @param roles the collection of role to evaluate
	 * 
	 * @return a mono emitting true if access is granted, false otherwise
	 */
	Mono<Boolean> hasAnyRole(Collection<String> roles);
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all of the specified roles.
	 * </p>
	 * 
	 * @param roles the array of role to evaluate
	 * 
	 * @return a mono emitting true if access is granted, false otherwise
	 */
	default Mono<Boolean> hasAllRoles(String... roles) {
		return this.hasAllRoles(roles != null ? Arrays.asList(roles) : List.of());
	}
	
	/**
	 * <p>
	 * Determines whether the authenticated entity has all of the specified roles.
	 * </p>
	 * 
	 * @param roles the collection of role to evaluate
	 * 
	 * @return a mono emitting true if access is granted, false otherwise
	 */
	Mono<Boolean> hasAllRoles(Collection<String> roles);
	
	/**
	 * <p>
	 * Creates a new role based access controller with the specified roles.
	 * </p>
	 * 
	 * @param roles the array of roles associated with an authenticated entity
	 * 
	 * @return a new role based access controller
	 */
	static RoleBasedAccessController of(String... roles) {
		return new GenericRoleBasedAccessController(Arrays.stream(roles).filter(Objects::nonNull).collect(Collectors.toSet()));
	}
	
	/**
	 * <p>
	 * Creates a new role based access controller with the specified roles.
	 * </p>
	 * 
	 * @param roles the collection of roles associated with an authenticated entity
	 * 
	 * @return a new role based access controller
	 */
	static RoleBasedAccessController of(Collection<String> roles) {
		return new GenericRoleBasedAccessController(roles.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
	}
}
