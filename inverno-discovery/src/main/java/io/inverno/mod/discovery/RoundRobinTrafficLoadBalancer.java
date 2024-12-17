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

import io.inverno.mod.base.concurrent.CommandExecutor;
import java.util.Collection;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A traffic load balancer that selects instances in a deterministic order by iterating on a list of instances.
 * </p>
 *
 * <p>
 * This implementation uses a {@link CommandExecutor} in order to return service instances in sequence.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 */
public class RoundRobinTrafficLoadBalancer<A extends ServiceInstance, B> implements TrafficLoadBalancer<A, B> {

	private final CommandExecutor<Void> commandExecutor;
	private final Mono<A> nextInstance;

	private Node current;

	/**
	 * <p>
	 * Creates a round-robin traffic load balancer.
	 * </p>
	 *
	 * @param instances a collection of service instances
	 */
	public RoundRobinTrafficLoadBalancer(Collection<A> instances) {
		this.commandExecutor = new CommandExecutor<>(null);

		for(A instance : instances) {
			if(this.current == null) {
				this.current = new Node(instance);
				this.current.next = this.current;
			}
			else {
				Node previousNode = this.current;
				this.current = new Node(instance);
				this.current.next = previousNode.next;
				previousNode.next = this.current;
			}
		}

		this.nextInstance = Mono.create(sink -> this.commandExecutor.execute(ign -> {
			Node next = this.current;
			this.current = next.next;
			sink.success(next.instance);
		}));
	}

	@Override
	public Mono<A> next(B serviceRequest) {
		return this.nextInstance;
	}

	/**
	 * <p>
	 * A node holding a service instance in a linked list of instances.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class Node {

		/**
		 * The node service instance.
		 */
		final A instance;

		/**
		 * The next node.
		 */
		Node next;

		/**
		 * <p>
		 * Creates a service instance node.
		 * </p>
		 *
		 * @param instance a service instance
		 */
		public Node(A instance) {
			this.instance = instance;
		}
	}
}
