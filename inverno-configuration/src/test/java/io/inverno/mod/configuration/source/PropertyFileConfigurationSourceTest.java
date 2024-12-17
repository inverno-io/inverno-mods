/*
 * Copyright 2020 Jeremy KUHN
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
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class PropertyFileConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testPropertyFileConfigurationSource() throws URISyntaxException {
		PropertyFileConfigurationSource src = new PropertyFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-configuration.properties")));
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
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(563, current.get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(65432, current.get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("url", current.get().getKey().getName());
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(new URI("https://localhost:8443"), current.get().asURI().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("table", current.get().getKey().getName());
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, current.get().asArrayOf(String.class).get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("some_string", current.get().getKey().getName());
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals("toto\ntata", current.get().asString().get());
	}
	
	@Test
	public void testList() {
		PropertyFileConfigurationSource source = new PropertyFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-configuration.properties")));
		
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

		result = source.list("logging.level").withParameters(Parameter.of("environment", "prod"), Parameter.wildcard("name")).execute().collectList().block();
		Assertions.assertEquals(3, result.size());
		Assertions.assertEquals(
			Set.of(
				"logging.level[environment=\"prod\",name=\"test1\"] = info", 
				"logging.level[environment=\"prod\",name=\"test3\"] = error", 
				"logging.level[environment=\"prod\",name=\"test2\"] = info"
			), 
			result.stream().map(p -> p.toString()).collect(Collectors.toSet())
		);

		result = source.list("logging.level").withParameters(Parameter.of("environment", "dev")).execute().collectList().block();
		Assertions.assertEquals(0, result.size());

		result = source.list("logging.level").withParameters(Parameter.of("environment", "dev")).executeAll().collectList().block();
		Assertions.assertEquals(1, result.size());
		Assertions.assertEquals(
			Set.of(
				"logging.level[environment=\"dev\",name=\"test1\"] = debug"
			), 
			result.stream().map(p -> p.toString()).collect(Collectors.toSet())
		);
	}

	@Test
	public void testWithParameters() throws URISyntaxException {
		PropertyFileConfigurationSource src = new PropertyFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-configuration.properties"))).withParameters("tutu", "plop", "test", 5);
		List<ConfigurationQueryResult> results = src
			.get("tata.toto")
			.execute()
			.collectList()
			.block();

//		results.stream().forEach(queryResult -> {
//			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
//		});

		Assertions.assertEquals(1, results.size());

		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();

		ConfigurationQueryResult current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(563, current.get().asInteger().get());
	}
}
