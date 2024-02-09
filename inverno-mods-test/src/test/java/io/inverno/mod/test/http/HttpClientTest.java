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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.boot.Boot;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Client;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class HttpClientTest extends AbstractInvernoModTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
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
	public void testHttpClient() throws IOException, InvernoCompilationException, ClassNotFoundException, InterruptedException {
		this.clearModuleTarget();
		
		InvernoModuleLoader moduleLoader = this.getInvernoCompiler().compile(MODULE_WEBROUTE);
		
		int port = getFreePort();
		
		Class<?> httpConfigClass = moduleLoader.loadClass(MODULE_WEBROUTE, "io.inverno.mod.http.server.HttpServerConfiguration");
		ConfigurationInvocationHandler httpConfigHandler = new ConfigurationInvocationHandler(httpConfigClass, Map.of("server_port", port, "h2_enabled", true));
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
		
		Boot bootMod = new Boot.Builder().build();
		InvernoModuleProxy testServerMod = moduleLoader.load(MODULE_WEBROUTE).optionalDependency("webRouteConfiguration", webRouteConfigClass, webRouteConfig).build();
		try {
			bootMod.start();
			testServerMod.start();
		
			Client clientMod = new Client.Builder(bootMod.netService(), bootMod.reactor()).build();
			try {
				clientMod.start();
				Endpoint endpointH2C = clientMod.httpClient().endpoint("127.0.0.1", port)
					.build();
				try {
//					this.test_fail(endpoint);
					
					this.test_get(endpointH2C);
					this.test_query_param(endpointH2C);
					this.test_cookie_param(endpointH2C);
					this.test_header_param(endpointH2C);
					this.test_path_param(endpointH2C);
					this.test_get_encoded(endpointH2C);
					this.test_form_param(endpointH2C);
					this.test_post(endpointH2C);
					this.test_post_multipart(endpointH2C);
					this.test_sse(endpointH2C);
					this.test_resource(endpointH2C);
					this.test_misc(endpointH2C);
				}
				finally {
					endpointH2C.close().block();
				}
				
				Endpoint endpointH1 = clientMod.httpClient().endpoint("127.0.0.1", port)
					.configuration(HttpClientConfigurationLoader.load(conf -> conf.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))))
					.build();
				try {
//					this.test_fail(endpoint);
					
					this.test_get(endpointH1);
					this.test_query_param(endpointH1);
					this.test_cookie_param(endpointH1);
					this.test_header_param(endpointH1);
					this.test_path_param(endpointH1);
					this.test_get_encoded(endpointH1);
					this.test_form_param(endpointH1);
					this.test_post(endpointH1);
					this.test_post_multipart(endpointH1);
					this.test_sse(endpointH1);
					this.test_resource(endpointH1);
					this.test_misc(endpointH1);
				}
				finally {
					endpointH1.close().block();
				}
			}
			finally {
				clientMod.stop();
			}
		}
		finally {
			testServerMod.stop();
			bootMod.stop();
		}
	}
	
	private void test_get(Endpoint endpoint) {
		//curl -i 'http://127.0.0.1:8080/get_void'
		endpoint
			.request(Method.GET, "/get_void")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertNull(exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(0), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_raw'
		endpoint
			.request(Method.GET, "/get_raw")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertNull(exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(7), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_raw", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_raw/pub'
		endpoint
			.request(Method.GET, "/get_raw/pub")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertNull(exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(11), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_raw_pub", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_raw/mono'
		endpoint
			.request(Method.GET, "/get_raw/mono")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertNull(exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(12), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_raw_mono", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_raw/flux'
		endpoint
			.request(Method.GET, "/get_raw/flux")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertNull(exchange.response().headers().getContentType());
				Assertions.assertTrue(exchange.response().headers().get(Headers.NAME_CONTENT_LENGTH).isEmpty());
				Assertions.assertEquals("chunked", exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_raw_flux", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded'
		endpoint
			.request(Method.GET, "/get_encoded")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(11), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/no_produce'
		endpoint
			.request(Method.GET, "/get_encoded/no_produce")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertNull(exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(22), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_no_produce", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/no_encoder'
		endpoint
			.request(Method.GET, "/get_encoded/no_encoder")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.INTERNAL_SERVER_ERROR, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/collection'
		endpoint
			.request(Method.GET, "/get_encoded/collection")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(22), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get,encoded,collection", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/list'
		endpoint
			.request(Method.GET, "/get_encoded/list")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(16), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get,encoded,list", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/set'
		endpoint
			.request(Method.GET, "/get_encoded/set")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(15), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals(Set.of("get","encoded","set"), new HashSet<>(Arrays.asList(body.split(","))));
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/array'
		endpoint
			.request(Method.GET, "/get_encoded/array")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(17), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get,encoded,array", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pub'
		endpoint
			.request(Method.GET, "/get_encoded/pub")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertTrue(exchange.response().headers().get(Headers.NAME_CONTENT_LENGTH).isEmpty());
				Assertions.assertEquals("chunked", exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pub", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/mono'
		endpoint
			.request(Method.GET, "/get_encoded/mono")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(16), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_mono", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/flux'
		endpoint
			.request(Method.GET, "/get_encoded/flux")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertTrue(exchange.response().headers().get(Headers.NAME_CONTENT_LENGTH).isEmpty());
				Assertions.assertEquals("chunked", exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).orElse(null));
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_flux", body);
			})
			.block();
	}
	
	private void test_query_param(Endpoint endpoint) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc&queryParam=def'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam?queryParam=abc&queryParam=def")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/opt?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_opt: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc&queryParam=def'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/opt?queryParam=abc&queryParam=def")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_opt: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/opt'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_opt: empty", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/collection?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc&queryParam=def,hij'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/collection?queryParam=abc&queryParam=def,hij")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/collection")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/collection/opt?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection_opt: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc&queryParam=def,hij'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/collection/opt?queryParam=abc&queryParam=def,hij")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(52), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/collection/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_collection_opt: ", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/list?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc&queryParam=def,hij'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/list?queryParam=abc&queryParam=def,hij")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/list")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/list/opt?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list_opt: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc&queryParam=def,hij'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/list/opt?queryParam=abc&queryParam=def,hij")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/list/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_list_opt: ", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/set?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_set: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc&queryParam=def,hij'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/set?queryParam=abc&queryParam=def,hij")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(41), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_queryParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/set")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/set/opt?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(35), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_set_opt: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc&queryParam=def,hij'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/set/opt?queryParam=abc&queryParam=def,hij")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_queryParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/set/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_set_opt: ", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/array?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc&queryParam=def,hij'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/array?queryParam=abc&queryParam=def,hij")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/array")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/array/opt?queryParam=abc")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(37), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array_opt: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc&queryParam=def,hij'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/array/opt?queryParam=abc&queryParam=def,hij")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/queryParam/array/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_queryParam_array_opt: ", body);
			})
			.block();
	}
	
	private void test_cookie_param(Endpoint endpoint) {
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam: abc", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam: abc", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam: abc", body);
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/opt")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_opt: abc", body);
			})
			.block();

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/opt")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_opt: abc", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_opt: abc", body);
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_opt: empty", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/collection")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection: abc", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/collection")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection: abc, def, hij", body);
			})
			.block();

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/collection")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/collection")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/collection/opt")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/collection/opt")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(53), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/collection/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(53), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/collection/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(40), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_collection_opt: ", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/list")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list: abc", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/list")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/list")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/list")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/list/opt")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(37), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc", body);
			})
			.block();

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/list/opt")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,ghi")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc, def, ghi", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/list/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/list/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_list_opt: ", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/set")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_set: abc", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/set")
			.headers(headers -> headers.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij")))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_cookieParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/set")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_cookieParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/set")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/set/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_set_opt: abc", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/set/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij"))
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_cookieParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();

		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/set/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_cookieParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/set/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_set_opt: ", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/array")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array: abc", body);
			})
			.block();

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/array")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij"))
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/array")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/array")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/array/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc", body);
			})
			.block();

		//curl -i -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/array/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc").addCookie("cookieParam", "def,hij"))
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/array/opt")
			.headers(headers -> headers
				.cookies(cookies -> cookies.addCookie("cookieParam", "abc"))
				.add(Headers.NAME_COOKIE, "cookieParam=def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/cookieParam/array/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(35), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_cookieParam_array_opt: ", body);
			})
			.block();
	}
	
	private void test_header_param(Endpoint endpoint) {
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam: abc", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_opt: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_opt: abc", body);
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_opt: empty", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/collection")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/collection")
			.headers(headers -> headers
				.add("headerParam", "abc,def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/collection")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/collection")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/collection/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/collection/opt")
			.headers(headers -> headers
				.add("headerParam", "abc,def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(53), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/collection/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(53), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection_opt: abc, def, hij", body);
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/collection/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(40), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_collection_opt: ", body);
			})
			.block();

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/list")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list: abc", body);
			})
			.block();

		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/list")
			.headers(headers -> headers
				.add("headerParam", "abc,def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list: abc, def, hij", body);
			})
			.block();

		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/list")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/list'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/list")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/list/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(37), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list_opt: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/list/opt")
			.headers(headers -> headers
				.add("headerParam", "abc,def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/list/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/list/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_list_opt: ", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/set")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_set: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/set")
			.headers(headers -> headers
				.add("headerParam", "abc,def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_headerParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/set")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_headerParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/set'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/set")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/set/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_set_opt: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/set/opt")
			.headers(headers -> headers
				.add("headerParam", "abc,def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_headerParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/set/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(46), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_headerParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("abc", "def", "hij"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/set/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_set_opt: ", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/array")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/array")
			.headers(headers -> headers
				.add("headerParam", "abc,def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/array")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/array'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/array")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i -H 'headerParam:abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/array/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array_opt: abc", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc; headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/array/opt")
			.headers(headers -> headers
				.add("headerParam", "abc,def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i -H 'headerParam:abc' -H 'headerParam:def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/array/opt")
			.headers(headers -> headers
				.add("headerParam", "abc")
				.add("headerParam", "def,hij")
			)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(48), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array_opt: abc, def, hij", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/headerParam/array/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(35), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_headerParam_array_opt: ", body);
			})
			.block();
	}
	
	private void test_path_param(Endpoint endpoint) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c'
		String requestTarget = URIs.uri("/get_encoded/pathParam/{param}", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam: a,b,c", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam/")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/opt'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_opt: a,b,c", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//opt'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_opt: empty", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/collection", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(41), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_collection: a, b, c", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//collection'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//collection")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection/opt'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/collection/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_collection_opt: a, b, c", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//collection/opt'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//collection/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_collection_opt: ", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/list", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(35), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_list: a, b, c", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//list'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//list")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list/opt'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/list/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_list_opt: a, b, c", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//list/opt'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//list/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_list_opt: ", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/set", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_pathParam_set", splitBody[0]);
				Assertions.assertEquals(Set.of("a", "b", "c"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//set'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//set")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set/opt'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/set/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				String[] splitBody = body.split(":");
				Assertions.assertEquals("get_encoded_pathParam_set_opt", splitBody[0]);
				Assertions.assertEquals(Set.of("a", "b", "c"), Arrays.stream(splitBody[1].split(",")).map(String::trim).collect(Collectors.toSet()));
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//set/opt'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//set/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_set_opt: ", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/array", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_array: a, b, c", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//array'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//array")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array/opt'
		requestTarget = URIs.uri("/get_encoded/pathParam/{param}/array/opt", URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED).buildPath(Map.of("param","a,b,c"));
		endpoint
			.request(Method.GET, requestTarget)
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(40), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_array_opt: a, b, c", body);
			})
			.block();

		//curl -i 'http://127.0.0.1:8080/get_encoded/pathParam//array/opt'
		endpoint
			.request(Method.GET, "/get_encoded/pathParam//array/opt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("get_encoded_pathParam_array_opt: ", body);
			})
			.block();
	}
	
	private void test_get_encoded(Endpoint endpoint) {
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/dto'
		endpoint
			.request(Method.GET, "/get_encoded/json/dto")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("{\"message\":\"Hello, world!\"}", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/dto'
		endpoint
			.request(Method.GET, "/get_encoded/json/pub/dto")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, exchange.response().headers().getContentType());
				Assertions.assertEquals(Headers.VALUE_CHUNKED, exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).get());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("[{\"message\":\"Hello, world!\"},{\"message\":\"Salut, monde!\"},{\"message\":\"Hallo, welt!\"}]", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/dto/generic'
		endpoint
			.request(Method.GET, "/get_encoded/json/dto/generic")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(51), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("{\"@type\":\"string\",\"id\":1,\"message\":\"Hello, world!\"}", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/dto/generic'
		endpoint
			.request(Method.GET, "/get_encoded/json/pub/dto/generic")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, exchange.response().headers().getContentType());
				Assertions.assertEquals(Headers.VALUE_CHUNKED, exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).get());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("[{\"@type\":\"string\",\"id\":1,\"message\":\"Hello, world!\"},{\"@type\":\"integer\",\"id\":2,\"message\":123456}]", body);
			})
			.block();
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/map'
		endpoint
			.request(Method.GET, "/get_encoded/json/map")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(13), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
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
		
		//curl -i 'http://127.0.0.1:8080/get_encoded/json/pub/map'
		endpoint
			.request(Method.GET, "/get_encoded/json/pub/map")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.APPLICATION_JSON, exchange.response().headers().getContentType());
				Assertions.assertEquals(Headers.VALUE_CHUNKED, exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).get());
				
				return exchange.response().body().string().stream();
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
	
	private void test_form_param(Endpoint endpoint) throws IOException, InterruptedException {
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		endpoint
			.request(Method.POST, "/post/formParam")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(factory.create("formParam", "a,b,c")))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(21), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		endpoint
			.request(Method.POST, "/post/formParam")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"), 
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(21), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam: a,b,c", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
		endpoint
			.request(Method.POST, "/post/formParam")
