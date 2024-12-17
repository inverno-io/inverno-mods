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

import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.discovery.TrafficLoadBalancer;
import io.inverno.mod.discovery.TrafficPolicy;
import io.inverno.mod.discovery.WeightedRandomTrafficLoadBalancer;
import io.inverno.mod.discovery.WeightedRoundRobinTrafficLoadBalancer;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.Collection;
import java.util.Objects;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An HTTP traffic policy defining load balancing strategy as well as configuration HTTP client and Net client configurations used when creating an HTTP client
 * {@link io.inverno.mod.http.client.Endpoint Endpoint} in a service instance.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see HttpServiceInstance
 */
public class HttpTrafficPolicy implements TrafficPolicy<HttpServiceInstance, UnboundExchange<?>> {

	/**
	 * <p>
	 * Load balancing strategies supported in HTTP services.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public enum LoadBalancingStrategy {
		//CONSISTENT_HASH,
		/**
		 * <p>
		 * Random load balancing strategy where service instances are randomly selected.
		 * </p>
		 *
		 * @see WeightedRandomTrafficLoadBalancer
		 */
		RANDOM(WeightedRandomTrafficLoadBalancer::new, true),
		/**
		 * <p>
		 * Round-robin load balancing strategy where service instances are selected in sequence.
		 * </p>
		 *
		 * @see WeightedRoundRobinTrafficLoadBalancer
		 */
		ROUND_ROBIN(WeightedRoundRobinTrafficLoadBalancer::new, true),
		/**
		 * <p>
		 * Least request load balancing strategy where service instances are selected based on their active requests count.
		 * </p>
		 *
		 * @see LeastRequestTrafficLoadBalancer
		 */
		LEAST_REQUEST(LeastRequestTrafficLoadBalancer::new, false),
		/**
		 * <p>
		 * Minimum load factor load balancing strategy where service instances are selected based on their load factor.
		 * </p>
		 *
		 * @see MinLoadFactorTrafficLoadBalancer
		 */
		MIN_LOAD_FACTOR(MinLoadFactorTrafficLoadBalancer::new, false);

		private final TrafficLoadBalancer.Factory<HttpServiceInstance, UnboundExchange<?>> defaultLoadBalancerFactory;
		private final boolean supportUnmanageable;

		/**
		 * <p>
		 * Creates a load balancing strategy.
		 * </p>
		 *
		 * @param loadBalancerFactory the default load balancer factory for the strategy
		 * @param supportUnmanageable true to indicate that the strategy supports unmanageable destination services
		 */
		LoadBalancingStrategy(TrafficLoadBalancer.Factory<HttpServiceInstance, UnboundExchange<?>> loadBalancerFactory, boolean supportUnmanageable) {
			this.defaultLoadBalancerFactory = loadBalancerFactory;
			this.supportUnmanageable = supportUnmanageable;
		}

		/**
		 * <p>
		 * Returns the default load balancer factory for the strategy.
		 * </p>
		 *
		 * @return a load balancer factory or null
		 */
		public TrafficLoadBalancer.Factory<HttpServiceInstance, UnboundExchange<?>> getDefaultLoadBalancerFactory() {
			return defaultLoadBalancerFactory;
		}

