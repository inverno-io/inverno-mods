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

import java.util.Set;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base {@link DiscoveryService} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 */
public abstract class AbstractDiscoveryService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>> implements DiscoveryService<A, B, C> {

	private final Set<String> supportedSchemes;

	/**
	 * <p>
	 * Creates a discovery service supporting the specified set of schemes.
	 * </p>
	 *
	 * @param supportedSchemes the set of supported schemes
	 */
	public AbstractDiscoveryService(Set<String> supportedSchemes) {
		this.supportedSchemes = supportedSchemes.stream().map(String::toLowerCase).collect(Collectors.toSet());
	}

	@Override
	public Set<String> getSupportedSchemes() {
		return this.supportedSchemes;
	}

	@Override
	public final Mono<? extends Service<A, B, C>> resolve(ServiceID serviceId, C trafficPolicy) throws IllegalArgumentException {
		if(!this.supports(serviceId)) {
			throw new IllegalArgumentException("Unsupported scheme: " + serviceId.getScheme());
		}
		return this.doResolve(serviceId, trafficPolicy);
	}

	/**
	 * <p>
	 * Resolves a service.
	 * </p>
	 *
	 * <p>
	 * This is basically invoked by {@link #resolve(ServiceID, TrafficPolicy)} after validating that the service ID scheme is supported.
	 * </p>
	 *
	 * @param serviceId     a service ID
	 * @param trafficPolicy a traffic policy
	 *
	 * @return a {@code Mono} emitting the resolved service or an empty {@code Mono} if no service could be resolved
	 */
	protected abstract Mono<? extends Service<A, B, C>> doResolve(ServiceID serviceId, C trafficPolicy);
}
