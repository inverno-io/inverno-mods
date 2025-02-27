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

import io.inverno.mod.base.concurrent.Reactor;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A basic session store implementation that stores sessions and their data in-memory in a concurrent map.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 */
public class InMemoryBasicSessionStore<A> extends AbstractSessionStore<A, Session<A>> implements BasicSessionStore<A> {

	/**
	 * The default session cleaning period in milliseconds: {@code 300000} (i.e. 5 minutes).
	 */
	public static final long DEFAULT_CLEAN_PERIOD = 300000L;

	private final EventLoop eventLoop;
	private final long cleanPeriod;
	private final Map<String, InMemorySession<A>> sessions;

	private ScheduledFuture<?> cleanFuture;

	/**
	 * <p>
	 * Creates an in-memory basic session store.
	 * </p>
	 *
	 * @param sessionIdGenerator  a session id generator
	 * @param maxInactiveInterval the initial maximum inactive interval in milliseconds
	 * @param expireAfterPeriod   the period in milliseconds after which a new session must expire
	 * @param reactor             the reactor
	 * @param cleanPeriod         the cleaning period
	 *
	 * @throws IllegalArgumentException if both maximum inactive interval and expire after period are null
	 */
	private InMemoryBasicSessionStore(SessionIdGenerator<A, Session<A>> sessionIdGenerator, Long maxInactiveInterval, Long expireAfterPeriod, Reactor reactor, long cleanPeriod) throws IllegalArgumentException{
		super(sessionIdGenerator, maxInactiveInterval, expireAfterPeriod);
		this.eventLoop = reactor.getEventLoop();
		this.cleanPeriod = cleanPeriod;
		this.sessions = new ConcurrentHashMap<>();
	}

	/**
	 * <p>
	 * Removes expired or invalidated session from the store and schedules the next cleaning operation.
	 * </p>
	 */
	private void clean() {
		try {
			for(Session<A> session : this.sessions.values()) {
				this.sessions.computeIfPresent(session.getId(), (ign, value) -> {
					if(value.isExpired() || value.isInvalidated()) {
						return null;
					}
					return value;
				});
			}
		}
		finally {
			this.cleanFuture = null;
			this.scheduleClean();
		}
	}

