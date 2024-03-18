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
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.ws.WebSocketExchange;
import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import io.inverno.test.InvernoTestCompiler;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakeException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
	
	private static Endpoint<ExchangeContext> endpoint;
	
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
				endpoint.exchange("/ws1").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws2").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws3").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws4").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws5").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws6").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws7() {
		Assertions.assertEquals(
			List.of("{\"message\":\"ws7a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws7").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws8() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.exchange("/ws8").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws9").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws10").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws11").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws12").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws13").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws14").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws15").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws16").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws17").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws18").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws19").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws20").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws21() {
		Assertions.assertEquals(
			List.<String>of("ws21", "a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws21").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws22() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws22\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws22").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws23").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws24").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws25").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws26").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws27").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws28() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws28\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws28").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws29() {
		Assertions.assertEquals(
			List.<String>of("ws29", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws29").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws30").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws31").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws32").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws33").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws34").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws35() {
		Assertions.assertEquals(
			List.<String>of("ws35", "a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws35").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws36() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws36\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws36").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws37").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws38").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws39").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws40").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws41").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws42() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws42\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws42").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws43() {
		Assertions.assertEquals(
			List.<String>of("ws43a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws43").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws44() {
		Assertions.assertEquals(
			List.<String>of("ws44a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws44").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws45() {
		Assertions.assertEquals(
			List.<String>of("ws45a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws45").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws46() {
		Assertions.assertEquals(
			List.<String>of("ws46a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws46").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws47() {
		Assertions.assertEquals(
			List.<String>of("ws47a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws47").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws48() {
		Assertions.assertEquals(
			List.<String>of("ws48a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws48").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
		
		}
	
	@Test
	public void test_ws49() {
		Assertions.assertEquals(
			List.<String>of("ws49a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws49").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws50() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws50a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws50").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws51() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws51a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws51").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws52() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws52a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws52").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws53() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws53a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws53").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws54() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws54a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws54").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws55() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws55a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws55").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws56() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws56a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws56").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws57() {
		Assertions.assertEquals(
			List.<String>of("ws57", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws57").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws58").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws59").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws60").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws61").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws62").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws63() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws63\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws63").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws64() {
		Assertions.assertEquals(
			List.<String>of("ws64", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws64").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws65").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws66").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws67").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws68").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws69").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws70() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws70\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws70").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws71() {
		Assertions.assertEquals(
			List.<String>of("ws71", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws71").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws72").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws73").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws74").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws75").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws76").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws77() {
		Assertions.assertEquals(
			List.<String>of("ws77", "a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws77").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws78() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws78\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws78").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws79").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws80").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws81").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws82").flatMap(wsExchange -> wsExchange.webSocket("json")),
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
				endpoint.exchange("/ws83").flatMap(wsExchange -> wsExchange.webSocket("json")),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws84() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws84\"}", "{\"message\":\"a\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws84").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws85").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws86").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws87").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws88").flatMap(Exchange::webSocket),
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
				endpoint.exchange("/ws89").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws90").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws91() {
		Assertions.assertEquals(
			List.<String>of("ws91", "a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws91").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws92() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws92\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws92").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws93").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws94").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws95").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws96").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws97").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws98() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws98\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws98").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws99() {
		Assertions.assertEquals(
			List.<String>of("ws99a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws99").flatMap(Exchange::webSocket),
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws100() {
		Assertions.assertEquals(
			List.<String>of("ws100a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws100").flatMap(Exchange::webSocket), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws101() {
		Assertions.assertEquals(
			List.<String>of("ws101a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws101").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws102() {
		Assertions.assertEquals(
			List.<String>of("ws102a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws102").flatMap(Exchange::webSocket), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws103() {
		Assertions.assertEquals(
			List.<String>of("ws103a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws103").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws104() {
		Assertions.assertEquals(
			List.<String>of("ws104a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws104").flatMap(Exchange::webSocket), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws105() {
		Assertions.assertEquals(
			List.<String>of("ws105a"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws105").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws106() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws106a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws106").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws107() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws107a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws107").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws108() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws108a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws108").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws109() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws109a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws109").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws110() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws110a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws110").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws111() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws111a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws111").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws112() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws112a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws112").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws113() {
		Assertions.assertEquals(
			List.<String>of("ws113", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.exchange("/ws113").flatMap(Exchange::webSocket), 
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
				endpoint.exchange("/ws114").flatMap(Exchange::webSocket), 
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
				endpoint.exchange("/ws115").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws116").flatMap(Exchange::webSocket), 
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
				endpoint.exchange("/ws117").flatMap(wsExchange -> wsExchange.webSocket("json")), 
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
				endpoint.exchange("/ws118").flatMap(Exchange::webSocket), 
				List.of("a", "b", "c")
			)
		);
	}
	
	@Test
	public void test_ws119() {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws119\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.exchange("/ws119").flatMap(wsExchange -> wsExchange.webSocket("json")), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws120() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.exchange("/ws120").flatMap(Exchange::webSocket)
			)
		);
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws120"));
	}
	
	@Test
	public void test_ws121() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.exchange("/ws121").flatMap(Exchange::webSocket)
			)
		);
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws121"));
	}
	
	@Test
	public void test_ws122() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.exchange("/ws122").flatMap(Exchange::webSocket)
			)
		);
		Assertions.assertTrue(getBooleanField(testServerWebSocketController, "ws122"));
	}
	
	@Test
	public void test_ws123() {
		Assertions.assertEquals(
			List.<String>of("ws", "123"), 
			receive(
				endpoint.exchange("/ws123").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws124() {
		Assertions.assertEquals(
			List.<String>of("ws", "124"), 
			receive(
				endpoint.exchange("/ws124").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws125() {
		Assertions.assertEquals(
			List.<String>of("ws125"), 
			receive(
				endpoint.exchange("/ws125").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws126() {
		Assertions.assertEquals(
			List.<String>of("ws126", "ws126"), 
			receive(
				endpoint.exchange("/ws126").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws127() {
		Assertions.assertEquals(
			List.<String>of("ws127", "ws127"), 
			receive(
				endpoint.exchange("/ws127").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws128() {
		Assertions.assertEquals(
			List.<String>of("ws128", "ws128"), 
			receive(
				endpoint.exchange("/ws128").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws129() {
		Assertions.assertEquals(
			List.<String>of("ws129", "ws129"), 
			receive(
				endpoint.exchange("/ws129").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws130() {
		Assertions.assertEquals(
			List.<String>of("ws130", "ws130"), 
			receive(
				endpoint.exchange("/ws130").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws131() {
		Assertions.assertEquals(
			List.<String>of("ws131", "ws131"), 
			receive(
				endpoint.exchange("/ws131").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws132() {
		Assertions.assertEquals(
			List.<String>of("ws132"), 
			receive(
				endpoint.exchange("/ws132").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws133() {
		Assertions.assertEquals(
			List.<String>of("ws133"), 
			receive(
				endpoint.exchange("/ws133").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws134() {
		Assertions.assertEquals(
			List.<String>of("ws134"), 
			receive(
				endpoint.exchange("/ws134").flatMap(Exchange::webSocket)
			)
		);
	}
	
	@Test
	public void test_ws135() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"135\"}"), 
			receive(
				endpoint.exchange("/ws135").flatMap(exchange -> exchange.webSocket("json"))
			)
		);
	}
	
	@Test
	public void test_ws136() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"136\"}"), 
			receive(
				endpoint.exchange("/ws136").flatMap(exchange -> exchange.webSocket("json"))
			)
		);
	}
	
	@Test
	public void test_ws137() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws137\"}"), 
			receive(
				endpoint.exchange("/ws137").flatMap(exchange -> exchange.webSocket("json"))
			)
		);
	}
	
	@Test
	public void test_ws138() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Assertions.assertEquals(
			List.<String>of(), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws138").flatMap(exchange -> exchange.webSocket("json")),
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
				endpoint.exchange("/ws139").flatMap(exchange -> exchange.webSocket("json")),
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
				endpoint.exchange("/ws140").flatMap(exchange -> exchange.webSocket("json")),
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
				endpoint.exchange("/ws141").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws142() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws142\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws142").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws143() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws143a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws143").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws144() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws144\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws144").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws145() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws145\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws145").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws146() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws146a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws146").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws147() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws147\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws147").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws148() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws148\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws148").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws149() {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws149a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.exchange("/ws149").flatMap(exchange -> exchange.webSocket("json")),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
	}
	
	@Test
	public void test_ws_not_a_ws_resource() {
		Assertions.assertThrows(
			WebSocketClientHandshakeException.class, 
			() -> endpoint.exchange("/no_ws").flatMap(Exchange::webSocket).block()
		);
	}
	
	@Test
	public void test_ws_interceptor_abort() {
		AtomicBoolean interceptorFlag = new AtomicBoolean(false);
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.interceptor(exchange -> {
				interceptorFlag.set(true);
				return Mono.empty();
			})
			.build();
		
		try {
			Assertions.assertEquals(
				"Can't open WebSocket on an intercepted exchange",
				Assertions.assertThrows(
					HttpClientException.class, 
					() -> endpoint.exchange("/ws120").flatMap(Exchange::webSocket).block()
				).getMessage()
			);
			Assertions.assertTrue(interceptorFlag.get());
		}
		finally {
			endpoint.close().block();
		}
	}
}
