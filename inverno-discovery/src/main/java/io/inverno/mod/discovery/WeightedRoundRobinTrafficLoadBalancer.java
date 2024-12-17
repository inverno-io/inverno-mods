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

import java.util.Collection;

/**
 * <p>
 * A traffic load balancer that selects instances in a deterministic order by iterating on a weighted list of instances.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of weighted service instance
 * @param <B> the type of service request
 */
public class WeightedRoundRobinTrafficLoadBalancer<A extends WeightedServiceInstance, B> extends RoundRobinTrafficLoadBalancer<A, B> {

	/**
	 * <p>
	 * Creates a weighted round-robin traffic load balancer.
	 * </p>
	 *
	 * @param weightedInstances a collection of weighted instances
	 */
	public WeightedRoundRobinTrafficLoadBalancer(Collection<A> weightedInstances) {
		super(WeightedUtils.expandToLoadBalanced(weightedInstances));
	}
}
