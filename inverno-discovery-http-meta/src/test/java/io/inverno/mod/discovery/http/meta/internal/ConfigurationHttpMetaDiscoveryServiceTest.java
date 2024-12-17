/*
 * Copyright 2024 Jeremy KUHN
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
package io.inverno.mod.discovery.http.meta.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.boot.json.InvernoBaseModule;
import io.inverno.mod.configuration.source.MapConfigurationSource;
import io.inverno.mod.discovery.ManageableService;
import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.meta.HttpMetaDiscoveryConfigurationLoader;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.InboundResponseHeaders;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.client.ExchangeInterceptor;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.InterceptedExchange;
import io.inverno.mod.http.client.InterceptedResponse;
import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.UnboundExchange;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ConfigurationHttpMetaDiscoveryServiceTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder()
		.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
		.addModules(new Jdk8Module(), new InvernoBaseModule())
		.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
		.build();

	private static UnboundExchange<ExchangeContext> mockExchange(String requestTarget) {
		return mockExchange(requestTarget, null);
	}

	@SuppressWarnings("unchecked")
	private static UnboundExchange<ExchangeContext> mockExchange(String requestTarget, String authority) {
		URIBuilder pathBuilder = URIs.uri(requestTarget, false, URIs.Option.NORMALIZED);

		InboundOutboundRequestHeaders requestHeaders = Mockito.mock(InboundOutboundRequestHeaders.class);

		Request request = Mockito.mock(Request.class);
		if(authority != null) {
			Mockito.when(request.getAuthority()).thenReturn(authority);
		}
		Mockito.when(request.getPathBuilder()).thenReturn(pathBuilder);
		Mockito.when(request.getPathAbsolute()).thenReturn(pathBuilder.buildRawPath());
		Mockito.when(request.getQuery()).thenReturn(pathBuilder.buildRawQuery());
		Mockito.when(request.headers()).thenReturn(requestHeaders);
		Mockito.when(request.headers(Mockito.any())).thenAnswer(invocation -> {
			((Consumer<OutboundRequestHeaders>)invocation.getArgument(0)).accept(requestHeaders);
			return request;
		});

		UnboundExchange<ExchangeContext> exchange = Mockito.mock(UnboundExchange.class);
		Mockito.when(exchange.request()).thenReturn(request);

		return exchange;
	}

	public interface InboundOutboundRequestHeaders extends InboundRequestHeaders, OutboundRequestHeaders {

	}

	public interface InboundOutboundResponseHeaders extends InboundResponseHeaders, OutboundResponseHeaders {

	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_single_destination_as_uri() {
		UnboundExchange<ExchangeContext> exchange = mockExchange("/resource?key=value");

		ServiceID destinationServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destinationServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destinationService = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destinationServiceInstance)).when(destinationService).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destinationService)).when(discoveryService).resolve(Mockito.eq(destinationServiceID), Mockito.any());

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				"http://localhost:8080/path/to?base_key=base_value"
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test")).block();

		Assertions.assertEquals(destinationServiceInstance, metaService.getInstance(exchange).block());
		Mockito.verify(exchange.request()).path("/path/to/resource?key=value&base_key=base_value");

		Mockito.verify(discoveryService).resolve(Mockito.eq(destinationServiceID), Mockito.any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_many_destinations_as_uri() {
		UnboundExchange<ExchangeContext> exchange = mockExchange("/resource?key=value");

		ServiceID destination1ServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destination1ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination1Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination1ServiceInstance)).when(destination1Service).getInstance(Mockito.any());

		ServiceID destination2ServiceID = ServiceID.of("http://localhost:8081");
		HttpServiceInstance destination2ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination2Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination2ServiceInstance)).when(destination2Service).getInstance(Mockito.any());

		ServiceID destination3ServiceID = ServiceID.of("http://localhost:8082");
		HttpServiceInstance destination3ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination3Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination3ServiceInstance)).when(destination3Service).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destination1Service)).when(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination2Service)).when(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination3Service)).when(discoveryService).resolve(Mockito.eq(destination3ServiceID), Mockito.any());

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				"[\"http://localhost:8080/?otherKey=otherValue\", \"http://localhost:8081/path\", \"http://localhost:8082/path/to\"]"
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test"), HttpTrafficPolicy.builder().roundRobinLoadBalancer().build()).block();

		// Using ROUND_ROBIN route load balancing strategy guarantees that destination are load balanced in a deterministic sequence
		// Considering 6 requests, each destination is returned exactly twice
		int destination1Count = 0;
		int destination2Count = 0;
		int destination3Count = 0;
		for(int i=0;i<6;i++) {
			HttpServiceInstance instance = metaService.getInstance(exchange).block();
			Assertions.assertNotNull(instance);
			if(instance.equals(destination1ServiceInstance)) {
				destination1Count++;
				Mockito.verify(exchange.request()).path("/resource?key=value&otherKey=otherValue");
			}
			else if(instance.equals(destination2ServiceInstance)) {
				destination2Count++;
				Mockito.verify(exchange.request()).path("/path/resource?key=value");
			}
			else if(instance.equals(destination3ServiceInstance)) {
				destination3Count++;
				Mockito.verify(exchange.request()).path("/path/to/resource?key=value");
			}
			Mockito.clearInvocations(exchange.request());
		}
		Assertions.assertEquals(2, destination1Count);
		Assertions.assertEquals(2, destination2Count);
		Assertions.assertEquals(2, destination3Count);

		Mockito.verify(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.verify(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
		Mockito.verify(discoveryService).resolve(Mockito.eq(destination3ServiceID), Mockito.any());
	}

	@Test
	public void test_many_destinations() {
		UnboundExchange<ExchangeContext> exchange = mockExchange("/resource?key=value");

		ServiceID destination1ServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destination1ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination1Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination1ServiceInstance)).when(destination1Service).getInstance(Mockito.any());

		ServiceID destination2ServiceID = ServiceID.of("http://localhost:8081");
		HttpServiceInstance destination2ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination2Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination2ServiceInstance)).when(destination2Service).getInstance(Mockito.any());

		ServiceID destination3ServiceID = ServiceID.of("http://localhost:8082");
		HttpServiceInstance destination3ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination3Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination3ServiceInstance)).when(destination3Service).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destination1Service)).when(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination2Service)).when(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination3Service)).when(discoveryService).resolve(Mockito.eq(destination3ServiceID), Mockito.any());

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				"[{\"uri\":\"http://localhost:8080/?otherKey=otherValue\",\"weight\":1}, {\"uri\":\"http://localhost:8081/path\",\"weight\":2}, {\"uri\":\"http://localhost:8082/path/to\",\"weight\":3}]"
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test"), HttpTrafficPolicy.builder().roundRobinLoadBalancer().build()).block();

		// Using ROUND_ROBIN route load balancing strategy guarantees that destination are load balanced in a deterministic sequence
		// Considering 12 requests, destination1 is returned twice, destination2 4 times and destination3 6 times
		int destination1Count = 0;
		int destination2Count = 0;
		int destination3Count = 0;
		for(int i=0;i<12;i++) {
			HttpServiceInstance instance = metaService.getInstance(exchange).block();
			Assertions.assertNotNull(instance);
			if(instance.equals(destination1ServiceInstance)) {
				destination1Count++;
				Mockito.verify(exchange.request()).path("/resource?key=value&otherKey=otherValue");
			}
			else if(instance.equals(destination2ServiceInstance)) {
				destination2Count++;
				Mockito.verify(exchange.request()).path("/path/resource?key=value");
			}
			else if(instance.equals(destination3ServiceInstance)) {
				destination3Count++;
				Mockito.verify(exchange.request()).path("/path/to/resource?key=value");
			}
			Mockito.clearInvocations(exchange.request());
		}
		Assertions.assertEquals(2, destination1Count);
		Assertions.assertEquals(4, destination2Count);
		Assertions.assertEquals(6, destination3Count);

		Mockito.verify(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.verify(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
		Mockito.verify(discoveryService).resolve(Mockito.eq(destination3ServiceID), Mockito.any());
	}

	@Test
	public void test_single_route_single_destination() throws JsonProcessingException {
		UnboundExchange<ExchangeContext> matchingExchange = mockExchange("/path");
		UnboundExchange<ExchangeContext> unmatchingExchange = mockExchange("/otherPath");

		ServiceID destinationServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destinationServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destinationService = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destinationServiceInstance)).when(destinationService).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destinationService)).when(discoveryService).resolve(Mockito.eq(destinationServiceID), Mockito.any());

		HttpMetaServiceDescriptor serviceDescriptor = new HttpMetaServiceDescriptor(
			null,
			null,
			List.of(
					new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					null,
					Set.of(new HttpMetaServiceDescriptor.PathMatcher("/path", false)),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8080"),
							null,
							null,
							null
						)
					)
				)
			)
		);

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				MAPPER.writeValueAsString(serviceDescriptor)
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test"), HttpTrafficPolicy.builder().roundRobinLoadBalancer().build()).block();

		Assertions.assertEquals(destinationServiceInstance, metaService.getInstance(matchingExchange).block());
		Assertions.assertNull(metaService.getInstance(unmatchingExchange).block());

		Mockito.verify(discoveryService).resolve(Mockito.eq(destinationServiceID), Mockito.any());
	}

	@Test
	public void test_single_route_many_destinations() throws JsonProcessingException {
		UnboundExchange<ExchangeContext> matchingExchange = mockExchange("/path");
		UnboundExchange<ExchangeContext> unmatchingExchange = mockExchange("/otherPath");

		ServiceID destination1ServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destination1ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination1Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination1ServiceInstance)).when(destination1Service).getInstance(Mockito.any());

		ServiceID destination2ServiceID = ServiceID.of("http://localhost:8081");
		HttpServiceInstance destination2ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination2Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination2ServiceInstance)).when(destination2Service).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destination1Service)).when(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination2Service)).when(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());

		HttpMetaServiceDescriptor serviceDescriptor = new HttpMetaServiceDescriptor(
			null,
			null,
			List.of(
				new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					null,
					Set.of(new HttpMetaServiceDescriptor.PathMatcher("/path", false)),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8080"),
							null,
							null,
							null
						),
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8081"),
							null,
							null,
							null
						)
					)
				)
			)
		);

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				MAPPER.writeValueAsString(serviceDescriptor)
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test"), HttpTrafficPolicy.builder().roundRobinLoadBalancer().build()).block();

		int destination1Count = 0;
		int destination2Count = 0;
		for(int i=0;i<4;i++) {
			HttpServiceInstance instance = metaService.getInstance(matchingExchange).block();
			Assertions.assertNotNull(instance);
			if(instance.equals(destination1ServiceInstance)) {
				destination1Count++;
			}
			else if(instance.equals(destination2ServiceInstance)) {
				destination2Count++;
			}
		}
		Assertions.assertEquals(2, destination1Count);
		Assertions.assertEquals(2, destination2Count);

		Assertions.assertNull(metaService.getInstance(unmatchingExchange).block());

		Mockito.verify(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.verify(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
	}

	@Test
	public void test_many_routes() throws JsonProcessingException {
		UnboundExchange<ExchangeContext> exchange1 = mockExchange("/path", "authority");

		ServiceID destination1ServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destination1ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination1Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination1ServiceInstance)).when(destination1Service).getInstance(Mockito.any());

		ServiceID destination2ServiceID = ServiceID.of("http://localhost:8081");
		HttpServiceInstance destination2ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination2Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination2ServiceInstance)).when(destination2Service).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destination1Service)).when(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination2Service)).when(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());

		HttpMetaServiceDescriptor serviceDescriptor = new HttpMetaServiceDescriptor(
			null,
			null,
			List.of(
				new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					Set.of(new HttpMetaServiceDescriptor.StaticValueMatcher("authority")),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8080"),
							null,
							null,
							null
						)
					)
				),
				new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8081"),
							null,
							null,
							null
						)
					)
				)
			)
		);

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				MAPPER.writeValueAsString(serviceDescriptor)
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test")).block();

		Assertions.assertEquals(destination1ServiceInstance, metaService.getInstance(exchange1).block());
		Assertions.assertEquals(destination1ServiceInstance, metaService.getInstance(exchange1).block());

		Assertions.assertEquals(destination2ServiceInstance, metaService.getInstance(mockExchange("/path", "other")).block());
		Assertions.assertEquals(destination2ServiceInstance, metaService.getInstance(mockExchange("/path", "other again")).block());

		Mockito.verify(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.verify(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_load_balancer_managed() throws JsonProcessingException {
		UnboundExchange<ExchangeContext> exchange = mockExchange("/resource");

		ServiceID destination1ServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destination1ServiceInstance1 = Mockito.mock(HttpServiceInstance.class);
		HttpServiceInstance destination1ServiceInstance2 = Mockito.mock(HttpServiceInstance.class);
		ManageableService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination1Service = Mockito.mock(ManageableService.class);
		Mockito.when(destination1Service.getInstances()).thenReturn(List.of(destination1ServiceInstance1, destination1ServiceInstance2));

		ServiceID destination2ServiceID = ServiceID.of("http://localhost:8081");
		HttpServiceInstance destination2ServiceInstance1 = Mockito.mock(HttpServiceInstance.class);
		HttpServiceInstance destination2ServiceInstance2 = Mockito.mock(HttpServiceInstance.class);
		ManageableService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination2Service = Mockito.mock(ManageableService.class);
		Mockito.when(destination2Service.getInstances()).thenReturn(List.of(destination2ServiceInstance1, destination2ServiceInstance2));

		ServiceID destination3ServiceID = ServiceID.of("http://localhost:8082");
		HttpServiceInstance destination3ServiceInstance1 = Mockito.mock(HttpServiceInstance.class);
		HttpServiceInstance destination3ServiceInstance2 = Mockito.mock(HttpServiceInstance.class);
		ManageableService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination3Service = Mockito.mock(ManageableService.class);
		Mockito.when(destination3Service.getInstances()).thenReturn(List.of(destination3ServiceInstance1, destination3ServiceInstance2));

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destination1Service)).when(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination2Service)).when(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination3Service)).when(discoveryService).resolve(Mockito.eq(destination3ServiceID), Mockito.any());

		HttpMetaServiceDescriptor serviceDescriptor = new HttpMetaServiceDescriptor(
			null,
			null,
			List.of(
				new HttpMetaServiceDescriptor.RouteDescriptor(
					new HttpMetaServiceDescriptor.HttpClientConfiguration(),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8080"),
							1,
							null,
							null
						),
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8081"),
							2,
							null,
							null
						),
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8082"),
							3,
							null,
							null
						)
					)
				)
			)
		);

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				MAPPER.writeValueAsString(serviceDescriptor)
			)),
			MAPPER,
			List.of(discoveryService)
		);

		HttpTrafficPolicy trafficPolicy = HttpTrafficPolicy.builder().roundRobinLoadBalancer().build();
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test"), trafficPolicy).block();
		Assertions.assertNotNull(metaService);

		// 1*2 instances of 1
		// 2*2 instances of 2
		// 3*2 instances of 3
		// 12 instances total

		Mockito.clearInvocations(exchange);
		for(int i=0;i<24;i++) {
			metaService.getInstance(exchange).doOnNext(instance -> instance.bind(exchange)).block();
		}

		Mockito.verify(destination1ServiceInstance1, Mockito.times(2)).bind(exchange);
		Mockito.verify(destination1ServiceInstance2, Mockito.times(2)).bind(exchange);
		Mockito.verify(destination2ServiceInstance1, Mockito.times(4)).bind(exchange);
		Mockito.verify(destination2ServiceInstance2, Mockito.times(4)).bind(exchange);
		Mockito.verify(destination3ServiceInstance1, Mockito.times(6)).bind(exchange);
		Mockito.verify(destination3ServiceInstance2, Mockito.times(6)).bind(exchange);

		Mockito.verify(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.verify(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
		Mockito.verify(discoveryService).resolve(Mockito.eq(destination3ServiceID), Mockito.any());
	}

	@Test
	public void test_client_configuration() throws JsonProcessingException {
		UnboundExchange<ExchangeContext> exchange1 = mockExchange("/resource1");
		UnboundExchange<ExchangeContext> exchange2 = mockExchange("/resource2");
		UnboundExchange<ExchangeContext> exchange3 = mockExchange("/resource3");
		UnboundExchange<ExchangeContext> exchange4 = mockExchange("/resource4");

		ServiceID destination1ServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destination1ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination1Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination1ServiceInstance)).when(destination1Service).getInstance(Mockito.any());

		ServiceID destination2ServiceID = ServiceID.of("http://localhost:8081");
		HttpServiceInstance destination2ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination2Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination2ServiceInstance)).when(destination2Service).getInstance(Mockito.any());
		
		ServiceID destination3ServiceID = ServiceID.of("http://localhost:8082");
		HttpServiceInstance destination3ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination3Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination3ServiceInstance)).when(destination3Service).getInstance(Mockito.any());
		
		ServiceID destination4ServiceID = ServiceID.of("http://localhost:8083");
		HttpServiceInstance destination4ServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destination4Service = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destination4ServiceInstance)).when(destination4Service).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destination1Service)).when(discoveryService).resolve(Mockito.eq(destination1ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination2Service)).when(discoveryService).resolve(Mockito.eq(destination2ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination3Service)).when(discoveryService).resolve(Mockito.eq(destination3ServiceID), Mockito.any());
		Mockito.doReturn(Mono.just(destination4Service)).when(discoveryService).resolve(Mockito.eq(destination4ServiceID), Mockito.any());

		// 4 route:
		// - configuration from original traffic policy + service level
		// - configuration from original traffic policy + service level + route level
		// - configuration from original traffic policy + service level + destination level
		// - configuration from original traffic policy + service level + route level + destination level

		HttpMetaServiceDescriptor.HttpClientConfiguration serviceConfiguration = new HttpMetaServiceDescriptor.HttpClientConfiguration();
		serviceConfiguration.setProxy_host("proxy-host");
		HttpMetaServiceDescriptor.HttpClientConfiguration routeConfiguration = new HttpMetaServiceDescriptor.HttpClientConfiguration();
		routeConfiguration.setProxy_username("proxy_username");
		HttpMetaServiceDescriptor.HttpClientConfiguration destinationConfiguration = new HttpMetaServiceDescriptor.HttpClientConfiguration();
		destinationConfiguration.setProxy_password("proxy_password");

		HttpTrafficPolicy trafficPolicy = HttpTrafficPolicy.builder().configuration(HttpClientConfigurationLoader.load(conf -> conf.user_agent("traffic policy"))).build();
		HttpTrafficPolicy trafficPolicy1 = HttpTrafficPolicy.builder().configuration(HttpClientConfigurationLoader.load(trafficPolicy.getConfiguration(), conf -> conf.proxy_host("proxy-host"))).build();
		HttpTrafficPolicy trafficPolicy2 = HttpTrafficPolicy.builder().configuration(HttpClientConfigurationLoader.load(trafficPolicy1.getConfiguration(), conf -> conf.proxy_username("proxy_username"))).build();
		HttpTrafficPolicy trafficPolicy3 = HttpTrafficPolicy.builder().configuration(HttpClientConfigurationLoader.load(trafficPolicy1.getConfiguration(), conf -> conf.proxy_password("proxy_password"))).build();
		HttpTrafficPolicy trafficPolicy4 = HttpTrafficPolicy.builder().configuration(HttpClientConfigurationLoader.load(trafficPolicy2.getConfiguration(), conf -> conf.proxy_password("proxy_password"))).build();

		HttpMetaServiceDescriptor serviceDescriptor = new HttpMetaServiceDescriptor(
			serviceConfiguration,
			null,
			List.of(
				new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					null,
					Set.of(new HttpMetaServiceDescriptor.PathMatcher("/resource1", false)),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8080"),
							1,
							null,
							null
						)
					)
				),
				new HttpMetaServiceDescriptor.RouteDescriptor(
					routeConfiguration,
					null,
					null,
					Set.of(new HttpMetaServiceDescriptor.PathMatcher("/resource2", false)),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8081"),
							1,
							null,
							null
						)
					)
				),
				new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					null,
					Set.of(new HttpMetaServiceDescriptor.PathMatcher("/resource3", false)),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							destinationConfiguration,
							null,
							URI.create("http://localhost:8082"),
							1,
							null,
							null
						)
					)
				),
				new HttpMetaServiceDescriptor.RouteDescriptor(
					routeConfiguration,
					null,
					null,
					Set.of(new HttpMetaServiceDescriptor.PathMatcher("/resource4", false)),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							destinationConfiguration,
							null,
							URI.create("http://localhost:8083"),
							1,
							null,
							null
						)
					)
				)
			)
		);

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				MAPPER.writeValueAsString(serviceDescriptor)
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test"), trafficPolicy).block();
		Assertions.assertNotNull(metaService);

		Mockito.verify(discoveryService).resolve(destination1ServiceID, trafficPolicy1);
		Mockito.verify(discoveryService).resolve(destination2ServiceID, trafficPolicy2);
		Mockito.verify(discoveryService).resolve(destination3ServiceID, trafficPolicy3);
		Mockito.verify(discoveryService).resolve(destination4ServiceID, trafficPolicy4);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_path_rewrite() throws JsonProcessingException {
		UnboundExchange<ExchangeContext> matchingExchange = mockExchange("/v1/path/to/resource?key=value");
		UnboundExchange<ExchangeContext> unmatchingExchange = mockExchange("/otherPath");

		ServiceID destinationServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destinationServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destinationService = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destinationServiceInstance)).when(destinationService).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destinationService)).when(discoveryService).resolve(Mockito.eq(destinationServiceID), Mockito.any());

		HttpMetaServiceDescriptor serviceDescriptor = new HttpMetaServiceDescriptor(
			null,
			null,
			List.of(
				new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8080"),
							null,
							new HttpMetaServiceDescriptor.RequestTransformer(
								Map.of(
									"/{version}/{path:**}", "/{path:**}"
								),
								null,
								null,
								null,
								null
							),
							null
						)
					)
				)
			)
		);

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				MAPPER.writeValueAsString(serviceDescriptor)
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test")).block();
		Assertions.assertNotNull(metaService);

		Assertions.assertEquals(destinationServiceInstance, metaService.getInstance(matchingExchange).block());
		Mockito.verify(matchingExchange.request()).path("/path/to/resource?key=value");

		Assertions.assertEquals(destinationServiceInstance, metaService.getInstance(unmatchingExchange).block());
		Mockito.verify(unmatchingExchange.request(), Mockito.times(0)).path("/otherPath");

		Mockito.verify(discoveryService).resolve(Mockito.eq(destinationServiceID), Mockito.any());
	}

	@Test
	public void test_request_headers_manipulation() throws JsonProcessingException {
		UnboundExchange<ExchangeContext> exchange = mockExchange("/v1/path/to/resource?key=value");

		ServiceID destinationServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destinationServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destinationService = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destinationServiceInstance)).when(destinationService).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destinationService)).when(discoveryService).resolve(Mockito.eq(destinationServiceID), Mockito.any());

		HttpMetaServiceDescriptor serviceDescriptor = new HttpMetaServiceDescriptor(
			null,
			null,
			List.of(
				new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					new HttpMetaServiceDescriptor.RequestTransformer(
						null,
						null,
						Map.of("route-header1", "abc"),
						Map.of("route-header2", "def", "route-header3", "ghi"),
						null
					),
					null,
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8080"),
							null,
							new HttpMetaServiceDescriptor.RequestTransformer(
								null,
								"authority",
								Map.of("destination-header", "123"),
								Map.of("route-header1", "456"),
								Set.of("route-header3")
							),
							null
						)
					)
				)
			)
		);

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				MAPPER.writeValueAsString(serviceDescriptor)
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test")).block();
		Assertions.assertNotNull(metaService);

		Assertions.assertEquals(destinationServiceInstance, metaService.getInstance(exchange).block());

		Mockito.verify(exchange.request()).authority("authority");

		OutboundRequestHeaders outboundRequestHeaders = (OutboundRequestHeaders)exchange.request().headers();
		Mockito.verify(outboundRequestHeaders).add("route-header1", "abc");
		Mockito.verify(outboundRequestHeaders).add("destination-header", "123");
		Mockito.verify(outboundRequestHeaders).set("route-header2", "def");
		Mockito.verify(outboundRequestHeaders).set("route-header3", "ghi");
		Mockito.verify(outboundRequestHeaders).set("route-header1", "456");
		Mockito.verify(outboundRequestHeaders).remove("route-header3");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_response_headers_manipulation() throws JsonProcessingException {
		UnboundExchange<ExchangeContext> exchange = mockExchange("/v1/path/to/resource?key=value");

		ServiceID destinationServiceID = ServiceID.of("http://localhost:8080");
		HttpServiceInstance destinationServiceInstance = Mockito.mock(HttpServiceInstance.class);
		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destinationService = Mockito.mock(Service.class);
		Mockito.doReturn(Mono.just(destinationServiceInstance)).when(destinationService).getInstance(Mockito.any());

		HttpDiscoveryService discoveryService = Mockito.mock(HttpDiscoveryService.class);
		Mockito.when(discoveryService.getSupportedSchemes()).thenReturn(Set.of("http"));
		Mockito.doReturn(Mono.just(destinationService)).when(discoveryService).resolve(Mockito.eq(destinationServiceID), Mockito.any());

		HttpMetaServiceDescriptor serviceDescriptor = new HttpMetaServiceDescriptor(
			null,
			null,
			List.of(
				new HttpMetaServiceDescriptor.RouteDescriptor(
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					new HttpMetaServiceDescriptor.ResponseTransformer(
						Map.of("route-header1", "abc"),
						Map.of("route-header2", "def", "route-header3", "ghi"),
						null
					),
					List.of(
						new HttpMetaServiceDescriptor.DestinationDescriptor(
							null,
							null,
							URI.create("http://localhost:8080"),
							null,
							null,
							new HttpMetaServiceDescriptor.ResponseTransformer(
								Map.of("destination-header", "123"),
								Map.of("route-header1", "456"),
								Set.of("route-header3")
							)
						)
					)
				)
			)
		);

		HttpDiscoveryService configMetaDiscoveryService = new ConfigurationHttpMetaDiscoveryService(
			HttpMetaDiscoveryConfigurationLoader.load(null),
			new MapConfigurationSource(Map.of(
				"io.inverno.mod.discovery.http.meta.service.test",
				MAPPER.writeValueAsString(serviceDescriptor)
			)),
			MAPPER,
			List.of(discoveryService)
		);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> metaService = configMetaDiscoveryService.resolve(ServiceID.of("conf://test")).block();
		Assertions.assertNotNull(metaService);

		Assertions.assertEquals(destinationServiceInstance, metaService.getInstance(exchange).block());

		InboundOutboundResponseHeaders responseHeaders = Mockito.mock(InboundOutboundResponseHeaders.class);

		InterceptedResponse interceptedResponse = Mockito.mock(InterceptedResponse.class);
		Mockito.when(interceptedResponse.headers()).thenReturn(responseHeaders);

		InterceptedExchange<ExchangeContext> interceptedExchange = Mockito.mock(InterceptedExchange.class);
		Mockito.when(interceptedResponse.headers(Mockito.any())).thenAnswer(invocation -> {
			((Consumer<OutboundResponseHeaders>)invocation.getArgument(0)).accept(responseHeaders);
			return interceptedResponse;
		});
		Mockito.when(interceptedExchange.response()).thenReturn(interceptedResponse);

		ArgumentCaptor<ExchangeInterceptor<ExchangeContext, InterceptedExchange<ExchangeContext>>> interceptorCaptor = ArgumentCaptor.forClass(ExchangeInterceptor.class);
		Mockito.verify(exchange, Mockito.times(2)).intercept(interceptorCaptor.capture());
		for(ExchangeInterceptor<ExchangeContext, InterceptedExchange<ExchangeContext>> exchangeInterceptor : interceptorCaptor.getAllValues()) {
			exchangeInterceptor.intercept(interceptedExchange).block();
		}

		Mockito.verify(responseHeaders).add("route-header1", "abc");
		Mockito.verify(responseHeaders).add("destination-header", "123");
		Mockito.verify(responseHeaders).set("route-header2", "def");
		Mockito.verify(responseHeaders).set("route-header3", "ghi");
		Mockito.verify(responseHeaders).set("route-header1", "456");
		Mockito.verify(responseHeaders).remove("route-header3");
	}
}
