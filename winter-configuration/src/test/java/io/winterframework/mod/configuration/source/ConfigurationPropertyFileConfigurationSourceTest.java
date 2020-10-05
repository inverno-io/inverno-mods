	package io.winterframework.mod.configuration.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.mod.configuration.ConfigurationKey.Parameter;
import io.winterframework.mod.configuration.internal.AbstractHashConfigurationSource.HashConfigurationQueryResult;

public class ConfigurationPropertyFileConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testConfigurationPropertyFileConfigurationSource() throws URISyntaxException {
		ConfigurationPropertyFileConfigurationSource src = new ConfigurationPropertyFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		List<HashConfigurationQueryResult<String, ConfigurationPropertyFileConfigurationSource>> results = src
			.get("tata.toto").withParameters("tutu", "plop","test", 5).and()
			.get("tata.toto").withParameters("tutu", "plop").and()
			.get("url", "table").and()
			.get("text_block").withParameters("context", "text_block").and()
			.get("plip.plap.json").and()
			.get("some_string")
			.execute()
			.collectList()
			.block();
		
		results.stream().forEach(queryResult -> {
			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
		});
		
		Assertions.assertEquals(7, results.size());
		
		Iterator<HashConfigurationQueryResult<String, ConfigurationPropertyFileConfigurationSource>> resultIterator = results.iterator();
		
		HashConfigurationQueryResult<String, ConfigurationPropertyFileConfigurationSource> current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(563, current.getResult().get().valueAsInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(65432, current.getResult().get().valueAsInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().valueAsURI().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("table", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, current.getResult().get().valueAsArrayOf(String.class).get());
		
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
				"	", current.getResult().get().valueAsString().get());
		
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
				"	", current.getResult().get().valueAsString().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("some_string", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("abc\ndef", current.getResult().get().valueAsString().get());
	}
	
	@Test
	public void testNull() throws URISyntaxException {
		ConfigurationPropertyFileConfigurationSource src = new ConfigurationPropertyFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		List<HashConfigurationQueryResult<String, ConfigurationPropertyFileConfigurationSource>> results = src
			.get("testNull")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(1, results.size());
		Assertions.assertTrue(results.get(0).getResult().isPresent());
		Assertions.assertFalse(results.get(0).getResult().get().valueAsString().isPresent());
	}
	
	@Test
	public void testUnset() throws URISyntaxException {
		ConfigurationPropertyFileConfigurationSource src = new ConfigurationPropertyFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		List<HashConfigurationQueryResult<String, ConfigurationPropertyFileConfigurationSource>> results = src
			.get("testUnset")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(1, results.size());
		Assertions.assertTrue(results.get(0).getResult().isPresent());
		Assertions.assertTrue(results.get(0).getResult().get().isUnset());
	}
	
}
