/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.test.web;

import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import io.inverno.test.InvernoTestCompiler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Disabled("Replaced by WebSocketClientTest")
public class WebSocketTest extends AbstractInvernoModTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final int TIMEOUT_SECONDS = 2;
	
	private static final String MODULE_WEBSOCKET = "io.inverno.mod.test.web.websocket";
	
	private static int testServerPort;
	private static InvernoModuleProxy testServerModuleProxy;
	private static Object testServerWebSocketController;
	
	private static URI baseURI;
	private static HttpClient httpClient;
	
	@BeforeAll
	public static void init() throws IOException, InvernoCompilationException, ClassNotFoundException, InterruptedException {
		InvernoTestCompiler invernoCompiler = InvernoTestCompiler.builder()
			.moduleOverride(AbstractInvernoModTest.MODULE_OVERRIDE)
			.annotationProcessorModuleOverride(AbstractInvernoModTest.ANNOTATION_PROCESSOR_MODULE_OVERRIDE)
			.build();
		
		invernoCompiler.cleanModuleTarget();
		
		InvernoModuleLoader moduleLoader = invernoCompiler.compile(MODULE_WEBSOCKET);
		
		testServerPort = getFreePort();
		
		Class<?> httpConfigClass = moduleLoader.loadClass(MODULE_WEBSOCKET, "io.inverno.mod.http.server.HttpServerConfiguration");
		ConfigurationInvocationHandler httpConfigHandler = new ConfigurationInvocationHandler(httpConfigClass, Map.of("server_port", testServerPort));
		Object httpConfig = Proxy.newProxyInstance(httpConfigClass.getClassLoader(),
			new Class<?>[] { httpConfigClass },
			httpConfigHandler);
		
		Class<?> webConfigClass = moduleLoader.loadClass(MODULE_WEBSOCKET, "io.inverno.mod.web.server.WebServerConfiguration");
		ConfigurationInvocationHandler webConfigHandler = new ConfigurationInvocationHandler(webConfigClass, Map.of("http_server", httpConfig));
		Object webConfig = Proxy.newProxyInstance(webConfigClass.getClassLoader(),
			new Class<?>[] { webConfigClass },
			webConfigHandler);
		
		Class<?> webSocketConfigClass = moduleLoader.loadClass(MODULE_WEBSOCKET, "io.inverno.mod.test.web.websocket.WebSocketConfiguration");
		ConfigurationInvocationHandler webSocketConfigHandler = new ConfigurationInvocationHandler(webSocketConfigClass, Map.of("web", webConfig));
		Object webSocketConfig = Proxy.newProxyInstance(webSocketConfigClass.getClassLoader(),
			new Class<?>[] { webSocketConfigClass },
			webSocketConfigHandler);
		
		testServerModuleProxy = moduleLoader.load(MODULE_WEBSOCKET).optionalDependency("webSocketConfiguration", webSocketConfigClass, webSocketConfig).build();
		testServerModuleProxy.start();
		testServerWebSocketController = testServerModuleProxy.getBean("webSocketController");
		
		baseURI = URI.create("ws://127.0.0.1:" + testServerPort);
		httpClient = HttpClient.newHttpClient();
	}
	
	@AfterAll
	public static void destroy() {
		if(testServerModuleProxy != null) {
			testServerModuleProxy.stop();
		}
	}
	
	private static int getFreePort() {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			return serverSocket.getLocalPort();
		} 
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static String getStringField(Object testServerWebSocketController, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		return (String)testServerWebSocketController.getClass().getField(name).get(testServerWebSocketController);
	}
	
	public static String getStringBuilderField(Object testServerWebSocketController, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		StringBuilder sb = (StringBuilder)testServerWebSocketController.getClass().getField(name).get(testServerWebSocketController);
		return sb != null ? sb.toString() : null;
	}
	
	public static boolean getBooleanField(Object testServerWebSocketController, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		return (boolean)testServerWebSocketController.getClass().getField(name).get(testServerWebSocketController);
	}
	
	public static CompletableFuture<TestWebSocket> openWebSocket(HttpClient client, URI uri, String... subprotocols) {
		java.net.http.WebSocket.Builder webSocketBuilder = client.newWebSocketBuilder();
		if(subprotocols != null && subprotocols.length > 0) {
			if(subprotocols.length == 1) {
				webSocketBuilder.subprotocols(subprotocols[0]);
			}
			else {
				webSocketBuilder.subprotocols(subprotocols[0], Arrays.copyOfRange(subprotocols, 1, subprotocols.length));
			}
		}
		
		TestWebSocketListener webSocketListener = new TestWebSocketListener();
		return webSocketBuilder.buildAsync(uri, webSocketListener).thenApply(webSocket -> new TestWebSocket(webSocket, webSocketListener));
	}
	
	private static class TestWebSocket {

		private final WebSocket webSocket;
		
		private final TestWebSocketListener webSocketListener;
		
		public TestWebSocket(WebSocket webSocket, TestWebSocketListener webSocketListener) {
			super();
			this.webSocket = webSocket;
			this.webSocketListener = webSocketListener;
		}
		
		public CompletableFuture<TestWebSocket> sendTextMessages(CharSequence... messages) {
			Objects.requireNonNull(messages);
			
			CompletableFuture<WebSocket> future = CompletableFuture.completedFuture(this.webSocket);
			for(CharSequence message : messages) {
				future = future.thenCompose(ws -> ws.sendText(message, true));
			}
			return future.thenApply(ign -> this);
		}
		
		public CompletableFuture<TestWebSocket> sendBinaryMessages(ByteBuffer... messages) {
			Objects.requireNonNull(messages);
			
			CompletableFuture<WebSocket> future = CompletableFuture.completedFuture(this.webSocket);
			for(ByteBuffer message : messages) {
				future = future.thenCompose(ws -> ws.sendBinary(message, true));
			}
			return future.thenApply(ign -> this);
		}

		public CompletableFuture<TestWebSocket> sendText(CharSequence data, boolean last) {
			return this.webSocket.sendText(data, last).thenApply(ign -> this);
		}

		public CompletableFuture<TestWebSocket> sendBinary(ByteBuffer data, boolean last) {
			return this.webSocket.sendBinary(data, last).thenApply(ign -> this);
		}

		public CompletableFuture<TestWebSocket> sendPing(ByteBuffer message) {
			return this.webSocket.sendPing(message).thenApply(ign -> this);
		}

		public CompletableFuture<TestWebSocket> sendPong(ByteBuffer message) {
			return this.webSocket.sendPong(message).thenApply(ign -> this);
		}

		public CompletableFuture<TestWebSocket> sendClose(int statusCode, String reason) {
			return this.webSocket.sendClose(statusCode, reason).thenApply(ign -> this);
		}

		public void request(long n) {
			this.webSocket.request(n);
		}

		public String getSubprotocol() {
			return this.webSocket.getSubprotocol();
		}

		public boolean isOutputClosed() {
			return this.webSocket.isOutputClosed();
		}

		public boolean isInputClosed() {
			return this.webSocket.isInputClosed();
		}

		public void abort() {
			this.webSocket.abort();
		}
		
		public CompletableFuture<List<String>> getResultAfterClose() {
			return this.webSocketListener.getResult();
		}
		
		public CompletableFuture<List<String>> getResultAndClose(int take) {
			return this.getResultAndClose(take, 1000, "");
		}
		
		public CompletableFuture<List<String>> getResultAndClose(int take, int code) {
			return this.getResultAndClose(take, code, "");
		}
		
		public CompletableFuture<List<String>> getResultAndClose(int take, int code, String reason) {
			return this.webSocketListener.getResult(take).thenCompose(result -> this.webSocket.sendClose(code, reason).thenApply(ign -> result));
		}
	}
	
	
	private static class TestWebSocketListener implements java.net.http.WebSocket.Listener {
		
		private final List<String> messages;

		private final CompletableFuture<List<String>> result;
		
		private int take;
		
		private StringBuilder acc;
		
		 public TestWebSocketListener() {
			super();
			this.take = Integer.MAX_VALUE;
			this.result = new CompletableFuture<>();
			this.messages = new ArrayList<>();
		}

		@Override
		public CompletionStage<?> onText(java.net.http.WebSocket webSocket, CharSequence data, boolean last) {
			synchronized(this.messages) {
				if(last) {
					if(this.acc != null) {
						this.acc.append(data);
						this.messages.add(this.acc.toString());
						this.acc = null;
					}
					else {
						this.messages.add(data.toString());
					}
				}
				else {
					if(this.acc == null) {
						this.acc = new StringBuilder();
					}
					this.acc.append(data);
				}
				if(this.messages.size() >= this.take) {
					this.result.complete(this.messages.subList(0, take));
					return null;
				}
				webSocket.request(1);
				return null;
			}
		}
		
		@Override
		public void onError(java.net.http.WebSocket webSocket, Throwable error) {
			this.result.completeExceptionally(error);
		}
		
		@Override
		public CompletionStage<?> onClose(java.net.http.WebSocket webSocket, int statusCode, String reason) {
			this.result.complete(this.messages);
			return null;
		}
		
		public CompletableFuture<List<String>> getResult() {
			return this.result;
		}
		
		public CompletableFuture<List<String>> getResult(int take) {
			synchronized(this.messages) {
				if(this.messages.size() >= take) {
					this.result.complete(this.messages.subList(0, take));
				}
				this.take = take;
			}
			return this.result;
		}
	}
	
	@Test
	public void test_ws1() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws1"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws1", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws2() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws2"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws2", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws3() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws3"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws3\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws4() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws4"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws4", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws5() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws5"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws5\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws6() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws6"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws6a"), received);
	}
	
	@Test
	public void test_ws7() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws7"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws7a\"}"), received);
	}
	
	@Test
	public void test_ws8() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws8"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws8"));
	}
	
	@Test
	public void test_ws9() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws9"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws9"));
	}
	
	@Test
	public void test_ws10() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws10"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws10"));
	}
	
	@Test
	public void test_ws11() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws11"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws11"));
	}
	
	@Test
	public void test_ws12() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws12"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws12"));
	}
	
	@Test
	public void test_ws13() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws13"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("a", getStringField(testServerWebSocketController, "ws13"));
	}
	
	@Test
	public void test_ws14() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws14"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("a", getStringField(testServerWebSocketController, "ws14"));
	}
	
	@Test
	public void test_ws15() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws15"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws15", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws16() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws16"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws16", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws17() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws17"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws17", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws18() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws18"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws18", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws19() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws19"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws19", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws20() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws20"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws20", "a"), received);
	}
	
	@Test
	public void test_ws21() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws21"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws21", "a"), received);
	}
	
	@Test
	public void test_ws22() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws22"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws22\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws23() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws23"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws23\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws24() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws24"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws24\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws25() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws25"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws25\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws26() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws26"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws26\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws27() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws27"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws27\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws28() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws28"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws28\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws29() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws29"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws29", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws30() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws30"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws30", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws31() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws31"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws31", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws32() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws32"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws32", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws33() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws33"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws33", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws34() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws34"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws34", "a"), received);
	}
	
	@Test
	public void test_ws35() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws35"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws35", "a"), received);
	}
	
	@Test
	public void test_ws36() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws36"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws36\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws37() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws37"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws37\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws38() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws38"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws38\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws39() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws39"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws39\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws40() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws40"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws40\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws41() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws41"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws41\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws42() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws42"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws42\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws43() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws43"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws43a"), received);
	}
	
	@Test
	public void test_ws44() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws44"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws44a"), received);
	}
	
	@Test
	public void test_ws45() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws45"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws45a"), received);
	}
	
	@Test
	public void test_ws46() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws46"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws46a"), received);
	}
	
	@Test
	public void test_ws47() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws47"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws47a"), received);
	}
	
	@Test
	public void test_ws48() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws48"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws48a"), received);
	}
	
	@Test
	public void test_ws49() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws49"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws49a"), received);
	}
	
	@Test
	public void test_ws50() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws50"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws50a\"}"), received);
	}
	
	@Test
	public void test_ws51() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws51"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws51a\"}"), received);
	}
	
	@Test
	public void test_ws52() throws Exception {		
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws52"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws52a\"}"), received);
	}
	
	@Test
	public void test_ws53() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws53"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws53a\"}"), received);
	}
	
	@Test
	public void test_ws54() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws54"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws54a\"}"), received);
	}
	
	@Test
	public void test_ws55() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws55"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws55a\"}"), received);
	}
	
	@Test
	public void test_ws56() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws56"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws56a\"}"), received);
	}
	
	@Test
	public void test_ws57() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws57"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws57", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws58() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws58"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws58", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws59() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws59"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws59\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws60() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws60"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws60", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws61() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws61"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws61\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws62() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws62"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws62", "a"), received);
	}
	
	@Test
	public void test_ws63() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws63"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws63\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws64() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws64"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws64", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws65() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws65"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws65", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws66() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws66"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws66\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws67() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws67"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws67", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws68() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws68"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws68\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws69() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws69"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws69", "a"), received);
	}
	
	@Test
	public void test_ws70() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws70"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws70\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws71() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws71"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws71", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws72() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws72"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws72", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws73() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws73"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws73", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws74() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws74"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws74", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws75() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws75"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws75", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws76() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws76"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws76", "a"), received);
	}
	
	@Test
	public void test_ws77() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws77"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws77", "a"), received);
	}
	
	@Test
	public void test_ws78() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws78"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws78\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws79() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws79"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws79\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws80() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws80"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws80\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws81() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws81"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws81\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws82() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws82"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws82\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws83() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws83"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws83\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws84() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws84"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws84\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws85() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws85"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws85", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws86() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws86"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws86", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws87() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws87"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws87", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws88() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws88"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws88", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws89() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws89"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws89", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws90() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws90"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws90", "a"), received);
	}
	
	@Test
	public void test_ws91() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws91"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws91", "a"), received);
	}
	
	@Test
	public void test_ws92() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws92"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws92\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws93() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws93"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws93\"}", "{\"message\":\"a\"}", "{\"message\":\"b\"}", "{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws94() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws94"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws94\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws95() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws95"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws95\"}", "{\"message\":\"a\"}", "{\"message\":\"b\"}", "{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws96() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws96"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws96\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws97() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws97"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws97\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws98() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws98"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws98\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws99() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws99"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws99a"), received);
	}
	
	@Test
	public void test_ws100() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws100"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws100a"), received);
	}
	
	@Test
	public void test_ws101() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws101"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws101a"), received);
	}
	
	@Test
	public void test_ws102() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws102"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws102a"), received);
	}
	
	@Test
	public void test_ws103() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws103"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws103a"), received);
	}
	
	@Test
	public void test_ws104() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws104"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws104a"), received);
	}
	
	@Test
	public void test_ws105() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws105"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws105a"), received);
	}
	
	@Test
	public void test_ws106() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws106"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws106a\"}"), received);
	}
	
	@Test
	public void test_ws107() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws107"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws107a\"}"), received);
	}
	
	@Test
	public void test_ws108() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws108"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws108a\"}"), received);
	}
	
	@Test
	public void test_ws109() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws109"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws109a\"}"), received);
	}
	
	@Test
	public void test_ws110() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws110"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws110a\"}"), received);
	}
	
	@Test
	public void test_ws111() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws111"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws111a\"}"), received);
	}
	
	@Test
	public void test_ws112() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws112"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws112a\"}"), received);
	}
	
	@Test
	public void test_ws113() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws113"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws113", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws114() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws114"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws114", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws115() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws115"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws115\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws116() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws116"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws116", "a", "b", "c"), received);
	}
	
	@Test
	public void test_ws117() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws117"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws117\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws118() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws118"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws118", "a"), received);
	}
	
	@Test
	public void test_ws119() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws119"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws119\"}", "{\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws120() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws120"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws120"));
	}
	
	@Test
	public void test_ws121() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws121"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws121"));
	}
	
	@Test
	public void test_ws122() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws122"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws122"));
	}
	
	@Test
	public void test_ws123() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws123"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws", "123"), received);
	}
	
	@Test
	public void test_ws124() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws124"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws", "124"), received);
	}
	
	@Test
	public void test_ws125() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws125"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws125"), received);
	}
	
	@Test
	public void test_ws126() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws126"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws126", "ws126"), received);
	}
	
	@Test
	public void test_ws127() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws127"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws127", "ws127"), received);
	}
	
	@Test
	public void test_ws128() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws128"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws128", "ws128"), received);
	}
	
	@Test
	public void test_ws129() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws129"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws129", "ws129"), received);
	}
	
	@Test
	public void test_ws130() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws130"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws130", "ws130"), received);
	}
	
	@Test
	public void test_ws131() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws131"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws131", "ws131"), received);
	}
	
	@Test
	public void test_ws132() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws132"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws132"), received);
	}
	
	@Test
	public void test_ws133() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws133"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws133"), received);
	}
	
	@Test
	public void test_ws134() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws134"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws134"), received);
	}
	
	@Test
	public void test_ws135() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws135"), "json")
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"135\"}"), received);
	}
	
	@Test
	public void test_ws136() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws136"), "json")
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"136\"}"), received);
	}
	
	@Test
	public void test_ws137() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws137"), "json")
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws137\"}"), received);
	}
	
	@Test
	public void test_ws138() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws138"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("1:a2:b3:c", getStringBuilderField(testServerWebSocketController, "ws138"));
	}
	
	@Test
	public void test_ws139() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws139"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("1:a2:b3:c", getStringBuilderField(testServerWebSocketController, "ws139"));
	}
	
	@Test
	public void test_ws140() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws140"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("1:a", getStringField(testServerWebSocketController, "ws140"));
	}
	
	@Test
	public void test_ws141() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws141"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws141\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws142() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws142"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws142\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws143() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws143"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws143a\"}"), received);
	}
	
	@Test
	public void test_ws144() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws144"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws144\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws145() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws145"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws145\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), received);
	}
	
	@Test
	public void test_ws146() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws146"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws146a\"}"), received);
	}
	
	@Test
	public void test_ws147() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws147"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws147\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws148() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws148"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws148\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), received);
	}
	
	@Test
	public void test_ws149() throws Exception {
		List<String> received = openWebSocket(httpClient, baseURI.resolve("/ws149"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws149a\"}"), received);
	}
}
