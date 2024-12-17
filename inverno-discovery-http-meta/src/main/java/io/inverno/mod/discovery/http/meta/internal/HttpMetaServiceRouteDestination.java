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

import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.Weighted;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.function.UnaryOperator;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An HTTP meta service route destination.
 * </p>
 *
 * <p>
 * It is associated with an HTTP {@link Service} used to process HTTP service requests after transformation, routing and load balancing.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class HttpMetaServiceRouteDestination implements Weighted, Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> {

	private final HttpMetaServiceDescriptor.DestinationDescriptor descriptor;
	private final UnaryOperator<HttpTrafficPolicy> trafficPolicyOverride;
	private final Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> service;

	private final HttpMetaServiceExchangeTransformer exchangeTransformer;

	/**
	 * <p>
	 * Creates an HTTP meta service route destination.
	 * </p>
	 *
	 * @param descriptor            a destination descriptor
	 * @param trafficPolicyOverride a traffic policy override
	 * @param service               an HTTP service
	 */
	public HttpMetaServiceRouteDestination(HttpMetaServiceDescriptor.DestinationDescriptor descriptor, UnaryOperator<HttpTrafficPolicy> trafficPolicyOverride, Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> service) {
		this.descriptor = descriptor;
		this.trafficPolicyOverride = trafficPolicyOverride;
		this.service = service;

		this.exchangeTransformer = HttpMetaServiceExchangeTransformer.from(descriptor);
	}

	/**
	 * <p>
	 * Returns the HTTP meta service route destination descriptor.
	 * </p>
	 *
	 * @return a route destination descriptor
	 */
	public HttpMetaServiceDescriptor.DestinationDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public int getWeight() {
		return this.descriptor.getWeight();
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

	/**
	 * <p>
	 * Returns the HTTP service.
	 * </p>
	 *
	 * @return the HTTP service
	 */
	public Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> getService() {
		return this.service;
	}

	@Override
	public ServiceID getID() {
		return this.service.getID();
	}

	@Override
	public HttpTrafficPolicy getTrafficPolicy() {
		return this.service.getTrafficPolicy();
	}

	@Override
	public Mono<? extends HttpServiceInstance> getInstance(UnboundExchange<?> serviceRequest) {
		if(this.exchangeTransformer != null) {
			this.exchangeTransformer.transform(serviceRequest);
		}
		return this.service.getInstance(serviceRequest);
	}

	@Override
	public Mono<? extends Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy>> refresh(HttpTrafficPolicy trafficPolicy) {
		return this.service.refresh(this.trafficPolicyOverride.apply(trafficPolicy)).map(ign -> this);
	}

	@Override
	public long getLastRefreshed() {
		return this.service.getLastRefreshed();
	}

	@Override
	public Mono<Void> shutdown() {
		return this.service.shutdown();
	}

	@Override
	public Mono<Void> shutdownGracefully() {
		return this.service.shutdownGracefully();
	}
}
