package io.inverno.mod.configuration.source;

import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
		
//		results.stream().forEach(queryResult -> {
//			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
//		});
		
		Assertions.assertEquals(4, results.size());
		
		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();
		
		ConfigurationQueryResult current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(123456789, current.get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(123456789, current.get().asInteger().get());
		
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
	}

	@Test
	public void testWithParameters() {
		System.setProperty("tata.toto", "123456789");
		System.setProperty("url", "https://localhost:8443");
		System.setProperty("table","a,b,c");

		SystemPropertiesConfigurationSource src = new SystemPropertiesConfigurationSource().withParameters("tutu", "plop","test", 5);

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
		Assertions.assertEquals(123456789, current.get().asInteger().get());
	}
}
