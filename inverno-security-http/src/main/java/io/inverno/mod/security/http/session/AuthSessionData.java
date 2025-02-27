/*
 * Copyright 2025 Jeremy KUHN
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
package io.inverno.mod.security.http.session;

import io.inverno.mod.security.authentication.Authentication;

/**
 * <p>
 * A session data type for storing authentication data.
 * </p>
 *
 * <p>
 * A session data type must implements {@code AuthSessionData} in order to support basic session authentication where the successful authentication is stored in the session.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @see BasicSessionLoginSuccessHandler
 *
 * @param <A> the authentication type
 */
public interface AuthSessionData<A extends Authentication> {

	/**
	 * <p>
	 * Returns the authentication stored in the session.
	 * </p>
	 *
	 * @return an authentication or null
	 */
	A getAuthentication();

	/**
	 * <p>
	 * Sets the authentication in the session
	 * </p>
	 *
	 * @param authentication the authentication
	 */
	void setAuthentication(A authentication);
}
