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

import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Username/password credentials.
 * </p>
 * 
 * TODO We could include the realm here as well in order to be able to use multiple Credentialsresolver per realm.
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class UserCredentials implements Credentials {

	private final String username;

	private final String password;
	
	private final Set<String> groups;

	public UserCredentials(String username, String password) throws InvalidCredentialsException {
		this(username, password, Set.of());
	}
	
	public UserCredentials(String username, String password, Set<String> groups) throws InvalidCredentialsException {
		if(StringUtils.isBlank(username)) {
			throw new InvalidCredentialsException("Username is blank");
		}
		if(StringUtils.isBlank(password)) {
			throw new InvalidCredentialsException("Password is blank");
		}
		this.username = username;
		this.password = password;
		this.groups = groups;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	
	public Set<String> getGroups() {
		return groups;
	}
}
