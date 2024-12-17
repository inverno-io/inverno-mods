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

import io.inverno.mod.base.concurrent.Reactor;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class CachingDiscoveryServiceTest {

	@Test
	@SuppressWarnings("unchecked")
	public void test_resolve() {
		try(EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(1)) {
			Reactor reactor = Mockito.mock(Reactor.class);
			Mockito.when(reactor.getEventLoop()).thenReturn(eventLoopGroup.next());

			DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService = Mockito.mock(DiscoveryService.class);

			ServiceID serviceId = ServiceID.of("http://test");

			TrafficPolicy<ServiceInstance, Object> trafficPolicy = Mockito.mock(TrafficPolicy.class);

			ServiceInstance serviceInstance = Mockito.mock(ServiceInstance.class);

			Service<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> service = Mockito.mock(Service.class);
			Mockito.when(service.getID()).thenReturn(serviceId);
			Mockito.when(service.getTrafficPolicy()).thenReturn(trafficPolicy);
			Mockito.doReturn(Mono.just(service)).when(service).refresh(trafficPolicy);
			Mockito.when(service.shutdown()).thenReturn(Mono.empty());
			Mockito.when(service.shutdownGracefully()).thenReturn(Mono.empty());
			Mockito.doReturn(Mono.just(serviceInstance)).when(service).getInstance(Mockito.any());

			Mockito.doReturn(Mono.just(service)).when(discoveryService).resolve(serviceId, trafficPolicy);

			CachingDiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> cachingDiscoveryService = new CachingDiscoveryService<>(reactor, discoveryService);

			for(int i=0;i<10;i++) {
				Assertions.assertEquals(serviceInstance, cachingDiscoveryService.resolve(serviceId, trafficPolicy).flatMap(resolvedService -> resolvedService.getInstance(null)).block());
			}
			Mockito.verify(discoveryService, Mockito.times(1)).resolve(serviceId, trafficPolicy);

			cachingDiscoveryService.shutdown().block();

			Mockito.verify(service).shutdown();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_cache_refresh() {
		try(EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(1)) {
			Reactor reactor = Mockito.mock(Reactor.class);
			Mockito.when(reactor.getEventLoop()).thenReturn(eventLoopGroup.next());

			DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService = Mockito.mock(DiscoveryService.class);

			ServiceID serviceId = ServiceID.of("http://test");

			TrafficPolicy<ServiceInstance, Object> trafficPolicy = Mockito.mock(TrafficPolicy.class);

			ServiceInstance serviceInstance = Mockito.mock(ServiceInstance.class);

			Service<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> service = Mockito.mock(Service.class);
			Mockito.when(service.getID()).thenReturn(serviceId);
			Mockito.when(service.getTrafficPolicy()).thenReturn(trafficPolicy);
			Mockito.doReturn(Mono.just(service)).when(service).refresh(trafficPolicy);
			Mockito.when(service.shutdown()).thenReturn(Mono.empty());
			Mockito.when(service.shutdownGracefully()).thenReturn(Mono.empty());
			Mockito.doReturn(Mono.just(serviceInstance)).when(service).getInstance(Mockito.any());

			Mockito.doReturn(Mono.just(service)).when(discoveryService).resolve(serviceId, trafficPolicy);

			CachingDiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> cachingDiscoveryService = new CachingDiscoveryService<>(reactor, discoveryService, 1000);

			Assertions.assertEquals(serviceInstance, cachingDiscoveryService.resolve(serviceId, trafficPolicy).flatMap(resolvedService -> resolvedService.getInstance(null)).block());

			Awaitility.await().atMost(Duration.ofMillis(2000)).pollInterval(Duration.ofMillis(750)).untilAsserted(() -> {
				Mockito.verify(service).refresh(trafficPolicy);
			});

			Assertions.assertEquals(serviceInstance, cachingDiscoveryService.resolve(serviceId, trafficPolicy).flatMap(resolvedService -> resolvedService.getInstance(null)).block());

			cachingDiscoveryService.shutdown().block();

			Mockito.verify(service).shutdown();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_concurrent() throws InterruptedException {
		try(EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(1)) {
			Reactor reactor = Mockito.mock(Reactor.class);
			Mockito.when(reactor.getEventLoop()).thenReturn(eventLoopGroup.next());

			DiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> discoveryService = Mockito.mock(DiscoveryService.class);

			ServiceID serviceId = ServiceID.of("http://test");

			TrafficPolicy<ServiceInstance, Object> trafficPolicy = Mockito.mock(TrafficPolicy.class);

			ServiceInstance serviceInstance = Mockito.mock(ServiceInstance.class);

			Service<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> service = Mockito.mock(Service.class);
			Mockito.when(service.getID()).thenReturn(serviceId);
			Mockito.when(service.getTrafficPolicy()).thenReturn(trafficPolicy);
			Mockito.doReturn(Mono.just(service)).when(service).refresh(trafficPolicy);
			Mockito.when(service.shutdown()).thenReturn(Mono.empty());
			Mockito.when(service.shutdownGracefully()).thenReturn(Mono.empty());
			Mockito.doReturn(Mono.just(serviceInstance)).when(service).getInstance(Mockito.any());

			Mockito.doReturn(Mono.just(service)).when(discoveryService).resolve(serviceId, trafficPolicy);

			CachingDiscoveryService<ServiceInstance, Object, TrafficPolicy<ServiceInstance, Object>> cachingDiscoveryService = new CachingDiscoveryService<>(reactor, discoveryService, 50);

			List<Thread> threads = new ArrayList<>();
			for(int i=0;i<16;i++) {
				Thread t = new Thread(() -> {
					while(Mockito.mockingDetails(service).getInvocations().isEmpty()) {
						Assertions.assertEquals(serviceInstance, cachingDiscoveryService.resolve(serviceId, trafficPolicy).flatMap(resolvedService -> resolvedService.getInstance(null)).block());
					}
					for(int j=0;j<10;j++) {
						Assertions.assertEquals(serviceInstance, cachingDiscoveryService.resolve(serviceId, trafficPolicy).flatMap(resolvedService -> resolvedService.getInstance(null)).block());
					}
				});
				threads.add(t);
				t.start();
			}

			for(Thread t : threads) {
				t.join();
			}

			cachingDiscoveryService.shutdown().block();

			Mockito.verify(service).shutdown();
		}
	}
}
