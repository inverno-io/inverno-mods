package io.winterframework.mod.configuration.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.mod.configuration.internal.AbstractPropertiesConfigurationSource.PropertyConfigurationQueryResult;

public class SystemPropertiesConfigurationSourceTest {

	@Test
	public void testSystemPropertiesConfigurationSource() throws URISyntaxException {

		System.setProperty("tata.toto", "123456789");
		System.setProperty("url", "https://localhost:8443");
		System.setProperty("table","a,b,c");
		
		SystemPropertiesConfigurationSource src = new SystemPropertiesConfigurationSource();
		
		List<PropertyConfigurationQueryResult<String, SystemPropertiesConfigurationSource>> results = src
			.get("tata.toto").withParameters("tutu", "plop","test", 5).and()
			.get("tata.toto").withParameters("tutu", "plop").and()
			.get("url", "table")
			.execute()
			.block();
		
		results.stream().forEach(queryResult -> {
			System.out.println(queryResult.getQuery() + " -> " + queryResult.getResult().orElse(null));
		});
		
		Assertions.assertEquals(4, results.size());
		
		Assertions.assertTrue(results.get(0).getResult().isPresent());
		Assertions.assertEquals("tata.toto", results.get(0).getResult().get().getKey().getName());
		Assertions.assertTrue(results.get(0).getResult().get().getKey().getParameters().containsKey("test"));
		Assertions.assertEquals(5, results.get(0).getResult().get().getKey().getParameters().get("test"));
		Assertions.assertTrue(results.get(0).getResult().get().getKey().getParameters().containsKey("tutu"));
		Assertions.assertEquals("plop", results.get(0).getResult().get().getKey().getParameters().get("tutu"));
		Assertions.assertTrue(results.get(0).getResult().get().isPresent());
		Assertions.assertEquals(123456789, results.get(0).getResult().get().valueAsInteger().get());
		
		Assertions.assertTrue(results.get(1).getResult().isPresent());
		Assertions.assertEquals("tata.toto", results.get(1).getResult().get().getKey().getName());
		Assertions.assertTrue(results.get(1).getResult().get().getKey().getParameters().containsKey("tutu"));
		Assertions.assertEquals("plop", results.get(1).getResult().get().getKey().getParameters().get("tutu"));
		Assertions.assertTrue(results.get(1).getResult().get().isPresent());
		Assertions.assertEquals(123456789, results.get(1).getResult().get().valueAsInteger().get());
		
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
