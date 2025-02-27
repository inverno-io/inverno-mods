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
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.base.reflect.Types;
import io.inverno.mod.boot.converter.JacksonStringConverter;
import io.inverno.mod.boot.converter.JsonStringMediaTypeConverter;
import io.inverno.mod.boot.converter.TextStringMediaTypeConverter;
import io.inverno.mod.security.jose.Jose;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.InMemoryJWKStore;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.internal.jwt.JWTSSessionIdGenerator;
import io.netty.channel.EventLoop;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class InMemoryJWTSessionStoreTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);
	private static final List<ScheduledFuture<?>> FUTURES = new ArrayList<>();

	private static EventLoop eventLoopMock;
	private static Jose joseModule;
	private static JWTSessionIdGenerator<Map<String, String>, Map<String, String>> jwtSessionIdGenerator;

	@BeforeAll
	public static void init() {
		eventLoopMock = Mockito.mock(EventLoop.class);
		Mockito.when(eventLoopMock.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any(TimeUnit.class))).thenAnswer(invocation -> {
			FUTURES.add(EXECUTOR.schedule(invocation.getArgument(0, Runnable.class), invocation.getArgument(1, Long.class), invocation.getArgument(2, TimeUnit.class)));
			return Mockito.mock(io.netty.util.concurrent.ScheduledFuture.class);
		});

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
		for(ScheduledFuture<?> future : FUTURES) {
			future.cancel(true);
		}
		Mockito.reset(eventLoopMock);
	}

	public static InMemoryJWTSessionStore.Builder<Map<String, String>, Map<String, String>> newSessionStoreBuilder() {
		Reactor reactorMock = Mockito.mock(Reactor.class);
		Mockito.when(reactorMock.getEventLoop()).thenReturn(eventLoopMock);
		return InMemoryJWTSessionStore.builder(jwtSessionIdGenerator, reactorMock, MAPPER, Types.type(Map.class).type(String.class).and().type(String.class).and().build());
	}

	@Test
	public void create_should_create_and_persist_session() {
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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

		Mockito.verify(eventLoopMock).schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any(TimeUnit.class));

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();

		Assertions.assertNotNull(resolvedSession);
	}

	@Test
	public void given_expireAfterPeriod_create_should_create_fixed_time_expiring_session() {
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.expireAfterPeriod(300000L)
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNull(session.getMaxInactiveInterval());
		Assertions.assertEquals(session.getCreationTime() + 300000L, session.getExpirationTime(), 10L);
	}

	@Test
	public void given_invalid_session_id_get_should_return_empty() {
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Assertions.assertNull(sessionStore.get("invalid").block());
	}

	@Test
	public void given_updates_session_should_not_be_updated_on_the_fly() {
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
	public void given_metadata_updates_session_save_should_refresh_jwt_session_id() {
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		sessionStore.remove(session.getId()).block();

		JWTSession<Map<String, String>, Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNull(resolvedSession);
	}

	@Test
	public void removeByTokenId_should_remove_session() {
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
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
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.invalidate().block();
		Assertions.assertTrue(session.isInvalidated());

		Assertions.assertNull(sessionStore.get(session.getId()).block());
	}

	@Test
	public void session_should_expire_after_inactivity_period() {
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.maxInactiveInterval(250L)
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertEquals(session.getLastAccessedTime() + 250L, session.getExpirationTime());

		Awaitility.await().pollDelay(Duration.ofMillis(100)).pollInterval(Duration.ofMillis(10)).atMost(Duration.ofMillis(110)).until(() -> !session.isExpired());

		Assertions.assertNotNull(sessionStore.get(session.getId()).block());
		session.save().block();
		Assertions.assertEquals(session.getCreationTime() + 100L, session.getLastAccessedTime(), 10);
		Assertions.assertEquals(session.getLastAccessedTime() + 250L, session.getExpirationTime());

		Awaitility.await().pollDelay(Duration.ofMillis(250)).pollInterval(Duration.ofMillis(10)).atMost(Duration.ofMillis(260)).until(session::isExpired);
		Assertions.assertNull(sessionStore.get(session.getId()).block());
	}

	@Test
	public void session_should_expire_at_expiration_time() {
		InMemoryJWTSessionStore<Map<String, String>, Map<String, String>> sessionStore = newSessionStoreBuilder()
			.expireAfterPeriod(250L)
			.build();

		JWTSession<Map<String, String>, Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertEquals(session.getCreationTime() + 250L, session.getExpirationTime(), 10);

		Awaitility.await().pollDelay(Duration.ofMillis(100)).pollInterval(Duration.ofMillis(10)).atMost(Duration.ofMillis(110)).until(() -> !session.isExpired());

		Assertions.assertNotNull(sessionStore.get(session.getId()).block());
		session.save().block();
		Assertions.assertEquals(session.getCreationTime() + 100L, session.getLastAccessedTime(), 10);
		Assertions.assertEquals(session.getCreationTime() + 250L, session.getExpirationTime(), 10);

		Awaitility.await().pollDelay(Duration.ofMillis(140)).pollInterval(Duration.ofMillis(10)).atMost(Duration.ofMillis(160)).until(() -> System.currentTimeMillis() >= session.getExpirationTime());
		Assertions.assertTrue(session.isExpired());
		Assertions.assertNull(sessionStore.get(session.getId()).block());
	}
}
