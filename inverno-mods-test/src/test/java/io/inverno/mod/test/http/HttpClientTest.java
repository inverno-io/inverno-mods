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
package io.inverno.mod.test.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.boot.Boot;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Client;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.RequestTimeoutException;
import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import io.inverno.test.InvernoTestCompiler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class HttpClientTest {

	static {
		System.setProperty("log4j2.simplelogLevel", "INFO");
		System.setProperty("log4j2.simplelogLogFile", "system.out");
//		System.setProperty("io.netty.leakDetection.level", "PARANOID");
//		System.setProperty("io.netty.leakDetection.targetRecords", "20");
	}
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final String MODULE_WEBROUTE = "io.inverno.mod.test.web.webroute";
	
	private static int testServerPort;
	private static InvernoModuleProxy testServerModuleProxy;
	
	private static Boot bootModule;
	private static Client httpClientModule;
	
	/*private static Endpoint<ExchangeContext> h11Endpoint;
	private static Endpoint<ExchangeContext> h2cEndpoint;*/
	
	@BeforeAll
	public static void init() throws IOException, InvernoCompilationException, ClassNotFoundException, InterruptedException {
		InvernoTestCompiler invernoCompiler = InvernoTestCompiler.builder()
			.moduleOverride(AbstractInvernoModTest.MODULE_OVERRIDE)
			.annotationProcessorModuleOverride(AbstractInvernoModTest.ANNOTATION_PROCESSOR_MODULE_OVERRIDE)
			.build();
		
		invernoCompiler.cleanModuleTarget();
		
		InvernoModuleLoader moduleLoader = invernoCompiler.compile(MODULE_WEBROUTE);
		
		testServerPort = getFreePort();
		
		Class<?> httpConfigClass = moduleLoader.loadClass(MODULE_WEBROUTE, "io.inverno.mod.http.server.HttpServerConfiguration");
		ConfigurationInvocationHandler httpConfigHandler = new ConfigurationInvocationHandler(httpConfigClass, Map.of("server_port", testServerPort, "h2c_enabled", true));
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
		
		bootModule = new Boot.Builder().build();
		bootModule.start();
		
		httpClientModule = new Client.Builder(bootModule.netService(), bootModule.reactor(), bootModule.resourceService()).build();
		httpClientModule.start();
		
		/*h11Endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))))
			.build();
		
		h2cEndpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.build();*/
	}
	
	@AfterAll
	public static void destroy() {
		/*if(h2cEndpoint != null) {
			h2cEndpoint.shutdown().block();
		}
		if(h11Endpoint != null) {
			h11Endpoint.shutdown().block();
		}*/
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
	
	/*public static Stream<Arguments> provideEndpointAndHttpVersion() {
		return Stream.of(
			Arguments.of(h11Endpoint, HttpVersion.HTTP_1_1),
			Arguments.of(h2cEndpoint, HttpVersion.HTTP_2_0)
		);
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_void(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_void'
		endpoint
			.exchange(Method.GET, "/get_void")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertNull(response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(0), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_raw'
		endpoint
			.exchange(Method.GET, "/get_raw")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertNull(response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(7), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_raw", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_raw_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_raw/pub'
		endpoint
			.exchange(Method.GET, "/get_raw/pub")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertNull(response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(11), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_raw_pub", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_raw_mono(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_raw/mono'
		endpoint
			.exchange(Method.GET, "/get_raw/mono")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertNull(response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(12), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_raw_mono", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_raw_flux(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_raw/flux'
		endpoint
			.exchange(Method.GET, "/get_raw/flux")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertNull(response.headers().getContentType());
				Assertions.assertTrue(response.headers().get(Headers.NAME_CONTENT_LENGTH).isEmpty());

				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_raw_flux", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded'
		endpoint
			.exchange(Method.GET, "/get_encoded")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(11), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_no_produce(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/no_produce'
		endpoint
			.exchange(Method.GET, "/get_encoded/no_produce")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertNull(response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(22), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_no_produce", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_no_encoder(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/no_encoder'
		endpoint
			.exchange(Method.GET, "/get_encoded/no_encoder")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.INTERNAL_SERVER_ERROR, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_collection(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/collection")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(22), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get,encoded,collection", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_list(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/list")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(16), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get,encoded,list", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_set(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/set")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(15), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals(Set.of("get","encoded","set"), new HashSet<>(Arrays.asList(body.split(","))));
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_array(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/array")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(17), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get,encoded,array", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pub'
		endpoint
			.exchange(Method.GET, "/get_encoded/pub")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertTrue(response.headers().get(Headers.NAME_CONTENT_LENGTH).isEmpty());
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pub", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_mono(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/mono'
		endpoint
			.exchange(Method.GET, "/get_encoded/mono")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(16), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_mono", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_flux(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/flux'
		endpoint
			.exchange(Method.GET, "/get_encoded/flux")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertTrue(response.headers().get(Headers.NAME_CONTENT_LENGTH).isEmpty());
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_flux", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc&queryParam=def'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam?queryParam=abc&queryParam=def")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/opt?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc&queryParam=def'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/opt?queryParam=abc&queryParam=def")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_opt: empty", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_collection(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/collection?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_collection_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc&queryParam=def,hij'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/collection?queryParam=abc&queryParam=def,hij")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_collection_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/collection")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_collection_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/collection/opt?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_collection_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc&queryParam=def,hij'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/collection/opt?queryParam=abc&queryParam=def,hij")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(52), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_collection_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/collection/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_list(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/list?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_list_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc&queryParam=def,hij'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/list?queryParam=abc&queryParam=def,hij")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_list_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/list")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_list_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/list/opt?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_list_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc&queryParam=def,hij'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/list/opt?queryParam=abc&queryParam=def,hij")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_list_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/list/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_set(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/set?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_set: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_set_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc&queryParam=def,hij'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/set?queryParam=abc&queryParam=def,hij")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(41), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_queryParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_set_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/set")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_set_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/set/opt?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(35), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_set_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_set_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc&queryParam=def,hij'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/set/opt?queryParam=abc&queryParam=def,hij")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_queryParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_set_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/set/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_set_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_array(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/array?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_array_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc&queryParam=def,hij'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/array?queryParam=abc&queryParam=def,hij")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_array_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/array")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_array_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/array/opt?queryParam=abc")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(37), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_array_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc&queryParam=def,hij'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/array/opt?queryParam=abc&queryParam=def,hij")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_queryParam_array_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/queryParam/array/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam: abc", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_opt: abc", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_opt_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_opt: abc", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_opt: empty", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_collection(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/collection")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_collection_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/collection")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection: abc, def, hij", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_collection_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/collection")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection: abc, def, hij", body);
			})
			.block();
	}
		
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_collection_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/collection")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_collection_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_collection_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(53), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_collection_opt_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(53), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_collection_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/collection/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(40), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_list(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/list")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_list_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/list")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_list_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/list")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_list_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/list")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_list_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(37), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_list_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,ghi")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc, def, ghi", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_list_opt_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_list_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/list/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_set(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/set")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_set: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_set_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/set")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_cookieParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_set_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/set")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_cookieParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_set_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/set")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_set_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_set_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_set_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_cookieParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_set_opt_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_cookieParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_set_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/set/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_set_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_array(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/array")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array: abc", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_array_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/array")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_array_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/array")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_array_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/array")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_array_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_array_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_array_opt_multi_header(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
					.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_cookieParam_array_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/cookieParam/array/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(35), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_opt: abc", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_opt: empty", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_collection(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/collection")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_collection_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/collection")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc,def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_collection_multi_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/collection")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_collection_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/collection")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_collection_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_collection_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc,def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(53), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_collection_opt_multi_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(53), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc, def, hij", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_collection_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/collection/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(40), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection_opt: ", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_list(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/list")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list: abc", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_list_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/list")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc,def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list: abc, def, hij", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_list_multi_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/list")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_list_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/list")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_list_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(37), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_list_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc,def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_list_opt_multi_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_list_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/list/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_set(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/set")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_set: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_set_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/set")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc,def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_headerParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_set_multi_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/set")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_headerParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_set_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/set")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_set_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_set_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_set_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc,def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_headerParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_set_opt_multi_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_headerParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_set_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/set/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_set_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_array(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/array")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_array_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/array")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc,def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_array_multi_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/array")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_array_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/array")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_array_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array_opt: abc", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_array_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc,def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_array_opt_multi_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'headerparam:abc' -H 'headerparam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers
					.add("headerparam", "abc")
					.add("headerparam", "def,hij")
				);
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array_opt: abc, def, hij", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_headerParam_array_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/headerParam/array/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(35), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam/")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/opt'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_opt: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_opt: empty", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_collection(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/collection", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(41), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_collection: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_collection_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//collection'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//collection")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_collection_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection/opt'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/collection/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_collection_opt: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_collection_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//collection/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//collection/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_collection_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_list(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/list", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(35), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_list: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_list_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//list'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//list")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_list_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list/opt'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/list/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_list_opt: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_list_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//list/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//list/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_list_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_set(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/set", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_pathParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("a", "b", "c"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_set_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//set'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//set")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_set_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set/opt'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/set/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_pathParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("a", "b", "c"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_set_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//set/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//set/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_set_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_set_array(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/array", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_array: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_set_array_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//array'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//array")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_set_array_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array/opt'
		endpoint
			.exchange(Method.GET, URIs.uri("/get_encoded/pathParam/{param}/array/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c")))
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(40), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_array_opt: a, b, c", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_pathParam_set_array_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//array/opt'
		endpoint
			.exchange(Method.GET, "/get_encoded/pathParam//array/opt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_array_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_json(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/dto'
		endpoint
			.exchange(Method.GET, "/get_encoded/json/dto")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("{\"message\":\"Hello, world!\"}", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_json_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/dto'
		endpoint
			.exchange(Method.GET, "/get_encoded/json/pub/dto")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, response.headers().getContentType());
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("[{\"message\":\"Hello, world!\"},{\"message\":\"Salut, monde!\"},{\"message\":\"Hallo, welt!\"}]", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_json_generic(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/dto/generic'
		endpoint
			.exchange(Method.GET, "/get_encoded/json/dto/generic")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(51), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("{\"@type\":\"string\",\"id\":1,\"message\":\"Hello, world!\"}", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_json_generic_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/dto/generic'
		endpoint
			.exchange(Method.GET, "/get_encoded/json/pub/dto/generic")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, response.headers().getContentType());
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("[{\"@type\":\"string\",\"id\":1,\"message\":\"Hello, world!\"},{\"@type\":\"integer\",\"id\":2,\"message\":123456}]", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_json_map(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/map'
		endpoint
			.exchange(Method.GET, "/get_encoded/json/map")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(13), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				try {
					Assertions.assertEquals(Map.of("a", 1, "b", 2), MAPPER.readerFor(new TypeReference<Map<String, Integer>>() {}).readValue(body));
				} 
				catch (JsonProcessingException ex) {
					Assertions.fail(ex);
				}
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_encoded_json_map_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/map'
		endpoint
			.exchange(Method.GET, "/get_encoded/json/pub/map")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, response.headers().getContentType());
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				try {
					Assertions.assertEquals(List.of(Map.of("a", 1, "b", 2), Map.of("c", 3, "d", 4)), MAPPER.readerFor(new TypeReference<List<Map<String, Integer>>>() {}).readValue(body));
				} 
				catch (JsonProcessingException ex) {
					Assertions.fail(ex);
				}
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		endpoint
			.exchange(Method.POST, "/post/formParam")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(21), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		endpoint
			.exchange(Method.POST, "/post/formParam")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(21), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		endpoint
			.exchange(Method.POST, "/post/formParam")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.empty()));
				return exchange.response();
			})
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_opt: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_opt: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_opt: empty", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_collection(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		endpoint
			.exchange(Method.POST, "/post/formParam/collection")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_collection_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		endpoint
			.exchange(Method.POST, "/post/formParam/collection")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection: a, b, c, d, e, f", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_collection_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		endpoint
			.exchange(Method.POST, "/post/formParam/collection")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_collection_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection_opt: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_collection_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection_opt: a, b, c, d, e, f", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_collection_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/collection/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_list(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		endpoint
			.exchange(Method.POST, "/post/formParam/list")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_list_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		endpoint
			.exchange(Method.POST, "/post/formParam/list")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(37), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list: a, b, c, d, e, f", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_list_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		endpoint
			.exchange(Method.POST, "/post/formParam/list")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_list_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list_opt: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_list_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(41), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list_opt: a, b, c, d, e, f", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_list_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/list/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_set(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		endpoint
			.exchange(Method.POST, "/post/formParam/set")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_set_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		endpoint
			.exchange(Method.POST, "/post/formParam/set")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set: a, b, c, d, e, f", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_set_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		endpoint
			.exchange(Method.POST, "/post/formParam/set")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_set_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set_opt: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_set_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(40), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set_opt: a, b, c, d, e, f", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_set_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/set/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_array(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		endpoint
			.exchange(Method.POST, "/post/formParam/array")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(29), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_array_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		endpoint
			.exchange(Method.POST, "/post/formParam/array")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array: a, b, c, d, e, f", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_array_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		endpoint
			.exchange(Method.POST, "/post/formParam/array")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_array_opt(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Mono.just(
					factory.create("formParam", "a,b,c")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array_opt: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_array_opt_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array_opt: a, b, c, d, e, f", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_array_opt_missing(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		endpoint
			.exchange(Method.POST, "/post/formParam/array/opt")
			.flatMap(exchange -> {
				exchange.request().headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(26), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array_opt: ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_mono(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a=1&b=2&c=3&c=4' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:39901/post/formParam/mono'
		endpoint
			.exchange(Method.POST, "/post/formParam/mono")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("a", "1"),
					factory.create("b", "2"),
					factory.create("c", "3"),
					factory.create("c", "4")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());

				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_mono: a=1", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_formParam_flux(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a=1&b=2&c=3&c=4' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:39901/post/formParam/flux'
		endpoint
			.exchange(Method.POST, "/post/formParam/flux")
			.flatMap(exchange -> {
				exchange.request().body().get().urlEncoded().from((factory, data) -> data.stream(Flux.just(
					factory.create("a", "1"),
					factory.create("b", "2"),
					factory.create("c", "3"),
					factory.create("c", "4")
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_flux: a=1, b=2, c=3, c=4, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw'
		endpoint
			.exchange(Method.POST, "/post_raw")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(15), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_raw_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_raw'
		endpoint
			.exchange(Method.POST, "/post_raw_raw")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(19), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_raw: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_raw_pub_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {	
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_pub_raw'
		endpoint
			.exchange(Method.POST, "/post_raw_pub_raw")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_pub_raw: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_raw_mono_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_mono_raw'
		endpoint
			.exchange(Method.POST, "/post_raw_mono_raw")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_mono_raw: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_raw_flux_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_flux_raw'
		endpoint
			.exchange(Method.POST, "/post_raw_flux_raw")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertEquals(Headers.VALUE_CHUNKED, response.headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
						break;
					case HTTP_2_0: Assertions.assertTrue(response.headers().get(Headers.NAME_TRANSFER_ENCODING).isEmpty());
						break;
				}
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_flux_raw: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_raw_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/pub'
		endpoint
			.exchange(Method.POST, "/post_raw/pub")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(19), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_pub: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_raw_mono(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/mono'
		endpoint
			.exchange(Method.POST, "/post_raw/mono")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(20), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_mono: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_raw_flux(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/flux'
		endpoint
			.exchange(Method.POST, "/post_raw/flux")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(20), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_flux: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded'
		endpoint
			.exchange(Method.POST, "/post_encoded")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(19), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_no_consume(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: ' -X POST 'http://127.0.0.1:8080/post_encoded/no_consume'
		endpoint
			.exchange(Method.POST, "/post_encoded/no_consume")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(30), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_no_consume: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_no_decoder(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/no_decoder'
		endpoint
			.exchange(Method.POST, "/post_encoded/no_decoder")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.doOnNext(response -> {
				Assertions.assertEquals(Status.INTERNAL_SERVER_ERROR, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_collection(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/collection'
		endpoint
			.exchange(Method.POST, "/post_encoded/collection")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_collection: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_list(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/list'
		endpoint
			.exchange(Method.POST, "/post_encoded/list")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(26), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_list: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_set(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/set'
		endpoint
			.exchange(Method.POST, "/post_encoded/set")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_set: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_array(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/array'
		endpoint
			.exchange(Method.POST, "/post_encoded/array")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_array: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/pub'
		endpoint
			.exchange(Method.POST, "/post_encoded/pub")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_pub: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_mono(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/mono'
		endpoint
			.exchange(Method.POST, "/post_encoded/mono")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_mono: a,b,c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_flux(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/flux'
		endpoint
			.exchange(Method.POST, "/post_encoded/flux")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
					.body().get().string().value("a,b,c");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(26), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_flux: a, b, c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_json(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d '{"message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto'
		endpoint
			.exchange(Method.POST, "/post_encoded/json/dto")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
					.body().get().string().value("{\"message\":\"Hello, world!\"}");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_dto: Hello, world!", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_json_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d '{"message":"Hello, world!"}{"message":"Hallo, welt!"}{"message":"Salut, monde!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto'
		endpoint
			.exchange(Method.POST, "/post_encoded/json/pub/dto")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
					.body().get().string().value("{\"message\":\"Hello, world!\"}{\"message\":\"Hallo, welt!\"}{\"message\":\"Salut, monde!\"}");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(71), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_pub_dto: Hello, world!, Hallo, welt!, Salut, monde!, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_json_generic(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d '{"@type":"string", "message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto/generic'
		endpoint
			.exchange(Method.POST, "/post_encoded/json/dto/generic")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
					.body().get().string().value("{\"@type\":\"string\", \"message\":\"Hello, world!\"}");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_dto_generic: Hello, world!", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_json_generic_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d '{"@type":"string","message":"Hello, world!"}{"@type":"integer","message":123456}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto/generic'
		endpoint
			.exchange(Method.POST, "/post_encoded/json/pub/dto/generic")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
					.body().get().string().value("{\"@type\":\"string\",\"message\":\"Hello, world!\"}{\"@type\":\"integer\",\"message\":123456}");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(58), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_pub_dto_generic: Hello, world!, 123456, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_json_map(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d '{"a":1, "b":2, "c":3}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/map'
		endpoint
			.exchange(Method.POST, "/post_encoded/json/map")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
					.body().get().string().value("{\"a\":1, \"b\":2, \"c\":3}");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_map: {a=1, b=2, c=3}", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_encoded_json_map_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i -d '{"a":1, "b":2, "c":3}{"d":4, "e":5, "f":6}{"g":7, "h":8, "i":9}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/map'
		endpoint
			.exchange(Method.POST, "/post_encoded/json/pub/map")
			.flatMap(exchange -> {
				exchange.request()
					.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
					.body().get().string().value("{\"a\":1, \"b\":2, \"c\":3}{\"d\":4, \"e\":5, \"f\":6}{\"g\":7, \"h\":8, \"i\":9}");
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(78), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_pub_map: {a=1, b=2, c=3}, {d=4, e=5, f=6}, {g=7, h=8, i=9}, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_pub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub'
		endpoint
			.exchange(Method.POST, "/post_multipart_pub")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(29), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub: a, b, c, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_pub_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/raw'
		endpoint
			.exchange(Method.POST, "/post_multipart_pub/raw")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub_raw: a = 1, b = 2, c = 3, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_encoded(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/encoded'
		endpoint
			.exchange(Method.POST, "/post_multipart_pub/encoded")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub_encoded: a = 1, b = 2, c = 3, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_mono(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
		endpoint
			.exchange(Method.POST, "/post_multipart_mono")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Mono.just(
					factory.string(part -> part.name("a").value("1"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(22), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono: a", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_mono_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
		endpoint
			.exchange(Method.POST, "/post_multipart_mono")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(22), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono: a", body);
			})
			.block();
	}

	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_mono_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
		endpoint
			.exchange(Method.POST, "/post_multipart_mono/raw")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Mono.just(
					factory.string(part -> part.name("a").value("1"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono_raw: a = 1, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_mono_raw_multi(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
		endpoint
			.exchange(Method.POST, "/post_multipart_mono/raw")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono_raw: a = 1, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_mono_encoded(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/encoded'
		endpoint
			.exchange(Method.POST, "/post_multipart_mono/encoded")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono_encoded: a = 1", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_flux(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux'
		endpoint
			.exchange(Method.POST, "/post_multipart_flux")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(30), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_flux: a, b, c, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_flux_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/raw'
		endpoint
			.exchange(Method.POST, "/post_multipart_flux/raw")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub_raw: a = 1, b = 2, c = 3, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_flux_encoded(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/encoded'
		endpoint
			.exchange(Method.POST, "/post_multipart_flux/encoded")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.string(part -> part.name("a").value("1")),
					factory.string(part -> part.name("b").value("2")),
					factory.string(part -> part.name("c").value("3"))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(50), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_flux_encoded: a = 1, b = 2, c = 3, ", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_fileUpload_small(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) throws IOException {
		File uploadsDir = new File("target/uploads/");
		uploadsDir.mkdirs();
		
		//curl -i -F 'file=@src/test/resources/post_resource_small.txt' http://127.0.0.1:8080/upload
		new File(uploadsDir, "post_resource_small.txt").delete();
		endpoint
			.exchange(Method.POST, "/upload")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.resource(part -> part.name("file").value(new FileResource(new File("src/test/resources/post_resource_small.txt"))))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(Long.valueOf(55), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("Uploaded post_resource_small.txt(text/plain): " + new File("src/test/resources/post_resource_small.txt").length() + " Bytes\n", body);
			})
			.block();
		
		Assertions.assertArrayEquals(Files.readAllBytes(Path.of("src/test/resources/post_resource_small.txt")), Files.readAllBytes(Path.of("target/uploads/post_resource_small.txt")));
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_post_multipart_fileUpload_big(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) throws IOException {
		File uploadsDir = new File("target/uploads/");
		uploadsDir.mkdirs();
		
		//curl -i -F 'file=@src/test/resources/post_resource_big.txt' http://127.0.0.1:8080/upload
		new File(uploadsDir, "post_resource_big.txt").delete();
		endpoint
			.exchange(Method.POST, "/upload")
			.flatMap(exchange -> {
				exchange.request().body().get().multipart().from((factory, output) -> output.stream(Flux.just(
					factory.resource(part -> part.name("file").value(new FileResource(new File("src/test/resources/post_resource_big.txt"))))
				)));
				return exchange.response();
			})
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(Long.valueOf(58), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("Uploaded post_resource_big.txt(text/plain): " + new File("src/test/resources/post_resource_big.txt").length() + " Bytes\n", body);
			})
			.block();
		
		Assertions.assertArrayEquals(Files.readAllBytes(Path.of("src/test/resources/post_resource_big.txt")), Files.readAllBytes(Path.of("target/uploads/post_resource_big.txt")));
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_sse_raw(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) throws IOException {
		// curl -i 'http://127.0.0.1:8080/get_sse_raw'
		byte[] get_sse_raw_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_raw_http11.dat"));
		byte[] get_sse_raw_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_raw_http2.dat"));
		endpoint
			.exchange(Method.GET, "/get_sse_raw")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8", response.headers().getContentType());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertArrayEquals(get_sse_raw_http11, body.getBytes(Charsets.DEFAULT));
						break;
					case HTTP_2_0: Assertions.assertArrayEquals(get_sse_raw_http2, body.getBytes(Charsets.DEFAULT));
						break;
				}
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_sse_encoded(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) throws IOException {
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded'
		byte[] get_sse_encoded_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_http11.dat"));
		byte[] get_sse_encoded_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_http2.dat"));
		endpoint
			.exchange(Method.GET, "/get_sse_encoded")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8", response.headers().getContentType());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_http11, body.getBytes(Charsets.DEFAULT));
						break;
					case HTTP_2_0: Assertions.assertArrayEquals(get_sse_encoded_http2, body.getBytes(Charsets.DEFAULT));
						break;
				}
				
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_sse_encoded_json(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) throws IOException {
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded/json'
		byte[] get_sse_encoded_json_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_http11.dat"));
		byte[] get_sse_encoded_json_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_http2.dat"));
		endpoint
			.exchange(Method.GET, "/get_sse_encoded/json")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8", response.headers().getContentType());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_json_http11, body.getBytes(Charsets.DEFAULT));
						break;
					case HTTP_2_0: Assertions.assertArrayEquals(get_sse_encoded_json_http2, body.getBytes(Charsets.DEFAULT));
						break;
				}
				
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_sse_encoded_json_map(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) throws IOException {
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded/json/map'
		byte[] get_sse_encoded_json_map_http11 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_map_http11.dat"));
		byte[] get_sse_encoded_json_map_http2 = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_map_http2.dat"));
		endpoint
			.exchange(Method.GET, "/get_sse_encoded/json/map")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8", response.headers().getContentType());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				switch(testHttpVersion) {
					case HTTP_1_1: Assertions.assertArrayEquals(get_sse_encoded_json_map_http11, body.getBytes(Charsets.DEFAULT));
						break;
					case HTTP_2_0: Assertions.assertArrayEquals(get_sse_encoded_json_map_http2, body.getBytes(Charsets.DEFAULT));
						break;
				}
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i 'http://127.0.0.1:8080/get_resource'
		endpoint
			.exchange(Method.GET, "/get_resource")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("This is a test resource.", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_small(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/get_resource_small.txt
		endpoint
			.exchange(Method.GET, "/static/get_resource_small.txt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("This is a test resource.", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_big(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) throws IOException {
		// curl -i http://127.0.0.1:8080/static/get_resource_big.txt
		endpoint
			.exchange(Method.GET, "/static/get_resource_big.txt")
			.flatMap(Exchange::response)
			.flatMap(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(new File("src/test/resources/post_resource_big.txt").length(), response.headers().getContentLength());
				
				return Flux.from(response.body().raw().stream())
					.reduceWith(
						() -> new ByteArrayOutputStream(), 
						(output, chunk) -> {
							try {
								chunk.readBytes(output, chunk.readableBytes());
							}
							catch(IOException e) {
								throw new UncheckedIOException(e);
							}
							finally {
								chunk.release();
							}
							return output;
						}
					)
					.map(output -> output.toByteArray());
			})
			.doOnNext(body -> {
				try {
					Assertions.assertArrayEquals(Files.readAllBytes(Path.of("src/test/resources/post_resource_big.txt")), body);
				} 
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_pc_encoded_space(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/some%20space.txt
		endpoint
			.exchange(Method.GET, "/static/some%20space.txt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(18), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("Space in file name", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_double_pc_encoded_space(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/some%2520space.txt
		endpoint
			.exchange(Method.GET, "/static/some%2520space.txt")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_dir(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/dir/get_resource.txt
		endpoint
			.exchange(Method.GET, "/static/dir/get_resource.txt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("This is a test resource.", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_dir_percent_encoded_slash(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/dir%2Fget_resource.txt
		endpoint
			.exchange(Method.GET, "/static/dir%2Fget_resource.txt")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("This is a test resource.", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_dir_double_percent_encoded_slash(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/dir%252Fget_resource.txt
		endpoint
			.exchange(Method.GET, "/static/dir%252Fget_resource.txt")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_path_traversal_parent(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/../pom.xml
		endpoint
			.exchange(Method.GET, "/static/../pom.xml")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_path_traversal_parent_percent_encoded_slash(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/%2E%2E%2Fpom.xml
		endpoint
			.exchange(Method.GET, "/static/%2E%2E%2Fpom.xml")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_path_traversal_parent_double_percent_encoded_slash(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/%252E%252E%252Fpom.xml
		endpoint
			.exchange(Method.GET, "/static/%252E%252E%252Fpom.xml")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_path_traversal_absolute(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static//pom.xml
		endpoint
			.exchange(Method.GET, "/static//pom.xml")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_path_traversal_absolute_percent_encoded_slash(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/%2Fpom.xml
		endpoint
			.exchange(Method.GET, "/static/%2Fpom.xml")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.BAD_REQUEST, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_resource_path_traversal_absolute_double_percent_encoded_slash(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// curl -i http://127.0.0.1:8080/static/%2Fpom.xml
		endpoint
			.exchange(Method.GET, "/static/%252Fpom.xml")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_qmark(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/qmark_1_
		endpoint
			.exchange(Method.GET, "/get_path_param/qmark_1_")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/qmark_1_", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_qmark_none(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/qmark__
		endpoint
			.exchange(Method.GET, "/get_path_param/qmark__")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_qmark_many(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/qmark_12_
		endpoint
			.exchange(Method.GET, "/get_path_param/qmark_12_")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_wcard(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/wcard_1_
		endpoint
			.exchange(Method.GET, "/get_path_param/wcard_1_")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/wcard_1_", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_wcard_none(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/wcard__
		endpoint
			.exchange(Method.GET, "/get_path_param/wcard__")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(23), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/wcard__", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_wcard_many(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/wcard_123456789_
		endpoint
			.exchange(Method.GET, "/get_path_param/wcard_123456789_")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/wcard_123456789_", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_directories(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/directories
		endpoint
			.exchange(Method.GET, "/get_path_param/directories")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_directories_trailingSlash(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/directories/
		endpoint
			.exchange(Method.GET, "/get_path_param/directories/")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories/", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_directories_sub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a
		endpoint
			.exchange(Method.GET, "/get_path_param/directories/a")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(29), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories/a", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_directories_sub_sub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a/b/
		endpoint
			.exchange(Method.GET, "/get_path_param/directories/a/b")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories/a/b", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_directories_sub_sub_sub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a/b/c
		endpoint
			.exchange(Method.GET, "/get_path_param/directories/a/b/c")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories/a/b/c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_regex_unmatching(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/
		endpoint
			.exchange(Method.GET, "/get_path_param/jsp")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_regex_matching(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/test.jsp
		endpoint
			.exchange(Method.GET, "/get_path_param/jsp/test.jsp")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/jsp/test.jsp - test.jsp", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_regex_matching_sub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/a/test.jsp
		endpoint
			.exchange(Method.GET, "/get_path_param/jsp/a/test.jsp")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(41), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/jsp/a/test.jsp - test.jsp", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_regex_matching_sub_sub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/a/b/test.jsp
		endpoint
			.exchange(Method.GET, "/get_path_param/jsp/a/b/test.jsp")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/jsp/a/b/test.jsp - test.jsp", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_terminal_unmatching(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal
		endpoint
			.exchange(Method.GET, "/get_path_param/terminal")
			.flatMap(Exchange::response)
			.doOnNext(response -> {
				Assertions.assertEquals(Status.NOT_FOUND, response.headers().getStatus());
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_terminal_matching_trailingSlash(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/
		endpoint
			.exchange(Method.GET, "/get_path_param/terminal/")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/terminal/", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_terminal_matching_sub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a
		endpoint
			.exchange(Method.GET, "/get_path_param/terminal/a")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(26), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/terminal/a", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_terminal_matching_sub_sub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a/b/
		endpoint
			.exchange(Method.GET, "/get_path_param/terminal/a/b/")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(29), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/terminal/a/b/", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_get_pathParam_terminal_matching_sub_sub_sub(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a/b/c
		endpoint
			.exchange(Method.GET, "/get_path_param/terminal/a/b/c")
			.flatMap(Exchange::response)
			.flatMapMany(response -> {
				Assertions.assertEquals(Status.OK, response.headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
				Assertions.assertEquals(Long.valueOf(30), response.headers().getContentLength());
				
				return response.body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/terminal/a/b/c", body);
			})
			.block();
	}
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_pool_concurrency(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		// By default: 
		// - pool_max_size=2
		// - http2_max_concurrent_streams=100
		// - http1_max_concurrent_requests=10
		
		// Just make sure all pool connections are created
		Flux.range(0, 10)
			.flatMap(i -> endpoint
				.exchange(Method.GET, "/get_delay100")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
			)
			.blockLast();
		
		switch(testHttpVersion) {
			case HTTP_1_1: {
				long t0 = System.currentTimeMillis();
				Flux.range(0, 50)
					.flatMap(i -> endpoint
						.exchange(Method.GET, "/get_delay100")
						.flatMap(Exchange::response)
						.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
					)
					.doOnNext(body -> Assertions.assertEquals("get_delay100", body))
					.blockLast();
				long total = System.currentTimeMillis() - t0;
				
				// We should take 100 * 50 / 2 = 2500ms in theory
				// In practice:
				// - ...
				
				// Let's be protective here and use a 2500ms delta
				// as long as we are not taking more than 5 seconds we know the pool is doing its job
				// Running a clean test the batch completes in 2552ms
				Assertions.assertEquals(2500, total, 2500); 
				break;
			}
			case HTTP_2_0: {
				long t0 = System.currentTimeMillis();
				Flux.range(0, 150)
					.flatMap(i -> endpoint
						.exchange(Method.GET, "/get_delay100")
						.flatMap(Exchange::response)
						.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
					)
					.doOnNext(body -> Assertions.assertEquals("get_delay100", body))
					.blockLast();
				long total = System.currentTimeMillis() - t0;
				// Each connection will process 75 requests since we allow up to 100 concurrent streams all requests should be processed in parallel, we should take around 100ms in theory
				// In practice:
				// - the event loop group is processing the request, we have 2*cores thread available (so all requests won't be processed in parallel)
				// - ...
				
				// Let's be protective here and use a 150ms delta
				// In theory with one connection we should process 100 requests and then 50 leading to 200ms
				// Running a clean test the batch should completes in 163ms
				Assertions.assertEquals(100, total, 200);
				break;
			}
		}
	}*/
	
	@Test
	public void test_h2c_tooBig() {
		File uploadsDir = new File("target/uploads/");
		uploadsDir.mkdirs();
		
		// This should result in a failed connection, next request will create a new connection
		Endpoint<ExchangeContext> blankH2cEndpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.build();
		try {
			//curl -i -F 'file=@src/test/resources/post_resource_big.txt' http://127.0.0.1:8080/upload
			new File(uploadsDir, "post_resource_big.txt").delete();
			blankH2cEndpoint
				.exchange(Method.POST, "/upload")
				.flatMap(exchange -> {
					exchange.request().body().get().multipart().from((factory, output) -> output.value(
						factory.resource(part -> part.name("file").value(new FileResource(new File("src/test/resources/post_resource_big.txt"))))
					));
					return exchange.response();
				})
				.doOnNext(response -> {
					Assertions.assertEquals(Status.PAYLOAD_TOO_LARGE, response.headers().getStatus());
				})
				.block();
		}
		catch(Exception e) {
			// TODO This fails some times with a broken pipe error, I couldn't figure out what's wrong because I wasn't able to reproduce it in a deterministic way
			// the problem arise when the connection is closed and we still are trying to write on the socket, this is normally handled but for some reason the exception propagates
			// Let's leave it for now at least we can check that the endpoint properly create a new connection on the next request
			e.printStackTrace();
		}
		finally {
			blankH2cEndpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_interceptor() {
		AtomicBoolean interceptorFlag = new AtomicBoolean(false);
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.interceptor(exchange -> {
				interceptorFlag.set(true);
				return Mono.just(exchange);
			})
			.build();
		
		try {
			endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMapMany(response -> {
					Assertions.assertEquals(Status.OK, response.headers().getStatus());
					Assertions.assertNull(response.headers().getContentType());
					Assertions.assertEquals(Long.valueOf(7), response.headers().getContentLength());

					return response.body().string().stream();
				})
				.collect(Collectors.joining())
				.doOnNext(body -> {
					Assertions.assertEquals("get_raw", body);
				})
				.block();
			
			Assertions.assertTrue(interceptorFlag.get());
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_interceptor_cookieParam() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.interceptor(exchange -> {
				exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "def,hij")));
				return Mono.just(exchange);
			})
			.build();
		
		try {
			endpoint
				.exchange(Method.GET, "/get_encoded/cookieParam/list")
				.flatMap(exchange -> {
					exchange.request().headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")));
					return exchange.response();
				})
				.flatMapMany(response -> {
					Assertions.assertEquals(Status.OK, response.headers().getStatus());
					Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
					Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());

					return response.body().string().stream();
				})
				.collect(Collectors.joining())
				.doOnNext(body -> {
					Assertions.assertEquals("get_encoded_cookieParam_list: abc, def, hij", body);
				})
				.block();
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_interceptor_headerParam() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))))
			.interceptor(exchange -> {
				exchange.request().headers(headers -> headers.add("headerparam", "def,hij"));
				return Mono.just(exchange);
			})
			.build();
		
		try {
			// TODO there seems to be something wrong on the server side with header in the upgrading request?
			endpoint
				.exchange(Method.GET, "/get_encoded/headerParam/list")
				.flatMap(exchange -> {
					exchange.request().headers(headers -> headers
						.add("headerparam", "abc")
					);
					return exchange.response();
				})
				.flatMapMany(response -> {
					Assertions.assertEquals(Status.OK, response.headers().getStatus());
					Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
					Assertions.assertEquals(Long.valueOf(43), response.headers().getContentLength());

					return response.body().string().stream();
				})
				.collect(Collectors.joining())
				.doOnNext(body -> {
					Assertions.assertEquals("get_encoded_headerParam_list: abc, def, hij", body);
				})
				.block();
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_interceptor_path() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.interceptor(exchange -> {
				exchange.request().path("/get_raw/pub");
				return Mono.just(exchange);
			})
			.build();
		
		try {
			endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMapMany(response -> {
					Assertions.assertEquals(Status.OK, response.headers().getStatus());
					Assertions.assertNull(response.headers().getContentType());
					Assertions.assertEquals(Long.valueOf(11), response.headers().getContentLength());

					return response.body().string().stream();
				})
				.collect(Collectors.joining())
				.doOnNext(body -> {
					Assertions.assertEquals("get_raw_pub", body);
				})
				.block();
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_interceptor_method() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.interceptor(exchange -> {
				exchange.request().method(Method.POST);
				return Mono.just(exchange);
			})
			.build();
		
		try {
			endpoint
				.exchange(Method.PUT, "/post_raw")
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
						.body().get().string().value("a,b,c");
					return exchange.response();
				})
				.flatMapMany(response -> {
					Assertions.assertEquals(Status.OK, response.headers().getStatus());
					Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
					Assertions.assertEquals(Long.valueOf(15), response.headers().getContentLength());

					return response.body().string().stream();
				})
				.collect(Collectors.joining())
				.doOnNext(body -> {
					Assertions.assertEquals("post_raw: a,b,c", body);
				})
				.block();
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_interceptor_transform_bodies() {
		final StringBuilder interceptedRequestBody = new StringBuilder();
		final StringBuilder interceptedResponseBody = new StringBuilder();
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.interceptor(exchange -> {
				exchange.request().body().ifPresent(body -> body.transform(data -> Flux.from(data)
					.doOnNext(buf -> interceptedRequestBody.append(buf.toString(Charsets.UTF_8)))
				));
				
				exchange.response().body().transform(data -> Flux.from(data)
					.doOnNext(buf -> interceptedResponseBody.append(buf.toString(Charsets.UTF_8)))
				);
				return Mono.just(exchange);
			})
			.build();
		
		try {
			endpoint
				.exchange(Method.POST, "/post_raw")
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
						.body().get().string().value("a,b,c");
					return exchange.response();
				})
				.flatMapMany(response -> {
					Assertions.assertEquals(Status.OK, response.headers().getStatus());
					Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
					Assertions.assertEquals(Long.valueOf(15), response.headers().getContentLength());

					return response.body().string().stream();
				})
				.collect(Collectors.joining())
				.doOnNext(body -> {
					Assertions.assertEquals("post_raw: a,b,c", body);
				})
				.block();
			
			Assertions.assertEquals("a,b,c", interceptedRequestBody.toString());
			Assertions.assertEquals("post_raw: a,b,c", interceptedResponseBody.toString());
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_interceptor_abort() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.interceptor(exchange -> {
				return Mono.empty();
			})
			.build();
		
		try {
			endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMapMany(response -> {
					Assertions.assertEquals(Status.OK, response.headers().getStatus());
					Assertions.assertNull(response.headers().getContentType());
					Assertions.assertNull(response.headers().getContentLength());

					return response.body().string().stream();
				})
				.collect(Collectors.joining())
				.doOnNext(body -> {
					Assertions.assertEquals("", body);
				})
				.block();
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_interceptor_abort_with_payload() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.interceptor(exchange -> {
				exchange.response()
					.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN).contentLength(11))
					.body().string().value("intercepted");
				return Mono.empty();
			})
			.build();
		
		try {
			endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMapMany(response -> {
					Assertions.assertEquals(Status.OK, response.headers().getStatus());
					Assertions.assertEquals(MediaTypes.TEXT_PLAIN, response.headers().getContentType());
					Assertions.assertEquals(Long.valueOf(11), response.headers().getContentLength());

					return response.body().string().stream();
				})
				.collect(Collectors.joining())
				.doOnNext(body -> {
					Assertions.assertEquals("intercepted", body);
				})
				.block();
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	public static Stream<Arguments> provideTimeoutEndpointsAndHttpVersion() {
		Endpoint<ExchangeContext> h11TimeoutEndpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(1000)
			))
			.build();
		
		Endpoint<ExchangeContext> h2cTimeoutEndpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.pool_max_size(1)
				.request_timeout(1000)
			))
			.build();
		
		return Stream.of(
			Arguments.of(h11TimeoutEndpoint, HttpVersion.HTTP_1_1),
			Arguments.of(h2cTimeoutEndpoint, HttpVersion.HTTP_2_0)
		);
	}
	
	@ParameterizedTest
	@MethodSource("provideTimeoutEndpointsAndHttpVersion")
	public void test_request_timeout(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		try {
			// Make sure connection is properly upgraded first
			endpoint
				.exchange(Method.GET, "/get_void")
				.flatMap(Exchange::response)
				.block();
			
			Assertions.assertEquals(
				"Exceeded timeout 1000ms", 
				Assertions.assertThrows(
					RequestTimeoutException.class, 
					() -> endpoint
						.exchange(Method.GET, "/get_timeout")
						.flatMap(Exchange::response)
						.block()
				).getMessage()
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@ParameterizedTest
	@MethodSource("provideTimeoutEndpointsAndHttpVersion")
	public void test_request_timeout_concurrent(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		try {
			// Make sure connection is properly upgraded first
			endpoint
				.exchange(Method.GET, "/get_void")
				.flatMap(Exchange::response)
				.block();
			
			Mono<Object> timeoutRequest = endpoint
				.exchange(Method.GET, "/get_timeout")
				.flatMap(Exchange::response)
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e));

			Mono<Object> noTimeoutRequest = endpoint
				.exchange(Method.GET, "/get_delay100")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e));

			List<Object> results = Flux.merge(timeoutRequest, noTimeoutRequest)
				.collectList()
				.block();
			
			Assertions.assertEquals(2, results.size());
			
			switch(testHttpVersion) {
				case HTTP_1_1: {
					// Requests run in sequence: the whole pipeline is discarded and connection closed if the first request times out.
					
					// Must be a RequestTimeoutException
					Object result = results.get(0);
					Assertions.assertEquals(
						"Exceeded timeout 1000ms",
						Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
					);
					
					// Must be a RequestTimeoutException as well (the same actually)
					result = results.get(1);
					Assertions.assertEquals(
						"Exceeded timeout 1000ms",
						Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
					);
					
					break;
				}
				case HTTP_2_0: {
					// Requests run in parallel using different streams: a request can time out without impacting the processing of the other requests.
					
					// Must be the successful response which returns first
					Object result = results.get(0);
					Assertions.assertEquals("get_delay100", result);
					
					// Must be a RequestTimeoutException
					result = results.get(1);
					Assertions.assertEquals(
						"Exceeded timeout 1000ms",
						Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
					);
				}
			}
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	// TODO: test pool request buffer
}
