	package io.inverno.mod.configuration.source;

import io.inverno.mod.base.resource.ClasspathResource;
import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
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
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(563, current.getResult().get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("tutu", "plop"))));
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
		Assertions.assertEquals("text_block", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("context", "text_block"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("\n" + 
				"		Hey \n" + 
				"		   This is \n" + 
				"				a \n" + 
				"			text 		block\n" + 
				"	", current.getResult().get().asString().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("plip.plap.json", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("\n" + 
				"		{\n" + 
				"			\"title\":\"Some json\",\n" + 
				"			table = [\"abc,\"bcd\"]\n" + 
				"		}\n" + 
				"	", current.getResult().get().asString().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("some_string", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("abc\ndef", current.getResult().get().asString().get());
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
		Assertions.assertTrue(results.get(0).getResult().isPresent());
		Assertions.assertFalse(results.get(0).getResult().get().asString().isPresent());
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
		Assertions.assertTrue(results.get(0).getResult().isPresent());
		Assertions.assertTrue(results.get(0).getResult().get().isUnset());
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
		Assertions.assertEquals("ERROR", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "prod", "name", "test2"), result.getQueryKey());
		Assertions.assertEquals("WARN", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "dev", "name", "test1"), result.getQueryKey());
		Assertions.assertEquals("INFO", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "prod"), result.getQueryKey());
		Assertions.assertEquals("WARN", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "dev"), result.getQueryKey());
		Assertions.assertEquals("INFO", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("log.level"), result.getQueryKey());
		Assertions.assertEquals("INFO", result.getResult().get().asString().get());
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
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "lp1200"), result.getResult().get().getKey());
		Assertions.assertEquals("query,print", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "epsoncolor"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer"), result.getResult().get().getKey());
		Assertions.assertEquals("query", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "XP-4100"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer","printer", "XP-4100"), result.getResult().get().getKey());
		Assertions.assertEquals("*", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer", "printer", "HL-L2310D"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "printer"), result.getResult().get().getKey());
		Assertions.assertEquals("query", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "resources", "printer", "epsoncolor"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "printer", "epsoncolor"), result.getResult().get().getKey());
		Assertions.assertEquals("manage", result.getResult().get().asString().get());
		
		
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
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "AF", "location", "FR"), result.getResult().get().getKey());
		Assertions.assertEquals("book", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "AF", "location", "UK"), result.getQueryKey());
		Assertions.assertTrue(result.getResult().isEmpty());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "LH", "location", "FR"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "LH"), result.getResult().get().getKey());
		Assertions.assertEquals("view,comment", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "LH", "location", "DE"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "LH", "location", "DE"), result.getResult().get().getKey());
		Assertions.assertEquals("book", result.getResult().get().asString().get());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "AF", "location", "DE"), result.getQueryKey());
		Assertions.assertTrue(result.getResult().isEmpty());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "O2", "location", "US"), result.getQueryKey());
		Assertions.assertTrue(result.getResult().isEmpty());
		
		result = resultsIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "company", "O2", "location", "FR"), result.getQueryKey());
		Assertions.assertEquals(ConfigurationKey.of("jsmith", "domain", "flight", "location", "FR"), result.getResult().get().getKey());
		Assertions.assertEquals("view", result.getResult().get().asString().get());
	}
}
