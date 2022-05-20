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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.inverno.mod.security.internal.authentication.GenericUserAuthentication;
import java.util.Set;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@JsonDeserialize( as = GenericUserAuthentication.class )
public interface UserAuthentication extends Authentication {
	
	/**
	 * <p>
	 * Returns the user name.
	 * </p>
	 * 
	 * @return a user name.
	 */
	@JsonProperty( "username" )
	String getUsername();
	
	/**
	 * <p>
	 * Returns the groups the user belongs to.
	 * </p>
	 * 
	 * @return a list of groups
	 */
	Set<String> getGroups();
}
