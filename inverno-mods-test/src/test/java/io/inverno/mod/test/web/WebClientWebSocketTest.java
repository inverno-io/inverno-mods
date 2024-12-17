/*
 * Copyright 2024 Jeremy Kuhn
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

import io.inverno.mod.base.Charsets;
import io.inverno.mod.configuration.source.MapConfigurationSource;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ws.BaseWebSocketExchange;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.ModsTestUtils;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.ws.Web2SocketExchange;
import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import io.inverno.test.InvernoTestCompiler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class WebClientWebSocketTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
//		System.setProperty("io.netty.leakDetection.level", "PARANOID");
//		System.setProperty("io.netty.leakDetection.targetRecords", "20");
	}

	private static final String MODULE_CLIENT = "io.inverno.mod.test.web.client.ws";
	private static final String MODULE_SERVER = "io.inverno.mod.test.web.client.ws.server";

	private static InvernoModuleProxy testWebSocketServerModuleProxy;
	private static Object testWebSocketClientController;
	private static Map<String, Field> testWebSocketClientControllerFields;

	private static InvernoModuleProxy testWebSocketClientModuleProxy;
	private static Object testWebSocketClient;
	private static Map<String , Method> testWebSocketClientMethods;
	private static Class<?> client_message_class;
	private static Constructor<?> client_Message_constructor;
	private static Method client_Message_getMessage;

	@BeforeAll
	public static void init() throws IOException, InvernoCompilationException, ClassNotFoundException, InterruptedException, NoSuchMethodException {
		InvernoTestCompiler invernoCompiler = InvernoTestCompiler.builder()
			.moduleOverride(AbstractInvernoModTest.MODULE_OVERRIDE)
			.annotationProcessorModuleOverride(AbstractInvernoModTest.ANNOTATION_PROCESSOR_MODULE_OVERRIDE)
			.build();

		invernoCompiler.cleanModuleTarget();

		int testServerPort = ModsTestUtils.getFreePort();

		InvernoModuleLoader serverModuleLoader = invernoCompiler.compile(MODULE_SERVER);

		Class<?> httpConfigClass = serverModuleLoader.loadClass(MODULE_SERVER, "io.inverno.mod.http.server.HttpServerConfiguration");
		ConfigurationInvocationHandler httpConfigHandler = new ConfigurationInvocationHandler(httpConfigClass, Map.of("server_port", testServerPort, "h2_enabled", false));
		Object httpConfig = Proxy.newProxyInstance(httpConfigClass.getClassLoader(),
			new Class<?>[] { httpConfigClass },
			httpConfigHandler
		);

		Class<?> webConfigClass = serverModuleLoader.loadClass(MODULE_SERVER, "io.inverno.mod.web.server.WebServerConfiguration");
		ConfigurationInvocationHandler webConfigHandler = new ConfigurationInvocationHandler(webConfigClass, Map.of("http_server", httpConfig));
		Object webConfig = Proxy.newProxyInstance(webConfigClass.getClassLoader(),
			new Class<?>[] { webConfigClass },
			webConfigHandler
		);

		Class<?> serverConfigClass = serverModuleLoader.loadClass(MODULE_SERVER, "io.inverno.mod.test.web.client.ws.server.ServerConfiguration");
		ConfigurationInvocationHandler webRouteConfigHandler = new ConfigurationInvocationHandler(serverConfigClass, Map.of("web", webConfig));
		Object serverConfig = Proxy.newProxyInstance(serverConfigClass.getClassLoader(),
			new Class<?>[] { serverConfigClass },
			webRouteConfigHandler
		);

		testWebSocketServerModuleProxy = serverModuleLoader.load(MODULE_SERVER).optionalDependency("serverConfiguration", serverConfigClass, serverConfig).build();

		InvernoModuleLoader clientModuleLoader = invernoCompiler.compile(MODULE_CLIENT);

		// inject a configuration source containing the service config
		MapConfigurationSource configurationSource = new MapConfigurationSource(Map.of("io.inverno.mod.discovery.http.meta.service.testWebSocketService", "{\"routes\":[{\"destinations\":[{\"uri\":\"http://127.0.0.1:" + testServerPort + "\"}]}]}"));

		testWebSocketClientModuleProxy = clientModuleLoader.load(MODULE_CLIENT).dependencies(configurationSource).build();

		testWebSocketServerModuleProxy.start();

		testWebSocketClientController = testWebSocketServerModuleProxy.getBean("testWebSocketClientController");
		testWebSocketClientControllerFields = Arrays.stream(testWebSocketClientController.getClass().getDeclaredFields())
			.filter(field -> Modifier.isPublic(field.getModifiers()))
			.collect(Collectors.toMap(Field::getName, Function.identity()));

		testWebSocketClientModuleProxy.start();

		testWebSocketClient = testWebSocketClientModuleProxy.getBean("testWebSocketClient");
		testWebSocketClientMethods = Arrays.stream(testWebSocketClient.getClass().getDeclaredMethods())
			.filter(method -> Modifier.isPublic(method.getModifiers()))
			.collect(Collectors.toMap(Method::getName, Function.identity()));

		client_message_class = clientModuleLoader.loadClass(MODULE_CLIENT, "io.inverno.mod.test.web.client.ws.dto.Message");
		client_Message_constructor = client_message_class.getConstructor(String.class);
		client_Message_getMessage = client_message_class.getMethod("getMessage");
	}

	@AfterAll
	public static void destroy() {
		testWebSocketClientController = null;
		testWebSocketClientControllerFields = null;
		testWebSocketClient = null;
		testWebSocketClientMethods = null;
		client_Message_constructor = null;
		if(testWebSocketServerModuleProxy != null) {
			testWebSocketServerModuleProxy.stop();
			testWebSocketServerModuleProxy = null;
		}
		if(testWebSocketClientModuleProxy != null) {
			testWebSocketClientModuleProxy.stop();
			testWebSocketClientModuleProxy = null;
		}
	}

	private static Object createMessage(String message) {
		try {
			return client_Message_constructor.newInstance(message);
		}
		catch(InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getMessage(Object message) {
		try {
			return (String)client_Message_getMessage.invoke(message);
		}
		catch(IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T getServerInvocationField(String field) {
		try {
			return (T) testWebSocketClientControllerFields.get(field).get(testWebSocketClientController);
		}
		catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Publisher<T> callForPublisher(String method, Object... args) {
		try {
			return (Publisher<T>)testWebSocketClientMethods.get(method).invoke(testWebSocketClient, args);
		}
		catch(IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Flux<T> callForFlux(String method, Object... args) {
		try {
			return (Flux<T>)testWebSocketClientMethods.get(method).invoke(testWebSocketClient, args);
		}
		catch(IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Mono<T> callForMono(String method, Object... args) {
		try {
			return (Mono<T>)testWebSocketClientMethods.get(method).invoke(testWebSocketClient, args);
		}
		catch(IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void test_out_publisher_raw() {
		Publisher<Publisher<ByteBuf>> outbound = Flux.just(
			Flux.just(Unpooled.copiedBuffer("message", Charsets.DEFAULT), Unpooled.copiedBuffer(" 1", Charsets.DEFAULT)),
			Flux.just(Unpooled.copiedBuffer("message", Charsets.DEFAULT), Unpooled.copiedBuffer(" 2", Charsets.DEFAULT)),
			Flux.just(Unpooled.copiedBuffer("message", Charsets.DEFAULT), Unpooled.copiedBuffer(" 3", Charsets.DEFAULT))
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_publisher_raw", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("out_publisher_raw", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_publisher_raw_reduced() {
		Publisher<ByteBuf> outbound = Flux.just(
			Unpooled.copiedBuffer("message 1", Charsets.DEFAULT),
			Unpooled.copiedBuffer("message 2", Charsets.DEFAULT),
			Unpooled.copiedBuffer("message 3", Charsets.DEFAULT)
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_publisher_raw_reduced", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("out_publisher_raw_reduced", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_publisher_string() {
		Publisher<Publisher<String>> outbound = Flux.just(
			Flux.just("message", " 1"),
			Flux.just("message", " 2"),
			Flux.just("message", " 3")
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_publisher_string", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("out_publisher_string", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_publisher_string_reduced() {
		Publisher<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_publisher_string_reduced", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("out_publisher_string_reduced", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_publisher_encoded() {
		Publisher<Object> outbound = Flux.just(
			createMessage("message 1"),
			createMessage("message 2"),
			createMessage("message 3")
		);
		List<String> inbound = WebClientWebSocketTest.<Object>callForFlux("out_publisher_encoded", outbound).take(4).map(WebClientWebSocketTest::getMessage).collectList().block();
		Assertions.assertEquals(List.of("out_publisher_encoded", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_publisher_void() {
		Publisher<Void> outbound = Mono.empty();
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_publisher_void", outbound).take(1).collectList().block();
		Assertions.assertEquals(List.of("out_publisher_void"), inbound);
	}

	@Test
	public void test_out_flux_raw() {
		Flux<Flux<ByteBuf>> outbound = Flux.just(
			Flux.just(Unpooled.copiedBuffer("message", Charsets.DEFAULT), Unpooled.copiedBuffer(" 1", Charsets.DEFAULT)),
			Flux.just(Unpooled.copiedBuffer("message", Charsets.DEFAULT), Unpooled.copiedBuffer(" 2", Charsets.DEFAULT)),
			Flux.just(Unpooled.copiedBuffer("message", Charsets.DEFAULT), Unpooled.copiedBuffer(" 3", Charsets.DEFAULT))
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_flux_raw", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("out_flux_raw", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_flux_raw_reduced() {
		Flux<ByteBuf> outbound = Flux.just(
			Unpooled.copiedBuffer("message 1", Charsets.DEFAULT),
			Unpooled.copiedBuffer("message 2", Charsets.DEFAULT),
			Unpooled.copiedBuffer("message 3", Charsets.DEFAULT)
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_flux_raw_reduced", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("out_flux_raw_reduced", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_flux_string() {
		Flux<Flux<String>> outbound = Flux.just(
			Flux.just("message", " 1"),
			Flux.just("message", " 2"),
			Flux.just("message", " 3")
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_flux_string", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("out_flux_string", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_flux_string_reduced() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_flux_string_reduced", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("out_flux_string_reduced", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_flux_encoded() {
		Flux<Object> outbound = Flux.just(
			createMessage("message 1"),
			createMessage("message 2"),
			createMessage("message 3")
		);
		List<String> inbound = WebClientWebSocketTest.<Object>callForFlux("out_flux_encoded", outbound).take(4).map(WebClientWebSocketTest::getMessage).collectList().block();
		Assertions.assertEquals(List.of("out_flux_encoded", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_out_flux_void() {
		Flux<Void> outbound = Flux.empty();
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_flux_void", outbound).take(1).collectList().block();
		Assertions.assertEquals(List.of("out_flux_void"), inbound);
	}

	@Test
	public void test_out_mono_raw() {
		Mono<Mono<ByteBuf>> outbound = Mono.just(
			Mono.just(Unpooled.copiedBuffer("message", Charsets.DEFAULT))
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_mono_raw", outbound).take(2).collectList().block();
		Assertions.assertEquals(List.of("out_mono_raw", "message"), inbound);
	}

	@Test
	public void test_out_mono_raw_reduced() {
		Mono<ByteBuf> outbound = Mono.just(
			Unpooled.copiedBuffer("message", Charsets.DEFAULT)
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_mono_raw_reduced", outbound).take(2).collectList().block();
		Assertions.assertEquals(List.of("out_mono_raw_reduced", "message"), inbound);
	}

	@Test
	public void test_out_mono_string() {
		Mono<Mono<String>> outbound = Mono.just(
			Mono.just("message")
		);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_mono_string", outbound).take(2).collectList().block();
		Assertions.assertEquals(List.of("out_mono_string", "message"), inbound);
	}

	@Test
	public void test_out_mono_string_reduced() {
		Mono<String> outbound = Mono.just("message");
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_mono_string_reduced", outbound).take(2).collectList().block();
		Assertions.assertEquals(List.of("out_mono_string_reduced", "message"), inbound);
	}

	@Test
	public void test_out_mono_encoded() {
		Mono<Object> outbound = Mono.just(
			createMessage("message")
		);
		List<String> inbound = WebClientWebSocketTest.<Object>callForFlux("out_mono_encoded", outbound).take(2).map(WebClientWebSocketTest::getMessage).collectList().block();
		Assertions.assertEquals(List.of("out_mono_encoded", "message"), inbound);
	}

	@Test
	public void test_out_mono_void() {
		Mono<Void> outbound = Mono.empty();
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_mono_void", outbound).take(1).collectList().block();
		Assertions.assertEquals(List.of("out_mono_void"), inbound);
	}

	@Test
	public void test_in_publisher_raw() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = Flux.from(WebClientWebSocketTest.<Publisher<ByteBuf>>callForPublisher("in_publisher_raw", outbound)).flatMap(messageData -> Flux.from(messageData).reduceWith(Unpooled::buffer, ByteBuf::writeBytes)).map(messageData -> messageData.toString(Charsets.DEFAULT)).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_publisher_raw", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_publisher_raw_reduced() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = Flux.from(WebClientWebSocketTest.<ByteBuf>callForPublisher("in_publisher_raw_reduced", outbound)).map(messageData -> messageData.toString(Charsets.DEFAULT)).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_publisher_raw_reduced", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_publisher_string() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = Flux.from(WebClientWebSocketTest.<Publisher<String>>callForPublisher("in_publisher_string", outbound)).flatMap(messageData -> Flux.from(messageData).collect(Collectors.joining())).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_publisher_string", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_publisher_string_reduced() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = Flux.from(WebClientWebSocketTest.<String>callForPublisher("in_publisher_string_reduced", outbound)).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_publisher_string_reduced", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_publisher_encoded() {
		Flux<Object> outbound = Flux.just(
			createMessage("message 1"),
			createMessage("message 2"),
			createMessage("message 3")
		);
		List<String> inbound = Flux.from(WebClientWebSocketTest.<Object>callForPublisher("in_publisher_encoded", outbound)).map(WebClientWebSocketTest::getMessage).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_publisher_encoded", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_publisher_void() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		Flux.from(WebClientWebSocketTest.<String>callForPublisher("in_publisher_void", outbound)).blockLast();
		Assertions.assertEquals(List.of("message 1", "message 2", "message 3"), WebClientWebSocketTest.<List<String>>getServerInvocationField("in_publisher_void"));
	}

	@Test
	public void test_in_flux_raw() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<Flux<ByteBuf>>callForFlux("in_flux_raw", outbound).flatMap(messageData -> messageData.reduceWith(Unpooled::buffer, ByteBuf::writeBytes)).map(messageData -> messageData.toString(Charsets.DEFAULT)).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_flux_raw", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_flux_raw_reduced() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<ByteBuf>callForFlux("in_flux_raw_reduced", outbound).map(messageData -> messageData.toString(Charsets.DEFAULT)).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_flux_raw_reduced", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_flux_string() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<Flux<String>>callForFlux("in_flux_string", outbound).flatMap(messageData -> messageData.collect(Collectors.joining())).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_flux_string", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_flux_string_reduced() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("in_flux_string_reduced", outbound).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_flux_string_reduced", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_flux_encoded() {
		Flux<Object> outbound = Flux.just(
			createMessage("message 1"),
			createMessage("message 2"),
			createMessage("message 3")
		);
		List<String> inbound = WebClientWebSocketTest.<Object>callForFlux("in_flux_encoded", outbound).map(WebClientWebSocketTest::getMessage).take(4).collectList().block();
		Assertions.assertEquals(List.of("in_flux_encoded", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_in_flux_void() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		WebClientWebSocketTest.<String>callForFlux("in_flux_void", outbound).blockLast();
		Assertions.assertEquals(List.of("message 1", "message 2", "message 3"), WebClientWebSocketTest.<List<String>>getServerInvocationField("in_flux_void"));
	}

	@Test
	public void test_in_mono_raw() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		String inbound = WebClientWebSocketTest.<Mono<ByteBuf>>callForMono("in_mono_raw", outbound).flatMap(Function.identity()).map(messageData -> messageData.toString(Charsets.DEFAULT)).block();
		Assertions.assertEquals("in_mono_raw: message 1, message 2, message 3, ", inbound);
	}

	@Test
	public void test_in_mono_raw_reduced() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		String inbound = WebClientWebSocketTest.<ByteBuf>callForMono("in_mono_raw_reduced", outbound).map(messageData -> messageData.toString(Charsets.DEFAULT)).block();
		Assertions.assertEquals("in_mono_raw_reduced: message 1, message 2, message 3, ", inbound);
	}

	@Test
	public void test_in_mono_string() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		String inbound = WebClientWebSocketTest.<Mono<String>>callForMono("in_mono_string", outbound).flatMap(Function.identity()).block();
		Assertions.assertEquals("in_mono_string: message 1, message 2, message 3, ", inbound);
	}

	@Test
	public void test_in_mono_string_reduced() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		String inbound = WebClientWebSocketTest.<String>callForMono("in_mono_string_reduced", outbound).block();
		Assertions.assertEquals("in_mono_string_reduced: message 1, message 2, message 3, ", inbound);
	}

	@Test
	public void test_in_mono_encoded() {
		Flux<Object> outbound = Flux.just(
			createMessage("message 1"),
			createMessage("message 2"),
			createMessage("message 3")
		);
		String inbound = WebClientWebSocketTest.<Object>callForMono("in_mono_encoded", outbound).map(WebClientWebSocketTest::getMessage).block();
		Assertions.assertEquals("in_mono_encoded: message 1, message 2, message 3", inbound);
	}

	@Test
	public void test_in_mono_void() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		WebClientWebSocketTest.<String>callForMono("in_mono_void", outbound).block();
		Assertions.assertEquals(List.of("message 1", "message 2", "message 3"), WebClientWebSocketTest.<List<String>>getServerInvocationField("in_mono_void"));
	}

	@Test
	public void test_out_none() {
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("out_none").collectList().block();
		Assertions.assertEquals(List.of("out_none", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_web_exchange_configurer() {
		WebExchange.Configurer<ExchangeContext> exchangeConfigurerMock = Mockito.mock(WebExchange.Configurer.class);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("web_exchange_configurer", exchangeConfigurerMock).collectList().block();
		Assertions.assertEquals(List.of("web_exchange_configurer", "message 1", "message 2", "message 3"), inbound);
		Mockito.verify(exchangeConfigurerMock).configure(Mockito.any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_web_exchange_configurer_generic() {
		WebExchange.Configurer<ExchangeContext> exchangeConfigurerMock = Mockito.mock(WebExchange.Configurer.class);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("web_exchange_configurer_generic", exchangeConfigurerMock).collectList().block();
		Assertions.assertEquals(List.of("web_exchange_configurer_generic", "message 1", "message 2", "message 3"), inbound);
		Mockito.verify(exchangeConfigurerMock).configure(Mockito.any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_web_socket_exchange_configurer() {
		Web2SocketExchange.Configurer<ExchangeContext> wsExchangeConfigurerMock = Mockito.mock(Web2SocketExchange.Configurer.class);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("web_socket_exchange_configurer", wsExchangeConfigurerMock).collectList().block();
		Assertions.assertEquals(List.of("web_socket_exchange_configurer", "message 1", "message 2", "message 3"), inbound);
		Mockito.verify(wsExchangeConfigurerMock).configure(Mockito.any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_web_socket_exchange_configurer_generic() {
		Web2SocketExchange.Configurer<ExchangeContext> wsExchangeConfigurerMock = Mockito.mock(Web2SocketExchange.Configurer.class);
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("web_socket_exchange_configurer_generic", wsExchangeConfigurerMock).collectList().block();
		Assertions.assertEquals(List.of("web_socket_exchange_configurer_generic", "message 1", "message 2", "message 3"), inbound);
		Mockito.verify(wsExchangeConfigurerMock).configure(Mockito.any());
	}

	@Test
	public void test_outbound_configurer() {
		Consumer<BaseWeb2SocketExchange.Outbound> outboundConfigurer = outbound -> {
			outbound.closeOnComplete(false).messages(factory -> Flux.just("message 1", "message 2", "message 3").map(factory::text));
		};
		List<String> inbound = WebClientWebSocketTest.<String>callForFlux("outbound_configurer", outboundConfigurer).take(4).collectList().block();
		Assertions.assertEquals(List.of("outbound_configurer", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_return_inbound() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<BaseWeb2SocketExchange.Inbound>callForMono("return_inbound", outbound)
			.flatMapMany(BaseWebSocketExchange.Inbound::textMessages)
			.flatMap(WebSocketMessage::stringReduced)
			.take(4)
			.collectList()
			.block();

		Assertions.assertEquals(List.of("return_inbound", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_return_web_socket_exchange() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<Web2SocketExchange<? extends ExchangeContext>>callForMono("return_web_socket_exchange", outbound)
			.flatMapMany(exchange -> exchange.inbound().textMessages())
			.flatMap(WebSocketMessage::stringReduced)
			.take(4)
			.collectList()
			.block();

		Assertions.assertEquals(List.of("return_web_socket_exchange", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	public void test_return_web_socket_exchange_generic() {
		Flux<String> outbound = Flux.just("message 1", "message 2", "message 3");
		List<String> inbound = WebClientWebSocketTest.<Web2SocketExchange<ExchangeContext>>callForMono("return_web_socket_exchange_generic", outbound)
			.flatMapMany(exchange -> exchange.inbound().textMessages())
			.flatMap(WebSocketMessage::stringReduced)
			.take(4)
			.collectList()
			.block();

		Assertions.assertEquals(List.of("return_web_socket_exchange_generic", "message 1", "message 2", "message 3"), inbound);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_mixed_parameters() {
		String pathParam = "path_parameter";
		String queryParam = "query_parameter";
		String headerParam = "header_parameter";
		String cookieParam = "cookie_parameter";
		WebExchange.Configurer<? extends ExchangeContext> exchangeConfigurerMock = Mockito.mock(WebExchange.Configurer.class);
		Flux<Object> outbound = Flux.just("message 1", "message 2", "message 3").map(WebClientWebSocketTest::createMessage);
		List<String> inbound = WebClientWebSocketTest.<Object>callForFlux("mixed_parameters", pathParam, queryParam, headerParam, cookieParam, exchangeConfigurerMock, outbound).take(8).map(WebClientWebSocketTest::getMessage).collectList().block();

		Assertions.assertEquals(List.of("mixed_parameters", pathParam, queryParam, headerParam, cookieParam, "message 1", "message 2", "message 3"), inbound);
		Mockito.verify(exchangeConfigurerMock).configure(Mockito.any());
	}
}
