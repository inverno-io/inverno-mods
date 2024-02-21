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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
	
	public static int getFreePort() {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			return serverSocket.getLocalPort();
		} 
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Test
	public void testWebRouteController() throws IOException, InvernoCompilationException, ClassNotFoundException, InterruptedException {
		this.clearModuleTarget();
		
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULE_WEBROUTE);
		
		int port = getFreePort();
		
		Class<?> httpConfigClass = moduleLoader.loadClass(MODULE_WEBROUTE, "io.inverno.mod.http.server.HttpServerConfiguration");
		ConfigurationInvocationHandler httpConfigHandler = new ConfigurationInvocationHandler(httpConfigClass, Map.of("server_port", port, "h2c_enabled", true));
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
		
		InvernoModuleProxy module = moduleLoader.load(MODULE_WEBROUTE).optionalDependency("webRouteConfiguration", webRouteConfigClass, webRouteConfig).build();
		module.start();
		try {
			final URI baseURI = URI.create("http://127.0.0.1:" + port);
			
			this.test_get(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_query_param(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_cookie_param(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_header_param(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_path_param(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_get_encoded(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_form_param(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_post(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_post_multipart(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_sse(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_resource(baseURI, HttpClient.Version.HTTP_1_1);
			this.test_misc(baseURI, HttpClient.Version.HTTP_1_1);
			
			this.test_get(baseURI, HttpClient.Version.HTTP_2);
			this.test_query_param(baseURI, HttpClient.Version.HTTP_2);
			this.test_cookie_param(baseURI, HttpClient.Version.HTTP_2);
			this.test_header_param(baseURI, HttpClient.Version.HTTP_2);
			this.test_path_param(baseURI, HttpClient.Version.HTTP_2);
			this.test_get_encoded(baseURI, HttpClient.Version.HTTP_2);
			this.test_form_param(baseURI, HttpClient.Version.HTTP_2);
			this.test_post(baseURI, HttpClient.Version.HTTP_2);
			this.test_post_multipart(baseURI, HttpClient.Version.HTTP_2);
			this.test_sse(baseURI, HttpClient.Version.HTTP_2);
			this.test_resource(baseURI, HttpClient.Version.HTTP_2);
			this.test_misc(baseURI, HttpClient.Version.HTTP_2);
		}
		finally {
			module.stop();
		}
	}
	
	private void test_get(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;

		//curl -i 'http://127.0.0.1:8080/get_void'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_raw'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_raw/pub'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_raw/mono'
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_raw/flux
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/no_produce'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/no_encoder'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/no_encoder"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(500, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/collection'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/list'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/set'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/array'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pub'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/mono'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/flux'
		response = client.send(
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
	
	private void test_query_param(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		// curl -i 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc&queryParam=def'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc&queryParam=def'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc&queryParam=def,hij'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc&queryParam=def,hij'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc&queryParam=def,hij'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc&queryParam=def,hij'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc&queryParam=def,hij'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc&queryParam=def,hij'
		response = client.send(
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
		splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_queryParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc&queryParam=def,hij'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/queryParam/array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc&queryParam=def,hij'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt'
		response = client.send(
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
	
	private void test_cookie_param(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		response = client.send(
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
		splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_cookieParam_set", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		response = client.send(
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
		splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_cookieParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		response = client.send(
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
		splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_cookieParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/cookieParam/array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		response = client.send(
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

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		response = client.send(
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
	
	private void test_header_param(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		response = client.send(
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

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		response = client.send(
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

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		response = client.send(
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

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		response = client.send(
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
		splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_headerParam_set", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		response = client.send(
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
		splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_headerParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		response = client.send(
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
		splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_headerParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		response = client.send(
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

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/headerParam/array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		response = client.send(
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

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		response = client.send(
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
	
	public void test_path_param(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam/"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//collection'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//collection"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//collection/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//list'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//list"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//list/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//set'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//set"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set/opt'
		response = client.send(
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
		splitBody = response.body().split(":");
		Assertions.assertEquals("get_encoded_pathParam_set_opt", splitBody[0]);
		Assertions.assertEquals(Set.of("a", "b", "c"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//set/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//array'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_encoded/pathParam//array"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array/opt'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//array/opt'
		response = client.send(
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
	
	public void test_get_encoded(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/dto'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/dto'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/json/dto/generic'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/dto/generic'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/json/map'
		response = client.send(
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

		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/map'
		response = client.send(
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
	
	public void test_form_param(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/collection"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/list"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/set"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post/formParam/array"))
					.version(version)
					.header("content-type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.noBody())
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		response = client.send(
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

		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		response = client.send(
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

		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		response = client.send(
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

		//curl -i -d 'a=1&b=2&c=3&c=4' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:39901/post/formParam/flux'
		response = client.send(
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
	
	public void test_post(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_raw'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_pub_raw'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_mono_raw'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_flux_raw'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/pub'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/mono'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/flux'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: ' -X POST 'http://127.0.0.1:8080/post_encoded/no_consume'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/no_decoder'
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/post_encoded/no_decoder"))
					.version(version)
					.header("content-type", "text/plain")
					.POST(HttpRequest.BodyPublishers.ofString("a,b,c"))
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(500, response.statusCode());

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/collection'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/list'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/set'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/array'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/pub'
		response = client.send(
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
		Assertions.assertEquals(25, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_pub: a, b, c", response.body());

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/mono'
		response = client.send(
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

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/flux'
		response = client.send(
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
		Assertions.assertEquals(26, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_encoded_flux: a, b, c", response.body());

		//curl -i -d '{"message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto'
		response = client.send(
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

		//curl -i -d '{"message":"Hello, world!"}{"message":"Hallo, welt!"}{"message":"Salut, monde!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto'
		response = client.send(
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

		//curl -i -d '{"@type":"string", "message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto/generic'
		response = client.send(
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

		//curl -i -d '{"@type":"string","message":"Hello, world!"}{"@type":"integer","message":123456}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto/generic'
		response = client.send(
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

		//curl -i -d '{"a":1, "b":2, "c":3}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/map'
		response = client.send(
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

		//curl -i -d '{"a":1, "b":2, "c":3}{"d":4, "e":5, "f":6}{"g":7, "h":8, "i":9}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/map'
		response = client.send(
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
	
	// TODO it seems there's a deadlock when pipelining is used, probably due to the multipart decoder
	public void test_post_multipart(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
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

		response = client.send(
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

		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/raw'
		multipartBody = "--------------------------2e80132a7cbf6596\n" +
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

		client = HttpClient.newHttpClient();
		response = client.send(
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
		Assertions.assertEquals(45, response.headers().firstValue("content-length").map(Integer::parseInt).orElse(-1));
		Assertions.assertEquals("post_multipart_pub_raw: a = 1, b = 2, c = 3, ", response.body());

		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/encoded'
		multipartBody = "--------------------------2e80132a7cbf6596\n" +
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

		client = HttpClient.newHttpClient();
		response = client.send(
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

		//curl -i --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
		multipartBody = "--------------------------2e2d7d4a9a26041b\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e2d7d4a9a26041b--";

		client = HttpClient.newHttpClient();
		response = client.send(
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

		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
		multipartBody = "--------------------------2e80132a7cbf6596\n" +
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

		client = HttpClient.newHttpClient();
		response = client.send(
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

		//curl -i --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
		multipartBody = "--------------------------2e2d7d4a9a26041b\n" +
			"Content-Disposition: form-data; name=\"a\"\n" +
			"\n" +
			"1\n" +
			"--------------------------2e2d7d4a9a26041b--";

		client = HttpClient.newHttpClient();
		response = client.send(
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

		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
		multipartBody = "--------------------------2e80132a7cbf6596\n" +
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

		client = HttpClient.newHttpClient();
		response = client.send(
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

		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/encoded'
		multipartBody = "--------------------------2e80132a7cbf6596\n" +
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

		client = HttpClient.newHttpClient();
		response = client.send(
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

		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux'
		multipartBody = "--------------------------2e80132a7cbf6596\n" +
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

		client = HttpClient.newHttpClient();
		response = client.send(
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

		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/raw'
		multipartBody = "--------------------------2e80132a7cbf6596\n" +
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

		client = HttpClient.newHttpClient();
		response = client.send(
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

		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/encoded'
		multipartBody = "--------------------------2e80132a7cbf6596\n" +
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

		client = HttpClient.newHttpClient();
		response = client.send(
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
	
	public void test_sse(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		// curl -i 'http://127.0.0.1:8080/get_sse_raw'
		byte[] get_sse_raw_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_raw_http11.dat"));
		byte[] get_sse_raw_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_raw_http2.dat"));
		response = client.send(
			HttpRequest.newBuilder()
				.uri(baseURI.resolve("/get_sse_raw"))
				.version(version)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/event-stream;charset=utf-8", response.headers().firstValue("content-type").orElse(null));
//		Files.write(Path.of("src/test/resources/get_sse_raw.dat"), response.body().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		switch(version) {
			case HTTP_1_1: Assertions.assertArrayEquals(get_sse_raw_http11, response.body().getBytes(StandardCharsets.UTF_8));
				break;
			case HTTP_2: Assertions.assertArrayEquals(get_sse_raw_http2, response.body().getBytes(StandardCharsets.UTF_8));
				break;
		}
		
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded'
		byte[] get_sse_encoded_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_http11.dat"));
		byte[] get_sse_encoded_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_http2.dat"));
		client = HttpClient.newHttpClient();
		response = client.send(
			HttpRequest.newBuilder()
				.uri(baseURI.resolve("/get_sse_encoded"))
				.version(version)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/event-stream;charset=utf-8", response.headers().firstValue("content-type").orElse(null));
//		Files.write(Path.of("src/test/resources/get_sse_encoded.dat"), response.body().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		switch(version) {
			case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_http11, response.body().getBytes(StandardCharsets.UTF_8));
				break;
			case HTTP_2: Assertions.assertArrayEquals(get_sse_encoded_http2, response.body().getBytes(StandardCharsets.UTF_8));
				break;
		}
		
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded/json'
		byte[] get_sse_encoded_json_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_http11.dat"));
		byte[] get_sse_encoded_json_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_http2.dat"));
		client = HttpClient.newHttpClient();
		response = client.send(
			HttpRequest.newBuilder()
				.uri(baseURI.resolve("/get_sse_encoded/json"))
				.version(version)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/event-stream;charset=utf-8", response.headers().firstValue("content-type").orElse(null));
//		Files.write(Path.of("src/test/resources/get_sse_encoded_json.dat"), response.body().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		switch(version) {
			case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_json_http11, response.body().getBytes(StandardCharsets.UTF_8));
				break;
			case HTTP_2: Assertions.assertArrayEquals(get_sse_encoded_json_http2, response.body().getBytes(StandardCharsets.UTF_8));
				break;
		}
		
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded/json/map'
		byte[] get_sse_encoded_json_map_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_map_http11.dat"));
		byte[] get_sse_encoded_json_map_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_map_http2.dat"));
		client = HttpClient.newHttpClient();
		response = client.send(
			HttpRequest.newBuilder()
				.uri(baseURI.resolve("/get_sse_encoded/json/map"))
				.version(version)
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("text/event-stream;charset=utf-8", response.headers().firstValue("content-type").orElse(null));
//		Files.write(Path.of("src/test/resources/get_sse_encoded_json_map.dat"), response.body().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		switch(version) {
			case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_json_map_http11, response.body().getBytes(StandardCharsets.UTF_8));
				break;
			case HTTP_2: Assertions.assertArrayEquals(get_sse_encoded_json_map_http2, response.body().getBytes(StandardCharsets.UTF_8));
				break;
		}
	}
	
	public void test_resource(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		//curl -i 'http://127.0.0.1:8080/get_resource'
		response = client.send(
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
		
		// curl -i http://127.0.0.1:8080/static/get_resource_small.txt
		response = client.send(
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
		
		// curl -i http://127.0.0.1:8080/static/some%20space.txt
		response = client.send(
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
		
		// curl -i http://127.0.0.1:8080/static/some%2520space.txt
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/some%2520space.txt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
		
		// curl -i http://127.0.0.1:8080/static/dir/get_resource.txt
		response = client.send(
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
		
		// curl -i http://127.0.0.1:8080/static/dir%2Fget_resource.txt
		response = client.send(
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
		
		// curl -i http://127.0.0.1:8080/static/dir%252Fget_resource.txt
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/dir%252Fget_resource.txt"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
		
		// curl -i http://127.0.0.1:8080/static/../pom.xml
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/../pom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
		
		// curl -i http://127.0.0.1:8080/static/%2E%2E%2Fpom.xml
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/%2E%2E%2Fpom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
		
		// curl -i http://127.0.0.1:8080/static/%252E%252E%252Fpom.xml
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/%252E%252E%252Fpom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());
		
		// curl -i http://127.0.0.1:8080/static//pom.xml
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static//pom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
		
		// curl -i http://127.0.0.1:8080/static/%2Fpom.xml
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/static/%2Fpom.xml"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(400, response.statusCode());
	}
	
	public void test_misc(URI baseURI, HttpClient.Version version) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response;
		
		
		//curl -i http://127.0.0.1:8080/get_path_param/qmark_1_
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/qmark_12_
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/qmark_12_"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());

		//curl -i http://127.0.0.1:8080/get_path_param/wcard__
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/wcard_1_
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/wcard_123456789_
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/directories
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/directories/
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/directories/a
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/directories/a/b/
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/directories/a/b/c
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/jsp/
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/jsp/"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());

		//curl -i http://127.0.0.1:8080/get_path_param/jsp/test.jsp
		response = client.send(
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
		
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/a/test.jsp
		response = client.send(
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
		
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/a/b/test.jsp
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/terminal
		response = client.send(
			HttpRequest.newBuilder()
					.uri(baseURI.resolve("/get_path_param/terminal"))
					.version(version)
					.GET()
					.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		Assertions.assertEquals(404, response.statusCode());

		//curl -i http://127.0.0.1:8080/get_path_param/terminal/
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a/b/
		response = client.send(
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

		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a/b/c
		response = client.send(
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
