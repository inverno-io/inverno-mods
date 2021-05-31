	package io.inverno.mod.configuration.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.inverno.mod.base.resource.ClasspathResource;
import io.inverno.mod.configuration.AbstractHashConfigurationSource.HashConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;

public class CPropsFileConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testConfigurationPropertyFileConfigurationSource() throws URISyntaxException {
//		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/test-configuration.cprops")));
		List<HashConfigurationQueryResult<String, CPropsFileConfigurationSource>> results = src
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
		
		Iterator<HashConfigurationQueryResult<String, CPropsFileConfigurationSource>> resultIterator = results.iterator();
		
		HashConfigurationQueryResult<String, CPropsFileConfigurationSource> current = resultIterator.next();
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
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		List<HashConfigurationQueryResult<String, CPropsFileConfigurationSource>> results = src
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
		CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-configuration.cprops").toURI()));
		List<HashConfigurationQueryResult<String, CPropsFileConfigurationSource>> results = src
			.get("testUnset")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(1, results.size());
		Assertions.assertTrue(results.get(0).getResult().isPresent());
		Assertions.assertTrue(results.get(0).getResult().get().isUnset());
	}
	
}
