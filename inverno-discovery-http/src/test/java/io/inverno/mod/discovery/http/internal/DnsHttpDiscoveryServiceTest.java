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
package io.inverno.mod.discovery.http.internal;

import io.inverno.mod.base.net.NetService;
import io.inverno.mod.discovery.ManageableService;
import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.InterceptedExchange;
import io.inverno.mod.http.client.UnboundExchange;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class DnsHttpDiscoveryServiceTest {

	// String scheme, boolean expectTls, ServiceId serviceID, int expected port

	public static Stream<Arguments> provideTestArguments() {
		return Stream.of(
			Arguments.of(ServiceID.of("http://test:8080"), 8080, false),
			Arguments.of(ServiceID.of("http://test"), 80, false),
			Arguments.of(ServiceID.of("ws://test:8080"), 8080, false),
			Arguments.of(ServiceID.of("ws://test"), 80, false),
			Arguments.of(ServiceID.of("https://test:8443"), 8443, true),
			Arguments.of(ServiceID.of("https://test"), 443, true),
			Arguments.of(ServiceID.of("wss://test:8443"), 8443, true),
			Arguments.of(ServiceID.of("wss://test"), 443, true)
		);
	}

	@ParameterizedTest
	@MethodSource("provideTestArguments")
	@SuppressWarnings("unchecked")
	public void test(ServiceID serviceId, int expectedPort, boolean expectedTls) throws UnknownHostException {
		NetService netService = Mockito.mock(NetService.class);

		InetSocketAddress address1 = new InetSocketAddress(InetAddress.getByAddress(new byte[] {1,2,3,4}), expectedPort);
		InetSocketAddress address2 = new InetSocketAddress(InetAddress.getByAddress(new byte[] {1,2,3,5}), expectedPort);
		InetSocketAddress address3 = new InetSocketAddress(InetAddress.getByAddress(new byte[] {1,2,3,6}), expectedPort);

		Mockito.when(netService.resolveAll(InetSocketAddress.createUnresolved("test", expectedPort))).thenReturn(Mono.just(List.of(address1, address2, address3)));

		Endpoint<ExchangeContext> endpoint1 = Mockito.mock(Endpoint.class);
		HttpClient.EndpointBuilder<ExchangeContext, Exchange<ExchangeContext>, InterceptedExchange<ExchangeContext>> endpointBuilder1 = Mockito.mock(HttpClient.EndpointBuilder.class);
		Mockito.when(endpointBuilder1.configuration(Mockito.any())).thenReturn(endpointBuilder1);
		Mockito.when(endpointBuilder1.netConfiguration(Mockito.any())).thenReturn(endpointBuilder1);
		Mockito.when(endpointBuilder1.build()).thenReturn(endpoint1);

		Endpoint<ExchangeContext> endpoint2 = Mockito.mock(Endpoint.class);
		HttpClient.EndpointBuilder<ExchangeContext, Exchange<ExchangeContext>, InterceptedExchange<ExchangeContext>> endpointBuilder2 = Mockito.mock(HttpClient.EndpointBuilder.class);
		Mockito.when(endpointBuilder2.configuration(Mockito.any())).thenReturn(endpointBuilder2);
		Mockito.when(endpointBuilder2.netConfiguration(Mockito.any())).thenReturn(endpointBuilder2);
		Mockito.when(endpointBuilder2.build()).thenReturn(endpoint2);

		Endpoint<ExchangeContext> endpoint3 = Mockito.mock(Endpoint.class);
		HttpClient.EndpointBuilder<ExchangeContext, Exchange<ExchangeContext>, InterceptedExchange<ExchangeContext>> endpointBuilder3 = Mockito.mock(HttpClient.EndpointBuilder.class);
		Mockito.when(endpointBuilder3.configuration(Mockito.any())).thenReturn(endpointBuilder3);
		Mockito.when(endpointBuilder3.netConfiguration(Mockito.any())).thenReturn(endpointBuilder3);
		Mockito.when(endpointBuilder3.build()).thenReturn(endpoint3);

		HttpClient httpClient = Mockito.mock(HttpClient.class);
		Mockito.when(httpClient.endpoint(address1)).thenReturn(endpointBuilder1);
		Mockito.when(httpClient.endpoint(address2)).thenReturn(endpointBuilder2);
		Mockito.when(httpClient.endpoint(address3)).thenReturn(endpointBuilder3);

		HttpClientConfiguration expectedConfiguration = HttpClientConfigurationLoader.load(configuration -> configuration.tls_enabled(expectedTls));

		DnsHttpDiscoveryService dnsHttpDiscoveryService = new DnsHttpDiscoveryService(netService, httpClient);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> service = dnsHttpDiscoveryService.resolve(serviceId).block();

		Mockito.verify(endpointBuilder1).configuration(expectedConfiguration);
		Mockito.verify(endpointBuilder1).build();

		Mockito.verify(endpointBuilder2).configuration(expectedConfiguration);
		Mockito.verify(endpointBuilder2).build();

		Mockito.verify(endpointBuilder3).configuration(expectedConfiguration);
		Mockito.verify(endpointBuilder3).build();

		Assertions.assertNotNull(service);

		UnboundExchange<ExchangeContext> exchange = Mockito.mock(UnboundExchange.class);
		((ManageableService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy>)service).getInstances().stream()
			.forEach(instance -> instance.bind(exchange));
		Mockito.verify(exchange, Mockito.times(3)).bind(Mockito.any());
		Mockito.verify(exchange).bind(endpoint1);
		Mockito.verify(exchange).bind(endpoint2);
		Mockito.verify(exchange).bind(endpoint3);
	}
}
