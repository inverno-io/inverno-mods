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
package io.inverno.mod.web.client.internal.discovery;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.discovery.CachingDiscoveryService;
import io.inverno.mod.discovery.CompositeDiscoveryService;
import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.http.client.UnboundExchange;
import io.inverno.mod.web.client.WebClientConfiguration;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A Web discovery service bean.
 * </p>
 *
 * <p>
 * This implementation is a {@link CachingDiscoveryService caching} {@link CompositeDiscoveryService composite} discovery service which composes the HTTP discovery services injected in the module and
 * cache resolved services which are refreshed every {@link WebClientConfiguration#discovery_service_ttl()} milliseconds.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class WebDiscoveryService extends CachingDiscoveryService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> implements HttpDiscoveryService {

	private final HttpTrafficPolicy trafficPolicy;

	/**
	 * <p>
	 * Creates a Web discovery service.
	 * </p>
	 *
	 * @param configuration           the Web client module configuration
	 * @param reactor                 the reactor
	 * @param discoveryServices       a list of HTTP discovery service to compose
	 */
	public WebDiscoveryService(WebClientConfiguration configuration, Reactor reactor, List<? extends HttpDiscoveryService> discoveryServices) {
		super(reactor, new CompositeDiscoveryService<>(discoveryServices), configuration.discovery_service_ttl());

		HttpTrafficPolicy.Builder trafficPolicyBuilder = HttpTrafficPolicy.builder()
			.configuration(configuration.http_client())
			.netConfiguration(configuration.net_client());

		switch(configuration.load_balancing_strategy()) {
			case RANDOM: trafficPolicyBuilder.randomLoadBalancer();
				break;
			case ROUND_ROBIN: trafficPolicyBuilder.roundRobinLoadBalancer();
				break;
			case LEAST_REQUEST: trafficPolicyBuilder.leastRequestLoadBalancer(configuration.least_request_load_balancer_choice_count(), configuration.least_request_load_balancer_bias());
				break;
			case MIN_LOAD_FACTOR: trafficPolicyBuilder.minLoadFactorLoadBalancer(configuration.min_load_factor_load_balancer_choice_count(), configuration.min_load_factor_load_balancer_bias());
				break;
			default: throw new IllegalStateException("Unsupported load balancing strategy: " + configuration.load_balancing_strategy());
		}
		this.trafficPolicy = trafficPolicyBuilder.build();
	}

	@Override
	public Mono<? extends Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy>> resolve(ServiceID serviceId) {
		return super.resolve(serviceId, this.trafficPolicy);
	}

	@Destroy
	public void stop() {
		this.shutdown().block();
	}
}
