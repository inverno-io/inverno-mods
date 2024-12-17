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
package io.inverno.mod.discovery.http.meta.internal;

import io.inverno.mod.discovery.TrafficLoadBalancer;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A managed HTTP meta service route.
 * </p>
 *
 * <p>
 * A managed route is created when all its destinations are manageable. A manageable route is able to load balance the manageable services instances directly following the route traffic
 * policy. This especially makes it possible to use {@link io.inverno.mod.discovery.http.LeastRequestTrafficLoadBalancer LeastRequestTrafficLoadBalancer} or
 * {@link io.inverno.mod.discovery.http.MinLoadFactorTrafficLoadBalancer MinLoadFactorTrafficLoadBalancer} to load balance the route destinations.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class ManagedHttpMetaServiceRoute extends AbstractHttpMetaServiceRoute<ManageableHttpMetaServiceRouteDestination, ManagedHttpMetaServiceRoute> {

	private final UnaryOperator<HttpTrafficPolicy> trafficPolicyOverride;

	private TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> routeLoadBalancer;

	/**
	 * <p>
	 * Creates a managed HTTP meta service route.
	 * </p>
	 *
	 * @param descriptor            the HTTP meta service route descriptor
	 * @param trafficPolicy         the original traffic policy
	 * @param trafficPolicyOverride the traffic policy override
	 * @param destinations          the HTTP meta service route destinations
	 */
	public ManagedHttpMetaServiceRoute(HttpMetaServiceDescriptor.RouteDescriptor descriptor, HttpTrafficPolicy trafficPolicy, UnaryOperator<HttpTrafficPolicy> trafficPolicyOverride, List<ManageableHttpMetaServiceRouteDestination> destinations) {
		super(descriptor, destinations);
		this.trafficPolicyOverride = trafficPolicyOverride;
		this.refreshLoadBalancer(trafficPolicyOverride.apply(trafficPolicy), destinations);
	}

	/**
	 * <p>
	 * Refreshes the load balancer.
	 * </p>
	 *
	 * @param trafficPolicy the traffic policy
	 * @param destinations  the list of manageable destinations
	 */
	private void refreshLoadBalancer(HttpTrafficPolicy trafficPolicy, List<ManageableHttpMetaServiceRouteDestination> destinations) {
		// each dest has a weight and a list of instances,  each instance must be assigned the weight of the dest?
		// This only works if all dest have the same number of instances => we have to ponder

		this.routeLoadBalancer = trafficPolicy.getLoadBalancer(destinations.stream().flatMap(destination -> {
			final int instanceWeight = destinations.stream()
				.filter(other -> other != destination)
				.map(other -> other.getService().getInstances().size())
				.reduce(destination.getWeight(), (acc, size) -> acc * size);

			int instanceWeightDebug = destination.getWeight();
			for(ManageableHttpMetaServiceRouteDestination other : destinations) {
				if(other != destination) {
					instanceWeightDebug *= other.getService().getInstances().size();
				}
			}
			return destination.getService().getInstances().stream().map(serviceInstance -> new ManagedHttpServiceInstance(destination.getExchangeTransformer(), serviceInstance, instanceWeight));
		}).collect(Collectors.toList()));
	}

	@Override
	protected Mono<? extends HttpServiceInstance> resolveInstance(UnboundExchange<?> serviceRequest) {
		return this.routeLoadBalancer.next(serviceRequest)
			.doOnNext(serviceInstance -> {
				if(((ManagedHttpServiceInstance)serviceInstance).getExchangeTransformer() != null) {
					((ManagedHttpServiceInstance)serviceInstance).getExchangeTransformer().transform(serviceRequest);
				}
			});
	}

	@Override
	public Mono<ManagedHttpMetaServiceRoute> refresh(HttpTrafficPolicy trafficPolicy) {
		HttpTrafficPolicy overriddenTrafficPolicy = this.trafficPolicyOverride.apply(trafficPolicy);
		return Flux.fromIterable(this.destinations)
			.flatMap(destination -> destination.refresh(overriddenTrafficPolicy))
			.collectList()
			.mapNotNull(refreshedDestinations -> {
				if(refreshedDestinations.isEmpty()) {
					return null;
				}
				this.refreshLoadBalancer(overriddenTrafficPolicy, destinations);
				return this;
			});
	}

	/**
	 * <p>
	 * A managed HTTP service instance.
	 * </p>
	 *
	 * <p>
	 * The weight of a managed HTTP service instance is deduced from the route destination weight.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private static class ManagedHttpServiceInstance implements HttpServiceInstance {

		private final HttpMetaServiceExchangeTransformer exchangeTransformer;
		private final HttpServiceInstance serviceInstance;
		private final int weight;

		/**
		 * <p>
		 * Creates a managed HTTP service instance.
		 * </p>
		 *
		 * @param exchangeTransformer the exchange transformer
		 * @param serviceInstance     the original HTTP service instance
		 * @param weight              the weight
		 */
		public ManagedHttpServiceInstance(HttpMetaServiceExchangeTransformer exchangeTransformer, HttpServiceInstance serviceInstance, int weight) {
			this.exchangeTransformer = exchangeTransformer;
			this.serviceInstance = serviceInstance;
			this.weight = weight;
			if(weight <= 0) {
				throw new IllegalArgumentException("weight must be a positive integer");
			}
		}

		/**
		 * <p>
		 * Returns the exchange transformer.
		 * </p>
		 *
		 * @return the exchange transformer
		 */
		public HttpMetaServiceExchangeTransformer getExchangeTransformer() {
			return exchangeTransformer;
		}

		@Override
		public int getWeight() {
			return this.weight;
		}

		@Override
		public <T extends ExchangeContext> Exchange<T> bind(UnboundExchange<T> exchange) throws IllegalStateException {
			return this.serviceInstance.bind(exchange);
		}

		@Override
		public long getActiveRequests() {
			return this.serviceInstance.getActiveRequests();
		}

		@Override
		public float getLoadFactor() {
			return this.serviceInstance.getLoadFactor();
		}

		@Override
		public Mono<Void> shutdown() {
			return this.serviceInstance.shutdown();
		}

		@Override
		public Mono<Void> shutdownGracefully() {
			return this.serviceInstance.shutdownGracefully();
		}
	}
}
