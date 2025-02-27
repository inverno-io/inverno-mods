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
package io.inverno.mod.session.jwt;

import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.session.SessionStore;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session store that uses JWT session identifier to store session expiration setting and stateless session data on the frontend.
 * </p>
 *
 * <p>
 * Implementors should use JWT token identifiers (i.e. JTI) to identify sessions in the underlying data store.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the stateless session data type
 */
public interface JWTSessionStore<A, B> extends SessionStore<A, JWTSession<A, B>> {

	/**
	 * <p>
	 * Returns the data of the session identified by the specified JWT token identifier (i.e. JTI).
	 * </p>
	 *
	 * @param tokenId the JWT token identifier extracted from the JWT session id claims set
	 *
	 * @return a mono emitting the session data or an empty mono if no session exists with specified identifier or if a session exists which does not define any data
	 */
	Mono<A> getDataByTokenId(String tokenId);

	/**
	 * <p>
	 * Moves the session identified by the specified JWT token identifier to the specified JWT session identifier.
	 * </p>
	 *
	 * @param tokenId a JWT token identifier
	 * @param newSessionJWT a new JWT session identifier
	 *
	 * @return a mono for moving a session
	 *
	 * @throws IllegalStateException if there is no session with the specified identifier or if a session already exists at the specified new identifier
	 */
	Mono<Void> moveByTokenId(String tokenId, JOSEObject<JWTClaimsSet, ?> newSessionJWT) throws IllegalStateException;

	/**
	 * <p>
	 * Removes the session identified by the specified JWT token identifier (i.e. JTI).
	 * </p>
	 *
	 * @param tokenId the JWT token identifier extracted from the JWT session id claims set
	 *
	 * @return a mono for removing a session
	 */
	Mono<Void> removeByTokenId(String tokenId);
}
