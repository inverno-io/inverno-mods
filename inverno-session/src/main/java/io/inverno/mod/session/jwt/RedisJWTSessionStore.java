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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.inverno.mod.redis.RedisClient;
import io.inverno.mod.redis.operations.RedisStringReactiveOperations;
import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.session.AbstractSessionStore;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.SessionDataSaveStrategy;
import io.inverno.mod.session.SessionIdGenerator;
import io.inverno.mod.session.internal.jwt.JWTSSessionIdGenerator;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWT session store implementation that stores sessions and their stateful data in a Redis data store.
 * </p>
 *
 * <p>
 * Only stateful session data are stored in Redis as JSON strings and set to expire ({@code pexpire} when maximum inactive interval is set or {@code pexpireat} when explicit expiration time is set),
 * other session attributes as well as stateless session data are stored in the JWT session id. Expiration time includes a two minutes buffer in order to make sure session data are still available to
 * the limits.
 * </p>
 *
 * <p>
 * Expiration due to inactivity is evaluated based on the session Redis key idle time which is rounded to the second, therefore expiration is also precise to the second. This is also true when an
 * explicit expiration time is set because this is stored in JWT expiration time claim which is also precise to the second, the specified value being rounded to the lowest integer when generating the
 * JWT session id.
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
 */
public class RedisJWTSessionStore<A, B> extends AbstractSessionStore<A, JWTSession<A, B>> implements JWTSessionStore<A, B> {

	/**
	 * The default Redis key prefix.
	 */
	public static final String DEFAULT_KEY_PREFIX = "SESSION";

	private static final Logger LOGGER = LogManager.getLogger(RedisJWTSessionStore.class);

	private final RedisClient<String, String> redisClient;
	private final ObjectMapper mapper;
	private final ObjectReader sessionDataReader;
	private final ObjectWriter sessionDataWriter;
	private final SessionDataSaveStrategy<A> sessionDataSaveStrategy;
	private final JavaType statelessSessionDataType;
	private final SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy;

	private final String keyPrefix;
	private final String sessionKeyFormat;

