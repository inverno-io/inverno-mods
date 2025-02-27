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
package io.inverno.mod.session.http;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.SessionStore;
import io.inverno.mod.session.http.context.SessionContext;
import io.inverno.mod.session.http.internal.GenericSessionInterceptor;
import reactor.core.publisher.Mono;

/**
 * <p>
 * The session interceptor extracts the session identifier send by a requester, resolves the session, populates the session context in the exchange, injects the session into the exchange and saves the
 * session after the request has been processed.
 * </p>
 *
 * <p>
 * It typically executes session related operations at various steps during the processing of an exchange:
 * </p>
 *
 * <ul>
 * <li>Before the exchange handler is invoked: it tries to extract the session identifier using a {@link SessionIdExtractor} and resolve the session and eventually populates the {@link SessionContext}.</li>
 * <li>When a session is created: it injects the session in the exchange using a {@link SessionInjector}.</li>
 * <li>Before sending the response: it injects the session in the exchange if the session identifier needed to be refreshed, or it removes the session from the exchange if the session has been
 * invalidated.</li>
 * <li>After the response was sent: it saves the session if it hasn't been invalidated.</li>
 * </ul>
 *
 * <p>
 * It is important to keep above sequence in mind when interacting with the session. For instance, changes that would result in a stale session identifier must be performed before sending the response
 * so this cannot happen in the response body publisher and must be done by invoking {@link io.inverno.mod.http.server.ResponseBody#before(Mono)}. Otherwise, the session identifier is eventually
 * refreshed when the session is saved, but the client might not be able to see it because the session is only injected before sending the response since it is not possible to set response headers
 * after.
 * </p>
 *
 * <p>
 * The session is only saved after the response has been completely sent which means it is possible to update session data while sending the response payload. It also means that any change is only
 * persisted after the exchange has been processed successfully, it can hopefully be saved explicitly before that.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the session type
 * @param <C> the exchange context type
 * @param <D> the exchange type
 */
public interface SessionInterceptor<A, B extends Session<A>, C extends SessionContext.Intercepted<A, B>, D extends Exchange<C>> extends ExchangeInterceptor<C, D> {

	/**
	 * <p>
	 * Creates a session interceptor with the specified session identifier extractor, session store and session injector.
	 * </p>
	 *
	 * @param <A>                the session data type
	 * @param <B>                the session type
	 * @param <C>                the exchange context type
	 * @param <D>                the exchange type
	 * @param sessionIdExtractor the session identifier extractor
	 * @param sessionStore       the session store
	 * @param sessionInjector    the session injector
	 *
	 * @return a new session interceptor
	 */
	static <A, B extends Session<A>, C extends SessionContext.Intercepted<A, B>, D extends Exchange<C>> SessionInterceptor<A, B, C, D> of(SessionIdExtractor<C, D> sessionIdExtractor, SessionStore<A, B> sessionStore, SessionInjector<A, B, C, D> sessionInjector) {
		return new GenericSessionInterceptor<>(sessionIdExtractor, sessionStore, sessionInjector);
	}
}
