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

import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class HttpTrafficPolicyTest {

	public void test() {
		HttpTrafficPolicy trafficPolicy = HttpTrafficPolicy.builder()
			.configuration(HttpClientConfigurationLoader.load(configuration -> configuration
				.pool_max_size(3)
			))
			.leastRequestLoadBalancer(3, 2)
			.build();

		HttpClient httpClient = null;
		HttpDiscoveryService discoveryService = null;

		String responseBody = discoveryService.resolve(ServiceID.of("http://hostname:8080"), trafficPolicy)
			.flatMap(service -> httpClient.exchange(Method.GET, "/path/to/resource")
				.flatMap(exchange -> service.getInstance(exchange)
					.map(instance -> instance.bind(exchange))
				)
			)
			.flatMap(Exchange::response)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining())
			.block();
	}
}