	/**
	 * <p>
	 * Schedules a cleaning operation if sessions exist in the store.
	 * </p>
	 */
	private void scheduleClean() {
		if(this.cleanFuture == null && !this.sessions.isEmpty()) {
			this.cleanFuture = this.eventLoop.schedule(this::clean, this.cleanPeriod, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * <p>
	 * Creates an in-memory basic session store builder using {@link SessionIdGenerator#uuid()} session id generator.
	 * </p>
	 *
	 * @param <A>                the session data type
	 * @param reactor            the reactor
	 *
	 * @return an in-memory basic session store builder
	 */
	public static <A> InMemoryBasicSessionStore.Builder<A> builder(Reactor reactor) {
		return new InMemoryBasicSessionStore.Builder<>(SessionIdGenerator.uuid(), reactor);
	}

	/**
	 * <p>
	 * Creates an in-memory basic session store builder.
	 * </p>
	 *
	 * @param <A>                the session data type
	 * @param reactor            the reactor
	 * @param sessionIdGenerator a session id generator
	 *
	 * @return an in-memory basic session store builder
	 */
	public static <A> InMemoryBasicSessionStore.Builder<A> builder(Reactor reactor, SessionIdGenerator<A, Session<A>> sessionIdGenerator) {
		return new InMemoryBasicSessionStore.Builder<>(sessionIdGenerator, reactor);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SessionIdGenerator<A, Session<A>> getSessionIdGenerator() {
		return (SessionIdGenerator<A, Session<A>>)super.getSessionIdGenerator();
	}

	@Override
	public Mono<Session<A>> create() {
		return Mono.defer(() -> {
			InMemorySession<A> session = new InMemorySession<>(this.sessionIdGenerator, this, this.maxInactiveInterval, this.maxInactiveInterval == null ? System.currentTimeMillis() + this.expireAfterPeriod : null);
			return session.refreshId(true)
				.map(sessionId -> {
					this.sessions.put(sessionId, session);
					this.scheduleClean();
					return new InMemorySession<>(session, true);
				});
		});
	}

	@Override
	public Mono<Session<A>> get(String sessionId) {
		return Mono.fromSupplier(() -> this.sessions.computeIfPresent(sessionId, (id, session) -> {
			if(session.isExpired()) {
				return null;
			}
			return session;
		}))
		.map(session -> new InMemorySession<>(session, false));
	}

	@Override
	public Mono<A> getData(String sessionId) {
		return Mono.fromSupplier(() -> {
				InMemorySession<A> session = this.sessions.get(sessionId);
				return session != null ? session.getSessionData() : null;
			});
	}

	@Override
	public Mono<Void> move(String sessionId, String newSessionId) throws IllegalStateException {
		return Mono.fromRunnable(() -> {
			InMemorySession<A> storedSession = this.sessions.get(sessionId);
			if(storedSession == null) {
				throw new IllegalStateException("Session " + sessionId + " doesn't exist");
			}
			InMemorySession<A> newStoredSession = new InMemorySession<>(storedSession, newSessionId);
			if(this.sessions.putIfAbsent(newSessionId, newStoredSession) != null) {
				throw new IllegalStateException("Session " + newSessionId + " already exists");
			}
			this.sessions.remove(sessionId);
		});
	}

	@Override
	public Mono<Void> remove(String sessionId) {
		return Mono.fromRunnable(() -> this.sessions.remove(sessionId));
	}

	@Override
	public Mono<Void> save(Session<A> session) throws IllegalArgumentException, IllegalStateException{
		if(!(session instanceof InMemoryBasicSessionStore.InMemorySession<A>)) {
			throw new IllegalArgumentException("Invalid session object");
		}
		return this.save((InMemorySession<A>)session);
	}

	/**
	 * <p>
	 * Saves the specified in-memory basic session.
	 * </p>
	 *
	 * @param session the session to save
	 *
	 * @return a mono for saving the session
	 *
	 * @throws IllegalStateException if the specified session does not exist in the store or if it was invalidated
	 */
	private Mono<Void> save(InMemorySession<A> session) throws IllegalStateException {
		return Mono.defer(() -> {
				if(session.isInvalidated()) {
					throw new IllegalStateException("Session has been invalidated");
				}
				return session.refreshId(false);
			})
			.then(Mono.fromRunnable(() -> this.sessions.compute(session.getId(), (sessionId, storedSession) -> {
					if(storedSession == null) {
						throw new IllegalStateException("Session " + sessionId + " doesn't exist");
					}
					storedSession.setLastAccessedTime(System.currentTimeMillis());
					session.setLastAccessedTime(storedSession.getLastAccessedTime());

					if(session.getMaxInactiveInterval() != null) {
						storedSession.setMaxInactiveInterval(session.getMaxInactiveInterval());
					}
					else {
						storedSession.setExpirationTime(session.getExpirationTime());
					}

					storedSession.setData(session.getSessionData());

					return storedSession;
				}))
			);
	}

	/**
	 * <p>
	 * An in-memory basic session implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 */
	private static class InMemorySession<A> extends AbstractSession<A, Session<A>> {

		private A sessionData;

		/**
		 * <p>
		 * Creates a new in-memory basic session.
		 * </p>
		 *
		 * @param sessionIdGenerator  a session id generator
		 * @param sessionStore        an in-memory basic session store
		 * @param maxInactiveInterval the maximum inactive interval in milliseconds
		 * @param expirationTime      the expiration time in milliseconds
		 */
		public InMemorySession(SessionIdGenerator<A, Session<A>> sessionIdGenerator, InMemoryBasicSessionStore<A> sessionStore, Long maxInactiveInterval, Long expirationTime) {
			super(sessionIdGenerator, sessionStore, maxInactiveInterval, expirationTime);
			this.sessionData = null;
		}

		/**
		 * <p>
		 * Creates an in-memory basic session from the specified session.
		 * </p>
		 *
		 * @param storedSession an existing session
		 * @param isNew         true to wrap a new session, false otherwise
		 */
		public InMemorySession(InMemorySession<A> storedSession, boolean isNew) {
			super(storedSession.sessionIdGenerator, storedSession.sessionStore, storedSession.getId(), storedSession.creationTime, storedSession.lastAccessedTime, storedSession.maxInactiveInterval, storedSession.expirationTime, isNew);
			this.sessionData = storedSession.getSessionData();
		}

		/**
		 * <p>
		 * Creates an in-memory basic session with the specified identifier from the specified session.
		 * </p>
		 *
		 * @param storedSession an existing session
		 * @param id            a session id
		 */
		public InMemorySession(InMemorySession<A> storedSession, String id) {
			super(storedSession.sessionIdGenerator, storedSession.sessionStore, id, storedSession.creationTime, storedSession.lastAccessedTime, storedSession.maxInactiveInterval, storedSession.expirationTime, false);
			this.sessionData = storedSession.getSessionData();
		}

		@Override
		public Mono<A> getData() {
			return Mono.justOrEmpty(this.sessionData);
		}

		/**
		 * <p>
		 * Returns the session data.
		 * </p>
		 *
		 * @return the session data
		 */
		public A getSessionData() {
			return sessionData;
		}

		@Override
		public void setData(A sessionData) {
			this.sessionData = sessionData;
		}
	}

	/**
	 * <p>
	 * The in-memory basic session store builder.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 */
	public static class Builder<A> extends AbstractSessionStore.Builder<A, Session<A>, InMemoryBasicSessionStore<A>, Builder<A>> {

		private final Reactor reactor;

		private long cleanPeriod;

		/**
		 * <p>
		 * Creates an in-memory basic session store builder.
		 * </p>
		 *
		 * @param sessionIdGenerator a session id generator
		 * @param reactor            the reactor
		 */
		private Builder(SessionIdGenerator<A, Session<A>> sessionIdGenerator, Reactor reactor) {
			super(sessionIdGenerator);
			this.reactor = reactor;
			this.cleanPeriod = DEFAULT_CLEAN_PERIOD;
		}

		/**
		 * <p>
		 * Sets the session cleaning period in milliseconds.
		 * </p>
		 *
		 * <p>
		 * Defaults to {@link #DEFAULT_CLEAN_PERIOD}.
		 * </p>
		 *
		 * @param cleanPeriod the cleaning period in milliseconds
		 *
		 * @return the builder
		 */
		public Builder<A> cleanPeriod(long cleanPeriod) {
			this.cleanPeriod = cleanPeriod;
			return this;
		}

		@Override
		public InMemoryBasicSessionStore<A> build() {
			return new InMemoryBasicSessionStore<>(this.sessionIdGenerator, this.maxInactiveInterval, this.expireAfterPeriod, this.reactor, this.cleanPeriod);
		}
	}
}
