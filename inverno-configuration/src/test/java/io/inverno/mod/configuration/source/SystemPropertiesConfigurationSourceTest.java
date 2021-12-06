package io.inverno.mod.configuration.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationQueryResult;

public class SystemPropertiesConfigurationSourceTest {

	@Test
	public void testSystemPropertiesConfigurationSource() throws URISyntaxException {

		System.setProperty("tata.toto", "123456789");
		System.setProperty("url", "https://localhost:8443");
		System.setProperty("table","a,b,c");
		
		SystemPropertiesConfigurationSource src = new SystemPropertiesConfigurationSource();
		
		List<ConfigurationQueryResult> results = src
			.get("tata.toto").withParameters("tutu", "plop","test", 5).and()
			.get("tata.toto").withParameters("tutu", "plop").and()
			.get("url", "table")
			.execute()
			.collectList()
			.block();
		
		results.stream().forEach(queryResult -> {
			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
		});
		
		Assertions.assertEquals(4, results.size());
		
		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();
		
		ConfigurationQueryResult current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(123456789, current.getResult().get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("tata.toto", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals(123456789, current.getResult().get().asInteger().get());
		
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
	}

}
