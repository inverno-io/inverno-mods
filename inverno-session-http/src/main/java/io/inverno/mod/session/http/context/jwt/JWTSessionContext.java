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
package io.inverno.mod.session.http.context.jwt;

import io.inverno.mod.session.http.context.SessionContext;
import io.inverno.mod.session.jwt.JWTSession;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session context for exposing basic sessions.
 * </p>
 *
 * <p>
 * It basically fixes the session type to {@link JWTSession} in order to simplify configuration in an application that relies on JWT sessions to be able to use stateless session data.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the stateless session data type
 */
public interface JWTSessionContext<A, B> extends SessionContext<A, JWTSession<A, B>> {

	/**
	 * <p>
	 * Returns the stateless session data.
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for {@code context.getSession().flatMap(JWTSession::getStatelessData)} and as a result a new session is created when if none is present.
	 * </p>
	 *
	 * @return a mono emitting stateless session data or an empty mono if none exist in the session or if a new session was created
	 */
	default Mono<B> getStatelessSessionData() {
		return this.getSession().mapNotNull(JWTSession::getStatelessData);
	}

	/**
	 * <p>
	 * Returns the stateless session data or creates them using the specified supplier if none exist in the session.
	 * </p>
	 *
	 * @param supplier a stateless session data supplier
	 *
	 * @return a mono emitting stateless session data
	 */
	default Mono<B> getStatelessSessionData(Supplier<B> supplier) {
		return this.getSession().map(session -> session.getStatelessData(supplier));
	}

	/**
	 * <p>
	 * An intercepted JWT session context used by the session interceptor to populate the session context with a JWT session.
	 * </p>
	 *
	 * <p>
	 * As for the {@link SessionContext.Intercepted}, it should only be considered when configuring JWT session support.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 * @param <B> the stateless session data type
	 */
	interface Intercepted<A, B> extends JWTSessionContext<A, B>, SessionContext.Intercepted<A, JWTSession<A, B>> {

	}
}
