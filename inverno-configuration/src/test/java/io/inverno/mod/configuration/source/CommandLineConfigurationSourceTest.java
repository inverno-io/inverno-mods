package io.inverno.mod.configuration.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationQueryResult;

public class CommandLineConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testCommandLineConfigurationSource() throws URISyntaxException {
		// --tata.toto[test=5,tutu=\"plop\"]=563 --tata.toto[tutu=\"plop\"]=65432  --url="\"https://localhost:8443\""
		
		String[] args = {
			"--tata.toto[test=5,tutu=\"plop\"]=563",
			"--tata.toto[tutu=\"plop\"]=65432",
			"--url=\"https://localhost:8443\"",
			"--table=\"a,b,c\""
		};
		
		CommandLineConfigurationSource src = new CommandLineConfigurationSource(args);
		
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
		
		// cast test
		/*ConfigurationSource src2 = src;
		List<? extends ConfigurationQueryResult<?,?>> l = src2.get("a", "b").withParameters("a", "b").and().get("c").withParameters("d","e").execute().block();
		l.get(0).getQuery().getParameters();
		l.get(0).getResult().ifPresent(result -> result.getKey().getParameters());*/
		
		Assertions.assertEquals(4, results.size());
		
		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();
		
		ConfigurationQueryResult current = resultIterator.next();
		Assertions.assertTrue(current.isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(563, current.get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(65432, current.get().asInteger().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals("url", current.get().getKey().getName());
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(new URI("https://localhost:8443"), current.get().asURI().get());
		
		current = resultIterator.next();
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals("table", current.get().getKey().getName());
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertArrayEquals(new String[] {"a","b","c"}, current.get().asArrayOf(String.class).get());
	}

	@Test
	public void testWithParameters() {
		String[] args = {
			"--tata.toto[test=5,tutu=\"plop\"]=563",
			"--tata.toto[tutu=\"plop\"]=65432",
			"--url=\"https://localhost:8443\"",
			"--table=\"a,b,c\""
		};

		CommandLineConfigurationSource src = new CommandLineConfigurationSource(args).withParameters("tutu", "plop","test", 5);

		List<ConfigurationQueryResult> results = src
			.get("tata.toto")
			.execute()
			.collectList()
			.block();

		Assertions.assertEquals(1, results.size());

		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();

		ConfigurationQueryResult current = resultIterator.next();
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals("tata.toto", current.get().getKey().getName());
		Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(Parameter.of("test", 5), Parameter.of("tutu", "plop"))));
		Assertions.assertTrue(current.get().isPresent());
		Assertions.assertEquals(563, current.get().asInteger().get());
	}
}
