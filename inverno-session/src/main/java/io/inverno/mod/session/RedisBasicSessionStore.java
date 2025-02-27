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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.inverno.mod.redis.RedisClient;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A basic session store implementation that stores sessions and their data in a Redis data store.
 * </p>
 *
 * <p>
 * Sessions are stored in Redis hash containing the session attributes (i.e. creation time, maximum inactive interval...) and the session data. They are stored with a two minutes expiration buffer in
 * order to make sure session data are still available to the limits.
 * </p>
 *
 * <p>
 * Session data are stored as JSON strings, as a result the session data type must be defined in a way that enables an object mapper to read and write data.
 * </p>
 *
 * <p>
 * This implementation uses a {@link SessionDataSaveStrategy} to determine whether resolved session data should be saved along with the session.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the session data type
 */
public class RedisBasicSessionStore<A> extends AbstractSessionStore<A, Session<A>> implements BasicSessionStore<A> {

	/**
	 * The default Redis key prefix.
	 */
	public static final String DEFAULT_KEY_PREFIX = "SESSION";

	/**
	 * The creation time hash field name.
	 */
	public static final String FIELD_CREATION_TIME = "creationTime";
	/**
	 * The last accessed time hash field name.
	 */
	public static final String FIELD_LAST_ACCESSED_TIME = "lastAccessedTime";
	/**
	 * The maximum inactive interval time hash field name.
	 */
	public static final String FIELD_MAX_INACTIVE_INTERVAL = "maxInactiveInterval";
	/**
	 * The expiration time hash field name.
	 */
	public static final String FIELD_EXPIRATION_TIME = "expirationTime";
	/**
	 * The data hash field name.
	 */
	public static final String FIELD_SESSION_DATA = "data";

	private final RedisClient<String, String> redisClient;
	private final ObjectReader sessionDataReader;
	private final ObjectWriter sessionDataWriter;
	private final SessionDataSaveStrategy<A> sessionDataSaveStrategy;
	private final String keyPrefix;
	private final String sessionKeyFormat;

	/**
	 * <p>
	 * Creates a Redis basic session store.
	 * </p>
	 *
	 * @param sessionIdGenerator      a session id generator
	 * @param maxInactiveInterval     the initial maximum inactive interval in milliseconds
	 * @param expireAfterPeriod       the period in milliseconds after which a new session must expire
	 * @param redisClient             a Redis client
	 * @param mapper                  an object mapper
	 * @param sessionDataType         the session data type
	 * @param sessionDataSaveStrategy the session data save strategy
	 * @param keyPrefix               the key prefix
	 */
	private RedisBasicSessionStore(
			SessionIdGenerator<A, Session<A>> sessionIdGenerator,
			Long maxInactiveInterval,
			Long expireAfterPeriod,
			RedisClient<String, String> redisClient,
			ObjectMapper mapper,
			Type sessionDataType,
			SessionDataSaveStrategy<A> sessionDataSaveStrategy,
			String keyPrefix) {
		super(sessionIdGenerator, maxInactiveInterval, expireAfterPeriod);
		this.redisClient = redisClient;
		JavaType javaSessionDataType = mapper.constructType(sessionDataType);
		this.sessionDataReader = mapper.readerFor(javaSessionDataType);
		this.sessionDataWriter = mapper.writerFor(javaSessionDataType);
		this.sessionDataSaveStrategy = sessionDataSaveStrategy;
		this.keyPrefix = keyPrefix;
		this.sessionKeyFormat = keyPrefix + ":%s";
	}

	/**
	 * <p>
	 * Creates a Redis basic session store builder using {@link SessionIdGenerator#uuid()} session id generator.
	 * </p>
	 *
	 * @param <A>                the session data type
	 * @param redisClient        a Redis client
	 * @param mapper             an object mapper
	 * @param sessionDataType    the session data type
	 *
	 *
	 * @return a Redis basic session store builder
	 */
	public static <A> RedisBasicSessionStore.Builder<A> builder(RedisClient<String, String> redisClient, ObjectMapper mapper, Class<A> sessionDataType) {
		return new RedisBasicSessionStore.Builder<>(SessionIdGenerator.uuid(), redisClient, mapper, sessionDataType);
	}

	/**
	 * <p>
	 * Creates a Redis basic session store builder.
	 * </p>
	 *
	 * @param <A>                the session data type
	 * @param redisClient        a Redis client
	 * @param mapper             an object mapper
	 * @param sessionDataType    the session data type
	 * @param sessionIdGenerator a session id generator
	 *
	 * @return a Redis basic session store builder
	 */
	public static <A> RedisBasicSessionStore.Builder<A> builder(RedisClient<String, String> redisClient, ObjectMapper mapper, Class<A> sessionDataType, SessionIdGenerator<A, Session<A>> sessionIdGenerator) {
		return new RedisBasicSessionStore.Builder<>(sessionIdGenerator, redisClient, mapper, sessionDataType);
	}