	/**
	 * <p>
	 * Creates a Redis JWT session store.
	 * </p>
	 *
	 * @param sessionIdGenerator               a JWT session id generator
	 * @param maxInactiveInterval              the initial maximum inactive interval in milliseconds
	 * @param expireAfterPeriod                the period in milliseconds after which a new session must expire
	 * @param redisClient                      a Redis client
	 * @param mapper                           an object mapper
	 * @param sessionDataType                  the session data type
	 * @param sessionDataSaveStrategy          the session data save strategy
	 * @param statelessSessionDataType         the stateless session data type
	 * @param statelessSessionDataSaveStrategy the stateless session data save strategy
	 * @param keyPrefix                        the key prefix
	 */
	private RedisJWTSessionStore(
			SessionIdGenerator<A, JWTSession<A, B>> sessionIdGenerator,
			Long maxInactiveInterval,
			Long expireAfterPeriod,
			RedisClient<String, String> redisClient,
			ObjectMapper mapper,
			Type sessionDataType,
			SessionDataSaveStrategy<A> sessionDataSaveStrategy,
			Type statelessSessionDataType,
			SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy,
			String keyPrefix
		) {
		super(sessionIdGenerator, maxInactiveInterval, expireAfterPeriod);
		this.redisClient = redisClient;
		this.mapper = mapper;
		JavaType javaSessionDataType = mapper.constructType(sessionDataType);
		this.sessionDataReader = mapper.readerFor(javaSessionDataType);
		this.sessionDataWriter = mapper.writerFor(javaSessionDataType);
		this.sessionDataSaveStrategy = sessionDataSaveStrategy;
		this.statelessSessionDataType = mapper.constructType(statelessSessionDataType);
		this.statelessSessionDataSaveStrategy = statelessSessionDataSaveStrategy;
		this.keyPrefix = keyPrefix;
		this.sessionKeyFormat = keyPrefix + ":%s";
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

	/**
	 * <p>
	 * Creates a Redis JWT session store builder.
	 * </p>
	 *
	 * @param <A>                      the session data type
	 * @param <B>                      the stateless session data type
	 * @param sessionIdGenerator       a JWT session id generator
	 * @param redisClient              a Redis client
	 * @param mapper                   an object mapper
	 * @param sessionDataType          the session data type
	 * @param statelessSessionDataType the stateless session data type
	 *
	 * @return a Redis JWT session store builder
	 */
	public static <A, B> RedisJWTSessionStore.Builder<A, B> builder(JWTSessionIdGenerator<A, B> sessionIdGenerator, RedisClient<String, String> redisClient, ObjectMapper mapper, Class<A> sessionDataType, Class<B> statelessSessionDataType) {
		return new RedisJWTSessionStore.Builder<>(sessionIdGenerator, redisClient, mapper, sessionDataType, statelessSessionDataType);
	}

	/**
	 * <p>
	 * Creates a Redis JWT session store builder.
	 * </p>
	 *
	 * @param <A>                      the session data type
	 * @param <B>                      the stateless session data type
	 * @param sessionIdGenerator       a JWT session id generator
	 * @param redisClient              a Redis client
	 * @param mapper                   an object mapper
	 * @param sessionDataType          the session data type
	 * @param statelessSessionDataType the stateless session data type
	 *
	 * @return a Redis JWT session store builder
	 */
	public static <A, B> RedisJWTSessionStore.Builder<A, B> builder(JWTSessionIdGenerator<A, B> sessionIdGenerator, RedisClient<String, String> redisClient, ObjectMapper mapper, Type sessionDataType, Type statelessSessionDataType) {
		return new RedisJWTSessionStore.Builder<>(sessionIdGenerator, redisClient, mapper, sessionDataType, statelessSessionDataType);
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

	@Override
	@SuppressWarnings("unchecked")
	public JWTSessionIdGenerator<A, B> getSessionIdGenerator() {
		return (JWTSessionIdGenerator<A, B>)super.getSessionIdGenerator();
	}

	@Override
	public Mono<JWTSession<A, B>> create() {
		return Mono.defer(() -> {
			RedisJWTSession<A, B> session = new RedisJWTSession<>((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator, this, this.maxInactiveInterval, this.maxInactiveInterval == null ? System.currentTimeMillis() + this.expireAfterPeriod : null, this.statelessSessionDataSaveStrategy);
			return session.refreshId(true)
				.flatMap(ign -> this.save(session, true))
				.thenReturn(session);
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
				.flatMap(jwt -> this.redisClient.objectIdletime(this.getSessionKey(jwt.getPayload().getJWTId()))
					.map(idleTime -> new RedisJWTSession<>(
						(JWTSessionIdGenerator<A, B>)this.sessionIdGenerator,
						this,
						sessionId,
						jwt.getPayload(),
						jwt.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).map(claim -> this.mapper.<B>convertValue(claim.getValue(), this.statelessSessionDataType)).orElse(null),
						this.statelessSessionDataSaveStrategy,
						System.currentTimeMillis() - idleTime * 1000
					))
					.filter(session -> !session.isExpired())
				);
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
				.flatMap(jwt -> this.getDataByTokenId(jwt.getPayload().getJWTId()));
		}
		catch(JOSEObjectReadException e) {
			LOGGER.warn("Malformed session id: {}", sessionId, e);
			return Mono.empty();
		}
	}

	@Override
	public Mono<A> getDataByTokenId(String tokenId) {
		return this.redisClient
			.get(this.getSessionKey(tokenId))
			.mapNotNull(value -> {
				try {
					return this.sessionDataReader.readValue(value);
				}
				catch(JsonProcessingException e) {
					throw new UncheckedIOException(e);
				}
			});
	}

	@Override
	public Mono<Void> move(String sessionId, String newSessionId) throws IllegalStateException {
		return Mono.zip(((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).readJWT(sessionId), ((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).readJWT(newSessionId))
			.flatMap(tokens -> this.moveByTokenId(tokens.getT1().getPayload().getJWTId(), tokens.getT2()));
	}

	@Override
	public Mono<Void> moveByTokenId(String tokenId, JOSEObject<JWTClaimsSet, ?> newSessionJWT) throws IllegalStateException {
		String oldSessionKey = this.getSessionKey(tokenId);
		String newSessionKey = this.getSessionKey(newSessionJWT.getPayload().getJWTId());
		return this.redisClient
			.renamenx(oldSessionKey, newSessionKey)
			.flatMap(result -> {
				if(!result) {
					throw new IllegalStateException("Session with token ID " + newSessionJWT.getPayload().getJWTId() + " already exists");
				}
				return newSessionJWT.getPayload().getCustomClaim(JWTSSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL)
					.map(claim -> this.redisClient.expire(newSessionKey, claim.asLong() + 120))
					.orElseGet(() -> this.redisClient.expireat(newSessionKey, newSessionJWT.getPayload().getExpirationTime() + 120))
					.then();
			});
	}

	@Override
	public Mono<Void> remove(String sessionId) {
		return ((JWTSessionIdGenerator<A, B>)this.sessionIdGenerator).readJWT(sessionId)
			.flatMap(jwt -> this.removeByTokenId(jwt.getPayload().getJWTId()));
	}

	@Override
	public Mono<Void> removeByTokenId(String tokenId) {
		return this.redisClient.del(this.getSessionKey(tokenId)).then();
	}

	@Override
	public Mono<Void> save(JWTSession<A, B> session) throws IllegalArgumentException, IllegalStateException {
		if(!(session instanceof RedisJWTSessionStore.RedisJWTSession<A, B>)) {
			throw new IllegalArgumentException("Invalid session object");
		}
		return this.save((RedisJWTSessionStore.RedisJWTSession<A, B>)session, false);
	}

	/**
	 * <p>
	 * Saves the specified Redis JWT session.
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
	private Mono<Void> save(RedisJWTSession<A, B> session, boolean create) throws IllegalStateException {
		// if new we should fail if a value already exists
		return Mono.defer(() -> {
				if(session.isInvalidated()) {
					throw new IllegalStateException("Session has been invalidated");
				}
				return session.refreshId(false);
			})
			.then(Mono.defer(() -> {
				if(create || (session.isSessionDataSet() || (session.isSessionDataFetched() && this.sessionDataSaveStrategy.getAndSetSaveState(session.getSessionData(), false)))) {
					RedisStringReactiveOperations.StringSetBuilder<String, String> saveBuilder = this.redisClient.set();

					if(create) {
						saveBuilder.nx();
					}
					else {
						saveBuilder.xx();
					}

					if(session.getMaxInactiveInterval() != null) {
						saveBuilder.px(session.getMaxInactiveInterval() + 120000);
					}
					else {
						saveBuilder.pxat(session.getExpirationTime() + 120000);
					}

					try {
						return saveBuilder
							.build(this.getSessionKey(session.getTokenId()), this.sessionDataWriter.writeValueAsString(session.getSessionData()))
							.doOnSuccess(result -> {
								if(!result.equals("OK")) {
									throw new IllegalStateException("Error saving session " + session.getId());
								}
							})
							.then();
					}
					catch(JsonProcessingException e) {
						throw new UncheckedIOException(e);
					}
				}
				else if(session.getMaxInactiveInterval() != null) {
					return this.redisClient.pexpire(this.getSessionKey(session.getTokenId()), session.getMaxInactiveInterval() + 120000).then();
				}
				else if(session.getOriginalId() != null && session.getOriginalId().equals(session.getId())) {
					return this.redisClient.touch(this.getSessionKey(session.getTokenId())).then();
				}
				else {
					return this.redisClient.pexpireat(this.getSessionKey(session.getTokenId()), session.getExpirationTime() + 120000).then();
				}
			}));
	}

	/**
	 * <p>
	 * A Redis JWT session implementation.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 * @param <B> the stateless session data type
	 */
	private static class RedisJWTSession<A, B> extends AbstractJWTSession<A, B> {

		private final Mono<A> sessionDataResolver = Mono.defer(() -> {
			if(this.sessionDataFetched || this.sessionDataSet) {
				return Mono.justOrEmpty(this.sessionData);
			}
			return ((JWTSessionStore<A, B>)this.sessionStore).getDataByTokenId(this.tokenId)
				.doOnSuccess(data -> {
					this.sessionData = data;
					this.sessionDataFetched = true;
				})
				.share();
		});

		private boolean sessionDataFetched;
		private boolean sessionDataSet;
		private A sessionData;

		/**
		 * <p>
		 * Creates a new Redis JWT session.
		 * </p>
		 *
		 * @param sessionIdGenerator               a JWT session id generator
		 * @param sessionStore                     a Redis JWT session store
		 * @param maxInactiveInterval              the maximum inactive interval in milliseconds
		 * @param expirationTime                   the expiration time in milliseconds
		 * @param statelessSessionDataSaveStrategy the stateless session data save strategy
		 */
		public RedisJWTSession(JWTSessionIdGenerator<A, B> sessionIdGenerator, RedisJWTSessionStore<A, B> sessionStore, Long maxInactiveInterval, Long expirationTime, SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy) {
			super(sessionIdGenerator, sessionStore, maxInactiveInterval, expirationTime, statelessSessionDataSaveStrategy);
		}

		/**
		 * <p>
		 * Creates an existing Redis JWT session.
		 * </p>
		 *
		 * @param sessionIdGenerator               a JWT session id generator
		 * @param sessionStore                     a Redis JWT session store
		 * @param id                               a JWT session id
		 * @param claimsSet                        a JWT session claims set
		 * @param statelessData                    the stateless session data
		 * @param statelessSessionDataSaveStrategy the stateless session data
		 * @param lastAccessedTime                 the last accessed time in milliseconds
		 */
		public RedisJWTSession(JWTSessionIdGenerator<A, B> sessionIdGenerator, RedisJWTSessionStore<A, B> sessionStore, String id, JWTClaimsSet claimsSet, B statelessData, SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy, long lastAccessedTime) {
			super(sessionIdGenerator, sessionStore, id, claimsSet, statelessData, statelessSessionDataSaveStrategy, lastAccessedTime, false);
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
	 * The Redis JWT session store builder.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.13
	 *
	 * @param <A> the session data type
	 * @param <B> the stateless session data type
	 */
	public static class Builder<A, B> extends AbstractSessionStore.Builder<A, JWTSession<A, B>, RedisJWTSessionStore<A, B>, RedisJWTSessionStore.Builder<A, B>> {

		private final RedisClient<String, String> redisClient;
		private final ObjectMapper mapper;
		private final Type sessionDataType;
		private final Type statelessSessionDataType;

		private String keyPrefix;
		private SessionDataSaveStrategy<A> sessionDataSaveStrategy;
		private SessionDataSaveStrategy<B> statelessSessionDataSaveStrategy;

		/**
		 * <p>
		 * Creates a Redis JWT session store builder.
		 * </p>
		 *
		 * @param sessionIdGenerator       a JWT session id generator
		 * @param redisClient              a Redis client
		 * @param mapper                   an object mapper
		 * @param sessionDataType          the session data type
		 * @param statelessSessionDataType the stateless session data type
		 */
		private Builder(JWTSessionIdGenerator<A, B> sessionIdGenerator, RedisClient<String, String> redisClient, ObjectMapper mapper, Class<A> sessionDataType, Class<B> statelessSessionDataType) {
			this(sessionIdGenerator, redisClient, mapper, (Type)sessionDataType, (Type)statelessSessionDataType);
		}

		/**
		 * <p>
		 * Creates a Redis JWT session store builder.
		 * </p>
		 *
		 * @param sessionIdGenerator       a JWT session id generator
		 * @param redisClient              a Redis client
		 * @param mapper                   an object mapper
		 * @param sessionDataType          the session data type
		 * @param statelessSessionDataType the stateless session data type
		 */
		private Builder(JWTSessionIdGenerator<A, B> sessionIdGenerator, RedisClient<String, String> redisClient, ObjectMapper mapper, Type sessionDataType, Type statelessSessionDataType) {
			super(sessionIdGenerator);
			this.redisClient = redisClient;
			this.mapper = mapper;
			this.sessionDataType = sessionDataType;
			this.statelessSessionDataType = statelessSessionDataType;

			this.keyPrefix = DEFAULT_KEY_PREFIX;
			this.statelessSessionDataSaveStrategy = SessionDataSaveStrategy.onSetOnly();
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
		public Builder<A, B> keyPrefix(String keyPrefix) {
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
		public Builder<A, B> sessionDataSaveStrategy(SessionDataSaveStrategy<A> sessionDataSaveStrategy) {
			this.sessionDataSaveStrategy = sessionDataSaveStrategy != null ? sessionDataSaveStrategy : SessionDataSaveStrategy.onGet();
			return this;
		}

		/**
		 * <p>
		 * Sets the stateless session data save strategy.
		 * </p>
		 *
		 * <p>
		 * Particular care must be taken when defining the stateless session data save strategy as this will trigger the refresh of the session id. It is important to make sure to only do it if/when
		 * it makes sense in order to avoid side effects, as a result {@link SessionDataSaveStrategy#onGet()} is not suited for stateless session data and shall not be used.
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
		public RedisJWTSessionStore<A, B> build() {
			return new RedisJWTSessionStore<>(this.sessionIdGenerator, this.maxInactiveInterval, this.expireAfterPeriod, this.redisClient, this.mapper, this.sessionDataType, this.sessionDataSaveStrategy, this.statelessSessionDataType, this.statelessSessionDataSaveStrategy, this.keyPrefix);
		}
	}
}
