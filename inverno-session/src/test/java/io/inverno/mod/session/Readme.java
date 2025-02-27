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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.redis.RedisClient;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwt.JWTService;
import io.inverno.mod.session.jwt.InMemoryJWTSessionStore;
import io.inverno.mod.session.jwt.JWTSession;
import io.inverno.mod.session.jwt.JWTSessionIdGenerator;
import io.inverno.mod.session.jwt.JWTSessionStore;
import io.inverno.mod.session.jwt.RedisJWTSessionStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * <p>
 *
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {

	@Wrapper @Bean( visibility = Bean.Visibility.PRIVATE )
	public static class SessionStoreWrapper implements Supplier<SessionStore<AtomicInteger, Session<AtomicInteger>>> {

		private final Reactor reactor;

		public SessionStoreWrapper(Reactor reactor) {
			this.reactor = reactor;
		}

		@Override
		public SessionStore<AtomicInteger, Session<AtomicInteger>> get() {
			return InMemoryBasicSessionStore.<AtomicInteger>builder(this.reactor).build();
		}
	}

	@Bean
	public static class SomeService {

		private final SessionStore<AtomicInteger, Session<AtomicInteger>> sessionStore;

		public SomeService(SessionStore<AtomicInteger, Session<AtomicInteger>> sessionStore) {
			this.sessionStore = sessionStore;
		}

		public String openSession() {
			return this.sessionStore.create().map(Session::getId).block();
		}

		public int incrementCounter(String sessionId) throws IllegalStateException {
			return this.sessionStore
				.getData(sessionId)
				.map(AtomicInteger::incrementAndGet)
				.switchIfEmpty(Mono.error(new IllegalStateException("Session does not exist or has expired")))
				.block();
		}

		public void closeSession(String sessionId) {
			this.sessionStore.get(sessionId).flatMap(Session::invalidate).block();
		}
	}

	public static void sessionStore() {
		SessionStore<Map<String, String>, Session<Map<String, String>>> sessionStore = null;

		Session<Map<String, String>> newSession = sessionStore.create().block();                           // 1
		String sessionId = newSession.getId();

		Session<Map<String, String>> resolvedSession = sessionStore                                        // 2
			.get(sessionId)
			.switchIfEmpty(Mono.error(new IllegalStateException("Session does not exist or has expired"))) // 3
			.block();

		Map<String, String> sessionData = resolvedSession.getData(HashMap::new).block();                   // 4
		sessionData.put("someAttribute", "someValue");                                                     // 5

		sessionStore.save(resolvedSession).block();                                                        // 6

		sessionStore.remove(sessionId).block();                                                            // 7

		SessionIdGenerator<Map<String, String>, Session<Map<String, String>>> base64UUIDSessionIdGenerator = SessionIdGenerator.uuid();

		SessionIdGenerator<Map<String, String>, Session<Map<String, String>>> uuidSessionIdGenerator = SessionIdGenerator.uuid(false);
	}

	public static void sessionData() {
		SessionStore<Map<String, String>, Session<Map<String, String>>> sessionStore = null;
		String sessionId = null;

		Map<String, String> sessionData = sessionStore.getData(sessionId).block();
		String someAttributeValue = sessionData.get("someAttribute");
	}

	public static void uuidSessionIdGenerator() {
		SessionIdGenerator<Map<String, String>, Session<Map<String, String>>> base64UUIDSessionIdGenerator = SessionIdGenerator.uuid();
		SessionIdGenerator<Map<String, String>, Session<Map<String, String>>> uuidSessionIdGenerator = SessionIdGenerator.uuid(false);
	}

	public static void newSessionId() {
		SessionStore<SessionData, Session<SessionData>> sessionStore = null;
		Session<SessionData> session = sessionStore.create().block();

		String originalSessionId = session.getOriginalId(); // null because this is a new session
		String sessionId = session.getId();                 // the session id
	}

	public static void sessionId() {
		SessionStore<SessionData, Session<SessionData>> sessionStore = null;
		Session<SessionData> session = sessionStore.get("123456").block();

		String originalSessionId = session.getOriginalId(); // the original session id
		String sessionId = session.getId();                 // same as original session id since it was not refreshed
	}

	public static void sessionIdRefreshed() {
		SessionStore<SessionData, Session<SessionData>> sessionStore = null;
		Session<SessionData> session = sessionStore.get("123456").block();

		session.refreshId(true).block();

		String originalSessionId = session.getOriginalId(); // the original session id
		String sessionId = session.getId();                 // different from the original session id since it was refreshed
	}

	public static void sessionMaxInactiveInterval() {
		Session<SessionData> session = null;

		long lastAccessedTime = session.getLastAccessedTime();
		Long maxInactiveInterval = session.getMaxInactiveInterval(); // null when the session is set to expire at a specific time in the future

		long expirationTime = session.getExpirationTime();           // lastAccessedTime + maxInactiveInterval

		session.setMaxInactiveInterval(300000L); // set session to expire after 5 minutes of inactivity
	}

	public static void sessionExpirationTime() {
		Session<SessionData> session = null;

		session.setExpirationTime(System.currentTimeMillis() + 600000L); // expires in 10 minutes
		long expirationTime = session.getExpirationTime();               // the expiration time that was set

		Long maxInactiveInterval = session.getMaxInactiveInterval();     // null since explicit expiration time has been set
	}

	public static void sessionData2() {
		Session<SessionData> session = null;

		SessionData sessionData = session.getData(SessionData::new).block(); // create data when missing

		String someData = sessionData.getSomeData();                         // update data
		sessionData.setSomeData("Updated data");

		session.setData(sessionData);                                 // set data explicitly

		session.save().block();                                              // save the session
	}

	public static void sessionInvalidate() {
		Session<SessionData> session = null;

		session.invalidate().block();
	}

	public static void inMemoryBasicSessionStore() {
		Reactor reactor = null;

		InMemoryBasicSessionStore<SessionData> build = InMemoryBasicSessionStore
			.<SessionData>builder(reactor, SessionIdGenerator.uuid(true)) // generate Base64 encoded UUID as session id
			.cleanPeriod(InMemoryBasicSessionStore.DEFAULT_CLEAN_PERIOD)                // 5 minutes
			.maxInactiveInterval(Session.DEFAULT_MAX_INACTIVE_INTERVAL)                 // 30 minutes
			.expireAfterPeriod(Session.DEFAULT_MAX_INACTIVE_INTERVAL)                   // overrides max inactive interval which is set by default
			.build();
	}

	public static void redisBasicSessionStore() {
		RedisClient<String, String> redisClient = null;
		ObjectMapper mapper = null;

		RedisBasicSessionStore<SessionData> sessionStore = RedisBasicSessionStore.<SessionData>builder(redisClient, mapper, SessionData.class, SessionIdGenerator.uuid())
			.keyPrefix(RedisBasicSessionStore.DEFAULT_KEY_PREFIX)
			.maxInactiveInterval(Session.DEFAULT_MAX_INACTIVE_INTERVAL)                 // 30 minutes
			.expireAfterPeriod(Session.DEFAULT_MAX_INACTIVE_INTERVAL)                   // overrides max inactive interval which is set by default
			.sessionDataSaveStrategy(SessionDataSaveStrategy.onSetOnly())               // only save data when setData() is explicitly invoked on the session
			.build();

	}

	public static void jwtVoidData() {
		JWTSessionStore<Void, AuthenticationData> sessionStore = null;

		JWTSession<Void, AuthenticationData> jwtSession = sessionStore.create().block();

		jwtSession.setStatelessData(new AuthenticationData("jsmith"));

		jwtSession.save().block(); // trigger refresh session id since stateless data has been set

		jwtSession.getId(); // JWT containing authentication data
	}

	public static void jwts() {
		JWKService jwkService = null;
		JWTService jwtService = null;

		String keyId = UUID.randomUUID().toString();

		jwkService.oct().generator()
			.keyId(keyId)
			.algorithm(OCTAlgorithm.HS256.getAlgorithm())
			.generate()
			.map(JWK::trust)
			.flatMap(jwkService.store()::set)
			.block();

		JWTSessionIdGenerator<SessionData, StatelessSessionData> jwtsSessionIdGenerator = JWTSessionIdGenerator.jws(jwtService, headers -> headers
			.keyId(keyId)
			.algorithm(OCTAlgorithm.HS256.getAlgorithm())
		);
	}

	public static void jwte() {
		JWKService jwkService = null;
		JWTService jwtService = null;

		String keyId = UUID.randomUUID().toString();

		jwkService.ec().generator()
			.keyId(keyId)
			.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
			.curve(ECCurve.P_256.getCurve())
			.generate()
			.map(JWK::trust)
			.flatMap(jwkService.store()::set)
			.block();

		JWTSessionIdGenerator<SessionData, StatelessSessionData> jwtsSessionIdGenerator = JWTSessionIdGenerator.jwe(jwtService, header -> header
			.keyId(keyId)
			.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
			.encryptionAlgorithm(OCTAlgorithm.A256GCM.getAlgorithm())
		);
	}

	public static void inMemoryJWTSessionStore() {
		Reactor reactor = null;
		ObjectMapper mapper = null;
		JWTSessionIdGenerator<SessionData, StatelessSessionData> jwtSessionIdGenerator = null;

		InMemoryJWTSessionStore<SessionData, StatelessSessionData> jwtSessionStore = InMemoryJWTSessionStore.builder(
				jwtSessionIdGenerator,
				reactor,
				mapper,
				StatelessSessionData.class
			)
			.cleanPeriod(InMemoryBasicSessionStore.DEFAULT_CLEAN_PERIOD)           // 5 minutes
			.maxInactiveInterval(Session.DEFAULT_MAX_INACTIVE_INTERVAL)            // 30 minutes
			.expireAfterPeriod(Session.DEFAULT_MAX_INACTIVE_INTERVAL)              // overrides max inactive interval which is set by default
			.statelessSessionDataSaveStrategy(SessionDataSaveStrategy.onSetOnly()) // must be onSetOnly() by default
			.build();
	}

	public static void redisJWTSessionStore() {
		RedisClient<String, String> redisClient = null;
		ObjectMapper mapper = null;
		JWTSessionIdGenerator<SessionData, StatelessSessionData> jwtSessionIdGenerator = null;

		RedisJWTSessionStore<SessionData, StatelessSessionData> jwtSessionStore = RedisJWTSessionStore.builder(
				jwtSessionIdGenerator,
				redisClient,
				mapper,
				SessionData.class,
				StatelessSessionData.class
			)
			.keyPrefix(RedisJWTSessionStore.DEFAULT_KEY_PREFIX)
			.maxInactiveInterval(Session.DEFAULT_MAX_INACTIVE_INTERVAL)            // 30 minutes
			.expireAfterPeriod(Session.DEFAULT_MAX_INACTIVE_INTERVAL)              // overrides max inactive interval which is set by default
			.sessionDataSaveStrategy(SessionDataSaveStrategy.onSetOnly())          // only save stateful data when setData() is explicitly invoked on the session
			.statelessSessionDataSaveStrategy(SessionDataSaveStrategy.onSetOnly()) // must be onSetOnly() by default
			.build();
	}

	public static class SessionData {

		private String someData;

		public String getSomeData() {
			return someData;
		}

		public void setSomeData(String someData) {
			this.someData = someData;
		}
	}

	public static class StatelessSessionData {

		private String someData;

		public String getSomeData() {
			return someData;
		}

		public void setSomeData(String someData) {
			this.someData = someData;
		}
	}

	public static class AuthenticationData {

		private final String user;

		public AuthenticationData(String user) {
			this.user = user;
		}

		public String getUser() {
			return user;
		}
	}
}
