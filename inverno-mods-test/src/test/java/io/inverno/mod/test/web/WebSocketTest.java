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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class WebSocketTest extends AbstractInvernoModTest {
	
	private static final int TIMEOUT_SECONDS = 2;
	
	private static final String MODULE_WEBSOCKET = "io.inverno.mod.test.web.websocket";
	
	public static int getFreePort() {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			return serverSocket.getLocalPort();
		} 
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Test
	public void testWebSocketController() throws IOException, InvernoCompilationException, ClassNotFoundException, InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ExecutionException, TimeoutException {
		this.clearModuleTarget();
		
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULE_WEBSOCKET);
		
		int port = getFreePort();
		
		Class<?> httpConfigClass = moduleLoader.loadClass(MODULE_WEBSOCKET, "io.inverno.mod.http.server.HttpServerConfiguration");
		ConfigurationInvocationHandler httpConfigHandler = new ConfigurationInvocationHandler(httpConfigClass, Map.of("server_port", port));
		Object httpConfig = Proxy.newProxyInstance(httpConfigClass.getClassLoader(),
			new Class<?>[] { httpConfigClass },
			httpConfigHandler);
		
		Class<?> webConfigClass = moduleLoader.loadClass(MODULE_WEBSOCKET, "io.inverno.mod.web.WebConfiguration");
		ConfigurationInvocationHandler webConfigHandler = new ConfigurationInvocationHandler(webConfigClass, Map.of("http_server", httpConfig));
		Object webConfig = Proxy.newProxyInstance(webConfigClass.getClassLoader(),
			new Class<?>[] { webConfigClass },
			webConfigHandler);
		
		Class<?> webRouteConfigClass = moduleLoader.loadClass(MODULE_WEBSOCKET, "io.inverno.mod.test.web.websocket.WebSocketConfiguration");
		ConfigurationInvocationHandler webRouteConfigHandler = new ConfigurationInvocationHandler(webRouteConfigClass, Map.of("web", webConfig));
		Object webRouteConfig = Proxy.newProxyInstance(webRouteConfigClass.getClassLoader(),
			new Class<?>[] { webRouteConfigClass },
			webRouteConfigHandler);
		
		final InvernoModuleProxy module = moduleLoader.load(MODULE_WEBSOCKET).optionalDependency("webSocketConfiguration", webRouteConfigClass, webRouteConfig).build();
		module.start();
		try {
			final Object webSocketController = module.getBean("webSocketController");
			final URI baseURI = URI.create("ws://127.0.0.1:" + port);
			
			System.out.println("0to10");
			test0to10(baseURI, webSocketController);
			System.out.println("11to20");
			test11to20(baseURI, webSocketController);
			System.out.println("21to30");
			test21to30(baseURI, webSocketController);
			System.out.println("31to40");
			test31to40(baseURI, webSocketController);
			System.out.println("41to50");
			test41to50(baseURI, webSocketController);
			System.out.println("51to60");
			test51to60(baseURI, webSocketController);
			System.out.println("61to70");
			test61to70(baseURI, webSocketController);
			System.out.println("71to80");
			test71to80(baseURI, webSocketController);
			System.out.println("81to90");
			test81to90(baseURI, webSocketController);
			System.out.println("91to100");
			test91to100(baseURI, webSocketController);
			System.out.println("101to110");
			test101to110(baseURI, webSocketController);
			System.out.println("111to120");
			test111to120(baseURI, webSocketController);
			System.out.println("121to130");
			test121to130(baseURI, webSocketController);
			System.out.println("131to140");
			test131to140(baseURI, webSocketController);
			System.out.println("141to150");
			test141to150(baseURI, webSocketController);
		}
		finally {
			module.stop();
		}
	}
	
	public static void test0to10(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		
		List<String> received;
		received = openWebSocket(client, baseURI.resolve("/ws1"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws1", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws2"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws2", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws3"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws3\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws4"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws4", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws5"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws5\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws6"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws6a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws7"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws7a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws8"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws8"));
		
		received = openWebSocket(client, baseURI.resolve("/ws9"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws9"));
		
		received = openWebSocket(client, baseURI.resolve("/ws10"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws10"));
	}
	
	public static void test11to20(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws11"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws11"));

		received = openWebSocket(client, baseURI.resolve("/ws12"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws12"));

		received = openWebSocket(client, baseURI.resolve("/ws13"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("a", getStringField(webSocketController, "ws13"));

		received = openWebSocket(client, baseURI.resolve("/ws14"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("a", getStringField(webSocketController, "ws14"));

		received = openWebSocket(client, baseURI.resolve("/ws15"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws15", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws16"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws16", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws17"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws17", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws18"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws18", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws19"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws19", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws20"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws20", "a"), received);
	}
	
	public static void test21to30(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws21"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws21", "a"), received);

		received = openWebSocket(client, baseURI.resolve("/ws22"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws22\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws23"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws23\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws24"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws24\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws25"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws25\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws26"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws26\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws27"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws27\"}", "{\"message\":\"a\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws28"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws28\"}", "{\"message\":\"a\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws29"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws29", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws30"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws30", "a", "b", "c"), received);
	}
	
	public static void test31to40(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws31"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws31", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws32"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws32", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws33"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws33", "a", "b", "c"), received);

		received = openWebSocket(client, baseURI.resolve("/ws34"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws34", "a"), received);

		received = openWebSocket(client, baseURI.resolve("/ws35"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws35", "a"), received);

		received = openWebSocket(client, baseURI.resolve("/ws36"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws36\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws37"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws37\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws38"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws38\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws39"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws39\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws40"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws40\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	public static void test41to50(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws41"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws41\"}", "{\"message\":\"a\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws42"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws42\"}", "{\"message\":\"a\"}"), received);

		received = openWebSocket(client, baseURI.resolve("/ws43"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws43a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws44"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws44a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws45"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws45a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws46"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws46a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws47"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws47a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws48"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws48a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws49"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws49a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws50"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws50a\"}"), received);
	}
	
	public static void test51to60(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws51"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws51a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws52"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws52a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws53"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws53a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws54"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws54a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws55"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws55a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws56"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws56a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws57"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws57", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws58"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws58", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws59"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws59\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws60"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws60", "a", "b", "c"), received);
	}
	
	public static void test61to70(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws61"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws61\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws62"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws62", "a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws63"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws63\"}", "{\"message\":\"a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws64"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws64", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws65"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws65", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws66"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws66\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws67"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws67", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws68"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws68\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws69"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws69", "a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws70"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws70\"}", "{\"message\":\"a\"}"), received);
	}
	
	public static void test71to80(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws71"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws71", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws72"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws72", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws73"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws73", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws74"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws74", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws75"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws75", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws76"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws76", "a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws77"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws77", "a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws78"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws78\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws79"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws79\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws80"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws80\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
	}
	
	public static void test81to90(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws81"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws81\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws82"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws82\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws83"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws83\"}", "{\"message\":\"a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws84"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws84\"}", "{\"message\":\"a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws85"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws85", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws86"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws86", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws87"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws87", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws88"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws88", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws89"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws89", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws90"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws90", "a"), received);
	}
	
	public static void test91to100(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws91"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws91", "a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws92"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws92\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws93"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws93\"}", "{\"message\":\"a\"}", "{\"message\":\"b\"}", "{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws94"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws94\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws95"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws95\"}", "{\"message\":\"a\"}", "{\"message\":\"b\"}", "{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws96"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws96\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws97"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws97\"}", "{\"message\":\"a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws98"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws98\"}", "{\"message\":\"a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws99"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws99a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws100"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws100a"), received);
	}
	
	public static void test101to110(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws101"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws101a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws102"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws102a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws103"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws103a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws104"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws104a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws105"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws105a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws106"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws106a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws107"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws107a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws108"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws108a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws109"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws109a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws110"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws110a\"}"), received);
	}
	
	public static void test111to120(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws111"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws111a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws112"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws112a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws113"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws113", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws114"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws114", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws115"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws115\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws116"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws116", "a", "b", "c"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws117"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAndClose(4))
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws117\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws118"))
			.thenCompose(webSocket -> webSocket.sendTextMessages("a", "b", "c"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws118", "a"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws119"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"message\":\"ws119\"}", "{\"message\":\"a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws120"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertTrue(getBooleanField(webSocketController, "ws120"));
	}
	
	public static void test121to130(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws121"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertTrue(getBooleanField(webSocketController, "ws121"));

		received = openWebSocket(client, baseURI.resolve("/ws122"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertTrue(getBooleanField(webSocketController, "ws122"));

		received = openWebSocket(client, baseURI.resolve("/ws123"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws", "123"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws124"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws", "124"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws125"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws125"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws126"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws126", "ws126"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws127"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws127", "ws127"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws128"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws128", "ws128"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws129"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws129", "ws129"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws130"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws130", "ws130"), received);
	}
	
	public static void test131to140(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws131"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws131", "ws131"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws132"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws132"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws133"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws133"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws134"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("ws134"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws135"), "json")
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"135\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws136"), "json")
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"136\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws137"), "json")
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws137\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws138"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("1:a2:b3:c", getStringBuilderField(webSocketController, "ws138"));

		received = openWebSocket(client, baseURI.resolve("/ws139"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("1:a2:b3:c", getStringBuilderField(webSocketController, "ws139"));

		received = openWebSocket(client, baseURI.resolve("/ws140"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertTrue(received.isEmpty());
		Assertions.assertEquals("1:a", getStringField(webSocketController, "ws140"));
	}
	
	public static void test141to150(URI baseURI, Object webSocketController) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = HttpClient.newHttpClient();
		List<String> received;
		
		received = openWebSocket(client, baseURI.resolve("/ws141"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws141\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws142"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws142\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws143"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws143a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws144"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws144\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws145"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.sendClose(1000, ""))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws145\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws146"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws146a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws147"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws147\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws148"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws148\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), received);
		
		received = openWebSocket(client, baseURI.resolve("/ws149"), "json")
			.thenCompose(webSocket -> webSocket.sendTextMessages("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"))
			.thenCompose(webSocket -> webSocket.getResultAfterClose())
			.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		Assertions.assertEquals(List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws149a\"}"), received);
	}
	
	public static String getStringField(Object webSocketController, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		return (String)webSocketController.getClass().getField(name).get(webSocketController);
	}
	
	public static String getStringBuilderField(Object webSocketController, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		StringBuilder sb = (StringBuilder)webSocketController.getClass().getField(name).get(webSocketController);
		return sb != null ? sb.toString() : null;
	}
	
	public static boolean getBooleanField(Object webSocketController, String name) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		return (boolean)webSocketController.getClass().getField(name).get(webSocketController);
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
			this.messages = new ArrayList<String>();
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
}