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

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.DefaultingStrategy;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Permission based access controller implementation using a {@link ConfigurationSource} to resolve permissions.
 * </p>
 *
 * <p>
 * Configuration source allows accessing parameterized properties with or without defaulting support which is particularly suited to defined access permissions and fits the
 * {@link PermissionBasedAccessController} contract. In the configuration source permissions must be defined in a comma separated list of strings as property value using the username as property name
 * and permissions parameters as configuration parameters.
 * </p>
 * 
 * <p>
 * A permission is defined as a string using the following rules:
 * </p>
 * 
 * <ul>
 * <li>{@code [permission]} to indicates that {@code permission} is granted</li>
 * <li>{@code ![permission]} to indicates that {@code permission} is not granted</li>
 * <li>{@code *} to indicate that all permissions are granted</li>
 * </ul>
 * 
 * <p>
 * A permission is then granted when it is present in a set of granted permission or when all permissions have been granted using a wildcard and if it hasn't been explicitly taken out using the
 * {@code !...} notation (e.g. {@code *,!admin} grants all permissions but {@code admin}).
 * </p>
 *
 * <p>
 * Permissions can be parameterized by defining multiple values with different combinations of configuration parameters. The defaulting strategy specified on the configuration source specifies
 * parameters prioritization and the defaulting behaviour when no property was defined for a particular combination of parameters.
 * </p>
 * 
 * <p>
 * This implementation can also uses user roles when applicable. In that context a role is used to group permissions by role which are then assigned to users in order to facilitate access control
 * management. When evaluating a permission the controller first tries to evaluate it in the context of the user which basically overrides permissions defined by role, in case the evalutation was
 * unsuccessful it then tries in the context of each role, using the role instead of the username as configuration key, the process stops when the evaluated permission is found or when all roles have
 * been scanned without success.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @see ConfigurationSource
 * @see DefaultingStrategy
 */
public class ConfigurationSourcePermissionBasedAccessController implements PermissionBasedAccessController {

	/**
	 * The default prefix prepended to role when resolving role permissions.
	 */
	public static final String DEFAULT_ROLE_PREFIX = "ROLE_";
	
	private final ConfigurationSource<?, ?, ?> configurationSource;
	private final String username;
	private final Set<String> roles;
	private final String rolePrefix;

	/**
	 * <p>
	 * Creates a permission based access controller backed by a configuration source for the specified username.
	 * </p>
	 *
	 * @param configurationSource a configuration source
	 * @param username            the username
	 */
	public ConfigurationSourcePermissionBasedAccessController(ConfigurationSource<?, ?, ?> configurationSource, String username) {
		this(configurationSource, username, null, DEFAULT_ROLE_PREFIX);
	}
	
	/**
	 * <p>
	 * Creates a permission based access controller backed by a configuration source for the specified username and set of roles.
	 * </p>
	 *
	 * @param configurationSource a configuration source
	 * @param username            a username
	 * @param roles               a set of roles
	 */
	public ConfigurationSourcePermissionBasedAccessController(ConfigurationSource<?, ?, ?> configurationSource, String username, Set<String> roles) {
		this(configurationSource, username, roles, DEFAULT_ROLE_PREFIX);
	}

	/**
	 * <p>
	 * Creates a permission based access controller backed by a configuration source for the specified username and set of roles.
	 * </p>
	 *
	 * @param configurationSource a configuration source
	 * @param username            a username
	 * @param roles               a set of roles
	 * @param rolePrefix          the prefix to prepend to a role when resolving role permissions
	 */
	public ConfigurationSourcePermissionBasedAccessController(ConfigurationSource<?, ?, ?> configurationSource, String username, Set<String> roles, String rolePrefix) {
		Objects.requireNonNull(configurationSource);
		Objects.requireNonNull(username);
		this.configurationSource = configurationSource;
		this.username = username;
		this.rolePrefix = rolePrefix != null ? rolePrefix : DEFAULT_ROLE_PREFIX;
		this.roles = roles != null ? roles.stream().map(role -> this.rolePrefix + role).collect(Collectors.toSet()) : Set.of();
	}
	
	/**
	 * <p>
	 * Returns the prefix prepended to the role when resolving role permissions.
	 * </p>
	 * 
	 * @return a prefix
	 */
	public final String getRolePrefix() {
		return rolePrefix;
	}
	
	/**
	 * <p>
	 * Determines whether the specified permission is granted considering the specified set of resolved permissions.
	 * </p>
	 * 
	 * <p>
	 * This basically checks the rules listed in the class documentation.
	 * </p>
	 * 
	 * @param permission          the permission to evaluate
	 * @param resolvedPermissions a set of permissions
	 *
	 * @return true if the permissions is granted, false otherwise
	 */
	private boolean hasPermission(String permission, Set<String> resolvedPermissions) {
		return resolvedPermissions != null && ((resolvedPermissions.contains(permission) || resolvedPermissions.contains("*")) && !resolvedPermissions.contains("!" + permission));
	}
	
