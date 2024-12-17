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

import io.inverno.mod.discovery.TrafficLoadBalancer;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An HTTP traffic load balancer that selects the service instance with the minimum load factor from a random subset of instances.
 * </p>
 *
 * <p>
 * In order to select a service instance, the load balancer first selects {@link #getChoiceCount()} instances and calculates a score for each of them based on the following formula:
 * </p>
 *
 * <pre>{@code
 * score = instance_weight * (1 - instance_load_factor) ^ bias
 * }</pre>
 *
 * <p>
 * The {@link #getBias()} is used to increase the importance of the load factor: the greater it is, the more instances with lower load factor are selected.
 * </p>
 *
 * <p>
 * The instance with the highest score is eventually then selected.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class MinLoadFactorTrafficLoadBalancer implements TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> {

	/**
	 * The default choice count.
	 */
	public static final int DEFAULT_CHOICE_COUNT = 2;

	/**
	 * The default bias on load factor.
	 */
	public static final int DEFAULT_BIAS = 1;

	private final HttpServiceInstance[] instances;
	private final int choiceCount;
	private final int bias;
	private final Mono<HttpServiceInstance> nextInstance;

	/**
	 * <p>
	 * Creates a minimum load factor traffic load balancer.
	 * </p>
	 *
	 * @param instances a collection of HTTP service instances
	 */
	public MinLoadFactorTrafficLoadBalancer(Collection<HttpServiceInstance> instances) {
		this(instances, DEFAULT_CHOICE_COUNT, DEFAULT_BIAS);
	}

	/**
	 * <p>
	 * Creates a minimum load factor traffic load balancer.
	 * </p>
	 *
	 * @param instances   a collection of HTTP service instances
	 * @param choiceCount the choice count
	 * @param bias        the bias on load factor
	 */
	public MinLoadFactorTrafficLoadBalancer(Collection<HttpServiceInstance> instances, int choiceCount, int bias) {
		this.instances = instances.toArray(HttpServiceInstance[]::new);
		this.choiceCount = Math.min(choiceCount, this.instances.length);
		this.bias = bias;
		this.nextInstance = Mono.fromSupplier(() -> {
			Stream<HttpServiceInstance> randomInstances;
			if(this.instances.length == this.choiceCount) {
				randomInstances = Arrays.stream(this.instances);
			}
			else {
				BitSet bs = new BitSet(this.instances.length);
				ThreadLocalRandom random = ThreadLocalRandom.current();
				int cardinality = 0;
				while(cardinality < this.choiceCount) {
					int randomIndex = random.nextInt(this.instances.length);
					if(!bs.get(randomIndex)) {
						bs.set(randomIndex);
						cardinality++;
					}
				}
				randomInstances = bs.stream().mapToObj(i -> this.instances[i]);
			}
			return randomInstances
				.max(Comparator.comparing(e -> e.getWeight() * Math.pow(1 - e.getLoadFactor(), this.bias)))
				.orElse(null);
		});
	}

	/**
	 * <p>
	 * Returns the number of service instances to consider when selecting an instance.
	 * </p>
	 *
	 * @return the choice count
	 */
	public int getChoiceCount() {
		return choiceCount;
	}

	/**
	 * <p>
	 * Returns the load factor bias used to increase the importance of load factor when selecting an instance.
	 * </p>
	 *
	 * <p>
	 * The greater it is, the more instances with lower load factor are selected.
	 * </p>
	 *
	 * @return the load factor bias
	 */
	public int getBias() {
		return bias;
	}

	@Override
	public Mono<HttpServiceInstance> next(UnboundExchange<?> serviceRequest) {
		return this.nextInstance;
	}

	/**
	 * <p>
	 * A minimum load factor traffic load balancer factory.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public static class Factory implements TrafficLoadBalancer.Factory<HttpServiceInstance, UnboundExchange<?>> {

		private final int choiceCount;
		private final int bias;

		/**
		 * <p>
		 * Creates a minimum load factor traffic load balancer factory.
		 * </p>
		 *
		 * @param choiceCount the choice count
		 * @param bias        the bias on load factor
		 */
		public Factory(int choiceCount, int bias) {
			this.choiceCount = choiceCount;
			this.bias = bias;
		}

		@Override
		public TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> create(Collection<HttpServiceInstance> instances) {
			return new MinLoadFactorTrafficLoadBalancer(instances, this.choiceCount, this.bias);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Factory factory = (Factory) o;
			return choiceCount == factory.choiceCount && bias == factory.bias;
		}

		@Override
		public int hashCode() {
			return Objects.hash(choiceCount, bias);
		}
	}
}
