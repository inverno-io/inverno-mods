package io.inverno.mod.configuration.source;

import io.inverno.mod.configuration.ConfigurationKey;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.source.CompositeConfigurationSource.CompositeConfigurationQueryResult;
import java.util.ArrayList;
import java.util.Comparator;

public class CompositeConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
		System.setProperty("name", "NameSysProp");
	}
	
	@Test
	public void testCompositeConfigurationSource() throws URISyntaxException {
		SystemPropertiesConfigurationSource src0 = new SystemPropertiesConfigurationSource();
		CPropsFileConfigurationSource src1 = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-service-src1.cprops").toURI()));
		CPropsFileConfigurationSource src2 = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-service-src2.cprops").toURI()));
		
		CompositeConfigurationSource src = new CompositeConfigurationSource(List.of(src0, src2, src1));
		
		List<ConfigurationQueryResult> results = src
			.get("datasource.url", "datasource.user", "datasource.password").withParameters("node", "node1", "zone", "US", "environment", "production").and()
			.get("datasource.url").withParameters("node", "node2", "zone", "EU", "environment", "production").and()
			.get("datasource.url", "datasource.user", "datasource.password").withParameters("zone", "EU", "environment", "production").and()
			.get("datasource.url").withParameters("zone", "US", "environment", "production").and()
			.get("datasource.url").withParameters("zone", "EU", "environment", "test").and()
			.get("datasource.url").withParameters("zone", "EU", "environment", "local").and()
			.get("datasource.url").withParameters("zone", "ASIA", "environment", "undefined").and()
			.get("undefined").and()
			.get("name").withParameters("environment", "test")
			.execute()
			.collectList()
			.block();
		
		results.stream().forEach(queryResult -> {
			System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
		});
		
		Assertions.assertEquals(13, results.size());
		
		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();
		
		ConfigurationQueryResult current = resultIterator.next(); // datasource.url[node=node1,zone=US,environment=production] -> datasource.url[node=node1,environment=production,zone=US] = jdbc:h2:file:/opt/1/node1-us-production
		Assertions.assertEquals("datasource.url", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("node","node1"), Parameter.of("zone","US"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("node","node1"), Parameter.of("zone","US"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("jdbc:h2:file:/opt/1/node1-us-production", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.user[node=node1,zone=US,environment=production] -> datasource.user[] = user
		Assertions.assertEquals("datasource.user", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("node","node1"), Parameter.of("zone","US"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.user", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("user", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.password[node=node1,zone=US,environment=production] -> datasource.password[environment=production] = password_prod
		Assertions.assertEquals("datasource.password", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("node","node1"), Parameter.of("zone","US"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.password", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("password_prod", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.url[node=node2,zone=EU,environment=production] -> datasource.url[node=node2,environment=production,zone=EU] = jdbc:h2:file:/opt/2/node2-eu-production
		Assertions.assertEquals("datasource.url", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("node","node2"), Parameter.of("zone","EU"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("node","node2"), Parameter.of("zone","EU"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("jdbc:h2:file:/opt/2/node2-eu-production", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.url[zone=EU,environment=production] -> datasource.url[environment=production] = jdbc:h2:file:/opt/1/production
		Assertions.assertEquals("datasource.url", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("zone","EU"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("jdbc:h2:file:/opt/1/production", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.user[zone=EU,environment=production] -> datasource.user[] = user
		Assertions.assertEquals("datasource.user", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("zone","EU"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.user", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("user", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.password[zone=EU,environment=production] -> datasource.password[environment=production] = password_prod
		Assertions.assertEquals("datasource.password", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("zone","EU"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.password", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("password_prod", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.url[zone=US,environment=production] -> datasource.url[zone=US,environment=production] = jdbc:h2:file:/opt/2/us-production
		Assertions.assertEquals("datasource.url", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("zone","US"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("zone","US"), Parameter.of("environment","production"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("jdbc:h2:file:/opt/2/us-production", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.url[zone=EU,environment=test] -> datasource.url[environment=test] = jdbc:h2:file:/opt/2/test
		Assertions.assertEquals("datasource.url", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("zone","EU"), Parameter.of("environment","test"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("environment","test"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("jdbc:h2:file:/opt/2/test", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.url[zone=EU,environment=local] -> datasource.url[environment=local] = jdbc:h2:file:/opt/1/local
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.url", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("zone","EU"), Parameter.of("environment","local"))));
		Assertions.assertEquals("datasource.url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("environment","local"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("jdbc:h2:file:/opt/1/local", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // datasource.url[zone=ASIA,environment=undefined] -> datasource.url[] = jdbc:h2:file:/opt/1/default
		Assertions.assertEquals("datasource.url", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("zone","ASIA"), Parameter.of("environment","undefined"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.url", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("jdbc:h2:file:/opt/1/default", current.getResult().get().asString().get());
		
		current = resultIterator.next(); // undefined[] -> null
		Assertions.assertEquals("undefined", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().isEmpty());
		Assertions.assertTrue(!current.getResult().isPresent());
		
		current = resultIterator.next(); // name[environment=test] -> name[environment=test] = NameEnv
		Assertions.assertEquals("name", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("environment","test"))));
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("name", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("environment","test"))));
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("NameSysProp", current.getResult().get().asString().get());
	}

	@Test
	public void testCompositeConfigurationSourceUnset() throws URISyntaxException {
		SystemPropertiesConfigurationSource src0 = new SystemPropertiesConfigurationSource();
		CPropsFileConfigurationSource src1 = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-service-src1.cprops").toURI()));
		CPropsFileConfigurationSource src2 = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-service-src2.cprops").toURI()));
		
		CompositeConfigurationSource src = new CompositeConfigurationSource(List.of(src0, src2, src1));
		
		List<ConfigurationQueryResult> results = src
			.get("datasource.password").and()
			.get("datasource.password").withParameters("environment", "testUnset")
			.execute()
			.collectList()
			.block();
		
		/*results.stream().forEach(queryResult -> {
			System.out.println(queryResult.getQuery() + " -> " + queryResult.getResult().orElse(null));
		});*/
		
		Assertions.assertEquals(2, results.size());
		
		Iterator<ConfigurationQueryResult> resultIterator = results.iterator();
		
		ConfigurationQueryResult current = resultIterator.next();
		
		Assertions.assertEquals("datasource.password", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().isEmpty());
		Assertions.assertTrue(current.getResult().isPresent());
		Assertions.assertEquals("datasource.password", current.getResult().get().getKey().getName());
		Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
		Assertions.assertTrue(current.getResult().get().isPresent());
		Assertions.assertEquals("password", current.getResult().get().asString().get());
		
		current = resultIterator.next();
		
		Assertions.assertEquals("datasource.password", current.getQueryKey().getName());
		Assertions.assertTrue(current.getQueryKey().getParameters().containsAll(List.of(Parameter.of("environment","testUnset"))));
		Assertions.assertFalse(current.getResult().isPresent());
	}
	
	@Test
	public void testList() throws URISyntaxException {
		SystemPropertiesConfigurationSource src0 = new SystemPropertiesConfigurationSource();
		CPropsFileConfigurationSource src1 = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-service-src1.cprops").toURI()));
		CPropsFileConfigurationSource src2 = new CPropsFileConfigurationSource(Paths.get(ClassLoader.getSystemResource("test-service-src2.cprops").toURI()));
		
		CompositeConfigurationSource src = new CompositeConfigurationSource(List.of(src0, src1, src2));
		
		List<ConfigurationProperty> result = src.list("logging.level").withParameters(Parameter.of("environment", "prod"), Parameter.wildcard("name")).execute().collectList().block();
		Assertions.assertEquals(5, result.size());
		
		Iterator<ConfigurationProperty> resultIterator = result.stream().sorted(Comparator.comparing(p -> p.getKey().toString())).iterator();

		ConfigurationProperty current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("warn", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test2")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test3")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("error", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test4")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("error", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test5")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
		
		result = src.list("logging.level").withParameters(Parameter.of("environment", "prod"), Parameter.wildcard("name")).executeAll().collectList().block();
		Assertions.assertEquals(6, result.size());
		
		resultIterator = result.stream().sorted(Comparator.comparing(p -> p.getKey().toString())).iterator();

		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("warn", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test2"), Parameter.of("node", "node-1")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("trace", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test2")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test3")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("error", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test4")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("error", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test5")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("info", current.asString().get());

		result = src.list("logging.level").withParameters(Parameter.of("environment", "dev"), Parameter.wildcard("name")).execute().collectList().block();
		Assertions.assertEquals(3, result.size());
		
		resultIterator = result.stream().sorted(Comparator.comparing(p -> p.getKey().toString())).iterator();
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev"), Parameter.of("name", "test1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("debug", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev"), Parameter.of("name", "test2")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("debug", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
		
		result = src.list("logging.level").withParameters(Parameter.of("environment", "dev"), Parameter.wildcard("name")).executeAll().collectList().block();
		Assertions.assertEquals(4, result.size());
		
		resultIterator = result.stream().sorted(Comparator.comparing(p -> p.getKey().toString())).iterator();
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev"), Parameter.of("name", "test1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("debug", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev"), Parameter.of("name", "test2"), Parameter.of("node", "node-1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("debug", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev"), Parameter.of("name", "test2")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("debug", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
		
		result = src.list("logging.level").withParameters(Parameter.of("environment", "prod")).executeAll().collectList().block();
		Assertions.assertEquals(6, result.size());
		
		resultIterator = result.stream().sorted(Comparator.comparing(p -> p.getKey().toString())).iterator();
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("warn", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test2"), Parameter.of("node", "node-1")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("trace", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test2")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test3")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("error", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test4")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("error", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test5")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
		
		result = src.list("logging.level").withParameters(Parameter.wildcard("name")).executeAll().collectList().block();
		Assertions.assertEquals(9, result.size());
		
		resultIterator = result.stream().sorted(Comparator.comparing(p -> p.getKey().toString())).iterator();
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev"), Parameter.of("name", "test1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("debug", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev"), Parameter.of("name", "test2"), Parameter.of("node", "node-1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("debug", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "dev"), Parameter.of("name", "test2")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("debug", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test1")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("warn", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test2"), Parameter.of("node", "node-1")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("trace", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test2")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test3")), current.getKey());
		Assertions.assertEquals(src1, current.getSource());
		Assertions.assertEquals("error", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test4")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("error", current.asString().get());
		
		current = resultIterator.next();
		Assertions.assertEquals(ConfigurationKey.of("logging.level", Parameter.of("environment", "prod"), Parameter.of("name", "test5")), current.getKey());
		Assertions.assertEquals(src2, current.getSource());
		Assertions.assertEquals("info", current.asString().get());
	}
}
