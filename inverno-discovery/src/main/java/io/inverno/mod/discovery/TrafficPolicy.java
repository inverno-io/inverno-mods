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
 * A traffic specifies the configuration and load balancing strategy used by a service to create its list of {@link ServiceInstance} and load balance service request among them.
 * </p>
 *
 * <p>
 * A traffic policy is used in conjunction with a {@link ServiceID} in order to uniquely identify a resolved service such as in a {@link CachingDiscoveryService}, implementations must then implement
 * {@link Object#equals(Object)} and {@link Object#hashCode()}.
 * </p>
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface TrafficPolicy<A extends ServiceInstance, B> {

	/**
	 * <p>
	 * Returns a traffic load balancer for load balancing service request among the specified collection of instances.
	 * </p>
	 *
	 * @param instances a collection of service instances
	 *
	 * @return a traffic load balancer
	 *
	 * @throws IllegalArgumentException if the collection of instance is empty
	 */
	TrafficLoadBalancer<A, B> getLoadBalancer(Collection<A> instances) throws IllegalArgumentException;
}
