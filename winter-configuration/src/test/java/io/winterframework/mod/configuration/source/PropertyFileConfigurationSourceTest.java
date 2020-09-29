package io.winterframework.mod.configuration.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.mod.configuration.internal.AbstractHashConfigurationSource.HashConfigurationQueryResult;

public class PropertyFileConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "DEBUG");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testPropertyFileConfigurationSource() throws URISyntaxException {
		PropertyFileConfigurationSource src = new PropertyFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-configuration.properties").toURI()));
		List<HashConfigurationQueryResult<String, PropertyFileConfigurationSource>> results = src
			.get("tata.toto").withParameters("tutu", "plop", "test", 5).and()
			.get("tata.toto").withParameters("tutu", "plop").and()
			.get("url", "table")
			.execute()
			.collectList()
			.block();
		
		Assertions.assertEquals(4, results.size());
		
		Assertions.assertTrue(results.get(0).getResult().isPresent());
		Assertions.assertEquals("tata.toto", results.get(0).getResult().get().getKey().getName());
		Assertions.assertTrue(results.get(0).getResult().get().getKey().getParameters().containsKey("test"));
		Assertions.assertEquals(5, results.get(0).getResult().get().getKey().getParameters().get("test"));
		Assertions.assertTrue(results.get(0).getResult().get().getKey().getParameters().containsKey("tutu"));
		Assertions.assertEquals("plop", results.get(0).getResult().get().getKey().getParameters().get("tutu"));
		Assertions.assertTrue(results.get(0).getResult().get().isPresent());
		Assertions.assertEquals(563, results.get(0).getResult().get().valueAsInteger().get());
		
		Assertions.assertTrue(results.get(1).getResult().isPresent());
		Assertions.assertEquals("tata.toto", results.get(1).getResult().get().getKey().getName());
		Assertions.assertTrue(results.get(1).getResult().get().getKey().getParameters().containsKey("tutu"));
		Assertions.assertEquals("plop", results.get(1).getResult().get().getKey().getParameters().get("tutu"));
		Assertions.assertTrue(results.get(1).getResult().get().isPresent());
		Assertions.assertEquals(65432, results.get(1).getResult().get().valueAsInteger().get());
		
		Assertions.assertTrue(results.get(2).getResult().isPresent());
		Assertions.assertEquals("url", results.get(2).getResult().get().getKey().getName());
		Assertions.assertTrue(results.get(2).getResult().get().isPresent());
		Assertions.assertEquals(new URI("https://localhost:8443"), results.get(2).getResult().get().valueAsURI().get());
		
		Assertions.assertTrue(results.get(3).getResult().isPresent());
		Assertions.assertEquals("table", results.get(3).getResult().get().getKey().getName());
		Assertions.assertTrue(results.get(3).getResult().get().isPresent());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, results.get(3).getResult().get().valueAsArrayOf(String.class).get());
	}
}
