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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An HTTP traffic load balancer that selects the service instance with the least current active requests from a random subset of instances.
 * </p>
 *
 * <p>
 * In order to select a service instance, the load balancer first selects {@link #getChoiceCount()} instances and calculates a score to each of them based on the following formula:
 * </p>
 *
 * <pre>{@code
 * score = instance_weight / (instance_active_requests + 1) ^ bias
 * }</pre>
 *
 * <p>
 * The {@link #getBias()} is used to increase the importance of active requests: the greater it is, the more instances with lower active requests count are selected.
 * </p>
 *
 * <p>
 * The instance with the highest score is eventually then selected.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class LeastRequestTrafficLoadBalancer implements TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> {

	/**
	 * The default choice count.
	 */
	public static final int DEFAULT_CHOICE_COUNT = 2;

	/**
	 * The default bias on active requests.
	 */
	public static final int DEFAULT_BIAS = 1;

	private final List<HttpServiceInstance> weightedInstances;
	private final int choiceCount;
	private final int bias;
	private final Mono<HttpServiceInstance> nextInstance;

	/**
	 * <p>
	 * Creates a least request traffic load balancer.
	 * </p>
	 *
	 * @param weightedInstances a collection of HTTP service instances
	 */
	public LeastRequestTrafficLoadBalancer(Collection<HttpServiceInstance> weightedInstances) {
		this(weightedInstances, DEFAULT_CHOICE_COUNT, DEFAULT_BIAS);
	}

	/**
	 * <p>
	 * Creates a least request traffic load balancer with the specified choice count and active request bias.
	 * </p>
	 *
	 * @param weightedInstances  a collection of weighted instances
	 * @param choiceCount        the choice count
	 * @param bias               the bias on active requests
	 */
	public LeastRequestTrafficLoadBalancer(Collection<HttpServiceInstance> weightedInstances, int choiceCount, int bias) {
		this.weightedInstances = new ArrayList<>(weightedInstances);
		this.choiceCount = choiceCount;
		this.bias = bias;
		this.nextInstance = Mono.fromSupplier(() -> {
			Stream<HttpServiceInstance> randomInstances;
			if(this.weightedInstances.size() == this.choiceCount) {
				randomInstances = this.weightedInstances.stream();
			}
			else {
				BitSet bs = new BitSet(this.weightedInstances.size());
				ThreadLocalRandom random = ThreadLocalRandom.current();
				int cardinality = 0;
				while(cardinality < this.choiceCount) {
					int randomIndex = random.nextInt(this.weightedInstances.size());
					if(!bs.get(randomIndex)) {
						bs.set(randomIndex);
						cardinality++;
					}
				}
				randomInstances = bs.stream().mapToObj(this.weightedInstances::get);
			}
			return randomInstances
				.max(Comparator.comparing(e -> e.getWeight() / Math.pow(e.getActiveRequests() + 1, this.bias)))
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
	 * Returns the active requests bias used to increase the importance of active requests when selecting an instance.
	 * </p>
	 *
	 * <p>
	 * The greater it is, the more instances with lower active requests count are selected.
	 * </p>
	 *
	 * @return the active requests bias
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
	 * A Least request traffic load balancer factory.
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
		 * Creates a least request traffic load balancer factory.
		 * </p>
		 *
		 * @param choiceCount the choice count
		 * @param bias        the bias on active requests
		 */
		public Factory(int choiceCount, int bias) {
			this.choiceCount = choiceCount;
			this.bias = bias;
		}

		@Override
		public TrafficLoadBalancer<HttpServiceInstance, UnboundExchange<?>> create(Collection<HttpServiceInstance> instances) {
			return new LeastRequestTrafficLoadBalancer(instances, this.choiceCount, this.bias);
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
