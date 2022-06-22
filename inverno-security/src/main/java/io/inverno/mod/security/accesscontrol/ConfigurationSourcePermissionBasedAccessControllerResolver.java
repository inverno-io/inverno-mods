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

import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.security.authentication.GroupAwareAuthentication;
import io.inverno.mod.security.authentication.PrincipalAuthentication;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Resolves a configuration source permission based access controller from a {@link PrincipalAuthentication}.
 * </p>
 * 
 * <p>
 * If the authentication is also a {@link GroupAwareAuthentication}, the groups the authenticated entity is in are used in the resulting access controller. Using roles allows to specifies permissions
 * to roles assigned to entities in order to facilitate access control management.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class ConfigurationSourcePermissionBasedAccessControllerResolver implements AccessControllerResolver<PrincipalAuthentication, PermissionBasedAccessController> {

	private final ConfigurationSource<?, ?, ?> configurationSource;
	private final String rolePrefix;
	
	/**
	 * <p>
	 * Creates permission based access controller resolver with the specified configuration source.
	 * </p>
	 * 
	 * @param configurationSource a configuration source
	 */
	public ConfigurationSourcePermissionBasedAccessControllerResolver(ConfigurationSource<?, ?, ?> configurationSource) {
		this(configurationSource, null);
	}
	
	/**
	 * <p>
	 * Creates permission based access controller resolver with the specified configuration source.
	 * </p>
	 *
	 * @param configurationSource a configuration source
	 * @param rolePrefix          the prefix to prepend to a role when resolving role permissions
	 */
	public ConfigurationSourcePermissionBasedAccessControllerResolver(ConfigurationSource<?, ?, ?> configurationSource, String rolePrefix) {
		this.configurationSource = configurationSource;
		this.rolePrefix = rolePrefix;
	}
	
	@Override
	public Mono<PermissionBasedAccessController> resolveAccessController(PrincipalAuthentication authentication) throws AccessControlException {
		return Mono.justOrEmpty(authentication)
			.map(auth -> new ConfigurationSourcePermissionBasedAccessController(
				this.configurationSource, 
				auth.getUsername(), 
				auth instanceof GroupAwareAuthentication ? ((GroupAwareAuthentication)auth).getGroups() : null, 
				this.rolePrefix)
			);
	}
}
