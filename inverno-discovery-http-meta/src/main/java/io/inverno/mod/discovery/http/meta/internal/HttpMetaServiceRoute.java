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

import io.inverno.mod.base.concurrent.CommandExecutor;
import io.inverno.mod.discovery.Weighted;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An HTTP meta service route.
 * </p>
 *
 * <p>
 * It is responsible for load balancing traffic among its destinations following the route load balancing strategy which must support unmanageable services
 * (see {@link HttpTrafficPolicy.LoadBalancingStrategy#isSupportUnmanageable()}).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class HttpMetaServiceRoute extends AbstractHttpMetaServiceRoute<HttpMetaServiceRouteDestination, HttpMetaServiceRoute> {

	private final UnaryOperator<HttpTrafficPolicy> trafficPolicyOverride;

	private DestinationsLoadBalancer destinationsLoadBalancer;

	/**
	 * <p>
	 * Creates an HTTP meta service route.
	 * </p>
	 *
	 * @param descriptor            the HTTP meta service route descriptor
	 * @param trafficPolicy         the original traffic policy
	 * @param trafficPolicyOverride the traffic policy override
	 * @param destinations          the HTTP meta service route destinations
	 */
	public HttpMetaServiceRoute(HttpMetaServiceDescriptor.RouteDescriptor descriptor, HttpTrafficPolicy trafficPolicy, UnaryOperator<HttpTrafficPolicy> trafficPolicyOverride, List<HttpMetaServiceRouteDestination> destinations) {
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
	 * @param destinations  the list of destinations
	 */
	private void refreshLoadBalancer(HttpTrafficPolicy trafficPolicy, List<HttpMetaServiceRouteDestination> destinations) {
		switch(trafficPolicy.getLoadBalancingStrategy()) {
			case RANDOM: this.destinationsLoadBalancer = new RandomLoadDestinationsBalancer(destinations);
				break;
			case ROUND_ROBIN: this.destinationsLoadBalancer = new RoundRobinDestinationsLoadBalancer(destinations);
				break;
			default: throw new IllegalStateException("Load balancing strategy not supported in the presence of an unmanageable destination service: " + trafficPolicy.getLoadBalancingStrategy());
		}
	}

	@Override
	protected Mono<? extends HttpServiceInstance> resolveInstance(UnboundExchange<?> serviceRequest) {
		return this.destinationsLoadBalancer.next().flatMap(destination -> destination.getInstance(serviceRequest));
	}

	@Override
	public Mono<HttpMetaServiceRoute> refresh(HttpTrafficPolicy trafficPolicy) {

		// TODO what to do when a service fails to refresh?
		// - we can have multiple reasons for this (DNS is no longer available, K8S API server connection error...)
		// - we should report the error and then maybe do nothing keeping current state
		// - circuit breaker should take over in case requests to that particular destination starts failing, the service itself has nothing to do with service discovery so as long as it is working let's keep it working
		// => we'd probably have to handle this here providing some circuit breaking policy
		// circuit breaker is actually quite simple like retry actually: after n errors assume service is down and park it, retry after n seconds
		// this should be completely independent from this
		// TODO what to do when a service is gone (i.e. empty is returned?)
		// - it should be parked until it gets back
		// - get instance on that particular service should just result in an error so that a circuit breaker can do something about it
		// - that's a bit different than when a single destination fails

		// 2 cases:
		// - no destinations could be resolved after merge, i.e. all refresh calls result in empty monos: all destinations are basically gone according to the discovery service
		//   - the whole service could be "parked" calling getInstance() resulting in an error because we should have no instance to return
		//   - the destinations we have might still work
		//   - this might fail eventually, we need actual use case like this to determine what to do:
		//     - how can all destinations be gone? and when that happen what should be the expected outcome? Is failing when requesting outdated destinations and waiting for destination to be back up the right answer?
		// - destinations are failing
		//   - it can be all destination or some specific destinations
		//   - evicting faulty instances should be the responsibility of the wrapped service
		//   - this might require some configuration to be provided in the traffic policy
		//   - let's do nothing for now this is related to circuit breaking which requires more thinking

		HttpTrafficPolicy overriddenTrafficPolicy = this.trafficPolicyOverride.apply(trafficPolicy);
		return Flux.fromIterable(this.destinations)
			.flatMap(destination -> destination.refresh(overriddenTrafficPolicy))
			.collectList()
			.mapNotNull(refreshedDestinations -> refreshedDestinations.isEmpty() ? null : this);
	}

	/**
	 * <p>
	 * A base destinations load balancer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private interface DestinationsLoadBalancer {

		/**
		 * <p>
		 * Returns the next destination.
		 * </p>
		 *
		 * @return a {@code Mono} emitting the next destination
		 */
		Mono<HttpMetaServiceRouteDestination> next();
	}

	/**
	 * <p>
	 * A round-robin destinations load balancer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private static class RoundRobinDestinationsLoadBalancer implements DestinationsLoadBalancer {

		private final CommandExecutor<Void> commandExecutor;
		private final Mono<HttpMetaServiceRouteDestination> nextDestination;

		private Node node;

		/**
		 * <p>
		 * Creates a round-robin destinations load balancer.
		 * </p>
		 *
		 * @param destinations a list of destinations
		 */
		private RoundRobinDestinationsLoadBalancer(List<HttpMetaServiceRouteDestination> destinations) {
			this.commandExecutor = new CommandExecutor<>(null);
			for(HttpMetaServiceRouteDestination destination : Weighted.expandToLoadBalanced(destinations)) {
				if(this.node == null) {
					this.node = new Node(destination);
					this.node.next = this.node;
				}
				else {
					Node previousNode = this.node;
					this.node = new Node(destination);
					this.node.next = previousNode.next;
					previousNode.next = this.node;
				}
			}
			this.nextDestination = Mono.create(sink -> this.commandExecutor.execute(ign -> {
				Node next = this.node;
				this.node = next.next;
				sink.success(next.destination);
			}));
		}

		@Override
		public Mono<HttpMetaServiceRouteDestination> next() {
			return this.nextDestination;
		}

		/**
		 * <p>
		 * A node holding a destination in a linked list of instances.
		 * </p>
		 *
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.12
		 */
		private static class Node {

			/**
			 * The node destination.
			 */
			final HttpMetaServiceRouteDestination destination;

			/**
			 * The next node.
			 */
			Node next;

			/**
			 * <p>
			 * Creates a destination node.
			 * </p>
			 *
			 * @param destination a destination
			 */
			public Node(HttpMetaServiceRouteDestination destination) {
				this.destination = destination;
			}
		}
	}

	/**
	 * <p>
	 * A random destinations load balancer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private static class RandomLoadDestinationsBalancer implements DestinationsLoadBalancer {

		private final HttpMetaServiceRouteDestination[] destinations;
		private final Mono<HttpMetaServiceRouteDestination> nextDestination;

		/**
		 * <p>
		 * Creates a random destinations load balancer.
		 * </p>
		 *
		 * @param destinations a list of destinations
		 */
		private RandomLoadDestinationsBalancer(List<HttpMetaServiceRouteDestination> destinations) {
			this.destinations = Weighted.expandToLoadBalanced(destinations).toArray(HttpMetaServiceRouteDestination[]::new);
			this.nextDestination = Mono.fromSupplier(() -> this.destinations[ThreadLocalRandom.current().nextInt(this.destinations.length)]);
		}

		@Override
		public Mono<HttpMetaServiceRouteDestination> next() {
			return this.nextDestination;
		}
	}
}
