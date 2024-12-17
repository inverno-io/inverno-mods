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
package io.inverno.mod.discovery;

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class CompositeDiscoveryServiceTest {

	@Test
	@SuppressWarnings("unchecked")
	public void test() {
		DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService1 = Mockito.mock(DiscoveryService.class);
		Mockito.when(discoveryService1.getSupportedSchemes()).thenReturn(Set.of("scheme1", "scheme2"));
		DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService2 = Mockito.mock(DiscoveryService.class);
		Mockito.when(discoveryService2.getSupportedSchemes()).thenReturn(Set.of("scheme3"));

		DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> compositeDiscoveryService = new CompositeDiscoveryService<>(List.of(discoveryService1, discoveryService2));

		Assertions.assertEquals(Set.of("scheme1", "scheme2", "scheme3"), compositeDiscoveryService.getSupportedSchemes());
		Assertions.assertTrue(compositeDiscoveryService.supports("scheme1"));
		Assertions.assertTrue(compositeDiscoveryService.supports(URI.create("scheme2://authority/path/to/resource")));
		Assertions.assertTrue(compositeDiscoveryService.supports(ServiceID.of("scheme3:ssp#/path/to/resource")));

		TrafficPolicy<ServiceInstance, Object> trafficPolicy = Mockito.mock(TrafficPolicy.class);

		ServiceID serviceId = ServiceID.of("scheme1://authority/path/to/resource");
		compositeDiscoveryService.resolve(serviceId, trafficPolicy);
		Mockito.verify(discoveryService1).resolve(serviceId, trafficPolicy);

		serviceId = ServiceID.of("scheme2://authority");
		compositeDiscoveryService.resolve(serviceId, trafficPolicy);
		Mockito.verify(discoveryService1).resolve(serviceId, trafficPolicy);

		serviceId = ServiceID.of("scheme3:ssp#/path/to/resource");
		compositeDiscoveryService.resolve(serviceId, trafficPolicy);
		Mockito.verify(discoveryService2).resolve(serviceId, trafficPolicy);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_conflictingSchemes() {
		DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService1 = Mockito.mock(DiscoveryService.class);
		Mockito.when(discoveryService1.getSupportedSchemes()).thenReturn(Set.of("scheme1", "scheme2"));
		Mockito.when(discoveryService1.toString()).thenReturn("discoveryService1");
		DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService2 = Mockito.mock(DiscoveryService.class);
		Mockito.when(discoveryService2.getSupportedSchemes()).thenReturn(Set.of("scheme2"));
		Mockito.when(discoveryService2.toString()).thenReturn("discoveryService2");

		Assertions.assertEquals("Multiple discovery services found for scheme scheme2: discoveryService1, discoveryService2", Assertions.assertThrows(IllegalArgumentException.class, () -> new CompositeDiscoveryService<>(List.of(discoveryService1, discoveryService2))).getMessage());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_resolveUnsupportedSchemes() {
		DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService1 = Mockito.mock(DiscoveryService.class);
		Mockito.when(discoveryService1.getSupportedSchemes()).thenReturn(Set.of("scheme1", "scheme2"));
		DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService2 = Mockito.mock(DiscoveryService.class);
		Mockito.when(discoveryService2.getSupportedSchemes()).thenReturn(Set.of("scheme3"));

		DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> compositeDiscoveryService = new CompositeDiscoveryService<>(List.of(discoveryService1, discoveryService2));

		Assertions.assertEquals("Unsupported scheme: http", Assertions.assertThrows(IllegalArgumentException.class, () -> compositeDiscoveryService.resolve(ServiceID.of("http://localhost:8080"), null)).getMessage());
	}
}