	/**
	 * <p>
	 * Determines whether any permission is granted in the specified set of permissions considering the specified set of resolved permissions.
	 * </p>
	 * 
	 * <p>
	 * This basically checks the rules listed in the class documentation.
	 * </p>
	 * 
	 * @param permissions         the set of permissions to evaluate
	 * @param resolvedPermissions a set of permissions
	 *
	 * @return true if any permission is granted, false otherwise
	 */
	private boolean hasAnyPermission(Set<String> permissions, Set<String> resolvedPermissions) {
		return permissions.stream().anyMatch(permission -> this.hasPermission(permission, resolvedPermissions));
	}
	
	/**
	 * <p>
	 * Determines whether all permission in the specified set of permissions are granted considering the specified set of resolved permissions.
	 * </p>
	 * 
	 * <p>
	 * This basically checks the rules listed in the class documentation.
	 * </p>
	 * 
	 * @param permissions         the set of permissions to evaluate
	 * @param resolvedPermissions a set of permissions
	 *
	 * @return true if any permission is granted, false otherwise
	 */
	private boolean hasAllPermissions(Set<String> permissions, Set<String> resolvedPermissions) {
		return permissions.stream().allMatch(permission -> this.hasPermission(permission, resolvedPermissions));
	}
	
	/**
	 * <p>
	 * Resolves the permissions for the specified username or role and set of parameters.
	 * </p>
	 *
	 * @param usernameOrRole a username or a role
	 * @param parameters     a set of parameters
	 *
	 * @return a mono emitting the resolved permissions or an empty mono
	 */
	private Mono<Set<String>> resolvePermissions(String usernameOrRole, List<ConfigurationKey.Parameter> parameters) {
		return this.configurationSource
			.get(usernameOrRole).withParameters(parameters.stream().map(parameter -> ConfigurationKey.Parameter.of(parameter.getKey(), parameter.getValue())).collect(Collectors.toList()))
			.execute()
			.single()
			.mapNotNull(result -> result.getResult().flatMap(property -> property.asSetOf(String.class)).orElse(null));
	}
	
	@Override
	public Mono<Boolean> hasPermission(String permission, List<Parameter> parameters) {
		List<ConfigurationKey.Parameter> configurationParameters = parameters.stream().map(parameter -> ConfigurationKey.Parameter.of(parameter.getKey(), parameter.getValue())).collect(Collectors.toList());
		return this.resolvePermissions(this.username, configurationParameters)
			.map(userPermissions -> this.hasPermission(permission, userPermissions))
			.switchIfEmpty(Flux.fromIterable(this.roles)
				.concatMap(role -> this.resolvePermissions(role, configurationParameters))
				.map(rolePermissions -> this.hasPermission(permission, rolePermissions))
				.filter(granted -> granted)
				.next()
			)
			.switchIfEmpty(Mono.just(false));
	}

	@Override
	public Mono<Boolean> hasAnyPermission(Set<String> permissions, List<Parameter> parameters) {
		List<ConfigurationKey.Parameter> configurationParameters = parameters.stream().map(parameter -> ConfigurationKey.Parameter.of(parameter.getKey(), parameter.getValue())).collect(Collectors.toList());
		return this.resolvePermissions(this.username, configurationParameters)
			.map(userPermissions -> this.hasAnyPermission(permissions, userPermissions))
			.switchIfEmpty(Flux.fromIterable(this.roles)
				.concatMap(role -> this.resolvePermissions(role, configurationParameters))
				.map(rolePermissions -> this.hasAnyPermission(permissions, rolePermissions))
				.filter(granted -> granted)
				.next()
			)
			.switchIfEmpty(Mono.just(false));
	}

	@Override
	public Mono<Boolean> hasAllPermissions(Set<String> permissions, List<Parameter> parameters) {
		List<ConfigurationKey.Parameter> configurationParameters = parameters.stream().map(parameter -> ConfigurationKey.Parameter.of(parameter.getKey(), parameter.getValue())).collect(Collectors.toList());
		return this.resolvePermissions(this.username, configurationParameters)
			.map(userPermissions -> this.hasAllPermissions(permissions, userPermissions))
			.switchIfEmpty(Flux.fromIterable(this.roles)
				.concatMap(role -> this.resolvePermissions(role, configurationParameters))
				.map(rolePermissions -> this.hasAllPermissions(permissions, rolePermissions))
				.filter(granted -> granted)
				.next()
			)
			.switchIfEmpty(Mono.just(false));
	}
}
