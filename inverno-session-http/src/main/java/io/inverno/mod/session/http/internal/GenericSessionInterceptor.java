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
package io.inverno.mod.session.http.internal;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.SessionStore;
import io.inverno.mod.session.http.SessionIdExtractor;
import io.inverno.mod.session.http.SessionInjector;
import io.inverno.mod.session.http.SessionInterceptor;
import io.inverno.mod.session.http.context.SessionContext;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link SessionInterceptor} implementation.
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
public class GenericSessionInterceptor<A, B extends Session<A>, C extends SessionContext.Intercepted<A, B>, D extends Exchange<C>> implements SessionInterceptor<A, B, C, D> {

	private static final Logger LOGGER = LogManager.getLogger(GenericSessionInterceptor.class);

	private final SessionIdExtractor<C, D> sessionIdExtractor;
	private final SessionStore<A, B> sessionStore;
	private final SessionInjector<A, B, C, D> sessionInjector;

	/**
	 * <p>
	 * Creates a generic session interceptor.
	 * </p>
	 *
	 * @param sessionIdExtractor a session identifier extractor
	 * @param sessionStore       a session store
	 * @param sessionInjector    a session injector
	 */
	public GenericSessionInterceptor(SessionIdExtractor<C, D> sessionIdExtractor, SessionStore<A, B> sessionStore, SessionInjector<A, B, C, D> sessionInjector) {
		this.sessionIdExtractor = sessionIdExtractor;
		this.sessionStore = sessionStore;
		this.sessionInjector = sessionInjector;
	}

	/**
	 * <p>
	 * Returns the session identifier extractor.
	 * </p>
	 *
	 * @return the session identifier extractor
	 */
	public SessionIdExtractor<C, D> getSessionIdExtractor() {
		return sessionIdExtractor;
	}

	/**
	 * <p>
	 * Returns the session store.
	 * </p>
	 *
	 * @return the session store
	 */
	public SessionStore<A, B> getSessionStore() {
		return sessionStore;
	}

	/**
	 * <p>
	 * Returns the session injector.
	 * </p>
	 *
	 * @return the session injector
	 */
	public SessionInjector<A, B, C, D> getSessionInjector() {
		return sessionInjector;
	}

	@Override
	public Mono<? extends D> intercept(D exchange) {
		AtomicReference<String> extractedSessionIdReference = new AtomicReference<>();
		return this.sessionIdExtractor.extract(exchange)
			.doOnNext(extractedSessionIdReference::set)
			.flatMap(this.sessionStore::get)
			.doOnSuccess(storedSession -> {
				AtomicReference<B> sessionReference = new AtomicReference<>();
				exchange.context().setSessionPresent(storedSession != null);
				exchange.context().setSession(Mono.fromSupplier(() -> storedSession == null || storedSession.isInvalidated() ? null : storedSession)
					.switchIfEmpty(this.sessionStore.create()
						.flatMap(newSession -> {
							exchange.context().setSessionPresent(true);
							return this.sessionInjector.inject(exchange, newSession).thenReturn(newSession);
						})
					)
					.doOnNext(sessionReference::set)
					.cacheInvalidateIf(Session::isInvalidated)
				);

				exchange.response().body()
					.before(Mono.defer(() -> {
						B session = sessionReference.get();
						if(session != null) {
							if(session.isInvalidated() && extractedSessionIdReference.get() != null) {
								return this.sessionInjector.remove(exchange);
							}
							return session.refreshId(false)
								.then(Mono.defer(() -> {
									if(!session.getId().equals(session.getOriginalId()) || !session.getId().equals(extractedSessionIdReference.get())) {
										return this.sessionInjector.inject(exchange, session);
									}
									return Mono.empty();
								}));
						}
						return Mono.empty();
					}))
					.after(Mono.defer(() -> {
						B session = sessionReference.get();
						if(session != null && !session.isInvalidated()) {
							String idBeforeSave = session.getId();
							return session.save().doOnSuccess(ign -> {
								if(!session.getId().equals(idBeforeSave)) {
									LOGGER.error("Session id has been refreshed after response");
								}
							});
						}
						return Mono.empty();
					}));
			})
			.thenReturn(exchange);
	}
}