		/**
		 * <p>
		 * Determines whether the strategy supports unmanageable destination services.
		 * </p>
		 *
		 * <p>
		 * A manageable destination service implements {@link io.inverno.mod.discovery.ManageableService} and exposes its underlying service instances to an HTTP meta service which can then directly
		 * manage service instances from multiple destinations and load balance traffic among them within a route. An unmanageable destination service, on the other hand, is opaque and doesn't expose
		 * its service instances which limits the choice of load balancing strategies to those that don't require direct access to service instances (e.g. active requests, load factor...).
		 * </p>
		 *
		 * @return true if unmanageable services are supported, false otherwise
		 */
		public boolean isSupportUnmanageable() {
			return supportUnmanageable;
		}
	}

	/**
	 * The default load balancer strategy.
	 */
	public static final LoadBalancingStrategy DEFAULT_LOAD_BALANCER_STRATEGY = LoadBalancingStrategy.RANDOM;

	private final HttpClientConfiguration configuration;
	private final NetClientConfiguration netConfiguration;
	private final HttpTrafficPolicy.LoadBalancingStrategy loadBalancingStrategy;
	private final TrafficLoadBalancer.Factory<HttpServiceInstance, UnboundExchange<?>> loadBalancerFactory;

	/**
	 * <p>
	 * Creates an HTTP traffic policy.
	 * </p>
	 *
	 * @param configuration         the HTTP client configuration
	 * @param netConfiguration      the Net client configuration
	 * @param loadBalancingStrategy the load balancing strategy
	 * @param loadBalancerFactory   the load balancer factory
	 */
	private HttpTrafficPolicy(HttpClientConfiguration configuration, NetClientConfiguration netConfiguration, HttpTrafficPolicy.LoadBalancingStrategy loadBalancingStrategy, TrafficLoadBalancer.Factory<HttpServiceInstance, UnboundExchange<?>> loadBalancerFactory) {
		this.configuration = configuration;
		this.netConfiguration = netConfiguration;
		this.loadBalancingStrategy = loadBalancingStrategy;
		this.loadBalancerFactory = loadBalancerFactory;
	}

	/**
	 * <p>
	 * Returns the HTTP client configuration.
	 * </p>
	 *
	 * @return the HTTP client configuration or null to use the default configuration
	 */
	public HttpClientConfiguration getConfiguration() {
		return this.configuration;
	}

	/**
	 * <p>
	 * Returns the Net client configuration.
	 * </p>
	 *
	 * @return the Net client configuration or null to use the default configuration
	 */
	public NetClientConfiguration getNetConfiguration() {
		return this.netConfiguration;
	}

	/**
	 * <p>
	 * Returns the load balancer factory.
	 * </p>
	 *
	 * @return the load balancer factory
	 */
	public TrafficLoadBalancer.Factory<HttpServiceInstance, UnboundExchange<?>> getLoadBalancerFactory() {
		return loadBalancerFactory;
	}

	/**
	 * <p>
	 * Returns the load balancing strategy.
	 * </p>
	 *
	 * @return the load balancing strategy
	 */
	public HttpTrafficPolicy.LoadBalancingStrategy getLoadBalancingStrategy() {
		return loadBalancingStrategy;
	}

	/**
	 * <p>
	 * Returns an HTTP traffic policy builder.
	 * </p>
	 *
	 * @return an HTTP traffic policy builder
	 */
	public static HttpTrafficPolicy.Builder builder() {
		return new HttpTrafficPolicy.Builder();
	}

	/**
	 * <p>
	 * Returns an HTTP traffic policy builder from an original traffic policy.
	 * </p>
	 *
	 * @param originalTrafficPolicy the original traffic policy
	 *
	 * @return an HTTP traffic policy builder
	 */
	public static HttpTrafficPolicy.Builder builder(HttpTrafficPolicy originalTrafficPolicy) {
		return new HttpTrafficPolicy.Builder(originalTrafficPolicy);
	}

	@Override
	public TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> getLoadBalancer(Collection<HttpServiceInstance> instances) throws IllegalArgumentException {
		if(instances.isEmpty()) {
			throw new IllegalArgumentException("Empty instances");
		}
		if(instances.size() == 1) {
			return new SingleLoadBalancer(instances.iterator().next());
		}
		else {
			return this.loadBalancerFactory.create(instances);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HttpTrafficPolicy that = (HttpTrafficPolicy) o;
		return Objects.equals(configuration, that.configuration) && Objects.equals(netConfiguration, that.netConfiguration) && Objects.equals(loadBalancerFactory, that.loadBalancerFactory);
	}

	@Override
	public int hashCode() {
		return Objects.hash(configuration, netConfiguration, loadBalancerFactory);
	}

	/**
	 * <p>
	 * An HTTP traffic policy builder.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public static class Builder {

		private HttpClientConfiguration configuration;
		private NetClientConfiguration netConfiguration;
		private LoadBalancingStrategy loadBalancingStrategy;
		private TrafficLoadBalancer.Factory<HttpServiceInstance, UnboundExchange<?>> loadBalancerFactory;

		/**
		 * <p>
		 * Creates an HTTP traffic policy builder.
		 * </p>
		 */
		private Builder() {
			this.loadBalancingStrategy = DEFAULT_LOAD_BALANCER_STRATEGY;
			this.loadBalancerFactory = DEFAULT_LOAD_BALANCER_STRATEGY.getDefaultLoadBalancerFactory();
		}

		/**
		 * <p>
		 * Creates an HTTP traffic policy builder.
		 * </p>
		 *
		 * @param originalTrafficPolicy an original traffic policy
		 */
		private Builder(HttpTrafficPolicy originalTrafficPolicy) {
			this.configuration = originalTrafficPolicy.configuration;
			this.netConfiguration = originalTrafficPolicy.netConfiguration;
			this.loadBalancingStrategy = originalTrafficPolicy.loadBalancingStrategy;
			this.loadBalancerFactory = originalTrafficPolicy.loadBalancerFactory;
		}

		/**
		 * <p>
		 * Sets the HTTP client configuration.
		 * </p>
		 *
		 * @param configuration an HTTP client configuration
		 *
		 * @return the builder
		 */
		public Builder configuration(HttpClientConfiguration configuration) {
			this.configuration = configuration;
			return this;
		}

		/**
		 * <p>
		 * Sets the Net client configuration.
		 * </p>
		 *
		 * @param netConfiguration an Net client configuration
		 *
		 * @return the builder
		 */
		public Builder netConfiguration(NetClientConfiguration netConfiguration) {
			this.netConfiguration = netConfiguration;
			return this;
		}

		/**
		 * <p>
		 * Sets the load balancer factory corresponding to the specified strategy.
		 * </p>
		 *
		 * @param strategy a load balancing strategy
		 *
		 * @return the builder
		 */
		public Builder loadBalancer(HttpTrafficPolicy.LoadBalancingStrategy strategy) {
			this.loadBalancingStrategy = strategy;
			this.loadBalancerFactory = strategy.getDefaultLoadBalancerFactory();
			return this;
		}

		/**
		 * <p>
		 * Sets the {@link io.inverno.mod.discovery.RandomTrafficLoadBalancer RandomTrafficLoadBalancer} factory.
		 * </p>
		 *
		 * @return the builder
		 */
		public Builder randomLoadBalancer() {
			this.loadBalancingStrategy = LoadBalancingStrategy.RANDOM;
			this.loadBalancerFactory = LoadBalancingStrategy.RANDOM.getDefaultLoadBalancerFactory();
			return this;
		}

		/**
		 * <p>
		 * Sets the {@link io.inverno.mod.discovery.RoundRobinTrafficLoadBalancer RoundRobinTrafficLoadBalancer} factory.
		 * </p>
		 *
		 * @return the builder
		 */
		public Builder roundRobinLoadBalancer() {
			this.loadBalancingStrategy = LoadBalancingStrategy.ROUND_ROBIN;
			this.loadBalancerFactory = LoadBalancingStrategy.ROUND_ROBIN.getDefaultLoadBalancerFactory();
			return this;
		}

		/**
		 * <p>
		 * Sets the {@link LeastRequestTrafficLoadBalancer} factory.
		 * </p>
		 *
		 * @return the builder
		 */
		public Builder leastRequestLoadBalancer() {
			this.loadBalancingStrategy = LoadBalancingStrategy.LEAST_REQUEST;
			this.loadBalancerFactory = LoadBalancingStrategy.LEAST_REQUEST.getDefaultLoadBalancerFactory();
			return this;
		}

		/**
		 * <p>
		 * Sets the {@link LeastRequestTrafficLoadBalancer} factory.
		 * </p>
		 *
		 * @param choiceCount the choice count
		 * @param bias        the active requests bias
		 *
		 * @return the builder
		 */
		public Builder leastRequestLoadBalancer(int choiceCount, int bias) {
			this.loadBalancingStrategy = LoadBalancingStrategy.LEAST_REQUEST;
			this.loadBalancerFactory = new LeastRequestTrafficLoadBalancer.Factory(choiceCount, bias);
			return this;
		}

		/**
		 * <p>
		 * Sets the {@link MinLoadFactorTrafficLoadBalancer} factory.
		 * </p>
		 *
		 * @return the builder
		 */
		public Builder minLoadFactorLoadBalancer() {
			this.loadBalancingStrategy = LoadBalancingStrategy.MIN_LOAD_FACTOR;
			this.loadBalancerFactory = LoadBalancingStrategy.MIN_LOAD_FACTOR.getDefaultLoadBalancerFactory();
			return this;
		}

		/**
		 * <p>
		 * Sets the {@link MinLoadFactorTrafficLoadBalancer} factory.
		 * </p>
		 *
		 * @param choiceCount the choice count
		 * @param bias        the load factor bias
		 *
		 * @return the builder
		 */
		public Builder minLoadFactorLoadBalancer(int choiceCount, int bias) {
			this.loadBalancingStrategy = LoadBalancingStrategy.MIN_LOAD_FACTOR;
			this.loadBalancerFactory =  new MinLoadFactorTrafficLoadBalancer.Factory(choiceCount, bias);
			return this;
		}

		/**
		 * <p>
		 * Builds and returns the HTTP traffic policy.
		 * </p>
		 *
		 * @return an HTTP traffic policy
		 */
		public HttpTrafficPolicy build() {
			return new HttpTrafficPolicy(this.configuration, this.netConfiguration, this.loadBalancingStrategy, this.loadBalancerFactory);
		}
	}

	/**
	 * <p>
	 * A single instance load balancer implementation to be used when a service only has one single instance.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private static class SingleLoadBalancer implements TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> {

		private final Mono<HttpServiceInstance> instance;

		/**
		 * <p>
		 * Creates a single instance load balancer.
		 * </p>
		 *
		 * @param instance the HTTP service instance
		 */
		public SingleLoadBalancer(HttpServiceInstance instance) {
			this.instance = Mono.just(instance);
		}

		@Override
		public Mono<HttpServiceInstance> next(UnboundExchange<?> serviceRequest) {
			return this.instance;
		}
	}
}
