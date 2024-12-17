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
 * A service resolved by configuration.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 * @param <D> the service descriptor type
 */
public abstract class AbstractConfigurationService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>, D> implements Service<A, B, C> {

	/**
	 * The service ID.
	 */
	protected final ServiceID serviceId;
	private final Mono<D> serviceMetadata;

	private volatile long lastRefreshed;
	/**
	 * The traffic policy.
	 */
	protected volatile C trafficPolicy;

	/**
	 * <p>
	 * Creates a configuration service.
	 * </p>
	 *
	 * @param serviceId       the service ID
	 * @param serviceMetadata the service metadata
	 */
	public AbstractConfigurationService(ServiceID serviceId, Mono<D> serviceMetadata) {
		this.serviceId = serviceId;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public ServiceID getID() {
		return serviceId;
	}

	@Override
	public C getTrafficPolicy() {
		return trafficPolicy;
	}

	@Override
	public final Mono<? extends Service<A, B, C>> refresh(C trafficPolicy) {
		return this.serviceMetadata
			.flatMap(metadata -> this.doRefresh(trafficPolicy, metadata))
			.doFinally(ign -> this.lastRefreshed = System.currentTimeMillis());
	}

	@Override
	public long getLastRefreshed() {
		return lastRefreshed;
	}

	/**
	 * <p>
	 * Returns the service refreshed with the specified metadata and traffic policy.
	 * </p>
	 *
	 * @param trafficPolicy   a traffic policy
	 * @param serviceMetadata the service metadata
	 *
	 * @return a {@code Mono} emitting the service or an empty {@code Mono} if no service instance could be found
	 */
	protected abstract Mono<? extends Service<A, B, C>> doRefresh(C trafficPolicy, D serviceMetadata);
}
