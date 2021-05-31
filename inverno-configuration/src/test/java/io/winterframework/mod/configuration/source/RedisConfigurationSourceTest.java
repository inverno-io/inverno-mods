package io.inverno.mod.configuration.source;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisClient;
import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationUpdate.SpecialValue;
import io.inverno.mod.configuration.source.RedisConfigurationSource.RedisConfigurationKey;
import io.inverno.mod.configuration.source.RedisConfigurationSource.RedisConfigurationQueryResult;
import io.inverno.mod.configuration.source.RedisConfigurationSource.RedisExecutableConfigurationQuery;

@Disabled
public class RedisConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	@Test
	public void testRedisConfigurationSourceRedisClient() throws IllegalArgumentException, URISyntaxException {
		RedisClient client = RedisClient.create("redis://localhost:6379");

		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			
			source.set("prop1", "abc")
				.and().set("prop2", 42).withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			List<RedisConfigurationQueryResult> result = source.get("prop1")
				.and().get("prop2").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(2, result.size());
			
			Iterator<RedisConfigurationQueryResult> resultIterator = result.iterator();
			
			RedisConfigurationQueryResult current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(42, current.getResult().get().asInteger().get());
			
			source.activate().block();
			
			result = source.get("prop1")
				.and().get("prop2").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(2, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(1, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(1, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(42, current.getResult().get().asInteger().get());
			
			source.set("prop3", new URI("https://localhost:8443"))
				.and().set("prop2", 84).withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			result = source.get("prop1")
				.and().get("prop3")
				.and().get("prop2").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(3, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(1, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(1, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertFalse(current.getResult().isPresent());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(1, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(42, current.getResult().get().asInteger().get());
			
			result = source.get("prop3").atRevision(2)
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(1, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			source.activate(2).block();
			
			result = source.get("prop1")
				.and().get("prop3")
				.and().get("prop2").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(3, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(84, current.getResult().get().asInteger().get());
			
			source.set("prop4", "Foo Bar").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			result = source.get("prop4").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(1, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertFalse(current.getResult().isPresent());
			
			source.activate(3, "env", "production", "customer", "cust1").block();
			
			result = source.get("prop1")
				.and().get("prop3")
				.and().get("prop2", "prop4").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(4, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(84, current.getResult().get().asInteger().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop4", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals("Foo Bar", current.getResult().get().asString().get());
			
			source.set("prop1", "abcdef")
				.and().set("prop2", 126).withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			result = source.get("prop1")
				.and().get("prop3")
				.and().get("prop2", "prop4").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(4, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(2, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(84, current.getResult().get().asInteger().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop4", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals("Foo Bar", current.getResult().get().asString().get());
			
			source.activate().block();
			
			result = source.get("prop1")
				.and().get("prop3")
				.and().get("prop2", "prop4").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(4, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abcdef", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(84, current.getResult().get().asInteger().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop4", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals("Foo Bar", current.getResult().get().asString().get());
			
			source.activate("env", "production", "customer", "cust1", "application", "app").block();
			
			result = source.get("prop1")
				.and().get("prop3")
				.and().get("prop2", "prop4").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(4, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abcdef", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(3, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(4, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(4, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(126, current.getResult().get().asInteger().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(current.getQueryKey().getRevision().isPresent());
			Assertions.assertEquals(4, current.getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop4", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((RedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals("Foo Bar", current.getResult().get().asString().get());
			
			Assertions.assertEquals(4, source.getMetaData().block().getWorkingRevision().get());
			Assertions.assertEquals(3, source.getMetaData().block().getActiveRevision().get());
			Assertions.assertEquals(5, source.getMetaData(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app")).block().getWorkingRevision().get());
			Assertions.assertEquals(4, source.getMetaData(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app")).block().getActiveRevision().get());
		}
		finally {
			client.connect().reactive().flushall().block();
			client.shutdown();
		}
	}
	
	@Test
	public void testConflictDetection() {
		RedisClient client = RedisClient.create("redis://localhost:6379");

		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			
			source.set("prop1", "val").withParameters("customer", "cust1")
				.and().set("prop2", "val").withParameters("app", "someApp")
				.execute()
				.collectList()
				.block();
			
			source.activate().block();
			source.activate("customer", "cust1").block();
			source.activate("app", "someApp").block();
			
			source.get("prop1").withParameters("customer", "cust1")
				.execute().collectList().block();
			
			source.get("prop2").withParameters("app", "someApp")
				.execute().collectList().block();
			
			try {
				source.get("prop1").withParameters("customer", "cust1", "app", "someApp")
					.execute().collectList().block();
				
				Assertions.fail("Should throw an IllegalStateException");
			} 
			catch (IllegalStateException e) {
				Assertions.assertEquals("MetaData CONF:META:[app=\"someApp\"] is conflicting with CONF:META:[customer=\"cust1\"] when considering parameters [customer=\"cust1\", app=\"someApp\"]", e.getMessage());
			}
			
			try {
				source.activate("app", "someApp", "customer", "cust1").block();
				
				Assertions.fail("Should throw an IllegalStateException");
			} 
			catch (IllegalStateException e) {
				Assertions.assertEquals("A conflict of MetaData has been detected when considering parameters [app=\"someApp\", customer=\"cust1\"]", e.getMessage());
			}
		}
		finally {
			client.connect().reactive().flushall().block();
			client.shutdown();
		}
	}
	
	@Test
	public void testUnset() {
		RedisClient client = RedisClient.create("redis://localhost:6379");

		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			
			source.set("prop1", SpecialValue.UNSET)
				.execute()
				.collectList()
				.block();

			RedisConfigurationQueryResult result = source.get("prop1")
				.execute().blockLast();
			
			Assertions.assertTrue(result.getResult().get().isUnset());
		}
		finally {
			client.connect().reactive().flushall().block();
			client.shutdown();
		}
	}
	
	@Test
	public void testNull() {
		RedisClient client = RedisClient.create("redis://localhost:6379");

		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			
			source.set("prop1", SpecialValue.NULL)
				.execute()
				.collectList()
				.block();

			RedisConfigurationQueryResult result = source.get("prop1")
				.execute().blockLast();
			
			Assertions.assertFalse(result.getResult().get().isPresent());
		}
		finally {
			client.connect().reactive().flushall().block();
			client.shutdown();
		}
	}
	
	@Test
	public void testSinglePerf() {
		RedisClient client = RedisClient.create("redis://localhost:6379");

		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			source.set("prop1", "val").execute().blockLast();
			
			int count = 1000;
			int total = 0;
			for(int i = 0;i < count;i++) {
				long t0 = System.nanoTime();
				source.get("prop1").execute().blockLast().getResult().get().asString().get();
				total += System.nanoTime() - t0;
			}
			double avgPerf = (total / count);
			System.out.println("AVG: " + (total / count));
			
			Assertions.assertEquals(600000, avgPerf, 200000);
		}
		finally {
			client.connect().reactive().flushall().block();
			client.shutdown();
		}
	}
	
	@Test
	public void testHeavyPerf() throws IllegalArgumentException, URISyntaxException {
		RedisClient client = RedisClient.create("redis://localhost:6379");

		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			
			source.set("prop1", "abc")
				.and().set("prop2", 42).withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();

			source.activate().block();

			source.set("prop3", new URI("https://localhost:8443"))
				.and().set("prop2", 84).withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();

			source.activate(2).block();

			source.set("prop4", "Foo Bar").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();

			source.activate(3, "env", "production", "customer", "cust1").block();

			source.set("prop1", "abcdef")
				.and().set("prop2", 126).withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();

			source.activate().block();

			source.activate("env", "production", "customer", "cust1", "application", "app").block();
			
			int count = 100;
			long total = 0;
			for(int i=0;i<count;i++) {
				long t0 = System.nanoTime();
				RedisExecutableConfigurationQuery query = source.get("prop1")
					.and().get("prop3")
					.and().get("prop2", "prop4").withParameters("env", "production", "customer", "cust1", "application", "app");
				
				for(int j=0;j<1000;j++) {
					query.and().get("prop1")
						.and().get("prop3")
						.and().get("prop2", "prop4").withParameters("env", "production", "customer", "cust1", "application", "app");
				}
				
				query.execute()
					.collectList()
					.block();
				total += (System.nanoTime() - t0);
			}
			double avgPerf = (total / count);
			System.out.println("AVG: " + (total / count));
			
			Assertions.assertEquals(100000000, avgPerf, 20000000);
		}
		finally {
			client.connect().reactive().flushall().block();
			client.shutdown();
		}
	}
}
