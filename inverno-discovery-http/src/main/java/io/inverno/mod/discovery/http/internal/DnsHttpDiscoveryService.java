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
package io.inverno.mod.discovery.http.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.discovery.AbstractDnsDiscoveryService;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.UnboundExchange;
import java.net.InetSocketAddress;
import java.util.Set;

/**
 * <p>
 * An HTTP discovery service bean resolving services through DNS lookup.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean
public class DnsHttpDiscoveryService extends AbstractDnsDiscoveryService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> implements @Provide HttpDiscoveryService {

	private final HttpClient httpClient;

	/**
	 * <p>
	 * Creates a DNS HTTP discovery service.
	 * </p>
	 *
	 * @param netService the Net service
	 * @param httpClient the HTTP client
	 */
	public DnsHttpDiscoveryService(NetService netService, HttpClient httpClient) {
		super(
			netService,
			Set.of("http", "https", "ws", "wss")
		);
		this.httpClient = httpClient;
	}

	@Override
	protected InetSocketAddress createUnresolvedAddress(ServiceID serviceId) {
		String host = serviceId.getURI().getHost();
		int port = serviceId.getURI().getPort();
		if(port == -1) {
			switch(serviceId.getScheme()) {
				case "ws":
				case "http": port = 80;
					break;
				case "wss":
				case "https": port = 443;
					break;
				default: throw new IllegalStateException();
			}
		}
		return InetSocketAddress.createUnresolved(host, port);
	}

	@Override
	protected HttpServiceInstance createServiceInstance(ServiceID serviceId, HttpTrafficPolicy trafficPolicy, InetSocketAddress resolvedAddress) {
		HttpClientConfiguration clientConfiguration = trafficPolicy.getConfiguration();
		switch (serviceId.getScheme()) {
			case "ws":
			case "http": {
				if(clientConfiguration == null || clientConfiguration.tls_enabled()) {
					clientConfiguration = HttpClientConfigurationLoader.load(clientConfiguration, configuration -> configuration.tls_enabled(false));
				}
				break;
			}
			case "wss":
			case "https": {
				if(clientConfiguration == null || !clientConfiguration.tls_enabled()) {
					clientConfiguration = HttpClientConfigurationLoader.load(clientConfiguration, configuration -> configuration.tls_enabled(true));
				}
				break;
			}
			default: throw new IllegalStateException();
		}
		return new GenericHttpServiceInstance(this.httpClient.endpoint(resolvedAddress)
			.configuration(clientConfiguration)
			.netConfiguration(trafficPolicy.getNetConfiguration())
			.build());
	}
}
