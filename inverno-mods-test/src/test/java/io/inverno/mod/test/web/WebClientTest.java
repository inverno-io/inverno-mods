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
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.configuration.source.MapConfigurationSource;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.ModsTestUtils;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.WebResponse;
import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import io.inverno.test.InvernoTestCompiler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
public class WebClientTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
//		System.setProperty("io.netty.leakDetection.level", "PARANOID");
//		System.setProperty("io.netty.leakDetection.targetRecords", "20");
	}

	private static final String MODULE_CLIENT = "io.inverno.mod.test.web.client";
	private static final String MODULE_SERVER = "io.inverno.mod.test.web.client.server";

	private static InvernoModuleProxy testServerModuleProxy;
	private static Object testClientController;
	private static Map<String, Field> testClientControllerFields;

	private static InvernoModuleProxy testClientModuleProxy;
	private static Object testClient;
	private static Map<String ,Method> testClientMethods;
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
		ConfigurationInvocationHandler httpConfigHandler = new ConfigurationInvocationHandler(httpConfigClass, Map.of("server_port", testServerPort, "h2_enabled", true));
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

		Class<?> serverConfigClass = serverModuleLoader.loadClass(MODULE_SERVER, "io.inverno.mod.test.web.client.server.ServerConfiguration");
		ConfigurationInvocationHandler webRouteConfigHandler = new ConfigurationInvocationHandler(serverConfigClass, Map.of("web", webConfig));
		Object serverConfig = Proxy.newProxyInstance(serverConfigClass.getClassLoader(),
			new Class<?>[] { serverConfigClass },
			webRouteConfigHandler
		);

		testServerModuleProxy = serverModuleLoader.load(MODULE_SERVER).optionalDependency("serverConfiguration", serverConfigClass, serverConfig).build();

		InvernoModuleLoader clientModuleLoader = invernoCompiler.compile(MODULE_CLIENT);

		// inject a configuration source containing the service config
		MapConfigurationSource configurationSource = new MapConfigurationSource(Map.of("io.inverno.mod.discovery.http.meta.service.testService", "http://127.0.0.1:" + testServerPort));
