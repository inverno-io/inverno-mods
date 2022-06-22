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

import io.inverno.mod.security.authentication.GroupAwareAuthentication;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Resolves a role based access controller from a {@link GroupAwareAuthentication}.
 * </p>
 *
 * <p>
 * It basically considers the groups to which the authenticated entity is in as roles in order to create the resulting role based access controller.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see RoleBasedAccessController
 */
public class GroupsRoleBasedAccessControllerResolver implements AccessControllerResolver<GroupAwareAuthentication, RoleBasedAccessController> {

	@Override
	public Mono<RoleBasedAccessController> resolveAccessController(GroupAwareAuthentication authentication) {
		return Mono.justOrEmpty(authentication)
			.map(auth -> RoleBasedAccessController.of(auth.getGroups()));
	}
}
