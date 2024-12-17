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
package io.inverno.mod.discovery.http.meta;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClient;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since
 */
public class Readme {


	@Bean
	public static class SomeService {

		private final HttpClient httpClient;
		private final HttpDiscoveryService configurationHttpMetaDiscoveryService;
		private final HttpTrafficPolicy httpTrafficPolicy;

		public SomeService(HttpClient httpClient, HttpDiscoveryService configurationHttpMetaDiscoveryService) {
			this.httpClient = httpClient;
			this.configurationHttpMetaDiscoveryService = configurationHttpMetaDiscoveryService;
			this.httpTrafficPolicy = HttpTrafficPolicy.builder().build();
		}

		public Mono<String> execute() {
			return this.httpClient.exchange(Method.GET, "/path/to/resource")
				.flatMap(exchange -> this.configurationHttpMetaDiscoveryService.resolve(ServiceID.of("conf://myService"))
					.flatMap(service -> service.getInstance(exchange))
					.map(instance -> instance.bind(exchange))
				)
				.flatMap(Exchange::response)
				.flatMapMany(response -> response.body().string().stream())
				.collect(Collectors.joining());
		}

	}
}
