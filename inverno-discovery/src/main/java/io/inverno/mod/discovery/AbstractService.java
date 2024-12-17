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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;

/**
 * <p>
 * Base {@link Service} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 */
public abstract class AbstractService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>> implements ManageableService<A, B, C> {

	private static final Logger LOGGER = LogManager.getLogger(AbstractService.class);

	/**
	 * The service ID.
	 */
	protected final ServiceID serviceId;

	private volatile long lastRefreshed;
	private volatile C trafficPolicy;
	private volatile Map<Integer, A> instances;
	private volatile TrafficLoadBalancer<A, B> loadBalancer;

	/**
	 * <p>
	 * Creates a service.
	 * </p>
	 *
	 * @param serviceId the service ID
	 */
	public AbstractService(ServiceID serviceId) {
		this.serviceId = serviceId;
		this.instances = Map.of();
	}

	@Override
	public ServiceID getID() {
		return this.serviceId;
	}

	@Override
	public C getTrafficPolicy() {
		return this.trafficPolicy;
	}

	@Override
	public Mono<? extends A> getInstance(B serviceRequest) {
		return Mono.defer(() -> {
			if(this.loadBalancer == null) {
				throw new IllegalStateException("Service is " + this.serviceId + " is gone");
			}
			return this.loadBalancer.next(serviceRequest);
		});
	}

	@Override
	public List<A> getInstances() {
		return new ArrayList<>(this.instances.values());
	}

	/**
	 * <p>
	 * Refreshes the service instances keeping the pre-existing instances, creating added instances and closing removed instances.
	 * </p>
	 * 
	 * <p>
	 * The load balancer is eventually recreated with the refreshed list of instances.
	 * </p>
	 * 
	 * @see #resolveInstances(TrafficPolicy)
	 */
	@Override
	public Mono<? extends Service<A, B, C>> refresh(C trafficPolicy) {
		return this.resolveInstances(trafficPolicy)
			.mapNotNull(instances -> {
				Map<Integer, Supplier<A>> resolvedInstances = new HashMap<>(instances);
				Map<Integer, A> newInstances = new HashMap<>(this.instances);

				// keep same instances, shutdown removed ones, add new ones
				for(Iterator<Map.Entry<Integer,A>> instanceIterator = newInstances.entrySet().iterator(); instanceIterator.hasNext();) {
					Map.Entry<Integer,A> instance = instanceIterator.next();
					if(resolvedInstances.remove(instance.getKey()) == null) {
						instanceIterator.remove();
						instance.getValue().shutdownGracefully()
							.doOnError(e -> LOGGER.error("Graceful shutdown error", e))
							.subscribe();
					}
				}

				resolvedInstances.forEach((key, instanceFactory) -> newInstances.put(key, instanceFactory.get()));

				// We have to synchronize to prevent race conditions, this is not ideal but invoking refresh concurrently would be an issue anyway: don't invoke refresh concurrently
				synchronized(this) {
					this.loadBalancer = newInstances.isEmpty() ? null : trafficPolicy.getLoadBalancer(newInstances.values());
					this.instances = newInstances;
					this.trafficPolicy = trafficPolicy;
				}
				return this.loadBalancer != null ? this : null;
			})
			.doFinally(ign -> this.lastRefreshed = System.currentTimeMillis());
	}

	/**
	 * <p>
	 * Resolves and returns the service instances using the specified traffic policy.
	 * </p>
	 *
	 * <p>
	 * The traffic policy must be considered to uniquely identify a service instance as it might impact how a service instance is eventually created. For instance, considering a server instance, it is
	 * uniquely identified by an Inet Socket Address and the traffic policy which specifies how to connect to the server. Such identifier is typically obtained with
	 * {@code Objects.hash(address, trafficPolicy)}.
	 * </p>
	 *
	 * @param trafficPolicy a traffic policy
	 *
	 * @return a map with unique service instance identifiers as key and service instance provider as value
	 */
	protected abstract Mono<Map<Integer, Supplier<A>>> resolveInstances(C trafficPolicy);

	@Override
	public long getLastRefreshed() {
		return this.lastRefreshed;
	}

	@Override
	public synchronized Mono<Void> shutdown() {
		return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.instances.values()).map(ServiceInstance::shutdown))
			.doFirst(() -> {
				this.instances = null;
				this.loadBalancer = null;
			})
			.then();
	}

	@Override
	public synchronized Mono<Void> shutdownGracefully() {
		return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.instances.values()).map(ServiceInstance::shutdownGracefully))
			.doFirst(() -> {
				this.instances = null;
				this.loadBalancer = null;
			})
			.then();
	}
}