//			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.empty())))
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		endpoint
			.request(Method.POST, "/post/formParam/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_opt: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		endpoint
			.request(Method.POST, "/post/formParam/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_opt: a,b,c", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
		endpoint
			.request(Method.POST, "/post/formParam/opt")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_opt: empty", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		endpoint
			.request(Method.POST, "/post/formParam/collection")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		endpoint
			.request(Method.POST, "/post/formParam/collection")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection: a, b, c, d, e, f", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
		endpoint
			.request(Method.POST, "/post/formParam/collection")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		endpoint
			.request(Method.POST, "/post/formParam/collection/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection_opt: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		endpoint
			.request(Method.POST, "/post/formParam/collection/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(47), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection_opt: a, b, c, d, e, f", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
		endpoint
			.request(Method.POST, "/post/formParam/collection/opt")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_collection_opt: ", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		endpoint
			.request(Method.POST, "/post/formParam/list")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		endpoint
			.request(Method.POST, "/post/formParam/list")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(37), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list: a, b, c, d, e, f", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
		endpoint
			.request(Method.POST, "/post/formParam/list")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		endpoint
			.request(Method.POST, "/post/formParam/list/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list_opt: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		endpoint
			.request(Method.POST, "/post/formParam/list/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
					factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(41), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list_opt: a, b, c, d, e, f", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
		endpoint
			.request(Method.POST, "/post/formParam/list/opt")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_list_opt: ", body);
			})
			.block();

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		endpoint
			.request(Method.POST, "/post/formParam/set")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		endpoint
			.request(Method.POST, "/post/formParam/set")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set: a, b, c, d, e, f", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
		endpoint
			.request(Method.POST, "/post/formParam/set")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		endpoint
			.request(Method.POST, "/post/formParam/set/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set_opt: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		endpoint
			.request(Method.POST, "/post/formParam/set/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(40), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set_opt: a, b, c, d, e, f", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
		endpoint
			.request(Method.POST, "/post/formParam/set/opt")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_set_opt: ", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		endpoint
			.request(Method.POST, "/post/formParam/array")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(29), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		endpoint
			.request(Method.POST, "/post/formParam/array")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array: a, b, c, d, e, f", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
		endpoint
			.request(Method.POST, "/post/formParam/array")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		endpoint
			.request(Method.POST, "/post/formParam/array/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.just(
				factory.create("formParam", "a,b,c")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array_opt: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		endpoint
			.request(Method.POST, "/post/formParam/array/opt")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("formParam", "a,b,c"),
				factory.create("formParam", "d,e,f")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(42), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array_opt: a, b, c, d, e, f", body);
			})
			.block();
		
		//curl -i -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
		endpoint
			.request(Method.POST, "/post/formParam/array/opt")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(26), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_array_opt: ", body);
			})
			.block();
		
		//curl -i -d 'a=1&b=2&c=3&c=4' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:39901/post/formParam/flux'
		endpoint
			.request(Method.POST, "/post/formParam/flux")
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Flux.just(
				factory.create("a", "1"),
				factory.create("b", "2"),
				factory.create("c", "3"),
				factory.create("c", "4")
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Headers.VALUE_CHUNKED, exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).get());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_formParam_flux: a=1, b=2, c=3, c=4, ", body);
			})
			.block();
	}
	
	private void test_post(Endpoint endpoint) {
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw'
		endpoint
			.request(Method.POST, "/post_raw")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(15), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_raw'
		endpoint
			.request(Method.POST, "/post_raw_raw")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(19), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_raw: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_pub_raw'
		endpoint
			.request(Method.POST, "/post_raw_pub_raw")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Headers.VALUE_CHUNKED, exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).get());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_pub_raw: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_mono_raw'
		endpoint
			.request(Method.POST, "/post_raw_mono_raw")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_mono_raw: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_flux_raw'
		endpoint
			.request(Method.POST, "/post_raw_flux_raw")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Headers.VALUE_CHUNKED, exchange.response().headers().get(Headers.NAME_TRANSFER_ENCODING).get());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_flux_raw: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/pub'
		endpoint
			.request(Method.POST, "/post_raw/pub")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(19), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_pub: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/mono'
		endpoint
			.request(Method.POST, "/post_raw/mono")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(20), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_mono: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/flux'
		endpoint
			.request(Method.POST, "/post_raw/flux")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(20), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_raw_flux: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded'
		endpoint
			.request(Method.POST, "/post_encoded")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(19), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: ' -X POST 'http://127.0.0.1:8080/post_encoded/no_consume'
		endpoint
			.request(Method.POST, "/post_encoded/no_consume")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(30), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_no_consume: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/no_decoder'
		endpoint
			.request(Method.POST, "/post_encoded/no_decoder")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.INTERNAL_SERVER_ERROR, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/collection'
		endpoint
			.request(Method.POST, "/post_encoded/collection")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_collection: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/list'
		endpoint
			.request(Method.POST, "/post_encoded/list")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(26), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_list: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/set'
		endpoint
			.request(Method.POST, "/post_encoded/set")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_set: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/array'
		endpoint
			.request(Method.POST, "/post_encoded/array")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_array: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/pub'
		endpoint
			.request(Method.POST, "/post_encoded/pub")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_pub: a, b, c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/mono'
		endpoint
			.request(Method.POST, "/post_encoded/mono")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_mono: a,b,c", body);
			})
			.block();
		
		//curl -i -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/flux'
		endpoint
			.request(Method.POST, "/post_encoded/flux")
			.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
			.body(body -> body.string().value("a,b,c"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(26), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_flux: a, b, c", body);
			})
			.block();
		
		//curl -i -d '{"message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto'
		endpoint
			.request(Method.POST, "/post_encoded/json/dto")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
			.body(body -> body.string().value("{\"message\":\"Hello, world!\"}"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(36), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_dto: Hello, world!", body);
			})
			.block();

		//curl -i -d '{"message":"Hello, world!"}{"message":"Hallo, welt!"}{"message":"Salut, monde!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto'
		endpoint
			.request(Method.POST, "/post_encoded/json/pub/dto")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
			.body(body -> body.string().value("{\"message\":\"Hello, world!\"}{\"message\":\"Hallo, welt!\"}{\"message\":\"Salut, monde!\"}"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(71), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_pub_dto: Hello, world!, Hallo, welt!, Salut, monde!, ", body);
			})
			.block();
		
		//curl -i -d '{"@type":"string", "message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto/generic'
		endpoint
			.request(Method.POST, "/post_encoded/json/dto/generic")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
			.body(body -> body.string().value("{\"@type\":\"string\", \"message\":\"Hello, world!\"}"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(44), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_dto_generic: Hello, world!", body);
			})
			.block();
		
		//curl -i -d '{"@type":"string","message":"Hello, world!"}{"@type":"integer","message":123456}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto/generic'
		endpoint
			.request(Method.POST, "/post_encoded/json/pub/dto/generic")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
			.body(body -> body.string().value("{\"@type\":\"string\",\"message\":\"Hello, world!\"}{\"@type\":\"integer\",\"message\":123456}"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(58), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_pub_dto_generic: Hello, world!, 123456, ", body);
			})
			.block();
		
		//curl -i -d '{"a":1, "b":2, "c":3}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/map'
		endpoint
			.request(Method.POST, "/post_encoded/json/map")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
			.body(body -> body.string().value("{\"a\":1, \"b\":2, \"c\":3}"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(38), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_map: {a=1, b=2, c=3}", body);
			})
			.block();
		
		//curl -i -d '{"a":1, "b":2, "c":3}{"d":4, "e":5, "f":6}{"g":7, "h":8, "i":9}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/map'
		endpoint
			.request(Method.POST, "/post_encoded/json/pub/map")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
			.body(body -> body.string().value("{\"a\":1, \"b\":2, \"c\":3}{\"d\":4, \"e\":5, \"f\":6}{\"g\":7, \"h\":8, \"i\":9}"))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(78), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_encoded_json_pub_map: {a=1, b=2, c=3}, {d=4, e=5, f=6}, {g=7, h=8, i=9}, ", body);
			})
			.block();
	}
	
	private void test_post_multipart(Endpoint endpoint) throws IOException {
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub'
		endpoint
			.request(Method.POST, "/post_multipart_pub")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(29), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub: a, b, c, ", body);
			})
			.block();
		
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/raw'
		endpoint
			.request(Method.POST, "/post_multipart_pub/raw")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub_raw: a = 1, b = 2, c = 3, ", body);
			})
			.block();
		
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/encoded'
		endpoint
			.request(Method.POST, "/post_multipart_pub/encoded")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(49), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub_encoded: a = 1, b = 2, c = 3, ", body);
			})
			.block();
		
		//curl -i --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
		endpoint
			.request(Method.POST, "/post_multipart_mono")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(22), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono: a", body);
			})
			.block();
		
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
		endpoint
			.request(Method.POST, "/post_multipart_mono")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(22), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono: a", body);
			})
			.block();
		
		//curl -i --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
		endpoint
			.request(Method.POST, "/post_multipart_mono/raw")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono_raw: a = 1, ", body);
			})
			.block();
		
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
		endpoint
			.request(Method.POST, "/post_multipart_mono/raw")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono_raw: a = 1, ", body);
			})
			.block();
		
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/encoded'
		endpoint
			.request(Method.POST, "/post_multipart_mono/encoded")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(34), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_mono_encoded: a = 1", body);
			})
			.block();
		
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux'
		endpoint
			.request(Method.POST, "/post_multipart_flux")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(30), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_flux: a, b, c, ", body);
			})
			.block();
		
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/raw'
		endpoint
			.request(Method.POST, "/post_multipart_flux/raw")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub_raw: a = 1, b = 2, c = 3, ", body);
			})
			.block();
		
		//curl -i --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/encoded'
		endpoint
			.request(Method.POST, "/post_multipart_flux/encoded")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(50), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_flux_encoded: a = 1, b = 2, c = 3, ", body);
			})
			.block();
		
		File uploadsDir = new File("target/uploads/");
		uploadsDir.mkdirs();
		
		//curl -i -F 'file=@src/test/resources/post_resource_small.txt' http://127.0.0.1:8080/upload
		new File(uploadsDir, "post_resource_small.txt").delete();
		endpoint
			.request(Method.POST, "/upload")
			.body(body -> body.multipart().from((factory, output) -> output.value(
				factory.resource(part -> part.name("file").value(new FileResource(new File("src/test/resources/post_resource_small.txt"))))
			)))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(Long.valueOf(55), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("Uploaded post_resource_small.txt(text/plain): " + new File("src/test/resources/post_resource_small.txt").length() + " Bytes\n", body);
			})
			.block();
		
		Assertions.assertArrayEquals(Files.readAllBytes(Path.of("src/test/resources/post_resource_small.txt")), Files.readAllBytes(Path.of("target/uploads/post_resource_small.txt")));
		
		//curl -i -F 'file=@src/test/resources/post_resource_big.txt' http://127.0.0.1:8080/upload
		new File(uploadsDir, "post_resource_big.txt").delete();
		endpoint
			.request(Method.POST, "/upload")
			.body(body -> body.multipart().from((factory, output) -> output.value(
				factory.resource(part -> part.name("file").value(new FileResource(new File("src/test/resources/post_resource_big.txt"))))
			)))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(Long.valueOf(58), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("Uploaded post_resource_big.txt(text/plain): " + new File("src/test/resources/post_resource_big.txt").length() + " Bytes\n", body);
			})
			.block();
		
		Assertions.assertArrayEquals(Files.readAllBytes(Path.of("src/test/resources/post_resource_big.txt")), Files.readAllBytes(Path.of("target/uploads/post_resource_big.txt")));
		// TODO the next request is test_sse, it seems the HTTP connection is not always in a proper state to proceed after this, we need to understand why... 
		// We probably release the exchange too early so the next exchange starts whereas the previous one is not yet finished
		// The state is ST_CONTENT_CHUNK, and we are uploading, could this be a netty issue?
		/*try {
			Thread.sleep(500);
		}
		catch(Exception e) {
			
		}*/
	}
	
	private void test_sse(Endpoint endpoint) throws IOException, InterruptedException {
		// curl -i 'http://127.0.0.1:8080/get_sse_raw'
		byte[] get_sse_raw = Files.readAllBytes(Path.of("src/test/resources/get_sse_raw.txt"));
		endpoint
			.request(Method.GET, "/get_sse_raw")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8", exchange.response().headers().getContentType());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertArrayEquals(get_sse_raw, body.getBytes(StandardCharsets.UTF_8));
			})
			.block();
		
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded'
		byte[] get_sse_encoded = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded.txt"));
		endpoint
			.request(Method.GET, "/get_sse_encoded")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8", exchange.response().headers().getContentType());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertArrayEquals(get_sse_encoded, body.getBytes(StandardCharsets.UTF_8));
			})
			.block();
		
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded/json'
		byte[] get_sse_encoded_json = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json.txt"));
		endpoint
			.request(Method.GET, "/get_sse_encoded/json")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8", exchange.response().headers().getContentType());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertArrayEquals(get_sse_encoded_json, body.getBytes(StandardCharsets.UTF_8));
			})
			.block();
		
		// curl -i 'http://127.0.0.1:8080/get_sse_encoded/json/map'
		byte[] get_sse_encoded_json_map = Files.readAllBytes(Path.of("src/test/resources/get_sse_encoded_json_map.txt"));
		endpoint
			.request(Method.GET, "/get_sse_encoded/json/map")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_EVENT_STREAM + ";charset=utf-8", exchange.response().headers().getContentType());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertArrayEquals(get_sse_encoded_json_map, body.getBytes(StandardCharsets.UTF_8));
			})
			.block();
	}
	
	private void test_resource(Endpoint endpoint) throws IOException, InterruptedException {
		//curl -i 'http://127.0.0.1:8080/get_resource'
		endpoint
			.request(Method.GET, "/get_resource")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("This is a test resource.", body);
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/get_resource_small.txt
		endpoint
			.request(Method.GET, "/static/get_resource_small.txt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("This is a test resource.", body);
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/get_resource_big.txt
		endpoint
			.request(Method.GET, "/static/get_resource_big.txt")
			.send()
			.flatMap(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(new File("src/test/resources/post_resource_big.txt").length(), exchange.response().headers().getContentLength());
				
				return Flux.from(exchange.response().body().raw().stream())
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
		
		// curl -i http://127.0.0.1:8080/static/some%20space.txt
		endpoint
			.request(Method.GET, "/static/some%20space.txt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(18), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("Space in file name", body);
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/some%2520space.txt
		endpoint
			.request(Method.GET, "/static/some%2520space.txt")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.NOT_FOUND, exchange.response().headers().getStatus());
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/dir/get_resource.txt
		endpoint
			.request(Method.GET, "/static/dir/get_resource.txt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("This is a test resource.", body);
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/dir%2Fget_resource.txt
		endpoint
			.request(Method.GET, "/static/dir%2Fget_resource.txt")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("This is a test resource.", body);
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/dir%252Fget_resource.txt
		endpoint
			.request(Method.GET, "/static/dir%252Fget_resource.txt")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.NOT_FOUND, exchange.response().headers().getStatus());
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/../pom.xml
		endpoint
			.request(Method.GET, "/static/../pom.xml")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.NOT_FOUND, exchange.response().headers().getStatus());
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/%2E%2E%2Fpom.xml
		endpoint
			.request(Method.GET, "/static/%2E%2E%2Fpom.xml")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.NOT_FOUND, exchange.response().headers().getStatus());
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/%252E%252E%252Fpom.xml
		endpoint
			.request(Method.GET, "/static/%252E%252E%252Fpom.xml")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.NOT_FOUND, exchange.response().headers().getStatus());
			})
			.block();

		// curl -i http://127.0.0.1:8080/static//pom.xml
		endpoint
			.request(Method.GET, "/static//pom.xml")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		// curl -i http://127.0.0.1:8080/static/%2Fpom.xml
		endpoint
			.request(Method.GET, "/static/%2Fpom.xml")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
	}
	
	private void test_misc(Endpoint endpoint) throws IOException, InterruptedException {
		//curl -i http://127.0.0.1:8080/get_path_param/qmark_1_
		endpoint
			.request(Method.GET, "/get_path_param/qmark_1_")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/qmark_1_", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/qmark_12_
		endpoint
			.request(Method.GET, "/get_path_param/qmark_12_")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.NOT_FOUND, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i http://127.0.0.1:8080/get_path_param/wcard__
		endpoint
			.request(Method.GET, "/get_path_param/wcard__")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(23), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/wcard__", body);
			})
			.block();

		//curl -i http://127.0.0.1:8080/get_path_param/wcard_1_
		endpoint
			.request(Method.GET, "/get_path_param/wcard_1_")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(24), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/wcard_1_", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/wcard_123456789_
		endpoint
			.request(Method.GET, "/get_path_param/wcard_123456789_")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(32), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/wcard_123456789_", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/directories
		endpoint
			.request(Method.GET, "/get_path_param/directories")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(27), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/directories/
		endpoint
			.request(Method.GET, "/get_path_param/directories/")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(28), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories/", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a
		endpoint
			.request(Method.GET, "/get_path_param/directories/a")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(29), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories/a", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a/b/
		endpoint
			.request(Method.GET, "/get_path_param/directories/a/b")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(31), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories/a/b", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/directories/a/b/c
		endpoint
			.request(Method.GET, "/get_path_param/directories/a/b/c")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(33), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/directories/a/b/c", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/
		endpoint
			.request(Method.GET, "/get_path_param/jsp")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.NOT_FOUND, exchange.response().headers().getStatus());
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/test.jsp
		endpoint
			.request(Method.GET, "/get_path_param/jsp/test.jsp")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(39), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/jsp/test.jsp - test.jsp", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/a/test.jsp
		endpoint
			.request(Method.GET, "/get_path_param/jsp/a/test.jsp")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(41), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/jsp/a/test.jsp - test.jsp", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/jsp/a/b/test.jsp
		endpoint
			.request(Method.GET, "/get_path_param/jsp/a/b/test.jsp")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(43), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/jsp/a/b/test.jsp - test.jsp", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/terminal
		endpoint
			.request(Method.GET, "/get_path_param/terminal")
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.NOT_FOUND, exchange.response().headers().getStatus());
			})
			.block();

		//curl -i http://127.0.0.1:8080/get_path_param/terminal/
		endpoint
			.request(Method.GET, "/get_path_param/terminal/")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(25), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/terminal/", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a
		endpoint
			.request(Method.GET, "/get_path_param/terminal/a")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(26), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/terminal/a", body);
			})
			.block();
		
		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a/b/
		endpoint
			.request(Method.GET, "/get_path_param/terminal/a/b/")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(29), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/terminal/a/b/", body);
			})
			.block();

		//curl -i http://127.0.0.1:8080/get_path_param/terminal/a/b/c
		endpoint
			.request(Method.GET, "/get_path_param/terminal/a/b/c")
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(30), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("/get_path_param/terminal/a/b/c", body);
			})
			.block();
	}
	
	private void test_fail(Endpoint endpoint) {
		/*endpoint
			.request(Method.POST, "/post/formParam")
			.headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED))
			// TODO setting an empty publisher seems to stuck the process
			.body(body -> body.urlEncoded().from((factory, data) -> data.stream(Mono.empty())))
			.send()
			.doOnNext(exchange -> {
				Assertions.assertEquals(Status.BAD_REQUEST, exchange.response().headers().getStatus());
			})
			.block();
		
		endpoint
			.request(Method.POST, "/post_multipart_pub/raw")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part.name("a").value("1")),
				factory.string(part -> part.name("b").value("2")),
				factory.string(part -> part.name("c").value("3"))
			))))
			.send()
			.flatMapMany(exchange -> {
				Assertions.assertEquals(Status.OK, exchange.response().headers().getStatus());
				Assertions.assertEquals(MediaTypes.TEXT_PLAIN, exchange.response().headers().getContentType());
				Assertions.assertEquals(Long.valueOf(45), exchange.response().headers().getContentLength());
				
				return exchange.response().body().string().stream();
			})
			.collect(Collectors.joining())
			.doOnNext(body -> {
				Assertions.assertEquals("post_multipart_pub_raw: a = 1, b = 2, c = 3, ", body);
			})
			.block();*/
	}
}
