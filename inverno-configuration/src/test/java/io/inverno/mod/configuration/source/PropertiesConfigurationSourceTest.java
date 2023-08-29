/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.mod.configuration.source;

import io.inverno.mod.base.resource.ClasspathResource;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class PropertiesConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testPropertyFileConfigurationSource() throws URISyntaxException, IOException {
		Properties properties = new Properties();
		try(InputStream input = ClassLoader.getSystemResourceAsStream("test-configuration.properties")) {
			properties.load(input);
		}

		PropertiesConfigurationSource src = new PropertiesConfigurationSource(properties);
		List<ConfigurationQueryResult> results = src
			.get("tata.toto").withParameters("tutu", "plop", "test", 5).and()
			.get("tata.toto").withParameters("tutu", "plop").and()
			.get("url", "table").and()
			.get("some_string")
			.execute()
			.collectList()
			.block();
		
//		results.stream().forEach(queryResult -> {
//			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
//		});
		
		Assertions.assertEquals(5, results.size());
		
		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();
		
		ConfigurationQueryResult current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(ConfigurationKey.Parameter.of("test", 5), ConfigurationKey.Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(563, current.getResult().get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(ConfigurationKey.Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(65432, current.getResult().get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("table", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, current.getResult().get().asArrayOf(String.class).get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("some_string", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("toto\ntata", current.getResult().get().asString().get());
	}
	
	@Test
	public void testList() throws IOException {
		Properties properties = new Properties();
		try(InputStream input = ClassLoader.getSystemResourceAsStream("test-configuration.properties")) {
			properties.load(input);
		}

		PropertiesConfigurationSource source = new PropertiesConfigurationSource(properties);
		
		List<ConfigurationProperty> result = source.list("logging.level").executeAll().collectList().block();
		Assertions.assertEquals(4, result.size());
		Assertions.assertEquals(
			Set.of(
				"logging.level[environment=\"prod\",name=\"test1\"] = info", 
				"logging.level[environment=\"dev\",name=\"test1\"] = debug", 
				"logging.level[environment=\"prod\",name=\"test3\"] = error", 
				"logging.level[environment=\"prod\",name=\"test2\"] = info"
			), 
			result.stream().map(p -> p.toString()).collect(Collectors.toSet())
		);

		result = source.list("logging.level").execute().collectList().block();
		Assertions.assertEquals(0, result.size());

		result = source.list("logging.level").withParameters(ConfigurationKey.Parameter.of("environment", "prod"), ConfigurationKey.Parameter.wildcard("name")).execute().collectList().block();
		Assertions.assertEquals(3, result.size());
		Assertions.assertEquals(
			Set.of(
				"logging.level[environment=\"prod\",name=\"test1\"] = info", 
				"logging.level[environment=\"prod\",name=\"test3\"] = error", 
				"logging.level[environment=\"prod\",name=\"test2\"] = info"
			), 
			result.stream().map(p -> p.toString()).collect(Collectors.toSet())
		);

		result = source.list("logging.level").withParameters(ConfigurationKey.Parameter.of("environment", "dev")).execute().collectList().block();
		Assertions.assertEquals(0, result.size());

		result = source.list("logging.level").withParameters(ConfigurationKey.Parameter.of("environment", "dev")).executeAll().collectList().block();
		Assertions.assertEquals(1, result.size());
		Assertions.assertEquals(
			Set.of(
				"logging.level[environment=\"dev\",name=\"test1\"] = debug"
			), 
			result.stream().map(p -> p.toString()).collect(Collectors.toSet())
		);
	}
}
