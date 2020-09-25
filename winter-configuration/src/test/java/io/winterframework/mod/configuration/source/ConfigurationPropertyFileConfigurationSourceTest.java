	package io.winterframework.mod.configuration.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.mod.configuration.internal.AbstractHashConfigurationSource.HashConfigurationQueryResult;

public class ConfigurationPropertyFileConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "DEBUG");
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
			.get("plip.plap.json")
			.execute()
			.block();
		
		Assertions.assertEquals(6, results.size());
		
		Iterator<HashConfigurationQueryResult<String, ConfigurationPropertyFileConfigurationSource>> resultIterator = results.iterator();
		
		HashConfigurationQueryResult<String, ConfigurationPropertyFileConfigurationSource> current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsKey("test"));
		Assertions.assertEquals(5, current.getResult().get().getKey().getParameters().get("test"));
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsKey("tutu"));
		Assertions.assertEquals("plop", current.getResult().get().getKey().getParameters().get("tutu"));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(563, current.getResult().get().valueAsInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsKey("tutu"));
		Assertions.assertEquals("plop", current.getResult().get().getKey().getParameters().get("tutu"));
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
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsKey("context"));
		Assertions.assertEquals("text_block", current.getResult().get().getKey().getParameters().get("context"));
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
		
		
	}
}
