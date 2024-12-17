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
package io.inverno.mod.discovery.http.meta;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.inverno.mod.boot.json.InvernoBaseModule;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class HttpMetaServiceDescriptorTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder()
		.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
		.addModules(new Jdk8Module(), new InvernoBaseModule())
		.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
		.build();

	@Test
	public void testHttpClientConfiguration() throws JsonProcessingException {
		HttpMetaServiceDescriptor.HttpClientConfiguration configuration = new HttpMetaServiceDescriptor.HttpClientConfiguration();
		configuration.setUser_agent("test");

		HttpClientConfigurationLoader.Configurator configurator = Mockito.mock(HttpClientConfigurationLoader.Configurator.class);

		configuration.getConfigurer().accept(configurator);
		Mockito.verify(configurator).user_agent("test");
		Mockito.verifyNoMoreInteractions(configurator);

		String configurationJson = MAPPER.writeValueAsString(configuration);
		Assertions.assertEquals("{\"user_agent\":\"test\"}", configurationJson);

		HttpMetaServiceDescriptor.HttpClientConfiguration configurationRead = MAPPER.readValue(configurationJson, HttpMetaServiceDescriptor.HttpClientConfiguration.class);
		Assertions.assertEquals(configuration, configurationRead);

		configuration.setUser_agent(null);

		configurationJson = MAPPER.writeValueAsString(configuration);
		Assertions.assertEquals("{\"user_agent\":null}", configurationJson);

		configurationRead = MAPPER.readValue(configurationJson, HttpMetaServiceDescriptor.HttpClientConfiguration.class);
		Assertions.assertEquals(configuration, configurationRead);
	}

	@Test
	public void testLoadBalancerDescriptor() throws JsonProcessingException {
		String randomLBJson = "{\"strategy\":\"RANDOM\"}";
		Assertions.assertEquals(new HttpMetaServiceDescriptor.LoadBalancerDescriptor(HttpTrafficPolicy.LoadBalancingStrategy.RANDOM), MAPPER.readValue(randomLBJson, HttpMetaServiceDescriptor.LoadBalancerDescriptor.class));

		String roundRobinLBJson = "{\"strategy\":\"ROUND_ROBIN\"}";
		Assertions.assertEquals(new HttpMetaServiceDescriptor.LoadBalancerDescriptor(HttpTrafficPolicy.LoadBalancingStrategy.ROUND_ROBIN), MAPPER.readValue(roundRobinLBJson, HttpMetaServiceDescriptor.LoadBalancerDescriptor.class));

		String leastRequestLBJson = "{\"strategy\":\"LEAST_REQUEST\",\"choiceCount\":3,\"bias\":4}";
		HttpMetaServiceDescriptor.LeastRequestLoadBalancerDescriptor leastRequestLB = new HttpMetaServiceDescriptor.LeastRequestLoadBalancerDescriptor();
		leastRequestLB.setChoiceCount(3);
		leastRequestLB.setBias(4);
		Assertions.assertEquals(leastRequestLB, MAPPER.readValue(leastRequestLBJson, HttpMetaServiceDescriptor.LoadBalancerDescriptor.class));

		String minLoadFactorLBJson = "{\"strategy\":\"MIN_LOAD_FACTOR\",\"choiceCount\":5,\"bias\":6}";
		HttpMetaServiceDescriptor.MinLoadFactorLoadBalancerDescriptor minLoadFactorLB = new HttpMetaServiceDescriptor.MinLoadFactorLoadBalancerDescriptor();
		minLoadFactorLB.setChoiceCount(5);
		minLoadFactorLB.setBias(6);
		Assertions.assertEquals(minLoadFactorLB, MAPPER.readValue(minLoadFactorLBJson, HttpMetaServiceDescriptor.LoadBalancerDescriptor.class));
	}

	@Test
	public void testValueMatcher() throws JsonProcessingException {
		String staticMatcherJson = "{\"value\":\"1234\"}";
		Assertions.assertEquals(new HttpMetaServiceDescriptor.StaticValueMatcher("1234"), MAPPER.readValue(staticMatcherJson, HttpMetaServiceDescriptor.ValueMatcher.class));

		String regexMatcherJson = "{\"regex\":\"\\\\d+\"}";
		Assertions.assertEquals(new HttpMetaServiceDescriptor.RegexValueMatcher("\\d+"), MAPPER.readValue(regexMatcherJson, HttpMetaServiceDescriptor.ValueMatcher.class));
	}
}
