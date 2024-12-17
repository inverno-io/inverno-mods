	package io.inverno.mod.configuration.source;

import io.inverno.mod.base.resource.ClasspathResource;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.DefaultingStrategy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CPropsFileConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testCPropsFileConfigurationSource() throws URISyntaxException {
//		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Path.of(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-configuration.cprops")));
		List<ConfigurationQueryResult> results = src
			.get("tata.toto").withParameters("tutu", "plop","test", 5).and()
			.get("tata.toto").withParameters("tutu", "plop").and()
			.get("url", "table").and()
			.get("text_block").withParameters("context", "text_block").and()
			.get("plip.plap.json").and()
			.get("some_string")
			.execute()
			.collectList()
			.block();

//		results.stream().forEach(queryResult -> {
//			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
//		});
		
		Assertions.assertEquals(7, results.size());
		
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
		Assertions.assertEquals("text_block", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("context", "text_block"))));
		Assertions.assertTrue(current.get().isPresent());
		
		
		
		Assertions.assertEquals(System.lineSeparator() + 
				"		Hey " + System.lineSeparator() +
				"		   This is " + System.lineSeparator() +
				"				a " + System.lineSeparator() +
				"			text 		block" + System.lineSeparator() +
				"	", current.get().asString().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("plip.plap.json", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(System.lineSeparator() + 
				"		{" + System.lineSeparator() +
				"			\"title\":\"Some json\"," + System.lineSeparator() +
				"			table = [\"abc,\"bcd\"]" + System.lineSeparator() +
				"		}" + System.lineSeparator() +
				"	", current.get().asString().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("some_string", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals("abc\ndef", current.get().asString().get());
	}
	
	@Test
	public void testNull() throws URISyntaxException {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Path.of(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		List<ConfigurationQueryResult> results = src
			.get("testNull")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(1, results.size());
		Assertions.assertTrue(results.get(0).isPresent());
		Assertions.assertFalse(results.get(0).get().asString().isPresent());
	}
	
	@Test
	public void testUnset() throws URISyntaxException {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Path.of(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		List<ConfigurationQueryResult> results = src
			.get("testUnset")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(1, results.size());
		Assertions.assertTrue(results.get(0).isPresent());
		Assertions.assertTrue(results.get(0).get().isUnset());
	}

	@Test
	public void testLookupDefaulting() {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-configuration.cprops")));
		src = src.withDefaultingStrategy(DefaultingStrategy.lookup());
		
		List<ConfigurationQueryResult> results = src
			.get("log.level").withParameters("environment", "prod", "name", "test1").and()
			.get("log.level").withParameters("environment", "prod", "name", "test2").and()
			.get("log.level").withParameters("environment", "dev", "name", "test1").and()
			.get("log.level").withParameters("environment", "prod").and()
			.get("log.level").withParameters("environment", "dev").and()
			.get("log.level")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(6, results.size());
		
		Iterator<ConfigurationQueryResult> resultsIterator = results.iterator();
		
		ConfigurationQueryResult result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "prod", "name", "test1"), result.getQueryKey());
		Assertions.assertEquals("ERROR", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "prod", "name", "test2"), result.getQueryKey());
		Assertions.assertEquals("WARN", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "dev", "name", "test1"), result.getQueryKey());
		Assertions.assertEquals("INFO", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "prod"), result.getQueryKey());
		Assertions.assertEquals("WARN", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "dev"), result.getQueryKey());
		Assertions.assertEquals("INFO", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level"), result.getQueryKey());
		Assertions.assertEquals("INFO", result.get().asString().get());
	}
	
	@Test
	public void testWildcardDefaulting() {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-wildcard.cprops")));
		src = src.withDefaultingStrategy(DefaultingStrategy.wildcard());
		
		List<ConfigurationQueryResult> results = src
			.get("jsmith").withParameters("domain", "printer", "printer", "lp1200").and()
			.get("jsmith").withParameters("domain", "printer", "printer", "epsoncolor").and()
			.get("jsmith").withParameters("domain", "printer", "printer", "XP-4100").and()
			.get("jsmith").withParameters("domain", "printer", "printer", "HL-L2310D").and()
			.get("jsmith").withParameters("domain", "resources", "printer", "epsoncolor")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(5, results.size());
		
//		results.stream().forEach(queryResult -> {
//			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
//		});
		
		Iterator<ConfigurationQueryResult> resultsIterator = results.iterator();
		
		ConfigurationQueryResult result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "lp1200"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "lp1200"), result.get().getKey());
		Assertions.assertEquals("query,print", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "epsoncolor"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer"), result.get().getKey());
		Assertions.assertEquals("query", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "XP-4100"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer","printer", "XP-4100"), result.get().getKey());
		Assertions.assertEquals("*", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "HL-L2310D"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer"), result.get().getKey());
		Assertions.assertEquals("query", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "resources", "printer", "epsoncolor"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "printer", "epsoncolor"), result.get().getKey());
		Assertions.assertEquals("manage", result.get().asString().get());
		
		
		results = src
			.get("jsmith").withParameters("domain", "flight", "company", "AF", "location", "FR").and()
			.get("jsmith").withParameters("domain", "flight", "company", "AF", "location", "UK").and()
			.get("jsmith").withParameters("domain", "flight", "company", "LH", "location", "FR").and()
			.get("jsmith").withParameters("domain", "flight", "company", "LH", "location", "DE").and()
			.get("jsmith").withParameters("domain", "flight", "company", "AF", "location", "DE").and()
			.get("jsmith").withParameters("domain", "flight", "company", "O2", "location", "US").and()
			.get("jsmith").withParameters("domain", "flight", "company", "O2", "location", "FR")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(7, results.size());
		
//		results.stream().forEach(queryResult -> {
//			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
//		});
		
		resultsIterator = results.iterator();
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "AF", "location", "FR"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "AF", "location", "FR"), result.get().getKey());
		Assertions.assertEquals("book", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "AF", "location", "UK"), result.getQueryKey());
		Assertions.assertTrue(result.isEmpty());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "LH", "location", "FR"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "LH"), result.get().getKey());
		Assertions.assertEquals("view,comment", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "LH", "location", "DE"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "LH", "location", "DE"), result.get().getKey());
		Assertions.assertEquals("book", result.get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "AF", "location", "DE"), result.getQueryKey());
		Assertions.assertTrue(result.isEmpty());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "O2", "location", "US"), result.getQueryKey());
		Assertions.assertTrue(result.isEmpty());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "O2", "location", "FR"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "location", "FR"), result.get().getKey());
		Assertions.assertEquals("view", result.get().asString().get());
	}

	@Test
	public void testWithParameters() {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-configuration.cprops"))).withParameters("tutu", "plop","test", 5);
		List<ConfigurationQueryResult> results = src
			.get("tata.toto")
			.execute()
			.collectList()
			.block();

		Assertions.assertEquals(1, results.size());

		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();

		ConfigurationQueryResult current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(563, current.get().asInteger().get());

		src.get("tata.toto")
			.execute()
			.single()
			.map(result -> result.toOptional()
				.flatMap(ConfigurationProperty::asString)
				.orElse("default")
			).block();

		src.get("tata.toto")
			.execute()
			.single()
			.map(result -> result.asString("default"))
			.block();
	}

	@Test
	public void testNestedCriteria() {
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-nested-criteria.cprops")));
		src = src.withDefaultingStrategy(DefaultingStrategy.lookup());
		List<ConfigurationQueryResult> results = src
			.get("load_balancer").withParameters("environment", "dev","host", "test").and()
			.get("load_balancer").withParameters("environment", "dev","host", "test", "port", "456").and()
			.get("load_balancer").withParameters("environment", "dev","host", "test", "port", "1234").and()
			.get("load_balancer").withParameters("environment", "prod", "host", "test").and()
			.get("load_balancer").withParameters("environment", "prod", "host", "test", "port", "456").and()
			.get("load_balancer").withParameters("environment", "prod", "host", "test", "port", "1234")
			.execute()
			.collectList()
			.block();

		Assertions.assertEquals(6, results.size());

		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();

		ConfigurationQueryResult current = resultIterator.next();
		Assertions.assertEquals("ROUND_ROBIN", current.get().asString().get());

		current = resultIterator.next();
		Assertions.assertEquals("ROUND_ROBIN", current.get().asString().get());

		current = resultIterator.next();
		Assertions.assertEquals("MIN_LOAD", current.get().asString().get());

		current = resultIterator.next();
		Assertions.assertEquals("RANDOM", current.get().asString().get());

		current = resultIterator.next();
		Assertions.assertEquals("RANDOM", current.get().asString().get());

		current = resultIterator.next();
		Assertions.assertEquals("LEAST_REQUEST", current.get().asString().get());

		List<ConfigurationProperty> resultList = src.list("load_balancer").withParameters("environment", "prod").executeAll().collectList().block();

		Assertions.assertEquals(4, resultList.size());
	}
}
