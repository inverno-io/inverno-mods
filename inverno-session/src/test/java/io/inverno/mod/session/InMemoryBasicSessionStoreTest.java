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

package io.inverno.mod.session;

import io.inverno.mod.base.concurrent.Reactor;
import io.netty.channel.EventLoop;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class InMemoryBasicSessionStoreTest {

	private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);
	private static final List<ScheduledFuture<?>> FUTURES = new ArrayList<>();

	private static EventLoop eventLoopMock;

	@BeforeAll
	public static void init() {
		eventLoopMock = Mockito.mock(EventLoop.class);
		Mockito.when(eventLoopMock.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any(TimeUnit.class))).thenAnswer(invocation -> {
			FUTURES.add(EXECUTOR.schedule(invocation.getArgument(0, Runnable.class), invocation.getArgument(1, Long.class), invocation.getArgument(2, TimeUnit.class)));
			return Mockito.mock(io.netty.util.concurrent.ScheduledFuture.class);
		});
	}

	@AfterEach
	public void cleanup() {
		for(ScheduledFuture<?> future : FUTURES) {
			future.cancel(true);
		}
		Mockito.reset(eventLoopMock);
	}

	public static InMemoryBasicSessionStore.Builder<Map<String, String>> newSessionStoreBuilder() {
		Reactor reactorMock = Mockito.mock(Reactor.class);
		Mockito.when(reactorMock.getEventLoop()).thenReturn(eventLoopMock);
		return InMemoryBasicSessionStore.builder(reactorMock);
	}

	@Test
	public void create_should_create_and_persist_session() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.maxInactiveInterval(300000L)
			.build();

		Assertions.assertEquals(300000L, sessionStore.getMaxInactiveInterval());
		Assertions.assertNull(sessionStore.getExpireAfterPeriod());
		Assertions.assertEquals(SessionIdGenerator.uuid(), sessionStore.getSessionIdGenerator());

		Session<Map<String, String>> session = sessionStore.create().block();

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

		Mockito.verify(eventLoopMock).schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any(TimeUnit.class));

		Session<Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();

		Assertions.assertNotNull(resolvedSession);
	}

	@Test
	public void given_expireAfterPeriod_create_should_create_fixed_time_expiring_session() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.expireAfterPeriod(300000L)
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNull(session.getMaxInactiveInterval());
		Assertions.assertEquals(session.getCreationTime() + 300000L, session.getExpirationTime(), 10L);
	}

	@Test
	public void given_invalid_session_id_get_should_return_empty() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Assertions.assertNull(sessionStore.get("invalid").block());
	}

	@Test
	public void given_updates_session_should_not_be_updated_on_the_fly() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());
		session.getData(HashMap::new).block().put("someKey", "someValue");

		Session<Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertNotNull(resolvedSession.getMaxInactiveInterval());
		Assertions.assertNotEquals(session.getExpirationTime(), resolvedSession.getExpirationTime());
		Assertions.assertNull(resolvedSession.getData().block());

		resolvedSession.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(resolvedSession.getMaxInactiveInterval());
		resolvedSession.getData(HashMap::new).block().put("someKey", "someValue");

		Session<Map<String, String>> resolvedSession2 = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession2);
		Assertions.assertNotNull(resolvedSession2.getMaxInactiveInterval());
		Assertions.assertNotEquals(resolvedSession.getExpirationTime(), resolvedSession2.getExpirationTime());
		Assertions.assertNull(resolvedSession2.getData().block());
	}

	@Test
	public void given_updates_session_save_should_persist_session() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());
		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		Session<Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertNull(resolvedSession.getMaxInactiveInterval());
		Assertions.assertEquals(session.getExpirationTime(), resolvedSession.getExpirationTime());
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());
	}

	@Test
	public void soft_refreshId_should_not_update_session_id() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());
		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		String sessionIdBeforeRefresh = session.getId();
		String refreshedSessionId = session.refreshId().block();

		Assertions.assertEquals(sessionIdBeforeRefresh, refreshedSessionId);
		Assertions.assertEquals(sessionIdBeforeRefresh, session.getId());
	}

	@Test
	public void hard_refreshId_should_update_session_id_and_move_session() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());
		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		String sessionIdBeforeRefresh = session.getId();
		String refreshedSessionId = session.refreshId(true).block();

		Assertions.assertNotEquals(sessionIdBeforeRefresh, refreshedSessionId);
		Assertions.assertNotEquals(session.getOriginalId(), session.getId());
		Assertions.assertNotEquals(sessionIdBeforeRefresh, session.getId());
		Assertions.assertEquals(refreshedSessionId, session.getId());

		Session<Map<String, String>> resolvedSession = sessionStore.get(sessionIdBeforeRefresh).block();
		Assertions.assertNull(resolvedSession);

		resolvedSession = sessionStore.get(refreshedSessionId).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertNull(resolvedSession.getMaxInactiveInterval());
		Assertions.assertEquals(session.getExpirationTime(), resolvedSession.getExpirationTime());
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());
	}

	@Test
	public void getData_should_return_data() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());
		session.getData(HashMap::new).block().put("someKey", "someValue");

		session.save().block();

		Map<String, String> resolvedData = sessionStore.getData(session.getId()).block();

		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedData);
	}

	@Test
	public void move_should_move_session() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());
		session.getData(HashMap::new).block().put("someKey", "someValue");

		String sessionId = session.getId();
		session.save().block();

		String newSessionId = sessionStore.getSessionIdGenerator().generate(session).block();

		sessionStore.move(session.getId(), newSessionId).block();

		Session<Map<String, String>> resolvedSession = sessionStore.get(newSessionId).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertEquals(newSessionId, resolvedSession.getId());
		Assertions.assertNull(resolvedSession.getMaxInactiveInterval());
		Assertions.assertEquals(session.getExpirationTime(), resolvedSession.getExpirationTime());
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());

		Assertions.assertNull(sessionStore.get(sessionId).block());
	}

	@Test
	public void save_should_save_session() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);
		Assertions.assertNotNull(session.getMaxInactiveInterval());

		session.setExpirationTime(System.currentTimeMillis() + 300000L);
		Assertions.assertNull(session.getMaxInactiveInterval());
		session.getData(HashMap::new).block().put("someKey", "someValue");

		sessionStore.save(session).block();
		Session<Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNotNull(resolvedSession);
		Assertions.assertNull(resolvedSession.getMaxInactiveInterval());
		Assertions.assertEquals(session.getExpirationTime(), resolvedSession.getExpirationTime());
		Assertions.assertEquals(Map.of("someKey", "someValue"), resolvedSession.getData().block());
	}

	@Test
	public void remove_should_remove_session() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		sessionStore.remove(session.getId()).block();

		Session<Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNull(resolvedSession);
	}

	@Test
	public void session_invalidate_should_remove_session() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
		Assertions.assertNotNull(session);

		session.invalidate().block();
		Assertions.assertTrue(session.isInvalidated());

		Session<Map<String, String>> resolvedSession = sessionStore.get(session.getId()).block();
		Assertions.assertNull(resolvedSession);
	}

	@Test
	public void session_should_expire_after_inactivity_period() {
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.maxInactiveInterval(250L)
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
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
		InMemoryBasicSessionStore<Map<String, String>> sessionStore = newSessionStoreBuilder()
			.expireAfterPeriod(250L)
			.build();

		Session<Map<String, String>> session = sessionStore.create().block();
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
