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
import reactor.core.publisher.Mono;

/**
 * <p>
 * A traffic load balancer is used in a {@link Service} to load balance the traffic among its list of {@link ServiceInstance}.
 * </p>
 *
 * <p>
 * A traffic load balancer is obtained by a service from the {@link TrafficPolicy} specified either when resolving the service in a {@link DiscoveryService} or when refreshing a service.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 */
public interface TrafficLoadBalancer<A extends ServiceInstance, B> {

	/**
	 * <p>
	 * Returns the next service instance to use to process the specified service request.
	 * </p>
	 *
	 * @param serviceRequest a service request
	 *
	 * @return a {@code Mono} emitting the next service instance
	 */
	Mono<A> next(B serviceRequest);

	/**
	 * <p>
	 * A traffic load balancer factory.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the type of service instance
	 * @param <B> the type of service request
	 */
	interface Factory<A extends ServiceInstance, B> {

		/**
		 * <p>
		 * Creates a traffic load balancer that load balances the specified collection of service instances.
		 * </p>
		 *
		 * @param instances a collection of service instances
		 *
		 * @return a traffic load balancer
		 */
		TrafficLoadBalancer<A, B> create(Collection<A> instances);
	}
}
