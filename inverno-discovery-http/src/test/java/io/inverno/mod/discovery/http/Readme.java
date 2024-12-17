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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {

	@Bean
	public class SomeService {

		private final HttpClient httpClient;
		private final HttpDiscoveryService dnsHttpDiscoveryService;
		private final HttpTrafficPolicy httpTrafficPolicy;

		private Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> apiService;

		public SomeService(HttpClient httpClient, HttpDiscoveryService dnsHttpDiscoveryService) {
			this.httpClient = httpClient;
			this.dnsHttpDiscoveryService = dnsHttpDiscoveryService;

			this.httpTrafficPolicy = HttpTrafficPolicy.builder().build();
		}

		@Init
		public void init() {
			this.apiService = this.dnsHttpDiscoveryService
				.resolve(ServiceID.of("https://api.example.org"), this.httpTrafficPolicy)
				.block();
		}

		@Destroy
		public void destroy() {
			this.apiService.shutdown().block();
		}

		public Mono<String> get(String id) {
			return httpClient.exchange(Method.GET, "/v1/some/service/" + id)
				.flatMap(exchange -> this.apiService.getInstance(exchange)
					.map(instance -> instance.bind(exchange))
				)
				.flatMap(Exchange::response)
				.flatMapMany(response -> response.body().string().stream())
				.collect(Collectors.joining());
		}
	}
}
