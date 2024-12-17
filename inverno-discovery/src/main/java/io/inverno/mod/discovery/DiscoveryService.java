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

import java.net.URI;
import java.util.Set;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A discovery service is used to resolve {@link Service} from a {@link ServiceID} identifying a service and a {@link TrafficPolicy} conveying local service configuration.
 * </p>
 *
 * <p>
 * The service ID is nothing more than an absolute URI providing the information required by a compatible discovery service to resolve a specific service. The scheme of the URI is used to determine
 * whether a service can be resolved by a given discovery service. How the service URI is interpreted during service resolution is implementation specific.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see Service
 * @see ServiceID
 * @see TrafficPolicy
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 */
public interface DiscoveryService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>> {

	/**
	 * <p>
	 * Determines whether the discovery service supports the specified scheme.
	 * </p>
	 *
	 * @param scheme a URI scheme
	 *
	 * @return true if the discovery service can resolve service URI with the specified scheme, false otherwise
	 */
	default boolean supports(String scheme) {
		return this.getSupportedSchemes().contains(scheme.toLowerCase());
	}

	/**
	 * <p>
	 * Determines whether the discovery service supports the specified service URI.
	 * </p>
	 *
	 * @param uri a service URI
	 *
	 * @return true if the discovery service can resolve the specified service URI, false otherwise
	 */
	default boolean supports(URI uri) {
		return this.supports(ServiceID.of(uri));
	}

	/**
	 * <p>
	 * Determines whether the discovery service can resolve the specified service ID.
	 * </p>
	 *
	 * @param serviceId a service ID
	 *
	 * @return true if the discovery service can resolve the specified service ID, false otherwise
	 */
	default boolean supports(ServiceID serviceId) {
		return this.supports(serviceId.getScheme());
	}

	/**
	 * <p>
	 * Returns the list of schemes supported by the discovery service.
	 * </p>
	 *
	 * @return a set of lower cased schemes
	 */
	Set<String> getSupportedSchemes();

	/**
	 * <p>
	 * Resolves a service.
	 * </p>
	 *
	 * @param serviceId     a service ID
	 * @param trafficPolicy a traffic policy
	 *
	 * @return a {@code Mono} emitting the resolved service or an empty {@code Mono} if no service could be resolved
	 *
	 * @throws IllegalArgumentException if the specified service ID is not supported
	 */
	Mono<? extends Service<A, B, C>> resolve(ServiceID serviceId, C trafficPolicy) throws IllegalArgumentException;
}
