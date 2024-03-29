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
package io.inverno.mod.security.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import java.util.Set;

/**
 * <p>
 * A specific authentication which exposes all groups to which the authenticated entity belongs.
 * </p>
 * 
 * <p>
 * This information can later be used to control access in an {@link AccessController}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see GroupsRoleBasedAccessControllerResolver
 */
public interface GroupAwareAuthentication extends Authentication {
	
	/**
	 * <p>
	 * Returns the groups to which the authenticated entity belongs to.
	 * </p>
	 * 
	 * @return a sets of groups
	 */
	@JsonProperty( "groups" )
	Set<String> getGroups();
}
