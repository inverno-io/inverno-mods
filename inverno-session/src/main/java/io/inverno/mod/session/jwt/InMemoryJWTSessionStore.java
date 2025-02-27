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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.session.AbstractSessionStore;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.SessionDataSaveStrategy;
import io.inverno.mod.session.SessionIdGenerator;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWT session store implementation that stores sessions and their stateful data in-memory in a concurrent map.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 * @param <B> the stateless session data type
 */
public class InMemoryJWTSessionStore<A, B> extends AbstractSessionStore<A, JWTSession<A, B>> implements JWTSessionStore<A, B> {

	/**
	 * The default session cleaning period in milliseconds: {@code 300000} (i.e. 5 minutes).
	 */
	public static final long DEFAULT_CLEAN_PERIOD = 300000L;

	private static final Logger LOGGER = LogManager.getLogger(InMemoryJWTSessionStore.class);

	private final ObjectMapper mapper;
	private final JavaType statelessSessionDataType;
	private final SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy;

	private final EventLoop eventLoop;
	private final long cleanPeriod;
	private final Map<String, InMemoryJWTSession<A, B>> sessions;

	private ScheduledFuture<?> cleanFuture;

	/**
	 * <p>
	 * Creates an in-memory JWT session store.
	 * </p>
	 *
	 * @param sessionIdGenerator               a JWT session id generator
	 * @param maxInactiveInterval              the initial maximum inactive interval in milliseconds
	 * @param expireAfterPeriod                the period in milliseconds after which a new session must expire
	 * @param reactor                          the reactor
	 * @param cleanPeriod                      the cleaning period
	 * @param mapper                           an object mapper
	 * @param statelessSessionDataType         the stateless session data type
	 * @param statelessSessionDataSaveStrategy the stateless session data save strategy
	 */
	private InMemoryJWTSessionStore(JWTSessionIdGenerator<A, B> sessionIdGenerator, Long maxInactiveInterval, Long expireAfterPeriod, Reactor reactor, long cleanPeriod, ObjectMapper mapper, Type statelessSessionDataType, SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy) {
		super(sessionIdGenerator, maxInactiveInterval, expireAfterPeriod);
		this.eventLoop = reactor.getEventLoop();
		this.cleanPeriod = cleanPeriod;
		this.sessions = new ConcurrentHashMap<>();
		this.mapper = mapper;
		this.statelessSessionDataType = mapper.constructType(statelessSessionDataType);
		this.statelessSessionDataSaveStrategy = statelessSessionDataSaveStrategy;
	}

