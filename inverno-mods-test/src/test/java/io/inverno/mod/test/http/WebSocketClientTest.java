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
import io.inverno.mod.http.client.Client;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.ws.WebSocketExchange;
import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class WebSocketClientTest extends AbstractInvernoModTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
	
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
	public void testWebSocketController() throws Exception {
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
		
		
		Boot bootMod = new Boot.Builder().build();
		final InvernoModuleProxy testWebSocketServerMod = moduleLoader.load(MODULE_WEBSOCKET).optionalDependency("webSocketConfiguration", webRouteConfigClass, webRouteConfig).build();
		try {
			bootMod.start();
			testWebSocketServerMod.start();
			final Object webSocketController = testWebSocketServerMod.getBean("webSocketController");
		
			Client clientMod = new Client.Builder(bootMod.netService(), bootMod.reactor()).build();
			try {
				clientMod.start();
				Endpoint endpoint = clientMod.httpClient().endpoint("127.0.0.1", port)
					.build();
				try {
					System.out.println("0to10");
					test0to10(endpoint, webSocketController);
					System.out.println("11to20");
					test11to20(endpoint, webSocketController);
					System.out.println("21to30");
					test21to30(endpoint, webSocketController);
					System.out.println("31to40");
					test31to40(endpoint, webSocketController);
					System.out.println("41to50");
					test41to50(endpoint, webSocketController);
					System.out.println("51to60");
					test51to60(endpoint, webSocketController);
					System.out.println("61to70");
					test61to70(endpoint, webSocketController);
					System.out.println("71to80");
					test71to80(endpoint, webSocketController);
					System.out.println("81to90");
					test81to90(endpoint, webSocketController);
					System.out.println("91to100");
					test91to100(endpoint, webSocketController);
					System.out.println("101to110");
					test101to110(endpoint, webSocketController);
					System.out.println("111to120");
					test111to120(endpoint, webSocketController);
					System.out.println("121to130");
					test121to130(endpoint, webSocketController);
					System.out.println("131to140");
					test131to140(endpoint, webSocketController);
					System.out.println("141to150");
					test141to150(endpoint, webSocketController);
				}
				finally {
					endpoint.close().block();
				}
			}
			finally {
				clientMod.stop();
			}
		}
		finally {
			testWebSocketServerMod.stop();
			bootMod.stop();
		}
	}
	
	public static void test(Endpoint endpoint, Object webSocketController) throws Exception {
		// sendMessagesReceiveAndClose
		Assertions.assertEquals(
			List.<String>of("ws1", "a", "b", "c"), 
			endpoint.webSocketRequest("/ws1").send()
				.flatMapMany(exchange -> Flux.from(exchange.inbound().textMessages())
					.doOnSubscribe(ign -> exchange.outbound()
						.messages(factory -> Flux.just("a", "b", "c").map(factory::text))
					)
					.flatMap(message -> message.reducedText())
					.bufferTimeout(4, Duration.ofSeconds(TIMEOUT_SECONDS))
					.doFinally(ign -> exchange.close())
				)
				.blockFirst()
		);
		
		// sendMessagesAndReceive
		Assertions.assertEquals(
			List.<String>of("ws6a"), 
			endpoint.webSocketRequest("/ws6").send()
				.flatMap(exchange -> Flux.from(exchange.inbound().textMessages())
					.doOnSubscribe(ign -> exchange.outbound()
						.messages(factory -> Flux.just("a", "b", "c").map(factory::text))
					)
					.flatMap(message -> message.reducedText())
					.collectList()
				)
				.block()
		);
		
		// sendMessagesCloseAndReceive
		Assertions.assertEquals(
			List.<String>of(), 
			endpoint.webSocketRequest("/ws8").send()
				.flatMap(exchange -> Flux.from(exchange.inbound().textMessages())
					.doOnSubscribe(ign -> exchange.outbound()
						.messages(factory -> Flux.just("a", "b", "c")
							.map(factory::text)
							.doOnComplete(() -> exchange.close())
						)
					)
					.flatMap(message -> message.reducedText())
					.collectList()
				)
				.block()
		);
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws8"));
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws149a\"}"), 
			endpoint.webSocketRequest("/ws149").subProtocol("json").send()
				.flatMap(exchange -> Flux.from(exchange.inbound().textMessages())
					.doOnSubscribe(ign -> exchange.outbound()
						.messages(factory -> Flux.just("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}").map(factory::text))
					)
					.flatMap(message -> message.reducedText())
					.collectList()
				)
				.block()
		);
	}
	
	public static void test0to10(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.of("ws1", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws1").send(), 
				List.of("a", "b", "c"), 
				4
			)
		);
		
		Assertions.assertEquals(
			List.of("ws2", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws2").send(), 
				List.of("a", "b", "c"), 
				4
			)
		);
		
		Assertions.assertEquals(
			List.of("{\"message\":\"ws3\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws3").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}", "{\"message\":\"b\"}", "{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.of("ws4", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws4").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.of("{\"message\":\"ws5\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws5").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.of("ws6a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws6").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.of("{\"message\":\"ws7a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws7").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws8").send(), 
				List.of("a", "b", "c")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws8"));

		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws9").send(), 
				List.of("a", "b", "c")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws9"));

		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws10").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws10"));
	}
	
	public static void test11to20(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws11").send(), 
				List.of("a", "b", "c")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws11"));
		
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws12").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("abc", getStringBuilderField(webSocketController, "ws12"));
		
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws13").send(), 
				List.of("a", "b","c")
			)
		);
		Assertions.assertEquals("a", getStringField(webSocketController, "ws13"));
		
		Assertions.assertEquals(
			List.<String>of(), 
			sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws14").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("a", getStringField(webSocketController, "ws14"));
		
		Assertions.assertEquals(
			List.<String>of("ws15", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws15").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws16", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws16").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws17", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws17").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws18", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws18").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws19", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws19").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws20", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws20").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	public static void test21to30(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("ws21", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws21").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws22\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws22").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws23\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws23").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws24\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws24").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws25\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws25").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws26\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws26").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws27\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws27").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws28\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws28").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws29", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws29").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws30", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws30").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	public static void test31to40(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("ws31", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws31").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws32", "a", "b", "c"), 
				sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws32").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws33", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws33").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws34", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws34").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws35", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws35").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws36\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws36").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws37\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws37").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws38\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws38").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws39\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws39").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws40\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws40").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	public static void test41to50(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws41\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws41").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws42\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws42").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws43a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws43").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws44a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws44").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws45a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws45").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws46a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws46").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws47a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws47").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws48a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws48").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws49a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws49").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws50a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws50").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	public static void test51to60(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws51a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws51").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws52a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws52").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws53a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws53").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws54a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws54").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws55a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws55").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws56a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws56").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws57", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws57").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws58", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws58").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws59\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws59").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws60", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws60").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
	}
	
	public static void test61to70(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws61\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws61").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws62", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws62").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws63\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws63").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws64", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws64").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws65", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws65").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws66\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws66").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws67", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws67").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws68\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws68").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws69", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws69").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws70\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws70").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	public static void test71to80(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("ws71", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws71").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws72", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws72").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws73", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws73").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws74", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws74").send(), 
				List.of( "a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws75", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws75").subProtocol("json").send(), 
				List.of( "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws76", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws76").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws77", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws77").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws78\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws78").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws79\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws79").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws80\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws80").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
	}
	
	public static void test81to90(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws81\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws81").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws82\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws82").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws83\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws83").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws84\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws84").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws84\"}", "{\"message\":\"a\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws84").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws85", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws85").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws86", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws86").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws87", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws87").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws88", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws88").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws89", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws89").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws90", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws90").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	public static void test91to100(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("ws91", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws91").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws92\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws92").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws93\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws93").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws94\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws94").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws95\"}", "{\"message\":\"a\"}", "{\"message\":\"b\"}", "{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws95").subProtocol("json").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws96\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws96").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws97\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws97").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws98\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws98").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws99a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws99").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws100a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws100").send(), 
				List.of("a", "b", "c")
			)
		);
	}
	
	public static void test101to110(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("ws101a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws101").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws102a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws102").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws103a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws103").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws104a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws104").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws105a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws105").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws106a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws106").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws107a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws107").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws109a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws109").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws110a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws110").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
	}
	
	public static void test111to120(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws111a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws111").subProtocol("json").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws112a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws112").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws113", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws113").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws114", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws114").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws115\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws115").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws116", "a", "b", "c"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws116").send(), 
				List.of("a", "b", "c"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws117\"}", "{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"), 
			sendMessagesReceiveAndClose(
				endpoint.webSocketRequest("/ws117").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}"),
				4
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws118", "a"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws118").send(), 
				List.of("a", "b", "c")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"message\":\"ws119\"}", "{\"message\":\"a\"}"), 
			sendMessagesAndReceive(
				endpoint.webSocketRequest("/ws119").subProtocol("json").send(), 
				List.of("{\"message\":\"a\"}","{\"message\":\"b\"}","{\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.webSocketRequest("/ws120").send()
			)
		);
		Assertions.assertTrue(getBooleanField(webSocketController, "ws120"));
	}
	
	public static void test121to130(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.webSocketRequest("/ws121").send()
			)
		);
		Assertions.assertTrue(getBooleanField(webSocketController, "ws121"));
		
		Assertions.assertEquals(
			List.<String>of(), 
			receive(
				endpoint.webSocketRequest("/ws122").send()
			)
		);
		Assertions.assertTrue(getBooleanField(webSocketController, "ws122"));
		
		Assertions.assertEquals(
			List.<String>of("ws", "123"), 
			receive(
				endpoint.webSocketRequest("/ws123").send()
			)
		);

		Assertions.assertEquals(
			List.<String>of("ws", "124"), 
			receive(
				endpoint.webSocketRequest("/ws124").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws125"), 
			receive(
				endpoint.webSocketRequest("/ws125").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws126", "ws126"), 
			receive(
				endpoint.webSocketRequest("/ws126").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws127", "ws127"), 
			receive(
				endpoint.webSocketRequest("/ws127").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws128", "ws128"), 
			receive(
				endpoint.webSocketRequest("/ws128").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws129", "ws129"), 
			receive(
				endpoint.webSocketRequest("/ws129").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws130", "ws130"), 
			receive(
				endpoint.webSocketRequest("/ws130").send()
			)
		);
	}
	
	public static void test131to140(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("ws131", "ws131"), 
			receive(
				endpoint.webSocketRequest("/ws131").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws132"), 
			receive(
				endpoint.webSocketRequest("/ws132").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws133"), 
			receive(
				endpoint.webSocketRequest("/ws133").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("ws134"), 
			receive(
				endpoint.webSocketRequest("/ws134").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"135\"}"), 
			receive(
				endpoint.webSocketRequest("/ws135").subProtocol("json").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"136\"}"), 
			receive(
				endpoint.webSocketRequest("/ws136").subProtocol("json").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws137\"}"), 
			receive(
				endpoint.webSocketRequest("/ws137").subProtocol("json").send()
			)
		);
		
		Assertions.assertEquals(
			List.<String>of(), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws138").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("1:a2:b3:c", getStringBuilderField(webSocketController, "ws138"));
		
		Assertions.assertEquals(
			List.<String>of(), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws139").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("1:a2:b3:c", getStringBuilderField(webSocketController, "ws139"));
		
		Assertions.assertEquals(
			List.<String>of(), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws140").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		Assertions.assertEquals("1:a", getStringField(webSocketController, "ws140"));
	}
	
	public static void test141to150(Endpoint endpoint, Object webSocketController) throws Exception {
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws141\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws141").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws142\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws142").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws143a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws143").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws144\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws144").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws145\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws145").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws146a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws146").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws147\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws147").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":0,\"message\":\"ws148\"}", "{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws148").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
		
		Assertions.assertEquals(
			List.<String>of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"ws149a\"}"), 
				sendMessagesCloseAndReceive(
				endpoint.webSocketRequest("/ws149").subProtocol("json").send(),
				List.of("{\"@type\":\"GenericMessage\",\"id\":1,\"message\":\"a\"}", "{\"@type\":\"GenericMessage\",\"id\":2,\"message\":\"b\"}", "{\"@type\":\"GenericMessage\",\"id\":3,\"message\":\"c\"}")
			)
		);
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
					.messages(factory -> Flux.fromIterable(messages).map(factory::text))
				)
				.flatMap(message -> message.reducedText())
				.bufferTimeout(take, Duration.ofSeconds(TIMEOUT_SECONDS))
				.doFinally(ign -> exchange.close(code))
			)
			.blockFirst();
	}
	
	private static List<String> sendMessagesCloseAndReceive(Mono<WebSocketExchange<ExchangeContext>> ws, List<String> messages) {
		return sendMessagesCloseAndReceive(ws, messages, (short)1000);
	}
	
	private static List<String> sendMessagesCloseAndReceive(Mono<WebSocketExchange<ExchangeContext>> ws, List<String> messages, short code) {
		return ws
			.flatMapMany(exchange -> Flux.from(exchange.inbound().textMessages())
				.doOnSubscribe(ign -> exchange.outbound()
					.messages(factory -> Flux.fromIterable(messages)
						.map(factory::text)
						.doOnComplete(() -> exchange.close(code))
					)
				)
			)
			.flatMap(message -> message.reducedText())
			.collectList()
			.block();
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
}
