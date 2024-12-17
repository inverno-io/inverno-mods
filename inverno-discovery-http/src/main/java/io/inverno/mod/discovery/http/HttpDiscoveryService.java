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

import io.inverno.mod.discovery.DiscoveryService;
import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.http.client.UnboundExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A discovery service for resolving HTTP services providing {@link HttpServiceInstance} in order to process HTTP requests.
 * </p>
 *
 * <p>
 * It is typically used as follows, to resolve an HTTP service and process HTTP requests on one or more HTTP service instances.
 * </p>
 *
 * <pre>{@code
 * HttpClient httpClient = ...
 * HttpDiscoveryService httpDiscoveryService = ...
 *
 * client.exchange("/path/to/resource")
 *     .flatMap(exchange -> httpDiscoveryService.resolve(ServiceId.of("http://example.org"))
 *     .flatMap(service -> service.getInstance(exchange))
 *         .map(serviceInstance -> serviceInstance.bind(exchange))
 *     )
 *     .flatMap(Exchange::response)
 *     ...
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see HttpTrafficPolicy
 * @see HttpServiceInstance
 */
public interface HttpDiscoveryService extends DiscoveryService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> {

	/**
	 * <p>
	 * Resolves an HTTP service using a default HTTP traffic policy.
	 * </p>
	 *
	 * @param serviceId a service ID
	 *
	 * @return a {@code Mono} emitting the resolved service or an empty {@code Mono} if no service could be resolved
	 *
	 * @throws IllegalArgumentException if the specified service ID is not supported
	 */
	default Mono<? extends Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy>> resolve(ServiceID serviceId) throws IllegalArgumentException {
		return this.resolve(serviceId, HttpTrafficPolicy.builder().build());
	}
}