	/**
	 * <p>
	 * Creates a Redis basic session store builder using {@link SessionIdGenerator#uuid()} session id generator.
	 * </p>
	 *
	 * @param <A>                the session data type
	 * @param redisClient        a Redis client
	 * @param mapper             an object mapper
	 * @param sessionDataType    the session data type
	 *
	 * @return a Redis basic session store builder
	 */
	public static <A> RedisBasicSessionStore.Builder<A> builder(RedisClient<String, String> redisClient, ObjectMapper mapper, Type sessionDataType) {
		return new RedisBasicSessionStore.Builder<>(SessionIdGenerator.uuid(), redisClient, mapper, sessionDataType);
	}

	/**
	 * <p>
	 * Creates a Redis basic session store builder.
	 * </p>
	 *
	 * @param <A>                the session data type
	 * @param sessionIdGenerator a session id generator
	 * @param redisClient        a Redis client
	 * @param mapper             an object mapper
	 * @param sessionDataType    the session data type
	 *
	 * @return a Redis basic session store builder
	 */
	public static <A> RedisBasicSessionStore.Builder<A> builder(SessionIdGenerator<A, Session<A>> sessionIdGenerator, RedisClient<String, String> redisClient, ObjectMapper mapper, Type sessionDataType) {
		return new RedisBasicSessionStore.Builder<>(sessionIdGenerator, redisClient, mapper, sessionDataType);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SessionIdGenerator<A, Session<A>> getSessionIdGenerator() {
		return (SessionIdGenerator<A, Session<A>>)super.getSessionIdGenerator();
	}

	/**
	 * <p>
	 * Returns the session Redis key prefix.
	 * </p>
	 *
	 * @return the session key prefix
	 */
	public String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * <p>
	 * Returns the session Redis key for the specified session id.
	 * </p>
	 *
	 * @param sessionId the session id
	 *
	 * @return a session Redis key
	 */
	private String getSessionKey(String sessionId) {
		return String.format(this.sessionKeyFormat, sessionId);
	}

	@Override
	public Mono<Session<A>> create() {
		return Mono.defer(() -> {
			RedisSession<A> session = new RedisSession<>(this.sessionIdGenerator, this, this.maxInactiveInterval, this.maxInactiveInterval == null ? System.currentTimeMillis() + this.expireAfterPeriod : null);
			return session.refreshId(true)
				.flatMap(ign -> this.save(session))
				.thenReturn(session);
		});
	}

	@Override
	public Mono<Session<A>> get(String sessionId) {
		return this.redisClient.hmget(this.getSessionKey(sessionId), keys -> keys
				.key(FIELD_CREATION_TIME)
				.key(FIELD_LAST_ACCESSED_TIME)
				.key(FIELD_MAX_INACTIVE_INTERVAL)
				.key(FIELD_EXPIRATION_TIME)
			)
			.collectList()
			.mapNotNull(fields -> {
				if(fields.get(0).getValue().isEmpty()) {
					return null;
				}
				long creationTime = fields.get(0).getValue().map(Long::parseLong).orElseThrow(IllegalStateException::new);
				long lastAccessedTime = fields.get(1).getValue().map(Long::parseLong).orElseThrow(IllegalStateException::new);
				Long maxInactiveInterval = fields.get(2).getValue().filter(s -> !s.isBlank()).map(Long::valueOf).orElse(null);
				Long expirationTime = fields.get(3).getValue().filter(s -> !s.isBlank()).map(Long::valueOf).orElse(null);

				RedisSession<A> session = new RedisSession<>(this.sessionIdGenerator, this, sessionId, creationTime, lastAccessedTime, maxInactiveInterval, expirationTime);

				return session.isExpired() ? null : session;
			});
	}

	@Override
	public Mono<A> getData(String sessionId) {
		return this.redisClient
			.hget(this.getSessionKey(sessionId), FIELD_SESSION_DATA)
			.mapNotNull(value -> {
				try {
					return StringUtils.isNotBlank(value) ?  this.sessionDataReader.readValue(value) : null;
				}
				catch(JsonProcessingException e) {
					throw new UncheckedIOException(e);
				}
			});
	}

	@Override
	public Mono<Void> move(String sessionId, String newSessionId) throws IllegalStateException {
		return this.redisClient
			.renamenx(this.getSessionKey(sessionId), this.getSessionKey(newSessionId))
			.doOnNext(result -> {
				if(!result) {
					throw new IllegalStateException("Session " + newSessionId + " already exists");
				}
			})
			.then();
	}

	@Override
	public Mono<Void> remove(String sessionId) {
		return this.redisClient
			.del(this.getSessionKey(sessionId))
			.then();
	}

	@Override
	public Mono<Void> save(Session<A> session) throws IllegalArgumentException, IllegalStateException {
		if(!(session instanceof RedisBasicSessionStore.RedisSession<A>)) {
			throw new IllegalArgumentException("Invalid session object");
		}
		return this.save((RedisSession<A>)session);
	}

	/**
	 * <p>
	 * Saves the specified Redis basic session.
	 * </p>
	 *
	 * <p>
	 * Session data are saved:
	 * </p>
	 *
	 * <ul>
	 * <li>if they were explicitly set using {@link Session#setData(Object)}</li>
	 * <li>OR if they were fetched and the session data save strategy returns {@code true}</li>
	 * </ul>
	 *
	 * @param session the session to save
	 *
	 * @return a mono for saving the session
	 *
	 * @throws IllegalStateException if the specified session does not exist in the store or if it was invalidated
	 */
	private Mono<Void> save(RedisSession<A> session) throws IllegalStateException {
		return  Mono.defer(() -> {
				if(session.isInvalidated()) {
					throw new IllegalStateException("Session has been invalidated");
				}
				return session.refreshId(false);
			})
			.then(Mono.defer(() -> {
				String sessionKey = this.getSessionKey(session.getId());
				session.setLastAccessedTime(System.currentTimeMillis());
				// We do not check for session existence first as Redis doesn't provide such feature (i.e. XX) for hash
				// one way to do it would be to use a Lua script with EVAL command, in the meantime let's assume it'll work considering we have a 2 minutes expiration buffer
				return Flux.from(this.redisClient.batch(operations -> Flux.just(
						operations.hset(sessionKey, entries -> {
								if(session.isNew()) {
									entries.entry(FIELD_CREATION_TIME, Long.toString(session.getCreationTime()));
								}
								entries.entry(FIELD_LAST_ACCESSED_TIME, Long.toString(session.getLastAccessedTime()));
								if(session.isNew() || session.isExpirationSet()) {
									entries.entry(FIELD_MAX_INACTIVE_INTERVAL, session.getMaxInactiveInterval() != null ? Long.toString(session.getMaxInactiveInterval()) : "");
									entries.entry(FIELD_EXPIRATION_TIME, session.getMaxInactiveInterval() == null ? Long.toString(session.getExpirationTime()) : "");
								}
								if(session.isSessionDataSet() || (session.isSessionDataFetched() && this.sessionDataSaveStrategy.getAndSetSaveState(session.getSessionData(), false))) {
									try {
										entries.entry(FIELD_SESSION_DATA, session.getSessionData() != null ? this.sessionDataWriter.writeValueAsString(session.getSessionData()) : "");
									}
									catch(JsonProcessingException e) {
										throw new UncheckedIOException(e);
									}
								}
							})
							.thenReturn(true),
						session.getMaxInactiveInterval() != null ? operations.pexpire(sessionKey, session.getMaxInactiveInterval() + 120000) : operations.pexpireat(sessionKey, session.getExpirationTime() + 120000)
					)))
					.doOnNext(result -> {
						if(!result) {
							throw new IllegalStateException("Error saving session " + session.getId());
						}
					})
					.then()
					.doOnSuccess(ign -> session.onSave());
			}));
	}

	/**
	 * <p>
	 * A Redis basic session implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 */
	private static class RedisSession<A> extends AbstractSession<A, Session<A>> {

		private final Mono<A> sessionDataResolver = Mono.defer(() -> {
			if(this.sessionDataFetched || this.sessionDataSet) {
				return Mono.justOrEmpty(this.sessionData);
			}
			return this.sessionStore.getData(this.id)
				.doOnSuccess(data -> {
					this.sessionData = data;
					this.sessionDataFetched = true;
				})
				.share();
		});

		private boolean sessionDataFetched;
		private boolean sessionDataSet;
		private A sessionData;
		private boolean expirationSet;

		/**
		 * <p>
		 * Creates a new Redis basic session.
		 * </p>
		 *
		 * @param sessionIdGenerator  a session id generator
		 * @param sessionStore        a Redis basic session store
		 * @param maxInactiveInterval the maximum inactive interval in milliseconds
		 * @param expirationTime      the expiration time in milliseconds
		 */
		public RedisSession(SessionIdGenerator<A, Session<A>> sessionIdGenerator, RedisBasicSessionStore<A> sessionStore, Long maxInactiveInterval, Long expirationTime) {
			super(sessionIdGenerator, sessionStore, maxInactiveInterval, expirationTime);
		}

		/**
		 * <p>
		 * Creates a Redis basic session.
		 * </p>
		 *
		 * @param sessionIdGenerator  a session id generator
		 * @param sessionStore        a Redis basic session store
		 * @param id                  the session id
		 * @param creationTime        the creation time in milliseconds
		 * @param lastAccessedTime    the last accessed time in milliseconds
		 * @param maxInactiveInterval the maximum inactive interval in milliseconds
		 * @param expirationTime      the expiration time in milliseconds
		 */
		public RedisSession(SessionIdGenerator<A, Session<A>> sessionIdGenerator, RedisBasicSessionStore<A> sessionStore, String id, long creationTime, long lastAccessedTime, Long maxInactiveInterval, Long expirationTime) {
			super(sessionIdGenerator, sessionStore, id, creationTime, lastAccessedTime, maxInactiveInterval, expirationTime, false);
		}

		/**
		 * <p>
		 * Determines whether session data were fetched.
		 * </p>
		 *
		 * @return true if session data were fetched, false otherwise
		 */
		public boolean isSessionDataFetched() {
			return sessionDataFetched;
		}

		/**
		 * <p>
		 * Determines whether session data were set explicitly.
		 * </p>
		 *
		 * @return true if session data were set, false otherwise
		 */
		public boolean isSessionDataSet() {
			return sessionDataSet;
		}

		/**
		 * <p>
		 * Determines whether expiration settings were changed.
		 * </p>
		 *
		 * @return true if expiration settings were changed, false otherwise
		 */
		public boolean isExpirationSet() {
			return expirationSet;
		}

		@Override
		public Mono<A> getData() {
			return this.sessionDataResolver;
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
			this.sessionDataSet = true;
		}

		@Override
		public void setMaxInactiveInterval(long maxInactiveInterval) {
			if(this.maxInactiveInterval == null || this.maxInactiveInterval != maxInactiveInterval) {
				super.setMaxInactiveInterval(maxInactiveInterval);
				this.expirationSet = true;
			}
		}

		@Override
		public void setExpirationTime(long expirationTime) {
			if(this.expirationTime == null || this.expirationTime != expirationTime) {
				super.setExpirationTime(expirationTime);
				this.expirationSet = true;
			}
		}

		/**
		 * <p>
		 * Invoked after save to reset session data flags.
		 * </p>
		 */
		public void onSave() {
			if(this.sessionDataSet) {
				this.sessionDataFetched = true;
				this.sessionDataSet = false;
			}
		}
	}

	/**
	 * <p>
	 * The Redis basic session store builder.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 */
	public static class Builder<A> extends AbstractSessionStore.Builder<A, Session<A>, RedisBasicSessionStore<A>, RedisBasicSessionStore.Builder<A>> {

		private final RedisClient<String, String> redisClient;
		private final ObjectMapper mapper;
		private final Type sessionDataType;

		private String keyPrefix;
		private SessionDataSaveStrategy<A> sessionDataSaveStrategy;

		/**
		 * <p>
		 * Creates a Redis basic session store builder.
		 * </p>
		 *
		 * @param sessionIdGenerator a session id generator
		 * @param redisClient        a Redis client
		 * @param mapper             an object mapper
		 * @param sessionDataType    the session data type
		 */
		private Builder(SessionIdGenerator<A, Session<A>> sessionIdGenerator,RedisClient<String, String> redisClient, ObjectMapper mapper, Type sessionDataType) {
			super(sessionIdGenerator);
			this.redisClient = redisClient;
			this.mapper = mapper;
			this.sessionDataType = sessionDataType;

			this.keyPrefix = DEFAULT_KEY_PREFIX;
			this.sessionDataSaveStrategy = SessionDataSaveStrategy.onGet();
		}

		/**
		 * <p>
		 * Sets the Redis key prefix.
		 * </p>
		 *
		 * <p>
		 * Defaults to: {@link #DEFAULT_KEY_PREFIX}.
		 * </p>
		 *
		 * @param keyPrefix a key prefix
		 *
		 * @return the builder
		 */
		public Builder<A> keyPrefix(String keyPrefix) {
			this.keyPrefix = StringUtils.defaultIfBlank(keyPrefix, DEFAULT_KEY_PREFIX);
			return this;
		}

		/**
		 * <p>
		 * Sets the session data save strategy.
		 * </p>
		 *
		 * <p>
		 * Defaults to {@link SessionDataSaveStrategy#onGet()}.
		 * </p>
		 *
		 * @param sessionDataSaveStrategy a session data save strategy
		 *
		 * @return the builder
		 */
		public Builder<A> sessionDataSaveStrategy(SessionDataSaveStrategy<A> sessionDataSaveStrategy) {
			this.sessionDataSaveStrategy = sessionDataSaveStrategy != null ? sessionDataSaveStrategy : SessionDataSaveStrategy.onGet();
			return this;
		}

		@Override
		public RedisBasicSessionStore<A> build() {
			return new RedisBasicSessionStore<>(this.sessionIdGenerator, this.maxInactiveInterval, this.expireAfterPeriod, this.redisClient, this.mapper, this.sessionDataType, this.sessionDataSaveStrategy, this.keyPrefix);
		}
	}
}
