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
 * An authentication which comprises a token that uniquely and securely identifies the authentication.
 * </p>
 * 
 * <p>
 * An {@link Authenticator} might respond with a token authentication to communicate temporary credentials resulting from the authentication to the authenticated entity. These {@link TokenCredentials}
 * can then later be used by the entity to authenticate using a different channel in a simpler or more performant way than the original authentication.
 * </p>
 * 
 * <p>
 * A properly secured token should be collision-free and hardly forgeable.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see TokenCredentials
 */
public interface TokenAuthentication extends Authentication {
	
	/**
	 * <p>
	 * Returns the token resulting from the authentication process.
	 * </p>
	 * 
	 * @return a token that securely identifies the authentication
	 */
	String getToken();
}
