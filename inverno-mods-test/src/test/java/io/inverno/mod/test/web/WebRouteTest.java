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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.ModsTestUtils;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import io.inverno.test.InvernoTestCompiler;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class WebRouteTest extends AbstractInvernoModTest {
	
	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
//		System.setProperty("io.netty.leakDetection.level", "PARANOID");
//		System.setProperty("io.netty.leakDetection.targetRecords", "20");
	}
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final String MODULE_WEBROUTE = "io.inverno.mod.test.web.webroute";
	
	private static int testServerPort;
	private static InvernoModuleProxy testServerModuleProxy;
	
	private static URI baseURI;
	private static HttpClient httpClient;
	
	@BeforeAll
	public static void init() throws IOException, InvernoCompilationException, ClassNotFoundException, InterruptedException {
		InvernoTestCompiler invernoCompiler = InvernoTestCompiler.builder()
			.moduleOverride(AbstractInvernoModTest.MODULE_OVERRIDE)
			.annotationProcessorModuleOverride(AbstractInvernoModTest.ANNOTATION_PROCESSOR_MODULE_OVERRIDE)
			.build();

		// TODO this is causing stackoverflow error in compiler, an alternate approach might be to precompile the module before test and start it with different configurations in order to avoid
		//  multiple compilations. The drawback is that the test is no longer isolated and becomes dependent on that preprocessing phase.
//		invernoCompiler.cleanModuleTarget();
		
		InvernoModuleLoader moduleLoader = invernoCompiler.compile(MODULE_WEBROUTE);
		
		testServerPort = ModsTestUtils.getFreePort();
		
		Class<?> httpConfigClass = moduleLoader.loadClass(MODULE_WEBROUTE, "io.inverno.mod.http.server.HttpServerConfiguration");
		ConfigurationInvocationHandler httpConfigHandler = new ConfigurationInvocationHandler(httpConfigClass, Map.of("server_port", testServerPort, "h2_enabled", true));
		Object httpConfig = Proxy.newProxyInstance(httpConfigClass.getClassLoader(),
			new Class<?>[] { httpConfigClass },
			httpConfigHandler);
		
		Class<?> webConfigClass = moduleLoader.loadClass(MODULE_WEBROUTE, "io.inverno.mod.web.server.WebServerConfiguration");
		ConfigurationInvocationHandler webConfigHandler = new ConfigurationInvocationHandler(webConfigClass, Map.of("http_server", httpConfig));
		Object webConfig = Proxy.newProxyInstance(webConfigClass.getClassLoader(),
			new Class<?>[] { webConfigClass },
			webConfigHandler);
		
		Class<?> webRouteConfigClass = moduleLoader.loadClass(MODULE_WEBROUTE, "io.inverno.mod.test.web.webroute.WebRouteConfiguration");
		ConfigurationInvocationHandler webRouteConfigHandler = new ConfigurationInvocationHandler(webRouteConfigClass, Map.of("web", webConfig));
		Object webRouteConfig = Proxy.newProxyInstance(webRouteConfigClass.getClassLoader(),
			new Class<?>[] { webRouteConfigClass },
			webRouteConfigHandler);
		
		testServerModuleProxy = moduleLoader.load(MODULE_WEBROUTE).optionalDependency("webRouteConfiguration", webRouteConfigClass, webRouteConfig).build();
		testServerModuleProxy.start();
		
		baseURI = URI.create("http://127.0.0.1:" + testServerPort);
		httpClient = HttpClient.newHttpClient();
	}
	
	@AfterAll
	public static void destroy() {
		if(testServerModuleProxy != null) {
			testServerModuleProxy.stop();
		}
	}

	public static Stream<Arguments> provideHttpVersion() {
		return Stream.of(
			Arguments.of(HttpClient.Version.HTTP_1_1),
			Arguments.of(HttpClient.Version.HTTP_2)
		);
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_void(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_void'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_void"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertTrue(response.headers().firstValue("content-type").isEmpty());
		Assertions.assertEquals(0, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_raw'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_raw"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertTrue(response.headers().firstValue("content-type").isEmpty());
		Assertions.assertEquals(7, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_raw", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_raw_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_raw/pub'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_raw/pub"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertTrue(response.headers().firstValue("content-type").isEmpty());
		Assertions.assertEquals(11, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_raw_pub", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_raw_mono(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_raw/mono'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_raw/mono"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertTrue(response.headers().firstValue("content-type").isEmpty());
		Assertions.assertEquals(12, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_raw_mono", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_raw_flux(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_raw/flux
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_raw/flux"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertTrue(response.headers().firstValue("content-type").isEmpty());
		Assertions.assertTrue(response.headers().firstValue("content-length").isEmpty());
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals("get_raw_flux", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(11, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_no_produce(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/no_produce'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/no_produce"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertTrue(response.headers().firstValue("content-type").isEmpty());
		Assertions.assertEquals(22, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_no_produce", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_no_encoder(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/no_encoder'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/no_encoder"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(500, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_collection(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(22, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get,encoded,collection", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_list(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(16, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get,encoded,list", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_set(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(15, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals(Set.of("get","encoded","set"), new HashSet<>(Arrays.asList(response.body().split(","))));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_array(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(17, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get,encoded,array", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pub'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pub"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertTrue(response.headers().firstValue("content-length").isEmpty());
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals("get_encoded_pub", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_mono(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/mono'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/mono"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(16, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_mono", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_flux(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/flux'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/flux"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertTrue(response.headers().firstValue("content-length").isEmpty());
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals("get_encoded_flux", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(27, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc&queryParam=def'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam?queryParam=abc&queryParam=def"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(27, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/opt?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(31, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc&queryParam=def'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/opt?queryParam=abc&queryParam=def"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(31, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_opt: empty", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_collection(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/collection?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(38, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_collection: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_collection_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc&queryParam=def,hij'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/collection?queryParam=abc&queryParam=def,hij"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(48, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_collection: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_collection_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_collection_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/collection/opt?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(42, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_collection_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_collection_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc&queryParam=def,hij'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/collection/opt?queryParam=abc&queryParam=def,hij"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(52, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_collection_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_collection_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/collection/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(39, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_collection_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_list(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/list?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_list: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_list_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc&queryParam=def,hij'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/list?queryParam=abc&queryParam=def,hij"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(42, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_list: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_list_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_list_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/list/opt?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(36, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_list_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_list_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc&queryParam=def,hij'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/list/opt?queryParam=abc&queryParam=def,hij"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(46, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_list_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_list_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/list/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_list_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_set(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/set?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(31, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_set: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_set_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc&queryParam=def,hij'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/set?queryParam=abc&queryParam=def,hij"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(41, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_queryParam_set", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_set_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_set_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/set/opt?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(35, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_set_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_set_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc&queryParam=def,hij'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/set/opt?queryParam=abc&queryParam=def,hij"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(45, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_queryParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_set_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/set/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_set_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_array(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/array?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_array: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_array_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc&queryParam=def,hij'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/array?queryParam=abc&queryParam=def,hij"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_array: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_array_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_array_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/array/opt?queryParam=abc"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(37, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_array_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_array_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc&queryParam=def,hij'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/array/opt?queryParam=abc&queryParam=def,hij"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(47, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_array_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_queryParam_array_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/array/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_queryParam_array_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(28, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(28, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(28, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_opt_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_opt: empty", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_collection(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(39, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_collection: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_collection_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(49, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_collection: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_collection_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(49, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_collection: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_collection_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_collection_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_collection_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(53, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_collection_opt_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(53, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_collection_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(40, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_collection_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_list(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_list: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_list_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_list: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_list_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_list: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_list_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_list_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(37, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_list_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(47, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_list_opt_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(47, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_list_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_list_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_set(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_set: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_set_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(42, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_cookieParam_set", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_set_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(42, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_cookieParam_set", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_set_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_set_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(36, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_set_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_set_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(46, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_cookieParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_set_opt_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(46, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_cookieParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_set_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_set_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_array(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_array: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_array_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(44, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_array: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_array_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(44, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_array: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_array_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_array_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(38, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_array_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc; cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(48, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_array_opt_multi_header(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array/opt"))
					.version(version)
					.GET()
					.header("cookie", "cookieParam=abc")
					.header("cookie", "cookieParam=def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(48, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_cookieParam_array_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(35, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_cookieParam_array_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(28, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(28, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_opt: empty", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_collection(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(39, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_collection: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_collection_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection"))
					.version(version)
					.GET()
					.header("headerParam", "abc,def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(49, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_collection: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_collection_multi_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(49, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_collection: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_collection_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_collection_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_collection_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc,def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(53, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_collection_opt_multi_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(53, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_collection_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(40, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_collection_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_list(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_list: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_list_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list"))
					.version(version)
					.GET()
					.header("headerParam", "abc,def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_list: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_list_multi_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_list: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_list_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_list_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(37, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_list_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_list_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc,def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(47, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_list_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_list_opt_multi_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(47, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_list_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_list_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_list_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_set(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_set: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_set_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set"))
					.version(version)
					.GET()
					.header("headerParam", "abc,def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(42, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_headerParam_set", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_set_multi_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(42, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_headerParam_set", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_set_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_set_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(36, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_set_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_set_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc,def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(46, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_headerParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_set_opt_multi_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(46, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_headerParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_set_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_set_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_array(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_array: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_array_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array"))
					.version(version)
					.GET()
					.header("headerParam", "abc,def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(44, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_array: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_array_multi_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(44, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_array: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_array_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_array_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(38, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_array_opt: abc", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_array_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc,def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(48, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_array_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_array_opt_multi_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array/opt"))
					.version(version)
					.GET()
					.header("headerParam", "abc")
					.header("headerParam", "def,hij")
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(48, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_array_opt: abc, def, hij", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_headerParam_array_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(35, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_headerParam_array_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(28, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_opt: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_opt: empty", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_collection(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(41, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_collection: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_collection_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_collection_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/collection/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(45, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_collection_opt: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_collection_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//collection/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(38, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_collection_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_list(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(35, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_list: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_list_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_list_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/list/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(39, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_list_opt: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_list_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//list/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_list_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_set(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_pathParam_set", splitBody[0]);
		Assertions.assertEquals(Set.of("a", "b", "c"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_set_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_set_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/set/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(38, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		String[] splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_pathParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("a", "b", "c"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_set_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//set/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(31, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_set_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_array(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(36, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_array: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_array_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_array_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/a,b,c/array/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(40, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_array_opt: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_pathParam_array_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//array/opt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("get_encoded_pathParam_array_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_json(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/dto'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/json/dto"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("application/json", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(27, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("{\"message\":\"Hello, world!\"}", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_json_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/dto'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/json/pub/dto"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("application/json", response.headers().firstValue("content-type").orElse(null));
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals("[{\"message\":\"Hello, world!\"},{\"message\":\"Salut, monde!\"},{\"message\":\"Hallo, welt!\"}]", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_json_generic(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/dto/generic'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/json/dto/generic"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("application/json", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(51, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("{\"@type\":\"string\",\"id\":1,\"message\":\"Hello, world!\"}", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_json_generic_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/dto/generic'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/json/pub/dto/generic"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("application/json", response.headers().firstValue("content-type").orElse(null));
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals("[{\"@type\":\"string\",\"id\":1,\"message\":\"Hello, world!\"},{\"@type\":\"integer\",\"id\":2,\"message\":123456}]", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_json_map(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/map'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/json/map"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("application/json", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(13, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals(Map.of("a", 1, "b", 2), MAPPER.readerFor(new TypeReference<Map<String, Integer>>() {}).readValue(response.body()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_encoded_json_map_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/map'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/json/pub/map"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("application/json", response.headers().firstValue("content-type").orElse(null));
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals(List.of(Map.of("a", 1, "b", 2), Map.of("c", 3, "d", 4)), MAPPER.readerFor(new TypeReference<List<Map<String, Integer>>>() {}).readValue(response.body()));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(21, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(21, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(25, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_opt: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(25, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_opt: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(25, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_opt: empty", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_collection(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/collection"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_collection: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_collection_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/collection"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_collection: a, b, c, d, e, f", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_collection_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/collection"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_collection_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/collection/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(38, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_collection_opt: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_collection_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/collection/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(47, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_collection_opt: a, b, c, d, e, f", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_collection_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/collection/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(31, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_collection_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_list(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/list"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(28, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_list: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_list_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/list"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(37, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_list: a, b, c, d, e, f", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_list_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/list"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_list_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/list/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_list_opt: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_list_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/list/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(41, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_list_opt: a, b, c, d, e, f", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_list_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/list/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(25, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_list_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_set(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/set"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(27, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_set: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_set_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/set"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(36, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_set: a, b, c, d, e, f", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_set_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/set"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_set_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/set/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(31, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_set_opt: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_set_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/set/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(40, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_set_opt: a, b, c, d, e, f", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_set_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/set/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_set_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_array(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/array"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(29, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_array: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_array_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/array"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(38, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_array: a, b, c, d, e, f", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_array_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/array"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_array_opt(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/array/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_array_opt: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_array_opt_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/array/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("formParam=a,b,c&formParam=d,e,f"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(42, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_array_opt: a, b, c, d, e, f", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_array_opt_missing(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/array/opt"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(26, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_formParam_array_opt: ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_mono(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a=1&b=2&c=3&c=4' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:39901/post/formParam/mono'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/mono"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("a=1&b=2&c=3&c=4"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals("post_formParam_mono: a=1", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_formParam_flux(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a=1&b=2&c=3&c=4' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:39901/post/formParam/flux'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/flux"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString("a=1&b=2&c=3&c=4"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals("post_formParam_flux: a=1, b=2, c=3, c=4, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_raw"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(15, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_raw: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_raw_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_raw'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_raw_raw"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(19, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_raw_raw: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_raw_pub_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_pub_raw'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_raw_pub_raw"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals("post_raw_pub_raw: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_raw_mono_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_mono_raw'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_raw_mono_raw"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_raw_mono_raw: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_raw_flux_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_flux_raw'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_raw_flux_raw"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		switch(version) {
			case HTTP_1_1: Assertions.assertEquals("chunked", response.headers().firstValue("transfer-encoding").orElse(null));
				break;
			case HTTP_2: Assertions.assertTrue(response.headers().firstValue("transfer-encoding").isEmpty());
				break;
		}
		Assertions.assertEquals("post_raw_flux_raw: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_raw_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/pub'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_raw/pub"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(19, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_raw_pub: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_raw_mono(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/mono'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_raw/mono"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(20, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_raw_mono: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_raw_flux(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/flux'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_raw/flux"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(20, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_raw_flux: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(19, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_no_consume(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: ' -X POST 'http://127.0.0.1:8080/post_encoded/no_consume'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/no_consume"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(30, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_no_consume: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_no_decoder(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/no_decoder'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/no_decoder"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(500, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_collection(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/collection'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/collection"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_collection: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_list(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/list'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/list"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(26, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_list: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_set(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/set'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/set"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(25, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_set: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_array(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/array'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/array"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(27, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_array: a, b, c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/pub'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/pub"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(23, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_pub: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_mono(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/mono'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/mono"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_mono: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_flux(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/flux'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/flux"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_flux: a,b,c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_json(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d '{"message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/json/dto"))
					.version(version)
					.header("content-type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"Hello, world!\"}"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(36, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_json_dto: Hello, world!", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_json_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d '{"message":"Hello, world!"}{"message":"Hallo, welt!"}{"message":"Salut, monde!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/json/pub/dto"))
					.version(version)
					.header("content-type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"Hello, world!\"}{\"message\":\"Hallo, welt!\"}{\"message\":\"Salut, monde!\"}"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(71, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_json_pub_dto: Hello, world!, Hallo, welt!, Salut, monde!, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_json_generic(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d '{"@type":"string", "message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto/generic'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/json/dto/generic"))
					.version(version)
					.header("content-type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString("{\"@type\":\"string\", \"message\":\"Hello, world!\"}"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(44, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_json_dto_generic: Hello, world!", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_json_generic_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d '{"@type":"string","message":"Hello, world!"}{"@type":"integer","message":123456}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto/generic'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/json/pub/dto/generic"))
					.version(version)
					.header("content-type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString("{\"@type\":\"string\",\"message\":\"Hello, world!\"}{\"@type\":\"integer\",\"message\":123456}"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(58, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_json_pub_dto_generic: Hello, world!, 123456, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_json_map(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d '{"a":1, "b":2, "c":3}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/map'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/json/map"))
					.version(version)
					.header("content-type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString("{\"a\":1, \"b\":2, \"c\":3}"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(38, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_json_map: {a=1, b=2, c=3}", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_encoded_json_map_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i -d '{"a":1, "b":2, "c":3}{"d":4, "e":5, "f":6}{"g":7, "h":8, "i":9}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/map'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/json/pub/map"))
					.version(version)
					.header("content-type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString("{\"a\":1, \"b\":2, \"c\":3}{\"d\":4, \"e\":5, \"f\":6}{\"g\":7, \"h\":8, \"i\":9}"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(78, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_json_pub_map: {a=1, b=2, c=3}, {d=4, e=5, f=6}, {g=7, h=8, i=9}, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_pub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_pub"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(29, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_pub: a, b, c, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_pub_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/raw'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_pub/raw"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals("post_multipart_pub_raw: a = 1, b = 2, c = 3, ", response.body());		
		Assertions.assertEquals(45, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_pub_encoded(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/encoded'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_pub/encoded"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(49, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_pub_encoded: a = 1, b = 2, c = 3, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_mono(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
		String multipartBody = "--------------------------2e2d7d4a9a26041b\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e2d7d4a9a26041b--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_mono"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e2d7d4a9a26041b")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(22, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_mono: a", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_mono_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_mono"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(22, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_mono: a", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_mono_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
		String multipartBody = "--------------------------2e2d7d4a9a26041b\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e2d7d4a9a26041b--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_mono/raw"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e2d7d4a9a26041b")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_mono_raw: a = 1, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_mono_raw_multi(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_mono/raw"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_mono_raw: a = 1, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_mono_encoded(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/encoded'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_mono/encoded"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(34, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_mono_encoded: a = 1", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_flux(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_flux"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(30, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_flux: a, b, c, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_flux_raw(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/raw'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_flux/raw"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(45, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_pub_raw: a = 1, b = 2, c = 3, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_post_multipart_flux_encoded(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/encoded'
		String multipartBody = "--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"b\"\n" +
			"\n" +
			"2\n" +
			"--------------------------2e80132a7cbf6596\n" +
			"Content-Disposition: form-data; name=\"c\"\n" +
			"\n" +
			"3\n" +
			"--------------------------2e80132a7cbf6596--";

		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_multipart_flux/encoded"))
					.version(version)
					.header("content-type", "multipart/form-data; boundary=------------------------2e80132a7cbf6596")
					.POST(HttpRequest.BodyPublishers.ofString(multipartBody))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(50, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_flux_encoded: a = 1, b = 2, c = 3, ", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_sse_raw(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i 'http://127.0.0.1:8080/get_sse_raw'
		byte[] get_sse_raw_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_raw_http11.dat"));
		byte[] get_sse_raw_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_raw_http2.dat"));
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(baseURI.resolve("/get_sse_raw"))
				.version(version)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/event-stream;charset=UTF-8", response.headers().firstValue("content-type").orElse(null));
//		Files.write(Path.of("src/test/resources/get_sse_raw.dat"), response.body().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		switch(version) {
			case HTTP_1_1: Assertions.assertArrayEquals(get_sse_raw_http11, response.body().getBytes(StandardCharsets.UTF_8));
				break;
			case HTTP_2: Assertions.assertArrayEquals(get_sse_raw_http2, response.body().getBytes(StandardCharsets.UTF_8));
				break;
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_sse_encoded(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded'
		byte[] get_sse_encoded_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_http11.dat"));
		byte[] get_sse_encoded_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_http2.dat"));
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(baseURI.resolve("/get_sse_encoded"))
				.version(version)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/event-stream;charset=UTF-8", response.headers().firstValue("content-type").orElse(null));
//		Files.write(Path.of("src/test/resources/get_sse_encoded.dat"), response.body().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		switch(version) {
			case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_http11, response.body().getBytes(StandardCharsets.UTF_8));
				break;
			case HTTP_2: Assertions.assertArrayEquals(get_sse_encoded_http2, response.body().getBytes(StandardCharsets.UTF_8));
				break;
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_sse_encoded_json(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded/json'
		byte[] get_sse_encoded_json_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_http11.dat"));
		byte[] get_sse_encoded_json_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_http2.dat"));
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(baseURI.resolve("/get_sse_encoded/json"))
				.version(version)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/event-stream;charset=UTF-8", response.headers().firstValue("content-type").orElse(null));
//		Files.write(Path.of("src/test/resources/get_sse_encoded_json.dat"), response.body().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		switch(version) {
			case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_json_http11, response.body().getBytes(StandardCharsets.UTF_8));
				break;
			case HTTP_2: Assertions.assertArrayEquals(get_sse_encoded_json_http2, response.body().getBytes(StandardCharsets.UTF_8));
				break;
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_sse_encoded_json_map(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded/json/map'
		byte[] get_sse_encoded_json_map_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_map_http11.dat"));
		byte[] get_sse_encoded_json_map_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_map_http2.dat"));
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
				.uri(baseURI.resolve("/get_sse_encoded/json/map"))
				.version(version)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/event-stream;charset=UTF-8", response.headers().firstValue("content-type").orElse(null));
//		Files.write(Path.of("src/test/resources/get_sse_encoded_json_map.dat"), response.body().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		switch(version) {
			case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_json_map_http11, response.body().getBytes(StandardCharsets.UTF_8));
				break;
			case HTTP_2: Assertions.assertArrayEquals(get_sse_encoded_json_map_http2, response.body().getBytes(StandardCharsets.UTF_8));
				break;
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_resource'
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_resource"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("This is a test resource.", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_small(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/get_resource_small.txt
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/get_resource_small.txt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("This is a test resource.", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_pc_encoded_space(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/some%20space.txt
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/some%20space.txt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(18, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("Space in file name", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_double_pc_encoded_space(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/some%2520space.txt
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/some%2520space.txt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_dir(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/dir/get_resource.txt
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/dir/get_resource.txt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("This is a test resource.", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_dir_percent_encoded_slash(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/dir%2Fget_resource.txt
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/dir%2Fget_resource.txt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("This is a test resource.", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_dir_double_percent_encoded_slash(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/dir%252Fget_resource.txt
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/dir%252Fget_resource.txt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_path_traversal_parent(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/../pom.xml
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/../pom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_path_traversal_parent_percent_encoded_slash(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/%2E%2E%2Fpom.xml
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/%2E%2E%2Fpom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_path_traversal_parent_double_percent_encoded_slash(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/%252E%252E%252Fpom.xml
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/%252E%252E%252Fpom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_path_traversal_absolute(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static//pom.xml
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static//pom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_path_traversal_absolute_percent_encoded_slash(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/%2Fpom.xml
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/%2Fpom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_resource_path_traversal_absolute_double_percent_encoded_slash(HttpClient.Version version) throws IOException, InterruptedException {
		// curl -i http://127.0.0.1:8080/static/%2Fpom.xml
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/%252Fpom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_qmark(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/qmark_1_
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/qmark_1_"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/qmark_1_", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_qmark_none(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/qmark_12_
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/qmark__"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_qmark_many(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/qmark_12_
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/qmark_12_"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_wcard(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/wcard_1_
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/wcard_1_"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(24, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/wcard_1_", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_wcard_none(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/wcard__
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/wcard__"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(23, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/wcard__", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_wcard_many(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/wcard_123456789_
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/wcard_123456789_"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(32, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/wcard_123456789_", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_directories(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/directories
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/directories"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(27, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/directories", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_directories_trailingSlash(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/directories/
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/directories/"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(28, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/directories/", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_directories_sub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/directories/a"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(29, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/directories/a", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_directories_sub_sub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a/b/
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/directories/a/b"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(31, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/directories/a/b", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_directories_sub_sub_sub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a/b/c
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/directories/a/b/c"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(33, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/directories/a/b/c", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_regex_unmatching(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/jsp/"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_regex_matching(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/test.jsp
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/jsp/test.jsp"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(39, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/jsp/test.jsp - test.jsp", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_regex_matching_sub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/a/test.jsp
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/jsp/a/test.jsp"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(41, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/jsp/a/test.jsp - test.jsp", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_regex_matching_sub_sub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/a/b/test.jsp
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/jsp/a/b/test.jsp"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(43, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/jsp/a/b/test.jsp - test.jsp", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_terminal_unmatching(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/terminal"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_terminal_matching_trailingSlash(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/terminal/"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(25, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/terminal/", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_terminal_matching_sub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/terminal/a"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(26, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/terminal/a", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_terminal_matching_sub_sub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a/b/
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/terminal/a/b/"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(29, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/terminal/a/b/", response.body());
	}
	
	@ParameterizedTest
	@MethodSource("provideHttpVersion")
	public void test_get_pathParam_terminal_matching_sub_sub_sub(HttpClient.Version version) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a/b/c
		HttpResponse<String> response = httpClient.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/terminal/a/b/c"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/plain", response.headers().firstValue("content-type").orElse(null));
		Assertions.assertEquals(30, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("/get_path_param/terminal/a/b/c", response.body());
	}
}
