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
import io.inverno.mod.security.authentication.Credentials;

/**
 * <p>
 * Credentials wrapping an authentication resolved from a session.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the authentication type
 */
public class SessionCredentials<A extends Authentication> implements Credentials {

	private final A authentication;

	/**
	 * <p>
	 * Creates session credentials containing the specified authentication.
	 * </p>
	 *
	 * @param authentication the authentication
	 */
	public SessionCredentials(A authentication) {
		this.authentication = authentication;
	}

	/**
	 * <p>
	 * Returns the authentication.
	 * </p>
	 *
	 * @return the authentication
	 */
	public A getAuthentication() {
		return authentication;
	}
}
