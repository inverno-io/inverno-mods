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
package io.inverno.mod.discovery;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class WeightedUtilsTest {

	@Test
	public void test() {
		List<WeightedServiceInstance> mockInstances = createMockInstances(new int[]{80, 10, 10});
		Map<WeightedServiceInstance, Integer> weightedServiceInstances = WeightedUtils.expandToLoadBalanced(mockInstances).stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
		Assertions.assertEquals(8, weightedServiceInstances.get(mockInstances.get(0)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(1)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(2)));

		mockInstances = createMockInstances(new int[]{80, 17, 13});
		weightedServiceInstances = WeightedUtils.expandToLoadBalanced(mockInstances).stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
		Assertions.assertEquals(80, weightedServiceInstances.get(mockInstances.get(0)));
		Assertions.assertEquals(17, weightedServiceInstances.get(mockInstances.get(1)));
		Assertions.assertEquals(13, weightedServiceInstances.get(mockInstances.get(2)));

		mockInstances = createMockInstances(new int[]{10,20,30,40,50,60,70,80,90,100});
		weightedServiceInstances = WeightedUtils.expandToLoadBalanced(mockInstances).stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(0)));
		Assertions.assertEquals(2, weightedServiceInstances.get(mockInstances.get(1)));
		Assertions.assertEquals(3, weightedServiceInstances.get(mockInstances.get(2)));
		Assertions.assertEquals(4, weightedServiceInstances.get(mockInstances.get(3)));
		Assertions.assertEquals(5, weightedServiceInstances.get(mockInstances.get(4)));
		Assertions.assertEquals(6, weightedServiceInstances.get(mockInstances.get(5)));
		Assertions.assertEquals(7, weightedServiceInstances.get(mockInstances.get(6)));
		Assertions.assertEquals(8, weightedServiceInstances.get(mockInstances.get(7)));
		Assertions.assertEquals(9, weightedServiceInstances.get(mockInstances.get(8)));
		Assertions.assertEquals(10, weightedServiceInstances.get(mockInstances.get(9)));

		mockInstances = createMockInstances(new int[]{2,4,6,8,10,12});
		weightedServiceInstances = WeightedUtils.expandToLoadBalanced(mockInstances).stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(0)));
		Assertions.assertEquals(2, weightedServiceInstances.get(mockInstances.get(1)));
		Assertions.assertEquals(3, weightedServiceInstances.get(mockInstances.get(2)));
		Assertions.assertEquals(4, weightedServiceInstances.get(mockInstances.get(3)));
		Assertions.assertEquals(5, weightedServiceInstances.get(mockInstances.get(4)));
		Assertions.assertEquals(6, weightedServiceInstances.get(mockInstances.get(5)));

		mockInstances = createMockInstances(new int[]{2,2,2,2});
		weightedServiceInstances = WeightedUtils.expandToLoadBalanced(mockInstances).stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(0)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(1)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(2)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(3)));

		mockInstances = createMockInstances(new int[]{1,2,3,4});
		weightedServiceInstances = WeightedUtils.expandToLoadBalanced(mockInstances).stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(0)));
		Assertions.assertEquals(2, weightedServiceInstances.get(mockInstances.get(1)));
		Assertions.assertEquals(3, weightedServiceInstances.get(mockInstances.get(2)));
		Assertions.assertEquals(4, weightedServiceInstances.get(mockInstances.get(3)));

		mockInstances = createMockInstances(new int[]{1,1,1,1});
		weightedServiceInstances = WeightedUtils.expandToLoadBalanced(mockInstances).stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(0)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(1)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(2)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(3)));

		mockInstances = createMockInstances(new int[]{2,3,4,1});
		weightedServiceInstances = WeightedUtils.expandToLoadBalanced(mockInstances).stream().collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));
		Assertions.assertEquals(2, weightedServiceInstances.get(mockInstances.get(0)));
		Assertions.assertEquals(3, weightedServiceInstances.get(mockInstances.get(1)));
		Assertions.assertEquals(4, weightedServiceInstances.get(mockInstances.get(2)));
		Assertions.assertEquals(1, weightedServiceInstances.get(mockInstances.get(3)));

		Assertions.assertEquals("weight must be a positive integer", Assertions.assertThrows(IllegalArgumentException.class, () -> WeightedUtils.expandToLoadBalanced(createMockInstances(new int[]{0,3,4,1}))).getMessage());
	}

	private static List<WeightedServiceInstance> createMockInstances(int[] weights) {
		return Arrays.stream(weights)
			.mapToObj(weight -> {
				WeightedServiceInstance mockInstance = Mockito.mock(WeightedServiceInstance.class);
				Mockito.when(mockInstance.getWeight()).thenReturn(weight);
				return mockInstance;
			})
			.collect(Collectors.toList());
	}
}
