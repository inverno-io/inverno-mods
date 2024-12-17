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

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.boot.Boot;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Client;
import io.inverno.mod.http.client.ConnectionResetException;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.RequestTimeoutException;
import io.inverno.mod.test.AbstractInvernoModTest;
import io.inverno.mod.test.ModsTestUtils;
import io.inverno.mod.test.configuration.ConfigurationInvocationHandler;
import io.inverno.test.InvernoCompilationException;
import io.inverno.test.InvernoModuleLoader;
import io.inverno.test.InvernoModuleProxy;
import io.inverno.test.InvernoTestCompiler;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.Duration;
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
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class HttpClientSpecificTest {
	
	static {
		System.setProperty("log4j2.simplelogLevel", "INFO");
		System.setProperty("log4j2.simplelogLogFile", "system.out");
//		System.setProperty("io.netty.leakDetection.level", "PARANOID");
//		System.setProperty("io.netty.leakDetection.targetRecords", "20");
	}
	
	private static final String MODULE_WEBROUTE = "io.inverno.mod.test.web.webroute";
	
	private static int testServerPort;
	private static InvernoModuleProxy testServerModuleProxy;
	
	private static Boot bootModule;
	private static Client httpClientModule;
	
	@BeforeAll
	public static void init() throws IOException, InvernoCompilationException, ClassNotFoundException, InterruptedException {
		InvernoTestCompiler invernoCompiler = InvernoTestCompiler.builder()
			.moduleOverride(AbstractInvernoModTest.MODULE_OVERRIDE)
			.annotationProcessorModuleOverride(AbstractInvernoModTest.ANNOTATION_PROCESSOR_MODULE_OVERRIDE)
			.build();
		
		invernoCompiler.cleanModuleTarget();
		
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
		
		bootModule = new Boot.Builder().build();
		bootModule.start();
		
		httpClientModule = new Client.Builder(bootModule.netService(), bootModule.reactor(), bootModule.resourceService()).build();
		httpClientModule.start();
	}
	
	@AfterAll
	public static void destroy() {
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

	@Test
	public void test_interceptor() {
		AtomicBoolean interceptorFlag = new AtomicBoolean(false);
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
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
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
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
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
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
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
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
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
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
						.body().string().value("a,b,c");
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
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
			.interceptor(exchange -> {
				if(exchange.request().getMethod().isBodyAllowed()) {
					exchange.request().body().transform(data -> Flux.from(data)
						.doOnNext(buf -> interceptedRequestBody.append(buf.toString(Charsets.UTF_8)))
					);
				}

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
						.body().string().value("a,b,c");
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
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
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
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
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
	
	public static Stream<Arguments> provideEndpointAndHttpVersion() {
		Endpoint<ExchangeContext> h11Endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
			))
			.build();
		
		Endpoint<ExchangeContext> h2Endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_2_0))
			))
			.build();
		
		return Stream.of(
			Arguments.of(h11Endpoint, HttpVersion.HTTP_1_1),
			Arguments.of(h2Endpoint, HttpVersion.HTTP_2_0)
		);
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
				.http_protocol_versions(Set.of(HttpVersion.HTTP_2_0))
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
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(200));

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
					
					// Must be a ConnectionResetException
					result = results.get(1);
					Assertions.assertEquals(
						"Connection closed after previous request timed out",
						Assertions.assertInstanceOf(ConnectionResetException.class, result).getMessage()
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
	
	@Test
	public void test_http1x_responding_requesting_headers_written_request_timeout() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(1000)
			))
			.build();
		
		try {
			Mono<Object> timeoutRequest = endpoint
				.exchange(Method.POST, "/post_timeout")
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType("text/plain"))
						.body().string().stream(Flux.concat(Flux.just("a", "b"), Mono.just("timeout").delayElement(Duration.ofSeconds(2))));
					return exchange.response();
				})
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e));
			
			Mono<Object> noTimeoutRequest = endpoint
				.exchange(Method.GET, "/get_delay100")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(200));
			
			List<Object> results = Flux.merge(timeoutRequest, noTimeoutRequest)
				.collectList()
				.block();
			
			Assertions.assertEquals(2, results.size());
			
			Object result = results.get(0);
			Assertions.assertEquals(
				"Exceeded timeout 1000ms",
				Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
			);

			result = results.get(1);
			Assertions.assertEquals(
				"Connection closed after previous request timed out",
				Assertions.assertInstanceOf(ConnectionResetException.class, result).getMessage()
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_http1x_responding_requesting_headers_not_written_request_timeout() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(1000)
			))
			.build();
		
		try {
			Mono<Object> timeoutRequest = endpoint
				.exchange(Method.POST, "/post_timeout")
				.flatMap(exchange -> {
					exchange.request().body().string().stream(Mono.just("timeout").delayElement(Duration.ofSeconds(2)));
					return exchange.response();
				})
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e));
			
			Mono<Object> noTimeoutRequest = endpoint
				.exchange(Method.GET, "/get_delay100")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(200));
			
			
			List<Object> results = Flux.merge(timeoutRequest, noTimeoutRequest)
				.collectList()
				.block();
			
			Assertions.assertEquals(2, results.size());
			
			Object result = results.get(0);
			Assertions.assertEquals(
				"Exceeded timeout 1000ms",
				Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
			);

			// Must be a RequestTimeoutException as well (the same actually)
			result = results.get(1);
			Assertions.assertEquals(
				"get_delay100",
				result
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_http1x_responding_not_requesting_request_timeout() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(1000)
			))
			.build();
		
		try {
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
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(200));

			List<Object> results = Flux.merge(timeoutRequest, noTimeoutRequest)
				.collectList()
				.block();

			Assertions.assertEquals(2, results.size());

			Object result = results.get(0);
			Assertions.assertEquals(
				"Exceeded timeout 1000ms",
				Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
			);

			// Must be a ConnectionResetException
			result = results.get(1);
			Assertions.assertEquals(
				"Connection closed after previous request timed out",
				Assertions.assertInstanceOf(ConnectionResetException.class, result).getMessage()
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_http1x_not_responding_requesting_headers_written_request_timeout() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(1000)
			))
			.build();
		
		try {
			Mono<Object> longRequestWithResponse = endpoint
				.exchange(Method.GET, "/get_timeout_with_response")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e));

			Mono<Object> timeoutRequest = endpoint
				.exchange(Method.POST, "/post_timeout")
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType("text/plain"))
						.body().string().stream(Flux.concat(Flux.just("a", "b"), Mono.just("timeout").delayElement(Duration.ofSeconds(2))));
					return exchange.response();
				})
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(200));
			
			Mono<Object> getRaw = endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(1200));

			List<Object> results = Flux.mergeSequential(longRequestWithResponse, timeoutRequest, getRaw)
				.collectList()
				.block();

			Assertions.assertEquals(3, results.size());

			Object result = results.get(0);
			Assertions.assertEquals(
				"abget_timeout_with_response",
				result
			);

			// Must be a RequestTimeoutException
			result = results.get(1);
			Assertions.assertEquals(
				"Exceeded timeout 1000ms",
				Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
			);
			
			result = results.get(2);
			Assertions.assertEquals(
				"Connection closed after previous request timed out",
				Assertions.assertInstanceOf(ConnectionResetException.class, result).getMessage()
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_http1x_not_responding_requesting_headers_not_written_request_timeout() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(1000)
			))
			.build();
		
		try {
			Mono<Object> longRequestWithResponse = endpoint
				.exchange(Method.GET, "/get_timeout_with_response")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e));

			Mono<Object> timeoutRequest = endpoint
				.exchange(Method.POST, "/post_timeout")
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType("text/plain"))
						.body().string().stream(Mono.just("timeout").delayElement(Duration.ofSeconds(2)));
					return exchange.response();
				})
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(500));
			
			Mono<Object> getRaw = endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(1300));

			List<Object> results = Flux.mergeSequential(longRequestWithResponse, timeoutRequest, getRaw)
				.collectList()
				.block();

			Assertions.assertEquals(3, results.size());

			Object result = results.get(0);
			Assertions.assertEquals(
				"abget_timeout_with_response",
				result
			);

			// Must be a RequestTimeoutException
			result = results.get(1);
			Assertions.assertEquals(
				"Exceeded timeout 1000ms",
				Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
			);
			
			result = results.get(2);
			Assertions.assertEquals(
				"get_raw",
				result
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_http1x_not_responding_not_requesting_request_timeout() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(1000)
			))
			.build();
		
		try {
			Mono<Object> longRequestWithResponse = endpoint
				.exchange(Method.GET, "/get_timeout_with_response")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e));

			Mono<Object> timeoutRequest = endpoint
				.exchange(Method.GET, "/get_delay100")
				.flatMap(Exchange::response)
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(200));
			
			Mono<Object> getRaw = endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(1200));

			List<Object> results = Flux.mergeSequential(longRequestWithResponse, timeoutRequest, getRaw)
				.collectList()
				.block();

			Assertions.assertEquals(3, results.size());

			Object result = results.get(0);
			Assertions.assertEquals(
				"abget_timeout_with_response",
				result
			);

			// Must be a RequestTimeoutException
			result = results.get(1);
			Assertions.assertEquals(
				"Exceeded timeout 1000ms",
				Assertions.assertInstanceOf(RequestTimeoutException.class, result).getMessage()
			);
			
			result = results.get(2);
			Assertions.assertEquals(
				"Connection closed after previous request timed out",
				Assertions.assertInstanceOf(ConnectionResetException.class, result).getMessage()
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_http1x_interupted_request() {
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(10000)
			))
			.build();
		
		try {
			Mono<Object> interuptedRequest = endpoint
				.exchange(Method.POST, "/post_ignore_body")
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType("text/plain"))
						.body().string().stream(Flux.concat(Flux.just("a", "b"), Mono.just("timeout").delayElement(Duration.ofSeconds(2))).delaySubscription(Duration.ofMillis(200)));
					return exchange.response();
				})
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e));
			
			Mono<Object> getRaw = endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.cast(Object.class)
				.onErrorResume(e -> Mono.just(e))
				.delaySubscription(Duration.ofMillis(100));

			List<Object> results = Flux.mergeSequential(interuptedRequest, getRaw)
				.collectList()
				.block();

			Assertions.assertEquals(2, results.size());

			Object result = results.get(0);
			Assertions.assertEquals(
				"get_timeout_with_response",
				result
			);

			// Must be a RequestTimeoutException
			result = results.get(1);
			Assertions.assertEquals(
				"get_raw",
				result
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
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
					exchange.request().body().multipart().from((factory, output) -> output.value(
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
	
	@ParameterizedTest
	@MethodSource("provideEndpointAndHttpVersion")
	public void test_expect_100_continue(Endpoint<ExchangeContext> endpoint, HttpVersion testHttpVersion) {
		try {
			String result = endpoint
				.exchange(Method.POST, "/post_100_continue")
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers
							.set(Headers.NAME_EXPECT, Headers.VALUE_100_CONTINUE)
							.contentLength(17)
						)
						.body().string().value("post_100_continue");
					return exchange.response();
				})
				.flatMap(response -> Flux.from(response.body().string().stream()).collect(Collectors.joining()))
				.block();
			
			Assertions.assertEquals(
				"post_100_continue",
				result
			);
		}
		finally {
			endpoint.shutdown().block();
		}
	}
	
	@Test
	public void test_http1x_proxy() {
		int proxyPort = ModsTestUtils.getFreePort();
		
		Endpoint<ExchangeContext> endpoint = httpClientModule.httpClient().endpoint("127.0.0.1", testServerPort)
			.configuration(HttpClientConfigurationLoader.load(conf -> conf
				.http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
				.pool_max_size(1)
				.request_timeout(10000)
				.proxy_host("127.0.0.1")
				.proxy_port(proxyPort)
			))
			.build();
		
		DummyHttpProxyServer dummyProxyServer = new DummyHttpProxyServer(proxyPort);
		dummyProxyServer.start();
		
		try {
			String result = endpoint
				.exchange(Method.GET, "/get_raw")
				.flatMap(Exchange::response)
				.flatMapMany(response -> response.body().string().stream())
				.collect(Collectors.joining())
				.block();
			
			Assertions.assertEquals("get_raw", result);
			Assertions.assertTrue(dummyProxyServer.isClientConnected());
			
		}
		finally {
			dummyProxyServer.stop();
			endpoint.shutdown().block();
		}
	}
}