//		MapConfigurationSource configurationSource = new MapConfigurationSource(Map.of("io.inverno.mod.discovery.http.meta.service.testService", "{\"routes\":[{\"destinations\":[{\"uri\":\"http://127.0.0.1:" + testServerPort + "\"}]}]}"));

		testClientModuleProxy = clientModuleLoader.load(MODULE_CLIENT).dependencies(configurationSource).build();

		testServerModuleProxy.start();

		testClientController = testServerModuleProxy.getBean("testClientController");
		testClientControllerFields = Arrays.stream(testClientController.getClass().getDeclaredFields())
			.filter(field -> Modifier.isPublic(field.getModifiers()))
			.collect(Collectors.toMap(Field::getName, Function.identity()));

		testClientModuleProxy.start();

		testClient = testClientModuleProxy.getBean("testClient");
		testClientMethods = Arrays.stream(testClient.getClass().getDeclaredMethods())
			.filter(method -> Modifier.isPublic(method.getModifiers()))
			.collect(Collectors.toMap(Method::getName, Function.identity()));

		client_message_class = clientModuleLoader.loadClass(MODULE_CLIENT, "io.inverno.mod.test.web.client.dto.Message");
		client_Message_constructor = client_message_class.getConstructor(String.class);
		client_Message_getMessage = client_message_class.getMethod("getMessage");
	}

	@AfterAll
	public static void destroy() {
		testClientController = null;
		testClientControllerFields = null;
		testClient = null;
		testClientMethods = null;
		client_Message_constructor = null;
		if(testServerModuleProxy != null) {
			testServerModuleProxy.stop();
			testServerModuleProxy = null;
		}
		if(testClientModuleProxy != null) {
			testClientModuleProxy.stop();
			testClientModuleProxy = null;
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

	private static Object createMessageArray(String... messages) {
		Object messageArray = Array.newInstance(client_message_class, messages.length);
		for(int i=0;i<messages.length;i++) {
			Array.set(messageArray, i, createMessage(messages[i]));
		}
		return messageArray;
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
			return (T) testClientControllerFields.get(field).get(testClientController);
		}
		catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Publisher<T> callForPublisher(String method, Object... args) {
		try {
			return (Publisher<T>)testClientMethods.get(method).invoke(testClient, args);
		}
		catch(IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Flux<T> callForFlux(String method, Object... args) {
		try {
			return (Flux<T>)testClientMethods.get(method).invoke(testClient, args);
		}
		catch(IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Mono<T> callForMono(String method, Object... args) {
		try {
			return (Mono<T>)testClientMethods.get(method).invoke(testClient, args);
		}
		catch(IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void test_create() {
		String message = "message";
		WebClientTest.<Void>callForMono("create", createMessage(message)).block();
		Assertions.assertEquals("message", WebClientTest.<String>getServerInvocationField("create"));
	}

	@Test
	public void test_list() {
		List<String> responseBody = WebClientTest.<Object>callForFlux("list").map(WebClientTest::getMessage).collectList().block();
		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("list"));
		Assertions.assertEquals(List.of("message1", "message2"), responseBody);
	}

	@Test
	public void test_get() {
		String id = "123";
		String responseBody = WebClientTest.<Object>callForMono("get", id).map(WebClientTest::getMessage).block();
		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get"));
		Assertions.assertEquals("message " + id, responseBody);
	}

	@Test
	public void test_update() {
		String id = "123";
		String message = "message";
		String responseBody = WebClientTest.<Object>callForMono("update", id, createMessage(message)).map(WebClientTest::getMessage).block();
		Assertions.assertEquals(id, WebClientTest.<String>getServerInvocationField("update"));
		Assertions.assertEquals(message, responseBody);
	}

	@Test
	public void test_delete() {
		String id = "123";
		String responseBody = WebClientTest.<Object>callForMono("delete", id).map(WebClientTest::getMessage).block();
		Assertions.assertEquals(id, WebClientTest.<String>getServerInvocationField("delete"));
	}

	@Test
	public void test_get_mono_web_exchange() {
		String responseBody = WebClientTest.<WebExchange<? extends ExchangeContext>>callForMono("get_mono_web_exchange")
			.flatMap(WebExchange::response)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining())
			.block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_mono_web_exchange"));
		Assertions.assertEquals("get_mono_web_exchange", responseBody);
	}

	@Test
	public void test_get_mono_web_exchange_generic() {
		String responseBody = WebClientTest.<WebExchange<? extends ExchangeContext>>callForMono("get_mono_web_exchange_generic")
			.flatMap(WebExchange::response)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining())
			.block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_mono_web_exchange_generic"));
		Assertions.assertEquals("get_mono_web_exchange_generic", responseBody);
	}

	@Test
	public void test_post_mono_web_exchange_with_params_and_body() {
		String headerparam = "header value";
		String body = "This is a test";
		String responseBody = WebClientTest.<WebExchange<? extends ExchangeContext>>callForMono("post_mono_web_exchange_with_params_and_body", headerparam, body)
			.flatMap(WebExchange::response)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining())
			.block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_mono_web_exchange_with_params_and_body"));
		Assertions.assertEquals(headerparam + ", " + body, responseBody);
	}

	@Test
	public void test_get_mono_web_response() {
		String responseBody = WebClientTest.<WebResponse>callForMono("get_mono_web_response")
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining())
			.block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_mono_web_response"));
		Assertions.assertEquals("get_mono_web_response", responseBody);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_get_exchange() {
		WebExchange.Configurer<ExchangeContext> exchangeConsumerMock = Mockito.mock(WebExchange.Configurer.class);
		String responseBody = WebClientTest.<String>callForMono("get_exchange", exchangeConsumerMock).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_exchange"));
		Assertions.assertEquals("get_exchange", responseBody);
		Mockito.verify(exchangeConsumerMock).configure(Mockito.any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_get_exchange_generic() {
		WebExchange.Configurer<ExchangeContext> exchangeConsumerMock = Mockito.mock(WebExchange.Configurer.class);
		String responseBody = WebClientTest.<String>callForMono("get_exchange_generic", exchangeConsumerMock).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_exchange_generic"));
		Assertions.assertEquals("get_exchange_generic", responseBody);
		Mockito.verify(exchangeConsumerMock).configure(Mockito.any());
	}

	@Test
	public void test_get_publisher_void() {
		Flux.from(WebClientTest.<Void>callForPublisher("get_publisher_void")).blockLast();
		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_publisher_void"));
	}

	@Test
	public void test_get_publisher_raw() {
		String responseBody = Flux.from(WebClientTest.<ByteBuf>callForPublisher("get_publisher_raw")).map(chunk -> {try{return chunk.toString(Charsets.DEFAULT);} finally {chunk.release();}}).collect(Collectors.joining()).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_publisher_raw"));
		Assertions.assertEquals("get_publisher_raw", responseBody);

		// Note that whether a stream is actually received depends on whether the request is a H2C upgrade request for which response data might be bufferized.
	}

	@Test
	public void test_get_publisher_string() {
		String responseBody = Flux.from(WebClientTest.<String>callForPublisher("get_publisher_string")).collect(Collectors.joining()).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_publisher_string"));
		Assertions.assertEquals("get_publisher_string", responseBody);

		// Note that whether a stream is actually received depends on whether the request is a H2C upgrade request for which response data might be bufferized.
	}

	@Test
	public void test_get_publisher_string_encoded() {
		String responseBody = Flux.from(WebClientTest.<String>callForPublisher("get_publisher_string_encoded")).collect(Collectors.joining()).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_publisher_string_encoded"));
		Assertions.assertEquals("get_publisher_string_encoded", responseBody);
	}

	@Test
	public void test_get_publisher_encoded() {
		List<String> responseBody = Flux.from(WebClientTest.<Object>callForPublisher("get_publisher_encoded")).map(WebClientTest::getMessage).collectList().block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_publisher_encoded"));
		Assertions.assertEquals(List.of("get_publisher", "_encoded"), responseBody);
	}

	@Test
	public void test_post_publisher_raw() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_publisher_raw", Mono.just(Unpooled.copiedBuffer(message, Charsets.DEFAULT))).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_publisher_raw"));
		Assertions.assertEquals("post_publisher_raw: " + message, responseBody);
	}

	@Test
	public void test_post_publisher_string() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_publisher_string", Mono.just(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_publisher_string"));
		Assertions.assertEquals("post_publisher_string: " + message, responseBody);
	}

	@Test
	public void test_post_publisher_string_encoded() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_publisher_string_encoded", Mono.just(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_publisher_string_encoded"));
		Assertions.assertEquals("post_publisher_string_encoded: " + message, responseBody);
	}

	@Test
	public void test_post_publisher_encoded() {
		String message1 = "message1";
		String message2 = "message2";
		String responseBody = WebClientTest.<String>callForMono("post_publisher_encoded", Flux.just(message1, message2).map(WebClientTest::createMessage)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_publisher_encoded"));
		Assertions.assertEquals("post_publisher_encoded: " + message1 + ", " + message2, responseBody);
	}

	@Test
	public void test_get_flux_void() {
		Flux.from(WebClientTest.<Void>callForFlux("get_flux_void")).blockLast();
		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_flux_void"));
	}

	@Test
	public void test_get_flux_raw() {
		String responseBody = WebClientTest.<ByteBuf>callForFlux("get_flux_raw").map(chunk -> {try{return chunk.toString(Charsets.DEFAULT);} finally {chunk.release();}}).collect(Collectors.joining()).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_flux_raw"));
		Assertions.assertEquals("get_flux_raw", responseBody);

		// Note that whether a stream is actually received depends on whether the request is a H2C upgrade request for which response data might be bufferized.
	}

	@Test
	public void test_get_flux_string() {
		String responseBody = WebClientTest.<String>callForFlux("get_flux_string").collect(Collectors.joining()).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_flux_string"));
		Assertions.assertEquals("get_flux_string", responseBody);

		// Note that whether a stream is actually received depends on whether the request is a H2C upgrade request for which response data might be bufferized.
	}

	@Test
	public void test_get_flux_string_encoded() {
		String responseBody = WebClientTest.<String>callForFlux("get_flux_string_encoded").collect(Collectors.joining()).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_flux_string_encoded"));
		Assertions.assertEquals("get_flux_string_encoded", responseBody);
	}

	@Test
	public void test_get_flux_encoded() {
		List<String> responseBody = WebClientTest.<Object>callForFlux("get_flux_encoded").map(WebClientTest::getMessage).collectList().block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_flux_encoded"));
		Assertions.assertEquals(List.of("get_flux", "_encoded"), responseBody);
	}

	@Test
	public void test_post_flux_raw() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_flux_raw", Flux.just(Unpooled.copiedBuffer(message, Charsets.DEFAULT))).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_flux_raw"));
		Assertions.assertEquals("post_flux_raw: " + message, responseBody);
	}

	@Test
	public void test_post_flux_string() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_flux_string", Flux.just(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_flux_string"));
		Assertions.assertEquals("post_flux_string: " + message, responseBody);
	}

	@Test
	public void test_post_flux_string_encoded() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_flux_string_encoded", Flux.just(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_flux_string_encoded"));
		Assertions.assertEquals("post_flux_string_encoded: " + message, responseBody);
	}

	@Test
	public void test_post_flux_encoded() {
		String message1 = "message1";
		String message2 = "message2";
		String responseBody = WebClientTest.<String>callForMono("post_flux_encoded", Flux.just(message1, message2).map(WebClientTest::createMessage)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_flux_encoded"));
		Assertions.assertEquals("post_flux_encoded: " + message1 + ", " + message2, responseBody);
	}

	@Test
	public void test_get_mono_void() {
		Flux.from(WebClientTest.<Void>callForMono("get_mono_void")).blockLast();
		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_mono_void"));
	}

	@Test
	public void test_get_mono_raw() {
		String responseBody = WebClientTest.<ByteBuf>callForMono("get_mono_raw").map(chunk -> {try{return chunk.toString(Charsets.DEFAULT);} finally {chunk.release();}}).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_mono_raw"));
		Assertions.assertEquals("get_mono_raw", responseBody);

		// Note that whether a stream is actually received depends on whether the request is a H2C upgrade request for which response data might be bufferized.
	}

	@Test
	public void test_get_mono_string() {
		String responseBody = WebClientTest.<String>callForMono("get_mono_string").block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_mono_string"));
		Assertions.assertEquals("get_mono_string", responseBody);

		// Note that whether a stream is actually received depends on whether the request is a H2C upgrade request for which response data might be bufferized.
	}

	@Test
	public void test_get_mono_string_encoded() {
		String responseBody = WebClientTest.<String>callForMono("get_mono_string_encoded").block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_mono_string_encoded"));
		Assertions.assertEquals("get_mono_string_encoded", responseBody);
	}

	@Test
	public void test_get_mono_encoded() {
		String responseBody = WebClientTest.<Object>callForMono("get_mono_encoded").map(WebClientTest::getMessage).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_mono_encoded"));
		Assertions.assertEquals("get_mono_encoded", responseBody);
	}

	@Test
	public void test_post_mono_raw() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_mono_raw", Mono.just(Unpooled.copiedBuffer(message, Charsets.DEFAULT))).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_mono_raw"));
		Assertions.assertEquals("post_mono_raw: " + message, responseBody);
	}

	@Test
	public void test_post_mono_string() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_mono_string", Mono.just(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_mono_string"));
		Assertions.assertEquals("post_mono_string: " + message, responseBody);
	}

	@Test
	public void test_post_mono_string_encoded() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_mono_string_encoded", Mono.just(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_mono_string_encoded"));
		Assertions.assertEquals("post_mono_string_encoded: " + message, responseBody);
	}

	@Test
	public void test_post_mono_encoded() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_mono_encoded", Mono.just(createMessage(message))).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_mono_encoded"));
		Assertions.assertEquals("post_mono_encoded: " + message, responseBody);
	}

	@Test
	public void test_get_header_param() {
		String param = "param";
		String responseBody = WebClientTest.<String>callForMono("get_header_param", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_header_param"));
		Assertions.assertEquals("get_header_param: " + param, responseBody);
	}

	@Test
	public void test_get_header_param_collection() {
		Collection<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("get_header_param_collection", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_header_param_collection"));
		Assertions.assertEquals("get_header_param_collection: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_header_param_list() {
		List<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("get_header_param_list", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_header_param_list"));
		Assertions.assertEquals("get_header_param_list: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_header_param_set() {
		Set<String> param = new TreeSet<>(List.of("a", "b", "c"));
		String responseBody = WebClientTest.<String>callForMono("get_header_param_set", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_header_param_set"));
		Assertions.assertEquals("get_header_param_set: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_header_param_array() {
		String[] param = new String[]{"a", "b", "c"};
		String responseBody = WebClientTest.<String>callForMono("get_header_param_array", (Object)param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_header_param_array"));
		Assertions.assertEquals("get_header_param_array: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_cookie_param() {
		String param = "param";
		String responseBody = WebClientTest.<String>callForMono("get_cookie_param", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_cookie_param"));
		Assertions.assertEquals("get_cookie_param: " + param, responseBody);
	}

	@Test
	public void test_get_cookie_param_collection() {
		Collection<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("get_cookie_param_collection", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_cookie_param_collection"));
		Assertions.assertEquals("get_cookie_param_collection: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_cookie_param_list() {
		List<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("get_cookie_param_list", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_cookie_param_list"));
		Assertions.assertEquals("get_cookie_param_list: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_cookie_param_set() {
		Set<String> param = new TreeSet<>(List.of("a", "b", "c"));
		String responseBody = WebClientTest.<String>callForMono("get_cookie_param_set", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_cookie_param_set"));
		Assertions.assertEquals("get_cookie_param_set: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_cookie_param_array() {
		String[] param = new String[]{"a", "b", "c"};
		String responseBody = WebClientTest.<String>callForMono("get_cookie_param_array", (Object)param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_cookie_param_array"));
		Assertions.assertEquals("get_cookie_param_array: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_query_param() {
		String param = "param";
		String responseBody = WebClientTest.<String>callForMono("get_query_param", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_query_param"));
		Assertions.assertEquals("get_query_param: " + param, responseBody);
	}

	@Test
	public void test_get_query_param_collection() {
		Collection<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("get_query_param_collection", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_query_param_collection"));
		Assertions.assertEquals("get_query_param_collection: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_query_param_list() {
		List<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("get_query_param_list", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_query_param_list"));
		Assertions.assertEquals("get_query_param_list: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_query_param_set() {
		Set<String> param = new TreeSet<>(List.of("a", "b", "c"));
		String responseBody = WebClientTest.<String>callForMono("get_query_param_set", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_query_param_set"));
		Assertions.assertEquals("get_query_param_set: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_query_param_array() {
		String[] param = new String[]{"a", "b", "c"};
		String responseBody = WebClientTest.<String>callForMono("get_query_param_array", (Object)param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_query_param_array"));
		Assertions.assertEquals("get_query_param_array: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_path_param() {
		String param = "param";
		String responseBody = WebClientTest.<String>callForMono("get_path_param", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_path_param"));
		Assertions.assertEquals("get_path_param: " + param, responseBody);
	}

	@Test
	public void test_get_path_param_collection() {
		Collection<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("get_path_param_collection", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_path_param_collection"));
		Assertions.assertEquals("get_path_param_collection: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_path_param_list() {
		List<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("get_path_param_list", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_path_param_list"));
		Assertions.assertEquals("get_path_param_list: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_path_param_set() {
		Set<String> param = new TreeSet<>(List.of("a", "b", "c"));
		String responseBody = WebClientTest.<String>callForMono("get_path_param_set", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_path_param_set"));
		Assertions.assertEquals("get_path_param_set: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_get_path_param_array() {
		String[] param = new String[]{"a", "b", "c"};
		String responseBody = WebClientTest.<String>callForMono("get_path_param_array", (Object)param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_path_param_array"));
		Assertions.assertEquals("get_path_param_array: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_post_form_param() {
		String param = "param";
		String responseBody = WebClientTest.<String>callForMono("post_form_param", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_form_param"));
		Assertions.assertEquals("post_form_param: " + param, responseBody);
	}

	@Test
	public void test_post_form_param_collection() {
		Collection<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("post_form_param_collection", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_form_param_collection"));
		Assertions.assertEquals("post_form_param_collection: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_post_form_param_list() {
		List<String> param = List.of("a", "b", "c");
		String responseBody = WebClientTest.<String>callForMono("post_form_param_list", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_form_param_list"));
		Assertions.assertEquals("post_form_param_list: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_post_form_param_set() {
		Set<String> param = new TreeSet<>(List.of("a", "b", "c"));
		String responseBody = WebClientTest.<String>callForMono("post_form_param_set", param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_form_param_set"));
		Assertions.assertEquals("post_form_param_set: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_post_form_param_array() {
		String[] param = new String[]{"a", "b", "c"};
		String responseBody = WebClientTest.<String>callForMono("post_form_param_array", (Object)param).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_form_param_array"));
		Assertions.assertEquals("post_form_param_array: " + String.join(", ", param), responseBody);
	}

	@Test
	public void test_post_raw() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_raw", Unpooled.copiedBuffer(message, Charsets.DEFAULT)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_raw"));
		Assertions.assertEquals("post_raw: " + message, responseBody);
	}

	@Test
	public void test_post_string() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_string", message).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_string"));
		Assertions.assertEquals("post_string: " + message, responseBody);
	}

	@Test
	public void test_post_encoded() {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_encoded", createMessage(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_encoded"));
		Assertions.assertEquals("post_encoded: " + message, responseBody);
	}

	@Test
	public void test_post_encoded_collection() {
		String message1 = "message1";
		String message2 = "message2";
		String responseBody = WebClientTest.<String>callForMono("post_encoded_collection", Stream.of(message1, message2).map(WebClientTest::createMessage).collect(Collectors.toList())).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_encoded_collection"));
		Assertions.assertEquals("post_encoded_collection: " + message1 + ", " + message2, responseBody);
	}

	@Test
	public void test_post_encoded_list() {
		String message1 = "message1";
		String message2 = "message2";
		String responseBody = WebClientTest.<String>callForMono("post_encoded_list", Stream.of(message1, message2).map(WebClientTest::createMessage).collect(Collectors.toList())).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_encoded_list"));
		Assertions.assertEquals("post_encoded_list: " + message1 + ", " + message2, responseBody);
	}

	@Test
	public void test_post_encoded_set() {
		String message1 = "message1";
		String message2 = "message2";
		String responseBody = WebClientTest.<String>callForMono("post_encoded_set", Stream.of(message1, message2).map(WebClientTest::createMessage).collect(Collectors.toCollection(TreeSet::new))).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_encoded_set"));
		Assertions.assertEquals("post_encoded_set: " + message1 + ", " + message2, responseBody);
	}

	@Test
	public void test_post_encoded_array() {
		String message1 = "message1";
		String message2 = "message2";
		String responseBody = WebClientTest.<String>callForMono("post_encoded_array", createMessageArray(message1, message2)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_encoded_array"));
		Assertions.assertEquals("post_encoded_array: " + message1 + ", " + message2, responseBody);
	}

	@Test
	public void test_post_multipart() throws IOException {
		String part = "part";
		FileResource resource = new FileResource("src/test/resources/post_resource_small.txt");
		String resourceContent = Files.readString(Path.of(resource.getURI()), Charsets.DEFAULT);
		String message = "message";

		String responseBody = WebClientTest.<String>callForMono("post_multipart", part, resource, createMessage(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_multipart"));
		Assertions.assertEquals("post_multipart: [part, null, null]=" + part + ", [resource, resourceFile, text/plain]=" + resourceContent + ", [message, null, application/json]={\"message\":\"" + message + "\"}", responseBody);
	}

	@Test
	public void test_post_multipart_flux_encoded() throws IOException {
		String message1 = "message1";
		String message2 = "message2";
		String responseBody = WebClientTest.<String>callForMono("post_multipart_flux_encoded", Flux.just(message1, message2).map(WebClientTest::createMessage)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_multipart_flux_encoded"));
		Assertions.assertEquals("post_multipart_flux_encoded: [messages, null, application/json]=[{\"message\":\"" + message1 + "\"},{\"message\":\"" + message2 + "\"}]", responseBody);
	}

	@Test
	public void test_post_multipart_mono_encoded() throws IOException {
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_multipart_mono_encoded", Mono.just(message).map(WebClientTest::createMessage)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_multipart_flux_encoded"));
		Assertions.assertEquals("post_multipart_mono_encoded: [message, null, application/json]={\"message\":\"" + message + "\"}", responseBody);
	}

	@Test
	public void test_post_resource() throws IOException {
		FileResource resource = new FileResource("src/test/resources/post_resource_small.txt");
		String resourceContent = Files.readString(Path.of(resource.getURI()), Charsets.DEFAULT);
		String responseBody = WebClientTest.<String>callForMono("post_resource", resource).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_resource"));
		Assertions.assertEquals("post_resource: " + resourceContent, responseBody);
	}

	@Test
	public void test_get_not_found() {
		Assertions.assertThrows(NotFoundException.class, () -> WebClientTest.<Void>callForMono("get_not_found").block());
		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_not_found"));
	}

	@Test
	public void test_get_internal_server_error() {
		Assertions.assertThrows(InternalServerErrorException.class, () -> WebClientTest.<Void>callForMono("get_internal_server_error").block());
		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("get_internal_server_error"));
	}

	@Test
	public void test_post_mixed_parameters_body() {
		String pathparam = "path_param";
		String queryparam = "query_param";
		String headerparam = "header_param";
		String cookieparam = "cookie_param";
		String message = "message";
		String responseBody = WebClientTest.<String>callForMono("post_mixed_parameters_body", pathparam, queryparam, headerparam, cookieparam, createMessage(message)).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_mixed_parameters_body"));
		Assertions.assertEquals("post_mixed_parameters_body: " + pathparam + ", " + queryparam + ", " + headerparam + ", " + cookieparam + ", " + message, responseBody);
	}

	@Test
	public void test_post_mixed_parameters_form() {
		String pathparam = "path_param";
		String queryparam = "query_param";
		String headerparam = "header_param";
		String cookieparam = "cookie_param";
		String formparam = "form_param";
		String responseBody = WebClientTest.<String>callForMono("post_mixed_parameters_form", pathparam, queryparam, headerparam, cookieparam, formparam).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_mixed_parameters_form"));
		Assertions.assertEquals("post_mixed_parameters_form: " + pathparam + ", " + queryparam + ", " + headerparam + ", " + cookieparam + ", " + formparam, responseBody);
	}

	@Test
	public void test_post_mixed_parameters_multipart() {
		String pathparam = "path_param";
		String queryparam = "query_param";
		String headerparam = "header_param";
		String cookieparam = "cookie_param";
		String partparam = "part_param";
		String responseBody = WebClientTest.<String>callForMono("post_mixed_parameters_multipart", pathparam, queryparam, headerparam, cookieparam, partparam).block();

		Assertions.assertTrue(WebClientTest.<Boolean>getServerInvocationField("post_mixed_parameters_multipart"));
		Assertions.assertEquals("post_mixed_parameters_multipart: " + pathparam + ", " + queryparam + ", " + headerparam + ", " + cookieparam + ", " + partparam, responseBody);
	}
}
