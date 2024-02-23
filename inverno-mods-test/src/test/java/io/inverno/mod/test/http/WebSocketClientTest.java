/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.mod.test.http;

import io.inverno.mod.boot.Boot;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.Client;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.ws.WebSocketExchange;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class WebSocketClientTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
//		System.setProperty("io.netty.leakDetection.level", "PARANOID");
//		System.setProperty("io.netty.leakDetection.targetRecords", "20");
	}
	
	private static final int TIMEOUT_SECONDS = 2;
	
	private static final String MODULE_WEBSOCKET = "io.inverno.mod.test.web.websocket";
	
	private static int testServerPort;
	private static InvernoModuleProxy testServerModuleProxy;
	private static Object testServerWebSocketController;
	
	private static Boot bootModule;
	private static Client httpClientModule;
	
	private static Endpoint endpoint;
	
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
		
		bootModule = new Boot.Builder().build();
		bootModule.start();
		
		httpClientModule = new Client.Builder(bootModule.netService(), bootModule.reactor(), bootModule.resourceService()).build();
		httpClientModule.start();
		
		endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))))
			.build();
	}
	
	@AfterAll
	public static void destroy() {
		if(endpoint != null) {
			endpoint.close().block();
		}
		if(httpClientModule != null) {
			httpClientModule.stop();
		}
		if(bootModule != null) {
			bootModule.stop();
		}
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
	
	private static List<String> receive(Mono<WebSocketExchange<ExchangeContext>> ws) {
		return ws
			.flatMapMany(exchange -> exchange.inbound().textMessages())
			.flatMap(message -> message.reducedText())
			.collectList()
			.block();
	}
	
	private static List<String> sendMessagesAndReceive(Mono<WebSocketExchange<ExchangeContext>> ws, List<String> messages) {
		return ws
			.flatMapMany(exchange -> Flux.from(exchange.inbound().textMessages())
				.doOnSubscribe(ign -> exchange.outbound()
					.closeOnComplete(false)
					.messages(factory -> Flux.fromIterable(messages)
						.map(factory::text)
					)
				)
			)
			.flatMap(message -> message.reducedText())
			.collectList()
			.block();
	}
	
	private static List<String> sendMessagesReceiveAndClose(Mono<WebSocketExchange<ExchangeContext>> ws, List<String> messages, int take) {
		return sendMessagesReceiveAndClose(ws, messages, take, (short)1000);
	}
	
	private static List<String> sendMessagesReceiveAndClose(Mono<WebSocketExchange<ExchangeContext>> ws, List<String> messages, int take, short code) {
		return ws
			.flatMapMany(exchange -> Flux.from(exchange.inbound().textMessages())
				.doOnSubscribe(ign -> exchange.outbound()
					.closeOnComplete(false)
					.messages(factory -> Flux.fromIterable(messages)
						.map(factory::text)
					)
				)
				.flatMap(message -> message.reducedText())
				.bufferTimeout(take, Duration.ofSeconds(TIMEOUT_SECONDS))
				.doFinally(ign -> exchange.close(code))
			)
			.blockFirst();
	}
	
	private static List<String> sendMessagesCloseAndReceive(Mono<WebSocketExchange<ExchangeContext>> ws, List<String> messages) {
		return ws
			.flatMapMany(exchange -> Flux.from(exchange.inbound().textMessages())
				.doOnSubscribe(ign -> exchange.outbound()
					.messages(factory -> Flux.fromIterable(messages)
						.map(factory::text)
					)
				)
			)
			.flatMap(message -> message.reducedText())
			.collectList()
			.block();
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
	
	@Test
	public void test_ws1() {
		Assertions.assertEquals(
			List.of("ws1", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws1").send(), 
				List.of("a", "b", "c"), 
				4
			)
		);
	}
	
	@Test
	public void test_ws2() {
		Assertions.assertEquals(
			List.of("ws2", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws2").send(), 
				List.of("a", "b", "c"), 
				4
			)
		);
	}
	
	@Test
	public void test_ws3() {
		Assertions.assertEquals(
			List.of("{\"message\":\"ws3\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws3").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}", "{\"message\":\"b\"}", "{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws4() {
		Assertions.assertEquals(
			List.of("ws4", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws4").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws5() {
		Assertions.assertEquals(
			List.of("{\"message\":\"ws5\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws5").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws6() {
		Assertions.assertEquals(
			List.of("ws6a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws6").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws7() {
		Assertions.assertEquals(
			List.of("{\"message\":\"ws7a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws7").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws8() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws8").send(), 
				List.of("a", "b", "c")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws8"));
	}
	
	@Test
	public void test_ws9() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws9").send(), 
				List.of("a", "b", "c")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws9"));
	}
	
	@Test
	public void test_ws10() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws10").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws10"));
	}
	
	@Test
	public void test_ws11() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws11").send(), 
				List.of("a", "b", "c")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws11"));
	}
	
	@Test
	public void test_ws12() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws12").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(testServerWebSocketController, "ws12"));
	}
	
	@Test
	public void test_ws13() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws13").send(), 
				List.of("a", "b","c")
			)
		);
		Assertions.assertEquals("a", getStringField(testServerWebSocketController, "ws13"));
	}
	
	@Test
	public void test_ws14() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws14").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("a", getStringField(testServerWebSocketController, "ws14"));
	}
	
	@Test
	public void test_ws15() {
		Assertions.assertEquals(
			List.<String>of("ws15", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws15").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws16() {
		Assertions.assertEquals(
			List.<String>of("ws16", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws16").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws17() {
		Assertions.assertEquals(
			List.<String>of("ws17", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws17").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws18() {
		Assertions.assertEquals(
			List.<String>of("ws18", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws18").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws19() {
		Assertions.assertEquals(
			List.<String>of("ws19", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws19").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws20() {
		Assertions.assertEquals(
			List.<String>of("ws20", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws20").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws21() {
		Assertions.assertEquals(
			List.<String>of("ws21", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws21").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws22() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws22\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws22").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws23() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws23\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws23").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws24() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws24\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws24").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws25() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws25\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws25").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws26() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws26\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws26").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws27() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws27\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws27").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws28() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws28\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws28").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws29() {
		Assertions.assertEquals(
			List.<String>of("ws29", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws29").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws30() {
		Assertions.assertEquals(
			List.<String>of("ws30", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws30").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws31() {
		Assertions.assertEquals(
			List.<String>of("ws31", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws31").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws32() {
		Assertions.assertEquals(
			List.<String>of("ws32", "a", "b", "c"), 
				sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws32").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws33() {
		Assertions.assertEquals(
			List.<String>of("ws33", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws33").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws34() {
		Assertions.assertEquals(
			List.<String>of("ws34", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws34").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws35() {
		Assertions.assertEquals(
			List.<String>of("ws35", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws35").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws36() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws36\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws36").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws37() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws37\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws37").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws38() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws38\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws38").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws39() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws39\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws39").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws40() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws40\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws40").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws41() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws41\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws41").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws42() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws42\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws42").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws43() {
		Assertions.assertEquals(
			List.<String>of("ws43a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws43").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws44() {
		Assertions.assertEquals(
			List.<String>of("ws44a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws44").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws45() {
		Assertions.assertEquals(
			List.<String>of("ws45a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws45").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws46() {
		Assertions.assertEquals(
			List.<String>of("ws46a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws46").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws47() {
		Assertions.assertEquals(
			List.<String>of("ws47a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws47").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws48() {
		Assertions.assertEquals(
			List.<String>of("ws48a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws48").send(), 
				List.of("a", "b", "c")
			)
		);
		
		}
	
	@Test
	public void test_ws49() {
		Assertions.assertEquals(
			List.<String>of("ws49a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws49").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws50() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws50a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws50").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws51() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws51a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws51").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws52() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws52a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws52").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws53() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws53a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws53").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws54() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws54a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws54").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws55() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws55a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws55").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws56() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws56a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws56").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws57() {
		Assertions.assertEquals(
			List.<String>of("ws57", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws57").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws58() {
		Assertions.assertEquals(
			List.<String>of("ws58", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws58").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws59() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws59\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws59").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws60() {
		Assertions.assertEquals(
			List.<String>of("ws60", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws60").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws61() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws61\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws61").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws62() {
		Assertions.assertEquals(
			List.<String>of("ws62", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws62").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws63() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws63\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws63").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws64() {
		Assertions.assertEquals(
			List.<String>of("ws64", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws64").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws65() {
		Assertions.assertEquals(
			List.<String>of("ws65", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws65").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws66() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws66\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws66").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws67() {
		Assertions.assertEquals(
			List.<String>of("ws67", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws67").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws68() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws68\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws68").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws69() {
		Assertions.assertEquals(
			List.<String>of("ws69", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws69").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws70() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws70\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws70").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws71() {
		Assertions.assertEquals(
			List.<String>of("ws71", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws71").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws72() {
		Assertions.assertEquals(
			List.<String>of("ws72", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws72").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws73() {
		Assertions.assertEquals(
			List.<String>of("ws73", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws73").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws74() {
		Assertions.assertEquals(
			List.<String>of("ws74", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws74").send(), 
				List.of( "a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws75() {
		Assertions.assertEquals(
			List.<String>of("ws75", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws75").subProtocol("json").send(), 
				List.of( "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws76() {
		Assertions.assertEquals(
			List.<String>of("ws76", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws76").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws77() {
		Assertions.assertEquals(
			List.<String>of("ws77", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws77").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws78() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws78\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws78").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws79() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws79\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws79").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws80() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws80\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws80").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws81() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws81\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws81").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws82() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws82\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws82").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws83() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws83\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws83").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws84() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws84\"}", "{\"message\":\"a\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws84").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws85() {
		Assertions.assertEquals(
			List.<String>of("ws85", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws85").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws86() {
		Assertions.assertEquals(
			List.<String>of("ws86", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws86").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws87() {
		Assertions.assertEquals(
			List.<String>of("ws87", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws87").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws88() {
		Assertions.assertEquals(
			List.<String>of("ws88", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws88").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws89() {
		Assertions.assertEquals(
			List.<String>of("ws89", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws89").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws90() {
		Assertions.assertEquals(
			List.<String>of("ws90", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws90").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws91() {
		Assertions.assertEquals(
			List.<String>of("ws91", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws91").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws92() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws92\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws92").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws93() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws93\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws93").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws94() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws94\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws94").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws95() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws95\"}", "{\"message\":\"a\"}", "{\"message\":\"b\"}", "{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws95").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws96() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws96\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws96").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws97() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws97\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws97").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws98() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws98\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws98").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws99() {
		Assertions.assertEquals(
			List.<String>of("ws99a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws99").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws100() {
		Assertions.assertEquals(
			List.<String>of("ws100a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws100").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws101() {
		Assertions.assertEquals(
			List.<String>of("ws101a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws101").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws102() {
		Assertions.assertEquals(
			List.<String>of("ws102a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws102").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws103() {
		Assertions.assertEquals(
			List.<String>of("ws103a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws103").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws104() {
		Assertions.assertEquals(
			List.<String>of("ws104a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws104").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws105() {
		Assertions.assertEquals(
			List.<String>of("ws105a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws105").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws106() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws106a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws106").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws107() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws107a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws107").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws108() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws108a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws108").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws109() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws109a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws109").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws110() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws110a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws110").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws111() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws111a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws111").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws112() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws112a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws112").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws113() {
		Assertions.assertEquals(
			List.<String>of("ws113", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws113").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws114() {
		Assertions.assertEquals(
			List.<String>of("ws114", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws114").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws115() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws115\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws115").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws116() {
		Assertions.assertEquals(
			List.<String>of("ws116", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws116").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	@Test
	public void test_ws117() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws117\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws117").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	@Test
	public void test_ws118() {
		Assertions.assertEquals(
			List.<String>of("ws118", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws118").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws119() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws119\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws119").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws120() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.webSocketRequest("/ws120").send()
			)
		);
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws120"));
	}
	
	@Test
	public void test_ws121() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.webSocketRequest("/ws121").send()
			)
		);
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws121"));
	}
	
	@Test
	public void test_ws122() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.webSocketRequest("/ws122").send()
			)
		);
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws122"));
	}
	
	@Test
	public void test_ws123() {
		Assertions.assertEquals(
			List.<String>of("ws", "123"), 
			receive(
				endpoint.webSocketRequest("/ws123").send()
			)
		);
	}
	
	@Test
	public void test_ws124() {
		Assertions.assertEquals(
			List.<String>of("ws", "124"), 
			receive(
				endpoint.webSocketRequest("/ws124").send()
			)
		);
	}
	
	@Test
	public void test_ws125() {
		Assertions.assertEquals(
			List.<String>of("ws125"), 
			receive(
				endpoint.webSocketRequest("/ws125").send()
			)
		);
	}
	
	@Test
	public void test_ws126() {
		Assertions.assertEquals(
			List.<String>of("ws126", "ws126"), 
			receive(
				endpoint.webSocketRequest("/ws126").send()
			)
		);
	}
	
	@Test
	public void test_ws127() {
		Assertions.assertEquals(
			List.<String>of("ws127", "ws127"), 
			receive(
				endpoint.webSocketRequest("/ws127").send()
			)
		);
	}
	
	@Test
	public void test_ws128() {
		Assertions.assertEquals(
			List.<String>of("ws128", "ws128"), 
			receive(
				endpoint.webSocketRequest("/ws128").send()
			)
		);
	}
	
	@Test
	public void test_ws129() {
		Assertions.assertEquals(
			List.<String>of("ws129", "ws129"), 
			receive(
				endpoint.webSocketRequest("/ws129").send()
			)
		);
	}
	
	@Test
	public void test_ws130() {
		Assertions.assertEquals(
			List.<String>of("ws130", "ws130"), 
			receive(
				endpoint.webSocketRequest("/ws130").send()
			)
		);
	}
	
	@Test
	public void test_ws131() {
		Assertions.assertEquals(
			List.<String>of("ws131", "ws131"), 
			receive(
				endpoint.webSocketRequest("/ws131").send()
			)
		);
	}
	
	@Test
	public void test_ws132() {
		Assertions.assertEquals(
			List.<String>of("ws132"), 
			receive(
				endpoint.webSocketRequest("/ws132").send()
			)
		);
	}
	
	@Test
	public void test_ws133() {
		Assertions.assertEquals(
			List.<String>of("ws133"), 
			receive(
				endpoint.webSocketRequest("/ws133").send()
			)
		);
	}
	
	@Test
	public void test_ws134() {
		Assertions.assertEquals(
			List.<String>of("ws134"), 
			receive(
				endpoint.webSocketRequest("/ws134").send()
			)
		);
	}
	
	@Test
	public void test_ws135() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"135\"}"), 
			receive(
				endpoint.webSocketRequest("/ws135").subProtocol("json").send()
			)
		);
	}
	
	@Test
	public void test_ws136() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"136\"}"), 
			receive(
				endpoint.webSocketRequest("/ws136").subProtocol("json").send()
			)
		);
	}
	
	@Test
	public void test_ws137() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws137\"}"), 
			receive(
				endpoint.webSocketRequest("/ws137").subProtocol("json").send()
			)
		);
	}
	
	@Test
	public void test_ws138() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws138").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("1:a2:b3:c", getStringBuilderField(testServerWebSocketController, "ws138"));
	}
	
	@Test
	public void test_ws139() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws139").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("1:a2:b3:c", getStringBuilderField(testServerWebSocketController, "ws139"));
	}
	
	@Test
	public void test_ws140() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws140").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("1:a", getStringField(testServerWebSocketController, "ws140"));
	}
	
	@Test
	public void test_ws141() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws141\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws141").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws142() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws142\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws142").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws143() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws143a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws143").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws144() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws144\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws144").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws145() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws145\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws145").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws146() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws146a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws146").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws147() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws147\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws147").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws148() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws148\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws148").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws149() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws149a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws149").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
}
