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
package io.inverno.mod.session.http.context;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.session.Session;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * <p>
 * The general session context which exposes the session in the exchange context.
 * </p>
 *
 * <p>
 * A session may or may not exist when a request is received, {@link #isSessionPresent()} can be used to determine whether a session is present in the context, in any case {@link #getSession()}
 * returns either the existing session or creates a new one.
 * </p>
 *
 * <p>
 * {@link #getSessionData()} and {@link #getSessionData(Supplier)} are shortcuts for accessing session data which can be created on the fly when missing using the provided supplier.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the session type
 */
public interface SessionContext<A, B extends Session<A>> extends ExchangeContext {

	/**
	 * <p>
	 * Determines whether a session is present in the context.
	 * </p>
	 *
	 * @return true if a session is present, false otherwise
	 */
	boolean isSessionPresent();

	/**
	 * <p>
	 * Resolves or creates the session.
	 * </p>
	 *
	 * @return a mono emitting the existing session if one has been resolved from the request or a mono emitting a new session
	 */
	Mono<B> getSession();

	/**
	 * <p>
	 * Resolves the session data.
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for {@code context.getSession().flatMap(Session::getData)} and as a result a new session is created when if none is present
	 * </p>
	 *
	 * @return a mono resolving session data or an empty mono if none exist in the session
	 */
	default Mono<A> getSessionData() {
		return this.getSession().flatMap(Session::getData);
	}

	/**
	 * <p>
	 * Resolves the session data or creates them using the specified supplier if none exist in the session.
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for {@code context.getSession().flatMap(session -> session.getData(supplier))} and as a result a new session is created when if none is present.
	 * </p>
	 *
	 * @param supplier a session data supplier
	 *
	 * @return a mono resolving or creating session data
	 */
	default Mono<A> getSessionData(Supplier<A> supplier) {
		return this.getSession().flatMap(session -> session.getData(supplier));
	}

	/**
	 * <p>
	 * An intercepted session context used by the session interceptor to populate the session context.
	 * </p>
	 *
	 * <p>
	 * It should only be considered when configuring the session interceptor which must be the only one allowed to set the session context. Other interceptors or handlers should always use the
	 * {@link SessionContext}.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 * @param <B> the session type
	 */
	interface Intercepted<A, B extends Session<A>> extends SessionContext<A, B> {

		/**
		 * <p>
		 * Sets whether the session has been resolved or not.
		 * </p>
		 *
		 * @param sessionPresent true if the session has been resolved, false otherwise
		 */
		void setSessionPresent(boolean sessionPresent);

		/**
		 * <p>
		 * Sets the session publisher in the context which returns the resolved session or creates a new one if none were present.
		 * </p>
		 *
		 * @param session a session publisher
		 */
		void setSession(Mono<B> session);
	}
}
