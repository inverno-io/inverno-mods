/*
 * Copyright 2025 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.inverno.mod.session.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.base.reflect.Types;
import io.inverno.mod.boot.converter.JacksonStringConverter;
import io.inverno.mod.boot.converter.JsonStringMediaTypeConverter;
import io.inverno.mod.boot.converter.TextStringMediaTypeConverter;
import io.inverno.mod.redis.RedisClient;
import io.inverno.mod.redis.lettuce.PoolRedisClient;
import io.inverno.mod.security.jose.Jose;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.InMemoryJWKStore;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.SessionDataSaveStrategy;
import io.inverno.mod.session.internal.jwt.JWTSSessionIdGenerator;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@EnabledIf( value = "isEnabled", disabledReason = "Failed to connect to test Redis database" )
public class RedisJWTSessionStoreTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final io.lettuce.core.RedisClient REDIS_CLIENT = io.lettuce.core.RedisClient.create();

	public static boolean isEnabled() {
		try (StatefulRedisConnection<String, String> connection = REDIS_CLIENT.connect(RedisURI.create("redis://localhost:6379"))) {
			return true;
		}
		catch (RedisConnectionException e) {
			return false;
		}
	}

	private static RedisClient<String, String> redisClient;

	private static Jose joseModule;
	private static JWTSessionIdGenerator<Map<String, String>, Map<String, String>> jwtSessionIdGenerator;

	@BeforeAll
	public static void init() {
		BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
			() -> REDIS_CLIENT.connectAsync(StringCodec.UTF8, RedisURI.create("redis://localhost:6379")),
			BoundedPoolConfig.create()
		);

		redisClient = new PoolRedisClient<>(pool, String.class, String.class);

		JsonStringMediaTypeConverter jsonConverter = new JsonStringMediaTypeConverter(new JacksonStringConverter(new ObjectMapper()));
		TextStringMediaTypeConverter textConverter = new TextStringMediaTypeConverter(new StringConverter());

		joseModule = new Jose.Builder(List.of(jsonConverter, textConverter)).setJwkStore(new InMemoryJWKStore()).build();
		joseModule.start();

		String keyId = UUID.randomUUID().toString();
		joseModule.jwkService().oct().generator()
			.keyId(keyId)
			.algorithm(OCTAlgorithm.HS256.getAlgorithm())
			.generate()
			.map(JWK::trust)
			.flatMap(joseModule.jwkService().store()::set)
			.block();

		jwtSessionIdGenerator = JWTSessionIdGenerator.jws(joseModule.jwtService(), headers -> headers.keyId(keyId).algorithm(OCTAlgorithm.HS256.getAlgorithm()));
	}

	@AfterAll
	public static void destroy() {
		joseModule.stop();
	}

	@AfterEach
	public void cleanup() {
		REDIS_CLIENT.connect(RedisURI.create("redis://localhost:6379")).reactive().flushall().block();
	}

	public static RedisJWTSessionStore.Builder<Map<String, String>, Map<String, String>> newSessionStoreBuilder() {
		return RedisJWTSessionStore.builder(
			jwtSessionIdGenerator,
			redisClient,
			MAPPER,
			Types.type(Map.class).type(String.class).and().type(String.class).and().build(),
			Types.type(Map.class).type(String.class).and().type(String.class).and().build()
		);
	}

	@Test
	public void create_should_create_and_persist_session() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.maxInactiveInterval(300000L)
			.build();

		Assertions.assertEquals(300000L, sessionStore.getMaxInactiveInterval());
		Assertions.assertNull(sessionStore.getExpireAfterPeriod());
		Assertions.assertEquals(jwtSessionIdGenerator, sessionStore.getSessionIdGenerator());

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();

		Assertions.assertNotNull(session);
		Assertions.assertNull(session.getOriginalId());
		Assertions.assertNotNull(session.getId());
		Assertions.assertTrue(session.getCreationTime() <= System.currentTimeMillis());
		Assertions.assertTrue(session.getLastAccessedTime() <= System.currentTimeMillis());
		Assertions.assertEquals(300000L, session.getMaxInactiveInterval());
		Assertions.assertEquals(300000L + session.getLastAccessedTime(), session.getExpirationTime());
		Assertions.assertTrue(session.isNew());
		Assertions.assertFalse(session.isInvalidated());
		Assertions.assertFalse(session.isExpired());
		Assertions.assertNull(session.getData().block());
		Assertions.assertNull(session.getStatelessData());

		JWS<JWTClaimsSet> jwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		Assertions.assertEquals(Math.floorDiv(session.getCreationTime(), 1000), jwtSessionId.getPayload().getIssuedAt());
		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(300000L, jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).map(JWTClaimsSet.Claim::asLong).orElse(null));
		Assertions.assertTrue(jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();

		Assertions.assertNotNull(resolvedSession);
	}

	@Test
	public void given_expireAfterPeriod_create_should_create_fixed_time_expiring_session() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.expireAfterPeriod(300000L)
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNull(session.getMaxInactiveInterval());
		Assertions.assertEquals(session.getCreationTime() + 300000L, session.getExpirationTime(), 10L);
	}

	@Test
	public void given_invalid_session_id_get_should_return_empty() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Assertions.assertNull(sessionStore.get("invalid").block());
	}

	@Test
	public void given_updates_session_should_not_be_updated_on_the_fly() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());
		session.getData(HashMap::new).block().put("someKey", "someValue");
		session.getStatelessData(HashMap::new).put("someStatelessKey", "someStatelessValue");

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertNotNull(resolvedSession.getMaxInactiveInterval());
		Assertions.assertNotEquals(session.getExpirationTime(), resolvedSession.getExpirationTime());
		Assertions.assertNull(resolvedSession.getData().block());

		resolvedSession.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(resolvedSession.getMaxInactiveInterval());
		resolvedSession.getData(HashMap::new).block().put("someKey", "someValue");
		session.getStatelessData(HashMap::new).put("someStatelessKey", "someStatelessValue");

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession2 = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession2);
		Assertions.assertNotNull(resolvedSession2.getMaxInactiveInterval());
		Assertions.assertNotEquals(resolvedSession.getExpirationTime(), resolvedSession2.getExpirationTime());
		Assertions.assertNull(resolvedSession2.getData().block());
	}

	@Test
	public void given_stateful_data_updates_session_save_should_persist_stateful_data() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		Session<Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());
	}

	@Test
	public void given_onSetOnly_data_save_strategy_and_new_data_save_should_persist_data() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.sessionDataSaveStrategy(SessionDataSaveStrategy.onSetOnly())
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());
	}

	@Test
	public void given_onSetOnly_data_save_strategy_and_existing_data_and_setData_not_invoked_save_should_not_persist_data() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.sessionDataSaveStrategy(SessionDataSaveStrategy.onSetOnly())
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		session = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(session);

		session.getData().block().put("someOtherKey", "someOtherValue");

		session.save().block();

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());
	}

	@Test
	public void given_onSetOnly_data_save_strategy_and_existing_data_and_setData_invoked_save_should_persist_data() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.sessionDataSaveStrategy(SessionDataSaveStrategy.onSetOnly())
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		session = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(session);

		Map<String, String> sessionData = session.getData().block();
		Assertions.assertNotNull(sessionData);
		sessionData.put("someOtherKey", "someOtherValue");
		session.setData(sessionData);

		session.save().block();

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(Map.of("someKey", "someValue", "someOtherKey", "someOtherValue"), resolvedSession.getData().block());
	}

	@Test
	public void given_metadata_updates_session_save_should_refresh_jwt_session_id() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());

		JWS<JWTClaimsSet> jwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		String sessionIdBeforeSave = session.getId();
		session.save().block();
		Assertions.assertNotEquals(sessionIdBeforeSave, session.getId());

		JWS<JWTClaimsSet> newJwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		Assertions.assertEquals(Math.floorDiv(session.getCreationTime(), 1000), newJwtSessionId.getPayload().getIssuedAt());
		Assertions.assertNotNull(newJwtSessionId.getPayload().getJWTId());
		Assertions.assertNotEquals(jwtSessionId.getPayload().getJWTId(), newJwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(Math.floorDiv(session.getExpirationTime(), 1000), newJwtSessionId.getPayload().getExpirationTime());
		Assertions.assertTrue(newJwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).isEmpty());
		Assertions.assertTrue(newJwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());

		Session<Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
	}

	@Test
	public void given_new_stateless_data_session_save_should_refresh_jwt_session_id() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.getStatelessData(HashMap::new).put("someStatelessKey", "someStatelessValue");

		JWS<JWTClaimsSet> jwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		String sessionIdBeforeSave = session.getId();
		session.save().block();
		Assertions.assertNotEquals(sessionIdBeforeSave, session.getId());

		JWS<JWTClaimsSet> newJwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		Assertions.assertEquals(Math.floorDiv(session.getCreationTime(), 1000), newJwtSessionId.getPayload().getIssuedAt());
		Assertions.assertNotNull(newJwtSessionId.getPayload().getJWTId());
		Assertions.assertNotEquals(jwtSessionId.getPayload().getJWTId(), newJwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(session.getMaxInactiveInterval(), newJwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).map(JWTClaimsSet.Claim::asLong).orElse(null));
		Assertions.assertEquals(Map.of("someStatelessKey", "someStatelessValue"), newJwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).map(claim -> claim.<Map<String, String>>as(Map.class)).orElse(null));

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
	}

	@Test
	public void given_existing_stateless_data_update_session_save_should_not_refresh_jwt_session_id() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.getStatelessData(HashMap::new).put("someStatelessKey", "someStatelessValue");

		String sessionIdBeforeSave = session.getId();
		session.save().block();
		Assertions.assertNotEquals(sessionIdBeforeSave, session.getId());

		session = sessionStore.get(session.getId()).block();
		session.getStatelessData().put("someOtherStatelessKey", "someOtherStatelessValue");

		sessionIdBeforeSave = session.getId();
		session.save().block();
		Assertions.assertEquals(sessionIdBeforeSave, session.getId());

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(Map.of("someStatelessKey", "someStatelessValue"), resolvedSession.getStatelessData());
	}

	@Test
	public void given_existing_stateless_data_update_and_setStatelessData_invoked_session_save_should_refresh_jwt_session_id() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.getStatelessData(HashMap::new).put("someStatelessKey", "someStatelessValue");

		String sessionIdBeforeSave = session.getId();
		session.save().block();
		Assertions.assertNotEquals(sessionIdBeforeSave, session.getId());

		session = sessionStore.get(session.getId()).block();
		Map<String, String> statelessData = session.getStatelessData();
		statelessData.put("someOtherStatelessKey", "someOtherStatelessValue");
		session.setStatelessData(statelessData);

		sessionIdBeforeSave = session.getId();
		session.save().block();
		Assertions.assertNotEquals(sessionIdBeforeSave, session.getId());

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(Map.of("someStatelessKey", "someStatelessValue", "someOtherStatelessKey", "someOtherStatelessValue"), resolvedSession.getStatelessData());

		JWS<JWTClaimsSet> newJwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		Assertions.assertEquals(Math.floorDiv(session.getCreationTime(), 1000), newJwtSessionId.getPayload().getIssuedAt());
		Assertions.assertEquals(Map.of("someStatelessKey", "someStatelessValue", "someOtherStatelessKey", "someOtherStatelessValue"), newJwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).map(claim -> claim.<Map<String, String>>as(Map.class)).orElse(null));
	}

	@Test
	public void soft_refreshId_should_not_update_session_id() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		String sessionIdBeforeRefresh = session.getId();
		String refreshedSessionId = session.refreshId().block();

		Assertions.assertEquals(sessionIdBeforeRefresh, refreshedSessionId);
		Assertions.assertEquals(sessionIdBeforeRefresh, session.getId());
	}

	@Test
	public void hard_refreshId_should_update_session_id_and_move_session() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		String sessionIdBeforeRefresh = session.getId();
		String refreshedSessionId = session.refreshId(true).block();

		Assertions.assertNotEquals(sessionIdBeforeRefresh, refreshedSessionId);
		Assertions.assertNotEquals(session.getOriginalId(), session.getId());
		Assertions.assertNotEquals(sessionIdBeforeRefresh, session.getId());
		Assertions.assertEquals(refreshedSessionId, session.getId());

		JWS<JWTClaimsSet> jwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		Assertions.assertEquals(Math.floorDiv(session.getCreationTime(), 1000), jwtSessionId.getPayload().getIssuedAt());
		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(session.getMaxInactiveInterval(), jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).map(JWTClaimsSet.Claim::asLong).orElse(null));
		Assertions.assertTrue(jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(sessionIdBeforeRefresh).block();
		Assertions.assertNull(resolvedSession);

		resolvedSession = sessionStore.get(refreshedSessionId).block();
		Assertions.assertNotNull(resolvedSession);
	}

	@Test
	public void getData_should_return_data() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		Map<String, String> resolvedData = sessionStore.getData(session.getId()).block();

		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedData);
	}

	@Test
	public void getDataByTokenId_should_return_data() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		JWS<JWTClaimsSet> jwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		Map<String, String> resolvedData = sessionStore.getDataByTokenId(jwtSessionId.getPayload().getJWTId()).block();

		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedData);
	}

	@Test
	public void move_should_move_session() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		String newSessionId = sessionStore.getSessionIdGenerator().generate(session).block();

		sessionStore.move(session.getId(), newSessionId).block();

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(newSessionId).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(newSessionId, resolvedSession.getId());
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());

		JWS<JWTClaimsSet> jwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		Assertions.assertEquals(Math.floorDiv(session.getCreationTime(), 1000), jwtSessionId.getPayload().getIssuedAt());
		Assertions.assertNotNull(jwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(session.getMaxInactiveInterval(), jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).map(JWTClaimsSet.Claim::asLong).orElse(null));
		Assertions.assertTrue(jwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());
	}

	@Test
	public void moveByTokenId_should_move_session() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		JWS<JWTClaimsSet> jwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		String newSessionId = sessionStore.getSessionIdGenerator().generate(session).block();
		JWS<JWTClaimsSet> newJwtSessionId = joseModule.jwtService().jwsReader().read(newSessionId).block();

		sessionStore.moveByTokenId(jwtSessionId.getPayload().getJWTId(), newJwtSessionId).block();

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(newSessionId).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(newSessionId, resolvedSession.getId());
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());

		JWS<JWTClaimsSet> refreshedJwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		Assertions.assertEquals(Math.floorDiv(session.getCreationTime(), 1000), refreshedJwtSessionId.getPayload().getIssuedAt());
		Assertions.assertNotNull(refreshedJwtSessionId.getPayload().getJWTId());
		Assertions.assertEquals(session.getMaxInactiveInterval(), refreshedJwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_MAX_INACTIVE_INTERVAL).map(JWTClaimsSet.Claim::asLong).orElse(null));
		Assertions.assertTrue(refreshedJwtSessionId.getPayload().getCustomClaim(JWTSessionIdGenerator.CLAIM_SESSION_DATA).isEmpty());
	}

	@Test
	public void remove_should_remove_session() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		sessionStore.remove(session.getId()).block();

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNull(resolvedSession);
	}

	@Test
	public void removeByTokenId_should_remove_session() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		JWS<JWTClaimsSet> jwtSessionId = joseModule.jwtService().jwsReader().read(session.getId()).block();

		sessionStore.removeByTokenId(jwtSessionId.getPayload().getJWTId()).block();

		Assertions.assertNull(sessionStore.getDataByTokenId(jwtSessionId.getPayload().getJWTId()).block());
		Assertions.assertNull(sessionStore.get(session.getId()).block());
	}

	@Test
	public void session_invalidate_should_remove_session() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.invalidate().block();
		Assertions.assertTrue(session.isInvalidated());

		Assertions.assertNull(sessionStore.get(session.getId()).block());
	}

	@Test
	public void session_should_expire_after_inactivity_period() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.maxInactiveInterval(2000L) // expiration is precise to the second
			.build();

		String sessionId = sessionStore.create().block().getId();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.get(sessionId).block();
		Assertions.assertEquals(session.getLastAccessedTime() + 2000L, session.getExpirationTime());

		Awaitility.await().pollDelay(Duration.ofMillis(1000)).pollInterval(Duration.ofMillis(10)).atMost(Duration.ofMillis(1010)).until(() -> !session.isExpired());

		Assertions.assertNotNull(sessionStore.get(session.getId()).block());
		session.save().block();

		JWTSession<Map<String, String>, Map<String, String>> touchedSession = sessionStore.get(sessionId).block();
		Assertions.assertEquals(touchedSession.getLastAccessedTime() + 2000L, touchedSession.getExpirationTime());
		Assertions.assertEquals(touchedSession.getCreationTime() + 1000L, touchedSession.getLastAccessedTime(), 10);
		Assertions.assertEquals(touchedSession.getLastAccessedTime() + 2000L, touchedSession.getExpirationTime());

		Awaitility.await().pollDelay(Duration.ofMillis(2000)).pollInterval(Duration.ofMillis(10)).atMost(Duration.ofMillis(2010)).until(touchedSession::isExpired);
		Assertions.assertNull(sessionStore.get(touchedSession.getId()).block());
	}

	@Test
	public void session_should_expire_at_expiration_time() {
		RedisJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.expireAfterPeriod(2000L) // expiration is precise to the second
			.build();

		String sessionId = sessionStore.create().block().getId();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.get(sessionId).block();
		Assertions.assertEquals(session.getCreationTime() + 2000L, session.getExpirationTime(), 10);

		Awaitility.await().pollDelay(Duration.ofMillis(1000)).pollInterval(Duration.ofMillis(10)).atMost(Duration.ofMillis(1010)).until(() -> !session.isExpired());

		Assertions.assertNotNull(sessionStore.get(session.getId()).block());
		session.save().block();

		JWTSession<Map<String, String>, Map<String, String>> touchedSession = sessionStore.get(sessionId).block();
		Assertions.assertEquals(touchedSession.getCreationTime() + 1000L, touchedSession.getLastAccessedTime(), 10);
		Assertions.assertEquals(touchedSession.getCreationTime() + 2000L, touchedSession.getExpirationTime(), 10);

		Awaitility.await().pollDelay(Duration.ofMillis(1000)).pollInterval(Duration.ofMillis(10)).atMost(Duration.ofMillis(1010)).until(() -> Math.floorDiv(System.currentTimeMillis(), 1000) * 1000 >= touchedSession.getExpirationTime());
		Assertions.assertTrue(touchedSession.isExpired());

		Assertions.assertNull(sessionStore.get(sessionId).block());
	}
}
