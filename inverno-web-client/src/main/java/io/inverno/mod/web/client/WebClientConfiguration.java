/*
 * Copyright 2024 Jeremy KUHN
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
package io.inverno.mod.web.client;

import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.discovery.CachingDiscoveryService;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.LeastRequestTrafficLoadBalancer;
import io.inverno.mod.discovery.http.MinLoadFactorTrafficLoadBalancer;
import io.inverno.mod.http.client.HttpClientConfiguration;

/**
 * <p>
 * Web client module configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Configuration( name = "configuration" )
public interface WebClientConfiguration {

	/**
	 * <p>
	 * The service cache time-to-live in milliseconds.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@value CachingDiscoveryService#DEFAULT_TIME_TO_lIVE}.
	 * </p>
	 *
	 * @return the service cache time-to-live
	 */
	default long discovery_service_ttl() {
		return CachingDiscoveryService.DEFAULT_TIME_TO_lIVE;
	}

	/**
	 * <p>
	 * The default service load balancing strategy.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@link HttpTrafficPolicy.LoadBalancingStrategy#RANDOM}.
	 * </p>
	 *
	 * @return the default load balancing strategy
	 */
	default HttpTrafficPolicy.LoadBalancingStrategy load_balancing_strategy() {
		return HttpTrafficPolicy.LoadBalancingStrategy.RANDOM;
	}

	/**
	 * <p>
	 * The least request load balancer choice count.
	 * </p>
	 *
	 * <p>
	 * This only applies when {@link #load_balancing_strategy()} is set to {@link HttpTrafficPolicy.LoadBalancingStrategy#LEAST_REQUEST}.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@value LeastRequestTrafficLoadBalancer#DEFAULT_CHOICE_COUNT}.
	 * </p>
	 *
	 * @return the least request load balancer choice count
	 */
	default int least_request_load_balancer_choice_count() {
		return LeastRequestTrafficLoadBalancer.DEFAULT_CHOICE_COUNT;
	}

	/**
	 * <p>
	 * The least request load balancer bias.
	 * </p>
	 *
	 * <p>
	 * This only applies when {@link #load_balancing_strategy()} is set to {@link HttpTrafficPolicy.LoadBalancingStrategy#LEAST_REQUEST}.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@value LeastRequestTrafficLoadBalancer#DEFAULT_BIAS}.
	 * </p>
	 *
	 * @return the least request load balancer bias
	 */
	default int least_request_load_balancer_bias() {
		return LeastRequestTrafficLoadBalancer.DEFAULT_BIAS;
	}

	/**
	 * <p>
	 * The min load factor load balancer choice count.
	 * </p>
	 *
	 * <p>
	 * This only applies when {@link #load_balancing_strategy()} is set to {@link HttpTrafficPolicy.LoadBalancingStrategy#MIN_LOAD_FACTOR}.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@value MinLoadFactorTrafficLoadBalancer#DEFAULT_CHOICE_COUNT}.
	 * </p>
	 *
	 * @return the min load factor load balancer choice count
	 */
	default int min_load_factor_load_balancer_choice_count() {
		return MinLoadFactorTrafficLoadBalancer.DEFAULT_CHOICE_COUNT;
	}

	/**
	 * <p>
	 * The min load factor load balancer bias.
	 * </p>
	 *
	 * <p>
	 * This only applies when {@link #load_balancing_strategy()} is set to {@link HttpTrafficPolicy.LoadBalancingStrategy#MIN_LOAD_FACTOR}.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@value MinLoadFactorTrafficLoadBalancer#DEFAULT_BIAS}.
	 * </p>
	 *
	 * @return the min load factor load balancer bias
	 */
	default int min_load_factor_load_balancer_bias() {
		return MinLoadFactorTrafficLoadBalancer.DEFAULT_BIAS;
	}

	/**
	 * <p>
	 * The HTTP client configuration.
	 * </p>
	 *
	 * <p>
	 * Note that this configuration basically supersedes the configuration provided in the HTTP client module.
	 * </p>
	 *
	 * @return the HTTP client configuration
	 */
	HttpClientConfiguration http_client();

	/**
	 * <p>
	 * The Net client configuration.
	 * </p>
	 *
	 * <p>
	 * Note that this configuration basically supersedes the configuration provided indirectly in the HTTP client module by the boot module.
	 * </p>
	 *
	 * @return the Net client configuration
	 */
	NetClientConfiguration net_client();
}
