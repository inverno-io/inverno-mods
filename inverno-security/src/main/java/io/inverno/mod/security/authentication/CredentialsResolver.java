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

import io.inverno.mod.security.authentication.user.UserAuthenticator;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A credentials resolver is used to resolve credentials identified by a unique identifier from a trusted source.
 * </p>
 * 
 * <p>
 * An {@link Authenticator} implementation can rely on a credentials resolver to resolve trusted credentials and compare them to the credentials provided by an entity using a
 * {@link CredentialsMatcher} during the authentication process. As a result, resolved credentials must come from trusted secured sources.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see PrincipalAuthenticator
 * @see UserAuthenticator
 * 
 * @param <A> The type of credentials
 */
@FunctionalInterface
public interface CredentialsResolver<A extends Credentials> {

	/**
	 * <p>
	 * Returns trusted credentials for the specified identifier.
	 * </p>
	 * 
	 * @param id the identifier of the credentials to resolve
	 * 
	 * @return a mono emitting the credentials or an empty mono if no credentials exist with the specified identifier
	 * 
	 * @throws AuthenticationException if there was an error during the resolution of credentials
	 */
	Mono<A> resolveCredentials(String id) throws AuthenticationException;
	
}
