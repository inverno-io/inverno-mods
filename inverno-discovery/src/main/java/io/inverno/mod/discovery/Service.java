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

import reactor.core.publisher.Mono;

/**
 * <p>
 * A service is resolved by a {@link DiscoveryService}, it gives access to one or more {@link ServiceInstance} that are eventually used to process a service request.
 * </p>
 *
 * <p>
 * It is uniquely identified by a {@link ServiceID} and a {@link TrafficPolicy} specified which are specified when resolving a service using a discovery service. The traffic policy can convey specific
 * configuration for the creation of the service instances and/or specify how a service should assign service instances to process requests as for instance load balancing strategy, content based
 * routing...
 * </p>
 *
 * <p>
 * A service is obtained from a {@link DiscoveryService}, it is uniquely identified by the {@link ServiceID} and {@link TrafficPolicy} passed to the discovery service. A traffic policy can provide
 * specific configuration for the creation of service instances and/or specify how service instances are selected to process requests. The {@link #getInstance(Object)} method resolves the service
 * instance to use to process a particular service request at a particular time based on the traffic policy. Depending on the implementation, service instances can be load balanced, matched against
 * some request criteria (i.e. content based routing), request can be transformed (e.g. path translation, headers manipulation...)...
 * </p>
 *
 * <p>
 * A service can be refreshed in order to update the list of service instances or change the traffic policy.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see DiscoveryService
 * @see TrafficPolicy
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 */
public interface Service<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>> {

	/**
	 * <p>
	 * Returns the service ID.
	 * </p>
	 *
	 * @return the service ID
	 */
	ServiceID getID();

	/**
	 * <p>
	 * Returns the traffic policy.
	 * </p>
	 *
	 * @return the traffic policy
	 */
	C getTrafficPolicy();

	/**
	 * <p>
	 * Returns a service instance for processing the specified service request.
	 * </p>
	 *
	 * <p>
	 * Implementations should typically rely on the service traffic policy to determine which instance should be assigned to process a particular request. A typical use case is service load balancing
	 * where a service is deployed on multiple servers and requests must be distributed among these servers in a random or round robin fashion, by taking the server load into account...
	 * </p>
	 *
	 * <p>
	 * Implementations can also assign instances based on the content of the request in which case it is possible that no service instance could match the request and an empty {@code Mono} shall then
	 * be returned.
	 * </p>
	 *
	 * @param serviceRequest a service request
	 *
	 * @return a {@code Mono} emitting the instance to use to process the service request or an empty {@code Mono} if no instance matching the request could be found
	 */
	Mono<? extends A> getInstance(B serviceRequest);

	/**
	 * <p>
	 * Refreshes the service.
	 * </p>
	 *
	 * <p>
	 * This basically refreshes the list of service instances. When no service instance could be resolved, the service is assumed to be gone and an empty {@code Mono} shall be returned.
	 * </p>
	 *
	 * <p>
	 * Whether a new instance is returned or not is implementation specific.
	 * </p>
	 *
	 * @return a {@code Mono} emitting a refreshed service or an empty {@code Mono} if no service instance could be resolved
	 */
	default Mono<? extends Service<A, B, C>> refresh() {
		return this.refresh(this.getTrafficPolicy());
	}

	/**
	 * <p>
	 * Refreshes the service using the specified traffic policy.
	 * </p>
	 *
	 * <p>
	 * This basically refreshes the list of service instances using the specified traffic policy. When no service instance could be resolved, the service is assumed to be gone and an empty
	 * {@code Mono} shall be returned.
	 * </p>
	 *
	 * <p>
	 * Whether a new instance is returned or not is implementation specific.
	 * </p>
	 *
	 * @param trafficPolicy a traffic policy
	 *
	 * @return a {@code Mono} emitting a refreshed service or an empty {@code Mono} if no service instance could be resolved
	 */
	Mono<? extends Service<A, B, C>> refresh(C trafficPolicy);

	/**
	 * <p>
	 * Returns the refreshed time stamp.
	 * </p>
	 *
	 * @return an epoch time in milliseconds
	 */
	long getLastRefreshed();

	/**
	 * <p>
	 * Shutdowns the service.
	 * </p>
	 *
	 * <p>
	 * This basically shutdowns all service instances and free resources.
	 * </p>
	 *
	 * @return a {@code Mono} which completes once the service is shutdown
	 */
	Mono<Void> shutdown();

	/**
	 * <p>
	 * Gracefully shutdowns the service.
	 * </p>
	 *
	 * <p>
	 * This basically gracefully shutdowns all service instances and free resources.
	 * </p>
	 *
	 * @return a {@code Mono} which completes once the service is shutdown
	 */
	Mono<Void> shutdownGracefully();
}
