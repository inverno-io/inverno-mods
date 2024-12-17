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
import java.util.concurrent.ThreadLocalRandom;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A traffic load balancer that selects a random service instance in a list of service instances.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 */
public class RandomTrafficLoadBalancer<A extends ServiceInstance, B> implements TrafficLoadBalancer<A, B> {

	private final Object[] instances;
	private final Mono<A> nextInstance;

	/**
	 * <p>
	 * Creates a random traffic load balancer.
	 * </p>
	 *
	 * @param instances a collection of service instances
	 */
	@SuppressWarnings("unchecked")
	public RandomTrafficLoadBalancer(Collection<A> instances) {
		this.instances = instances.toArray(Object[]::new);
		this.nextInstance = Mono.fromSupplier((() -> (A)this.instances[ThreadLocalRandom.current().nextInt(this.instances.length)]));
	}

	@Override
	public Mono<A> next(B serviceRequest) {
		return this.nextInstance;
	}
}
