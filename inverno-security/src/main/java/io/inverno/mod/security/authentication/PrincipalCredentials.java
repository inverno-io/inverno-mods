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

/**
 * <p>
 * Credentials used to authenticate a principal entity identified by a unique username.
 * </p>
 *
 * <p>
 * This is a base type for representing principal credentials that do not presume of any particular authentication method (e.g. password, 2FA, biometric...).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface PrincipalCredentials extends Credentials {
	
	/**
	 * <p>
	 * Returns the unique username associated to the entity.
	 * </p>
	 * 
	 * @return a username
	 */
	String getUsername();
}
