/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.configuration;

import io.inverno.mod.configuration.source.CompositeConfigurationSource;
import io.inverno.mod.configuration.source.PropertyFileConfigurationSource;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {
	
	@Test
	public void compositeConfigurationSource() throws URISyntaxException {
		PropertyFileConfigurationSource src1 = new PropertyFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("readme_comp2.properties").toURI()));
		PropertyFileConfigurationSource src2 = new PropertyFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("readme_comp2.properties").toURI()));
		
		CompositeConfigurationSource source = new CompositeConfigurationSource(List.of(src1, src2));
		
		List<ConfigurationQueryResult> results = source                                                         // 1
			.get("server.url").withParameters("zone", "US", "environment", "production")
			.and().get("server.url").withParameters("environment", "test")
			.and().get("server.url")
			.and().get("server.url").withParameters("zone", "EU", "environment", "production")
			.and().get("server.url").withParameters("environment", "production", "zone", "EU")
			.execute()
			.collectList()
			.block();
		
		results.stream().forEach(queryResult -> {
			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
		});
		
		System.out.println("================================================================================");
		
		List<ConfigurationProperty> listResults = source
			.list("logging.level")
			.withParameters(
					ConfigurationKey.Parameter.of("environment", "prod"),
					ConfigurationKey.Parameter.wildcard("name")
			)
			.execute()
			.collectList()
			.block();
			
		listResults.stream().forEach(result -> {
			System.out.println(result.getKey() + " -> " + result.asString().orElse(null));
		});

		System.out.println("================================================================================");
		
		listResults = source
			.list("logging.level")
			.withParameters(
				ConfigurationKey.Parameter.of("environment", "dev"), 
				ConfigurationKey.Parameter.wildcard("name")
			)
			.executeAll()
			.collectList()
			.block();
		
		listResults.stream().forEach(result -> {
			System.out.println(result.getKey() + " -> " + result.asString().orElse(null));
		});
	}
}
