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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class WeightedRandomTrafficLoadBalancerTest {

	@Test
	public void test() {
		WeightedServiceInstance instance1 = Mockito.mock(WeightedServiceInstance.class);
		Mockito.when(instance1.getWeight()).thenReturn(2);
		WeightedServiceInstance instance2 = Mockito.mock(WeightedServiceInstance.class);
		Mockito.when(instance2.getWeight()).thenReturn(4);
		WeightedServiceInstance instance3 = Mockito.mock(WeightedServiceInstance.class);
		Mockito.when(instance3.getWeight()).thenReturn(6);

		TrafficLoadBalancer<WeightedServiceInstance, Object> lb = new WeightedRandomTrafficLoadBalancer<>(List.of(instance1, instance2, instance3));

		int count1 = 0;
		int count2 = 0;
		int count3 = 0;
		for(int i=0;i<900000;i++) {
			ServiceInstance instance = lb.next(null).block();
			if(instance == instance1) {
				count1++;
			}
			else if(instance == instance2) {
				count2++;
			}
			else if(instance == instance3) {
				count3++;
			}
			else {
				Assertions.fail("Unknown instance");
			}
		}

		// Let's consider 1% delta proves the point
		Assertions.assertEquals(150000, count1, 150000*0.01);
		Assertions.assertEquals(300000, count2, 300000*0.01);
		Assertions.assertEquals(450000, count3, 450000*0.01);
	}

	@Test
	public void test_concurrent() throws InterruptedException {
		WeightedServiceInstance instance1 = Mockito.mock(WeightedServiceInstance.class);
		Mockito.when(instance1.getWeight()).thenReturn(2);
		WeightedServiceInstance instance2 = Mockito.mock(WeightedServiceInstance.class);
		Mockito.when(instance2.getWeight()).thenReturn(4);
		WeightedServiceInstance instance3 = Mockito.mock(WeightedServiceInstance.class);
		Mockito.when(instance3.getWeight()).thenReturn(6);

		TrafficLoadBalancer<WeightedServiceInstance, Object> lb = new WeightedRandomTrafficLoadBalancer<>(List.of(instance1, instance2, instance3));

		AtomicInteger count1 = new AtomicInteger();
		AtomicInteger count2 = new AtomicInteger();
		AtomicInteger count3 = new AtomicInteger();

		List<Thread> threads = new ArrayList<>();
		for(int i=0;i<16;i++) {
			Thread t = new Thread(() -> {
				for(int j=0;j<60000;j++) {
					ServiceInstance instance = lb.next(null).block();
					if(instance == instance1) {
						count1.incrementAndGet();
					}
					else if(instance == instance2) {
						count2.incrementAndGet();
					}
					else if(instance == instance3) {
						count3.incrementAndGet();
					}
					else {
						Assertions.fail("Unknown instance");
					}
				}
			});
			threads.add(t);
			t.start();
		}

		for(Thread t : threads) {
			t.join();
		}

		// Let's consider 1% delta proves the point
		Assertions.assertEquals(160000, count1.get(), 160000*0.01);
		Assertions.assertEquals(320000, count2.get(), 320000*0.01);
		Assertions.assertEquals(480000, count3.get(), 480000*0.01);
	}
}
