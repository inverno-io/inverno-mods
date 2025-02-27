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
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An authenticator that simply returns the authentication resolved from a session.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 */
public class SessionAuthenticator<A extends Authentication> implements Authenticator<SessionCredentials<A>, A> {

	@Override
	public Mono<A> authenticate(SessionCredentials<A> credentials) throws AuthenticationException {
		return Mono.justOrEmpty(credentials.getAuthentication());
	}
}
