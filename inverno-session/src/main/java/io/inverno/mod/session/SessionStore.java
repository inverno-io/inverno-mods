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
package io.inverno.mod.session;

import reactor.core.publisher.Mono;

/**
 * <p>
 * A session store is used to create, get, save, move and remove session and its data uniquely identified by a session id in a data store.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the session type
 */
public interface SessionStore<A, B extends Session<A>> {

	/**
	 * <p>
	 * Creates a new session in the session store.
	 * </p>
	 *
	 * <p>
	 * This shall physically create a new session in the data store.
	 * </p>
	 *
	 * @return a mono for creating a session
	 */
	Mono<B> create();

	/**
	 * <p>
	 * Returns the session identified by the specified session id.
	 * </p>
	 *
	 * @param sessionId a session id
	 *
	 * @return a mono emitting the session or an empty mono if no session exists with the specified identifier
	 */
	Mono<B> get(String sessionId);

	/**
	 * <p>
	 * Returns the data of the session identified by the specified session id.
	 * </p>
	 *
	 * @param sessionId a session id
	 *
	 * @return a mono emitting the session data or an empty mono if no session exists with specified identifier or if a session exists which does not define any data
	 */
	Mono<A> getData(String sessionId);

	/**
	 * <p>
	 * Moves the session identified by the specified identifier to a new identifier.
	 * </p>
	 *
	 * @param sessionId    a session id
	 * @param newSessionId a new session id
	 *
	 * @return a mono for moving a session
	 *
	 * @throws IllegalStateException if there is no session with the specified identifier or if a session already exists at the specified new identifier
	 */
	Mono<Void> move(String sessionId, String newSessionId) throws IllegalStateException;

	/**
	 * <p>
	 * Removes the session identified by the specified identifier.
	 * </p>
	 *
	 * @param sessionId a session id
	 *
	 * @return a mono for removing a session
	 */
	Mono<Void> remove(String sessionId);

	/**
	 * <p>
	 * Saves the specified session.
	 * </p>
	 *
	 * <p>
	 * Whether resolved session data are saved along with the session is implementation specific. Implementors must however at least guarantee that data that is explicitly set on a session using
	 * {@link Session#setData(Object)} are saved.
	 * </p>
	 *
	 * @param session a session
	 *
	 * @return a mono for saving a session
	 *
	 * @throws IllegalArgumentException if the specified session does not originate from this session store
	 * @throws IllegalStateException    if the specified session does not exist in the store or if it was invalidated
	 */
	Mono<Void> save(B session) throws IllegalArgumentException, IllegalStateException;
}
