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
package io.inverno.mod.discovery.http;

import io.inverno.mod.discovery.TrafficLoadBalancer;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class LeastRequestTrafficLoadBalancerTest {

	@Test
	public void test_same_weights() {
		HttpServiceInstance instance1 = Mockito.mock(HttpServiceInstance.class);
		Mockito.when(instance1.getWeight()).thenReturn(1);
		Mockito.when(instance1.getActiveRequests()).thenReturn(0L);

		HttpServiceInstance instance2 = Mockito.mock(HttpServiceInstance.class);
		Mockito.when(instance2.getWeight()).thenReturn(1);
		Mockito.when(instance2.getActiveRequests()).thenReturn(1L);

		HttpServiceInstance instance3 = Mockito.mock(HttpServiceInstance.class);
		Mockito.when(instance3.getWeight()).thenReturn(1);
		Mockito.when(instance3.getActiveRequests()).thenReturn(2L);

		TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> leastRequestLoadBalancer = new LeastRequestTrafficLoadBalancer(List.of(instance1, instance2, instance3), 3, 1);

		Assertions.assertEquals(instance1, leastRequestLoadBalancer.next(null).block());
		Assertions.assertEquals(instance1, leastRequestLoadBalancer.next(null).block());

		Mockito.when(instance1.getActiveRequests()).thenReturn(3L);
		Assertions.assertEquals(instance2, leastRequestLoadBalancer.next(null).block());
		Assertions.assertEquals(instance2, leastRequestLoadBalancer.next(null).block());

		Mockito.when(instance2.getActiveRequests()).thenReturn(4L);
		Assertions.assertEquals(instance3, leastRequestLoadBalancer.next(null).block());
		Assertions.assertEquals(instance3, leastRequestLoadBalancer.next(null).block());

		Mockito.when(instance1.getActiveRequests()).thenReturn(0L);
		Assertions.assertEquals(instance1, leastRequestLoadBalancer.next(null).block());
		Assertions.assertEquals(instance1, leastRequestLoadBalancer.next(null).block());
	}

	@Test
	public void test_different_weights() {
		HttpServiceInstance instance1 = Mockito.mock(HttpServiceInstance.class);
		Mockito.when(instance1.getWeight()).thenReturn(3);
		Mockito.when(instance1.getActiveRequests()).thenReturn(0L);

		HttpServiceInstance instance2 = Mockito.mock(HttpServiceInstance.class);
		Mockito.when(instance2.getWeight()).thenReturn(2);
		Mockito.when(instance2.getActiveRequests()).thenReturn(0L);

		HttpServiceInstance instance3 = Mockito.mock(HttpServiceInstance.class);
		Mockito.when(instance3.getWeight()).thenReturn(1);
		Mockito.when(instance3.getActiveRequests()).thenReturn(0L);

		TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> leastRequestLoadBalancer = new LeastRequestTrafficLoadBalancer(List.of(instance1, instance2, instance3), 3, 1);

		// 3, 2, 1
		Assertions.assertEquals(instance1, leastRequestLoadBalancer.next(null).block());

		// 1.5, 2, 1
		Mockito.when(instance1.getActiveRequests()).thenReturn(1L);
		Assertions.assertEquals(instance2, leastRequestLoadBalancer.next(null).block());

		// 1.5, 1, 1
		Mockito.when(instance2.getActiveRequests()).thenReturn(1L);
		Assertions.assertEquals(instance1, leastRequestLoadBalancer.next(null).block());

		// 0.75, 0.67, 1
		Mockito.when(instance1.getActiveRequests()).thenReturn(3L);
		Mockito.when(instance2.getActiveRequests()).thenReturn(2L);
		Assertions.assertEquals(instance3, leastRequestLoadBalancer.next(null).block());

		// 0.75, 0.67, 0.5
		Mockito.when(instance3.getActiveRequests()).thenReturn(1L);
		Assertions.assertEquals(instance1, leastRequestLoadBalancer.next(null).block());

		// 0.6, 0.67, 0.5
		Mockito.when(instance1.getActiveRequests()).thenReturn(4L);
		Assertions.assertEquals(instance2, leastRequestLoadBalancer.next(null).block());
	}
}
