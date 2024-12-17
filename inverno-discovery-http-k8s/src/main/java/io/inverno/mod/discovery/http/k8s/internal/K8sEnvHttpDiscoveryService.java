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
package io.inverno.mod.discovery.http.k8s.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.discovery.AbstractDiscoveryService;
import io.inverno.mod.discovery.AbstractService;
import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.k8s.K8sHttpDiscoveryConfiguration;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.UnboundExchange;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Kubernetes environment HTTP discovery service.
 * </p>
 *
 * <p>
 * This implementation resolves Kubernetes HTTP services from the environment variables define by Kubernetes in pods for any clusterIP service: {@code <SERVICE_NAME>_SERVICE_HOST},
 * {@code <SERVICE_NAME>_SERVICE_PORT_HTTP} or {@code <SERVICE_NAME>_SERVICE_PORT_HTTPS}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean
public class K8sEnvHttpDiscoveryService extends AbstractDiscoveryService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> implements @Provide HttpDiscoveryService {

	private final K8sHttpDiscoveryConfiguration configuration;
	private final HttpClient httpClient;

	/**
	 * <p>
	 * Creates a Kubernetes environment HTTP discovery service.
	 * </p>
	 *
	 * @param configuration the Kubernetes HTTP discovery module configuration
	 * @param httpClient    the HTTP client
	 */
	public K8sEnvHttpDiscoveryService(K8sHttpDiscoveryConfiguration configuration, HttpClient httpClient) {
		super(Set.of("k8s-env"));
		this.configuration = configuration;
		this.httpClient = httpClient;
	}

	@Override
	protected Mono<? extends Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy>> doResolve(ServiceID serviceId, HttpTrafficPolicy trafficPolicy) {
		K8sEnvService k8sService = new K8sEnvService(serviceId);
		return k8sService.refresh(trafficPolicy);
	}

	/**
	 * <p>
	 * Kubernetes environment HTTP service.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class K8sEnvService extends AbstractService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> {

		/**
		 * <p>
		 * Creates a Kubernetes environment HTTP service.
		 * </p>
		 *
		 * @param serviceId the service ID
		 */
		public K8sEnvService(ServiceID serviceId) {
			super(serviceId);
		}

		@Override
		protected Mono<Map<Integer, Supplier<HttpServiceInstance>>> resolveInstances(HttpTrafficPolicy trafficPolicy) {
			return Mono.fromSupplier(() -> {
				String serviceName = this.serviceId.getURI().getAuthority().toUpperCase().replaceAll("-", "_");
				String host = System.getenv(serviceName + "_SERVICE_HOST");
				if(StringUtils.isBlank(host)) {
					return null;
				}

				String http_port = System.getenv(serviceName + "_SERVICE_PORT_HTTP");
				String https_port = System.getenv(serviceName + "_SERVICE_PORT_HTTPS");

				HttpClientConfiguration clientConfiguration;
				InetSocketAddress resolvedAddress;
				if(K8sEnvHttpDiscoveryService.this.configuration.prefer_https()) {
					if(StringUtils.isNotBlank(https_port)) {
						clientConfiguration = trafficPolicy.getConfiguration() != null && trafficPolicy.getConfiguration().tls_enabled() ? trafficPolicy.getConfiguration() : HttpClientConfigurationLoader.load(trafficPolicy.getConfiguration(), configuration -> configuration.tls_enabled(true));
						resolvedAddress = new InetSocketAddress(host, Integer.parseInt(https_port));
					}
					else if(StringUtils.isNotBlank(http_port)) {
						clientConfiguration = trafficPolicy.getConfiguration() != null && !trafficPolicy.getConfiguration().tls_enabled() ? trafficPolicy.getConfiguration() : HttpClientConfigurationLoader.load(trafficPolicy.getConfiguration(), configuration -> configuration.tls_enabled(false));
						resolvedAddress = new InetSocketAddress(host, Integer.parseInt(http_port));
					}
					else {
						return null;
					}
				}
				else {
					if(StringUtils.isNotBlank(http_port)) {
						clientConfiguration = trafficPolicy.getConfiguration() != null && !trafficPolicy.getConfiguration().tls_enabled() ? trafficPolicy.getConfiguration() : HttpClientConfigurationLoader.load(trafficPolicy.getConfiguration(), configuration -> configuration.tls_enabled(false));
						resolvedAddress = new InetSocketAddress(host, Integer.parseInt(http_port));
					}
					else if(StringUtils.isNotBlank(https_port)) {
						clientConfiguration = trafficPolicy.getConfiguration() != null && trafficPolicy.getConfiguration().tls_enabled() ? trafficPolicy.getConfiguration() : HttpClientConfigurationLoader.load(trafficPolicy.getConfiguration(), configuration -> configuration.tls_enabled(true));
						resolvedAddress = new InetSocketAddress(host, Integer.parseInt(https_port));
					}
					else {
						return null;
					}
				}

				return Map.of(Objects.hash(resolvedAddress, trafficPolicy), () -> new K8sHttpServiceInstance(K8sEnvHttpDiscoveryService.this.httpClient.endpoint(resolvedAddress)
					.configuration(clientConfiguration)
					.netConfiguration(trafficPolicy.getNetConfiguration())
					.build())
				);
			});
		}
	}
}