	/**
	 * <p>
	 * Removes expired or invalidated session from the store and schedules the next cleaning operation.
	 * </p>
	 */
	private void clean() {
		try {
			for(JWTSession<A, B> session : this.sessions.values()) {
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
	 * Creates an in-memory JWT session store builder.
	 * </p>
	 *
	 * @param <A> the session data type
	 * @param <B> the stateless session data type
	 * @param sessionIdGenerator a JWT session id generator
	 * @param reactor the reactor
	 * @param mapper an object mapper
	 * @param statelessSessionDataType the stateless session data type
	 *
	 * @return an in-memory JWT session store builder
	 */
	public static <A, B> InMemoryJWTSessionStore.Builder<A, B> builder(JWTSessionIdGenerator<A, B> sessionIdGenerator, Reactor reactor, ObjectMapper mapper, Class<B> statelessSessionDataType) {
		return new InMemoryJWTSessionStore.Builder<>(sessionIdGenerator, reactor, mapper, statelessSessionDataType);
	}

	/**
	 * <p>
	 * Creates an in-memory JWT session store builder.
	 * </p>
	 *
	 * @param <A> the session data type
	 * @param <B> the stateless session data type
	 * @param sessionIdGenerator a JWT session id generator
	 * @param reactor the reactor
	 * @param mapper an object mapper
	 * @param statelessSessionDataType the stateless session data type
	 *
	 * @return an in-memory JWT session store builder
	 */
	public static <A, B> InMemoryJWTSessionStore.Builder<A, B> builder(JWTSessionIdGenerator<A, B> sessionIdGenerator, Reactor reactor, ObjectMapper mapper, Type statelessSessionDataType) {
		return new InMemoryJWTSessionStore.Builder<>(sessionIdGenerator, reactor, mapper, statelessSessionDataType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public JWTSessionIdGenerator<A, B> getSessionIdGenerator() {
		return (JWTSessionIdGenerator<A, B>)super.getSessionIdGenerator();
	}

	@Override
	public Mono<JWTSession<A, B>> create() {
		return Mono.defer(() -> {
			InMemoryJWTSession<A, B> session = new InMemoryJWTSession<>((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator, this, this.maxInactiveInterval, this.maxInactiveInterval == null ? System.currentTimeMillis() + this.expireAfterPeriod : null, this.statelessSessionDataSaveStrategy);
			return session.refreshId(true).map(sessionId -> {
				this.sessions.put(session.getTokenId(), session);
				this.scheduleClean();
				return new InMemoryJWTSession<>(session);
			});
		});
	}

	@Override
	public Mono<JWTSession<A, B>> get(String sessionId) {
		try {
			return ((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).readJWT(sessionId)
				.onErrorResume(error -> {
					LOGGER.warn("Invalid session id: {}", sessionId, error);
					return Mono.empty();
				})
				.mapNotNull(jwt -> {
					InMemoryJWTSession<A, B> storedSession = this.sessions.computeIfPresent(jwt.getPayload().getJWTId(), (tokenId, session) -> {
						if(session.isExpired()) {
							return null;
						}
						return session;
					});
					return storedSession == null ? null : new InMemoryJWTSession<>(
						(JWTSessionIdGenerator<A, B>)this.sessionIdGenerator,
						this,
						sessionId,
						jwt.getPayload(),
						jwt.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).map(claim -> this.mapper.<B>convertValue(claim.getValue(), this.statelessSessionDataType)).orElse(null),
						this.statelessSessionDataSaveStrategy,
						storedSession.getSessionData(),
						storedSession.getLastAccessedTime()
					);
				});
		}
		catch(JOSEObjectReadException e) {
			LOGGER.warn("Not a JWT session id: {}", sessionId, e);
			return Mono.empty();
		}
	}

	@Override
	public Mono<A> getData(String sessionId) {
		try {
			return ((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).readJWT(sessionId)
				.onErrorResume(error -> {
					LOGGER.warn("Invalid session id: {}", sessionId, error);
					return Mono.empty();
				})
				.mapNotNull(jwt -> {
					InMemoryJWTSession<A, B> session = this.sessions.get(jwt.getPayload().getJWTId());
					return session != null ? session.getSessionData() : null;
				});
		}
		catch(JOSEObjectReadException e) {
			LOGGER.warn("Malformed session id: {}", sessionId, e);
			return Mono.empty();
		}
	}

	@Override
	public Mono<A> getDataByTokenId(String tokenId) {
		return Mono.fromSupplier(() -> {
			InMemoryJWTSession<A, B> session = this.sessions.get(tokenId);
			return session != null ? session.getSessionData() : null;
		});
	}

	@Override
	public Mono<Void> move(String sessionId, String newSessionId) throws IllegalStateException {
		return Mono.zip(((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).readJWT(sessionId), ((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).readJWT(newSessionId))
			.flatMap(tokens -> this.moveByTokenId(tokens.getT1().getPayload().getJWTId(), tokens.getT2()));
	}

	@Override
	public Mono<Void> moveByTokenId(String tokenId, JOSEObject<JWTClaimsSet, ?> newSessionJWT) throws IllegalStateException {
		return Mono.fromRunnable(() -> {
			InMemoryJWTSession<A, B> storedSession = this.sessions.get(tokenId);
			if(storedSession == null) {
				throw new IllegalStateException("Session with token ID " + tokenId + " doesn't exist");
			}

			InMemoryJWTSession<A, B> newStoredSession = new InMemoryJWTSession<>((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator, this, newSessionJWT.toCompact(), newSessionJWT.getPayload(), null, this.statelessSessionDataSaveStrategy, storedSession.getSessionData(), System.currentTimeMillis());
			if(this.sessions.putIfAbsent(newSessionJWT.getPayload().getJWTId(), newStoredSession) != null) {
				throw new IllegalStateException("Session with token ID " + newSessionJWT.getPayload().getJWTId() + " already exists");
			}
			this.sessions.remove(tokenId);
		});
	}

	@Override
	public Mono<Void> remove(String sessionId) {
		return ((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).readJWT(sessionId)
			.doOnSuccess(jwt -> this.sessions.remove(jwt.getPayload().getJWTId())).then();
	}

	@Override
	public Mono<Void> removeByTokenId(String tokenId) {
		return Mono.fromRunnable(() -> this.sessions.remove(tokenId));
	}

	@Override
	public Mono<Void> save(JWTSession<A, B> session) throws IllegalArgumentException, IllegalStateException {
		if(!(session instanceof InMemoryJWTSession<A, B>)) {
			throw new IllegalArgumentException("Invalid session object");
		}
		return this.save((InMemoryJWTSession<A, B>)session);
	}

	/**
	 * <p>
	 * Saves the specified in-memory JWT session.
	 * </p>
	 *
	 * @param session the session to save
	 *
	 * @return a mono for saving the session
	 *
	 * @throws IllegalStateException if the specified session does not exist in the store or if it was invalidated
	 */
	private Mono<Void> save(InMemoryJWTSession<A, B> session) throws IllegalStateException {
		return Mono.defer(() -> {
				if(session.isInvalidated()) {
					throw new IllegalStateException("Session has been invalidated");
				}
				return session.refreshId(false);
			})
			.then(Mono.fromRunnable(() -> this.sessions.compute(session.getTokenId(), (tokenId, storedSession) -> {
				if(storedSession == null) {
					throw new IllegalStateException("Session with token ID " + tokenId + " doesn't exist");
				}
				storedSession.setLastAccessedTime(System.currentTimeMillis());
				storedSession.setData(session.getSessionData());
				return storedSession;
			})));
	}

	/**
	 * <p>
	 * An in-memory JWT session implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 * @param <B> the stateless session data type
	 */
	private static class InMemoryJWTSession<A, B> extends AbstractJWTSession<A, B> {

		private A sessionData;

		/**
		 * <p>
		 * Creates a new in-memory JWT session.
		 * </p>
		 *
		 * @param sessionIdGenerator               a JWT session id generator
		 * @param sessionStore                     an in-memory JWT session store
		 * @param maxInactiveInterval              the maximum inactive interval in milliseconds
		 * @param expirationTime                   the expiration time in milliseconds
		 * @param statelessSessionDataSaveStrategy the stateless session data save strategy
		 */
		public InMemoryJWTSession(JWTSessionIdGenerator<A, B> sessionIdGenerator, InMemoryJWTSessionStore<A, B> sessionStore, Long maxInactiveInterval, Long expirationTime, SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy) {
			super(sessionIdGenerator, sessionStore, maxInactiveInterval, expirationTime, statelessSessionDataSaveStrategy);
		}

		/**
		 * <p>
		 * Wraps a new session.
		 * </p>
		 *
		 * @param newSession the new session to wrap
		 */
		public InMemoryJWTSession(InMemoryJWTSession<A, B> newSession) {
			super((JWTSessionIdGenerator<A, B>)newSession.sessionIdGenerator, (InMemoryJWTSessionStore<A, B>)newSession.sessionStore, newSession.id, newSession.tokenId, newSession.creationTime, newSession.lastAccessedTime, newSession.maxInactiveInterval, newSession.expirationTime, newSession.statelessSessionData, newSession.statelessSessionDataSaveStrategy, true);
		}

		/**
		 * <p>
		 * Creates an existing in-memory JWT session.
		 * </p>
		 *
		 * @param sessionIdGenerator               a JWT session id generator
		 * @param sessionStore                     an in-memory JWT session store
		 * @param id                               the JWT session identifier
		 * @param claimsSet                        the JWT session claims set
		 * @param statelessData                    the stateless session data
		 * @param statelessSessionDataSaveStrategy the stateless session data save strategy
		 * @param sessionData                      the session data
		 * @param lastAccessedTime                 the last accessed time in milliseconds
		 */
		public InMemoryJWTSession(JWTSessionIdGenerator<A, B> sessionIdGenerator, InMemoryJWTSessionStore<A, B> sessionStore, String id, JWTClaimsSet claimsSet, B statelessData, SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy, A sessionData, long lastAccessedTime) {
			super(sessionIdGenerator, sessionStore, id, claimsSet, statelessData, statelessSessionDataSaveStrategy, lastAccessedTime, false);
			this.sessionData = sessionData;
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
	 * The in-memory JWT session store builder.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 * @param <B> the stateless session data type
	 */
	public static class Builder<A, B> extends AbstractSessionStore.Builder<A, JWTSession<A, B>, InMemoryJWTSessionStore<A, B>, InMemoryJWTSessionStore.Builder<A, B>> {

		private final Reactor reactor;
		private final ObjectMapper mapper;
		private final Type statelessSessionDataType;

		private SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy;
		private long cleanPeriod;

		/**
		 * <p>
		 * Creates an in-memory JWT session store builder.
		 * </p>
		 *
		 * @param sessionIdGenerator       a JWT session id generator
		 * @param reactor                  the reactor
		 * @param mapper                   an object mapper
		 * @param statelessSessionDataType the stateless session data type
		 */
		private Builder(JWTSessionIdGenerator<A, B> sessionIdGenerator, Reactor reactor, ObjectMapper mapper, Class<B> statelessSessionDataType) {
			this(sessionIdGenerator, reactor, mapper, (Type)statelessSessionDataType);
		}

		/**
		 * <p>
		 * Creates an in-memory JWT session store builder.
		 * </p>
		 *
		 * @param sessionIdGenerator       a JWT session id generator
		 * @param reactor                  the reactor
		 * @param mapper                   an object mapper
		 * @param statelessSessionDataType the stateless session data type
		 */
		private Builder(JWTSessionIdGenerator<A, B> sessionIdGenerator, Reactor reactor, ObjectMapper mapper, Type statelessSessionDataType) {
			super(sessionIdGenerator);
			this.reactor = reactor;
			this.mapper = mapper;
			this.statelessSessionDataSaveStrategy = SessionDataSaveStrategy.onSetOnly();
			this.statelessSessionDataType = statelessSessionDataType;
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
		public Builder<A, B> cleanPeriod(long cleanPeriod) {
			this.cleanPeriod = cleanPeriod;
			return this;
		}

		/**
		 * <p>
		 * Sets the stateless session data save strategy.
		 * </p>
		 *
		 * <p>
		 * Particular care must be taken when defining the stateless session data save strategy as this will trigger the refresh of the session identifier. It is important to make sure to only do it
		 * if/when it makes sense in order to avoid side effects, as a result {@link SessionDataSaveStrategy#onGet()} is not suited for stateless session data and shall not be used.
		 * </p>
		 *
		 * <p>
		 * Defaults to {@link SessionDataSaveStrategy#onSetOnly()}.
		 * </p>
		 *
		 * @param statelessSessionDataSaveStrategy a session data save strategy
		 *
		 * @return the builder
		 */
		public Builder<A, B> statelessSessionDataSaveStrategy(SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy) {
			this.statelessSessionDataSaveStrategy = statelessSessionDataSaveStrategy != null ? statelessSessionDataSaveStrategy : SessionDataSaveStrategy.onSetOnly();
			return this;
		}

		@Override
		public InMemoryJWTSessionStore<A, B> build() {
			return new InMemoryJWTSessionStore<>((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator, this.maxInactiveInterval, this.expireAfterPeriod, this.reactor, this.cleanPeriod, this.mapper, this.statelessSessionDataType, this.statelessSessionDataSaveStrategy);
		}
	}
}
