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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A composite discovery service composing multiple discovery services.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 */
public class CompositeDiscoveryService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>> implements DiscoveryService<A, B, C> {

	private final Map<String, DiscoveryService<A, B, C>> discoveryServicesMap;

	/**
	 * <p>
	 * Creates a composite discovery service.
	 * </p>
	 *
	 * @param discoveryServices a list of discovery services to compose
	 *
	 * @throws IllegalArgumentException if the same scheme is supported by multiple discovery services
	 */
	@SuppressWarnings("unchecked")
	public CompositeDiscoveryService(List<? extends DiscoveryService<? extends A, ? super B, ? super C>> discoveryServices) throws IllegalArgumentException {
		Map<String, DiscoveryService<A, B, C>> tmpDiscoveryServicesMap = new HashMap<>();
		for(DiscoveryService<? extends A, ? super B, ? super C> discoveryService : discoveryServices) {
			for(String scheme : discoveryService.getSupportedSchemes()) {
				DiscoveryService<? extends A, ? super B, ? super C> previous = tmpDiscoveryServicesMap.put(scheme.toLowerCase(), (DiscoveryService<A, B, C>) discoveryService);
				if(previous != null) {
					throw new IllegalArgumentException("Multiple discovery services found for scheme " + scheme.toLowerCase() + ": " + previous + ", " + discoveryService);
				}
			}
		}
		this.discoveryServicesMap = Collections.unmodifiableMap(tmpDiscoveryServicesMap);
	}

	@Override
	public boolean supports(String scheme) {
		return this.discoveryServicesMap.containsKey(scheme);
	}

	@Override
	public Set<String> getSupportedSchemes() {
		return this.discoveryServicesMap.keySet();
	}

	@Override
	public Mono<? extends Service<A, B, C>> resolve(ServiceID serviceId, C trafficPolicy) {
		DiscoveryService<A, B, C> discoveryService = this.discoveryServicesMap.get(serviceId.getScheme());
		if(discoveryService == null) {
			throw new IllegalArgumentException("Unsupported scheme: " + serviceId.getScheme());
		}
		return discoveryService.resolve(serviceId, trafficPolicy);
	}
}
