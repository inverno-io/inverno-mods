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

/**
 * <p>
 * The Inverno framework session module provides general support to manage session in an application.
 * </p>
 *
 * <p>
 * A session is typically used to store information for a client accessing the application and make them available between requests. It is identified by a unique session id generated by the
 * application and passed to the client which must provide it on each request to the application so it can resolve back the session.
 * </p>
 *
 * <p>
 * A session is temporary and its data volatile, it has to be set to expire either after a specific period of inactivity or at a specific time in the future.
 * </p>
 *
 * <p>
 * The session API defines the general {@link io.inverno.mod.session.Session} interface for managing a session and its data in an application and the general
 * {@link io.inverno.mod.session.SessionStore} interface for specifying a session persistence layer. The {@link io.inverno.mod.session.SessionIdGenerator} interface is used to define strategies for
 * generating unique session identifier.
 * </p>
 *
 * <p>
 * {@link io.inverno.mod.session.jwt.JWTSession}, {@link io.inverno.mod.session.jwt.JWTSessionStore} and {@link io.inverno.mod.session.jwt.JWTSessionIdGenerator} extends above general API to support
 * stateless session data embedded in a JWT used as session identifier and as a result stored on the client side. Sessions are not fully stateless as they are still tacked in a session store and as
 * such can be expired or invalidated independently of the token.
 * </p>
 *
 * <p>
 * The module provides the following session store implementations:
 * </p>
 *
 * <ul>
 * <li>{@link io.inverno.mod.session.InMemoryBasicSessionStore} which stores session in a concurrent map in memory using opaque session identifiers.</li>
 * <li>{@link io.inverno.mod.session.RedisBasicSessionStore} which stores session in a Redis data store using opaque session identifiers.</li>
 * <li>{@link io.inverno.mod.session.jwt.InMemoryJWTSessionStore} which stores session in a concurrent map in memory using JWT session identifiers which may contain stateless session data.</li>
 * <li>{@link io.inverno.mod.session.jwt.RedisJWTSessionStore} which stores session in a Redis data store using JWT session identifiers which may contain stateless session data.</li>
 * </ul>
 *
 * <p>
 * The following shows how a basic session can be created, resolved and used in an application:
 * </p>
 *
 * <pre>{@code
 * public class SomeService {
 *
 *     public static class SessionData {
 *         AtomicInteger counter;
 *     }
 *
 *     private final SessionStore<SessionData, Session<SessionData>> sessionStore;
 *
 *     public SomeService(SessionStore<SessionData, Session<SessionData>> sessionStore) {
 *         this.sessionStore = sessionStore;
 *     }
 *
 *     public void someAction(String sessionId) {
 *         Mono.justOrEmpty(sessionId)
 *             .flatMap(this.sessionStore::get)
 *             .switchIfEmpty(sessionStore.create())
 *             .flatMap(session -> session.getData()
 *                 .doOnNext(sessionData -> System.out.println("This is request #" + sessionData.counter.incrementAndGet()))
 *                 .then(session.save())
 *             );
 *     }
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 */
module io.inverno.mod.session {
	requires io.inverno.mod.base;
	requires static io.inverno.mod.redis;
	requires static io.inverno.mod.security.jose;

	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	requires io.netty.common;
	requires com.fasterxml.jackson.databind;
	requires org.apache.commons.lang3;
	requires org.apache.logging.log4j;

	exports io.inverno.mod.session;
	exports io.inverno.mod.session.jwt;
	exports io.inverno.mod.session.internal.jwt;
}
