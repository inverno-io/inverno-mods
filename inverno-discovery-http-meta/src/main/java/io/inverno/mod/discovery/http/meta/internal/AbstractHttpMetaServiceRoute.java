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

import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;

/**
 * <p>
 * Base HTTP meta service route.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public abstract class AbstractHttpMetaServiceRoute<A extends HttpMetaServiceRouteDestination, B extends AbstractHttpMetaServiceRoute<A, B>> {

	private final HttpMetaServiceDescriptor.RouteDescriptor descriptor;
	protected final List<A> destinations;
	private final HttpMetaServiceExchangeTransformer exchangeTransformer;

	/**
	 * <p>
	 * Creates an HTTP meta service route.
	 * </p>
	 *
	 * @param descriptor   the HTTP meta service route descriptor
	 * @param destinations the HTTP meta service route destinations
	 */
	public AbstractHttpMetaServiceRoute(HttpMetaServiceDescriptor.RouteDescriptor descriptor, List<A> destinations) {
		this.descriptor = descriptor;
		this.destinations = destinations;
		this.exchangeTransformer = HttpMetaServiceExchangeTransformer.from(descriptor);
	}

	/**
	 * <p>
	 * Returns the HTTP meta service route descriptor.
	 * </p>
	 *
	 * @return the route descriptor
	 */
	public HttpMetaServiceDescriptor.RouteDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * <p>
	 * Returns the HTTP meta service route exchange transformer.
	 * </p>
	 *
	 * @return the exchange transformer.
	 */
	public HttpMetaServiceExchangeTransformer getExchangeTransformer() {
		return exchangeTransformer;
	}

	/**
	 * <p>
	 * Returns the HTTP meta service route destinations.
	 * </p>
	 *
	 * @return the route destinations
	 */
	public List<A> getDestinations() {
		return this.destinations;
	}

	/**
	 * <p>
	 * Transforms the specified exchange and returns a matching HTTP service instance.
	 * </p>
	 *
	 * <p>
	 * This method transforms the request using the request and response transformers defined in the route descriptor and and invokes {@link #resolveInstance(UnboundExchange)} to resolve the matching service
	 * instance.
	 * </p>
	 *
	 * @param serviceRequest the exchange to process
	 *
	 * @return a {@code Mono} emitting an HTTP service instance matching the exchange or an empty {@code Mono} if no instance was found
	 */
	public final Mono<? extends HttpServiceInstance> getInstance(UnboundExchange<?> serviceRequest) {
		if(this.exchangeTransformer != null) {
			this.exchangeTransformer.transform(serviceRequest);
		}
		return this.resolveInstance(serviceRequest);
	}

	/**
	 * <p>
	 * Resolves the HTTP service instance matching the specified exchange.
	 * </p>
	 *
	 * @param serviceRequest the exchange to process
	 *
	 * @return a {@code Mono} emitting an HTTP service instance matching the exchange or an empty {@code Mono} if no instance was found
	 */
	protected abstract Mono<? extends HttpServiceInstance> resolveInstance(UnboundExchange<?> serviceRequest);

	/**
	 * <p>
	 * Refreshes the HTTP meta service route.
	 * </p>
	 *
	 * <p>
	 * This method refreshes the route destinations and return an empty {@code Mono} when they no longer hold any service instance (i.e. destination is gone).
	 * </p>
	 *
	 * @param trafficPolicy a traffic policy
	 *
	 * @return a {@code Mono} emitting the refreshed route or an empty {@code Mono} if the route no longer exist
	 */
	public abstract Mono<B> refresh(HttpTrafficPolicy trafficPolicy);

	/**
	 * <p>
	 * Shutdowns the HTTP meta service route.
	 * </p>
	 *
	 * <p>
	 * This method shutdowns the route destinations.
	 * </p>
	 *
	 * @return a {@code Mono} which completes once the route is shutdown
	 */
	public Mono<Void> shutdown() {
		return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.destinations)
			.map(HttpMetaServiceRouteDestination::shutdown))
			.then();
	}

	/**
	 * <p>
	 * Gracefully shutdowns the HTTP meta service route.
	 * </p>
	 *
	 * <p>
	 * This method gracefully shutdowns the route destinations.
	 * </p>
	 *
	 * @return a {@code Mono} which completes once the route is shutdown
	 */
	public Mono<Void> shutdownGracefully() {
		return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.destinations)
			.map(HttpMetaServiceRouteDestination::shutdownGracefully))
			.then();
	}
}
