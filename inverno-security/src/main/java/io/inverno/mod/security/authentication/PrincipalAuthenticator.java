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

import io.inverno.mod.security.internal.authentication.GenericUserAuthentication;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class UserAuthenticator implements Authenticator<UserCredentials, UserAuthentication> {

	private final CredentialsResolver<UserCredentials> credentialsResolver;
	
	public UserAuthenticator(CredentialsResolver<UserCredentials> credentialsResolver) {
		this.credentialsResolver = credentialsResolver;
	}

	@Override
	public Mono<UserAuthentication> authenticate(UserCredentials credentials) throws AuthenticationException {
		return this.credentialsResolver
			.resolveCredentials(credentials.getUsername())
			.filter(resolvedCredentials -> resolvedCredentials.getPassword().equals(credentials.getPassword()))
			.map(resolvedCredentials -> (UserAuthentication)new GenericUserAuthentication(resolvedCredentials.getUsername(), resolvedCredentials.getGroups(), true))
			.switchIfEmpty(Mono.error(() -> new InvalidCredentialsException("Invalid password")));
	}
}
