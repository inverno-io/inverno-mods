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

import io.inverno.mod.base.net.NetService;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base DNS based discovery service.
 * </p>
 *
 * <p>
 * {@link NetService#resolveAll(InetSocketAddress)} is used to resolve services identified by an unresolved Inet Socket Address obtained from the service ID with
 * {@link #createUnresolvedAddress(ServiceID)}. The DNS lookup might return one or more resolved Inet Socket Addresses from which service instances are created with
 * {@link #createServiceInstance(ServiceID, TrafficPolicy, InetSocketAddress)}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 */
public abstract class AbstractDnsDiscoveryService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>> extends AbstractDiscoveryService<A, B, C> {

	private final NetService netService;

	/**
	 * <p>
	 * Creates a DNS discovery service.
	 * </p>
	 *
	 * @param netService       the net service
	 * @param supportedSchemes the set of supported schemes
	 */
	public AbstractDnsDiscoveryService(NetService netService, Set<String> supportedSchemes) {
		super(supportedSchemes);
		this.netService = netService;
	}

	/**
	 * <p>
	 * Returns the unresolved Inet Socket Address deduced from the specified service ID.
	 * </p>
	 *
	 * @param serviceId a service ID
	 *
	 * @return an unresolved Inet Socket Address
	 */
	protected abstract InetSocketAddress createUnresolvedAddress(ServiceID serviceId);

	/**
	 * <p>
	 * Creates a service instance from the specified resolved Inet Socket Address.
	 * </p>
	 *
	 * @param serviceId the service ID
	 * @param trafficPolicy the traffic policy
	 * @param resolvedAddress the resolved address
	 *
	 * @return a new service instance
	 */
	protected abstract A createServiceInstance(ServiceID serviceId, C trafficPolicy, InetSocketAddress resolvedAddress);

	@Override
	protected final Mono<? extends Service<A, B, C>> doResolve(ServiceID serviceId, C trafficPolicy) {
		DnsService dnsService = new DnsService(serviceId);
		return dnsService.refresh(trafficPolicy);
	}

	/**
	 * <p>
	 * A service resolved by DNS lookup.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class DnsService extends AbstractService<A, B, C> {

		private final InetSocketAddress unresolvedSocketAddress;

		/**
		 * <p>
		 * Creates a DNS service.
		 * </p>
		 *
		 * @param serviceId the service ID
		 */
		public DnsService(ServiceID serviceId) {
			super(serviceId);
			this.unresolvedSocketAddress = AbstractDnsDiscoveryService.this.createUnresolvedAddress(serviceId);
		}

		@Override
		protected Mono<Map<Integer, Supplier<A>>> resolveInstances(C trafficPolicy) {
			return AbstractDnsDiscoveryService.this.netService
				.resolveAll(this.unresolvedSocketAddress)
				.filter(addresses -> !addresses.isEmpty())
				.map(addresses -> {
					Map<Integer, Supplier<A>> resolvedInstances = new HashMap<>();
					for(InetSocketAddress address : addresses) {
						resolvedInstances.put(Objects.hash(address, trafficPolicy), () -> AbstractDnsDiscoveryService.this.createServiceInstance(this.serviceId, trafficPolicy, address));
					}
					return resolvedInstances;
				});
		}
	}
}
