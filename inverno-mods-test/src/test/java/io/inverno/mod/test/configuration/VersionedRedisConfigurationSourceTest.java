package io.inverno.mod.test.configuration;

import io.inverno.mod.configuration.ConfigurationKey.Parameter;
import io.inverno.mod.configuration.ConfigurationProperty;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationUpdate.SpecialValue;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.source.VersionedRedisConfigurationSource;
import io.inverno.mod.configuration.source.VersionedRedisConfigurationSource.VersionedRedisConfigurationKey;
import io.inverno.mod.configuration.source.VersionedRedisConfigurationSource.VersionedRedisConfigurationQueryResult;
import io.inverno.mod.configuration.source.VersionedRedisConfigurationSource.VersionedRedisExecutableConfigurationQuery;
import io.inverno.mod.redis.RedisTransactionalClient;
import io.inverno.mod.redis.lettuce.PoolRedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

@EnabledIf( value = "isEnabled", disabledReason = "Failed to connect to test Redis datastore" )
public class VersionedRedisConfigurationSourceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}

	private static final io.lettuce.core.RedisClient REDIS_CLIENT = io.lettuce.core.RedisClient.create();
	
	private static PoolRedisClient<String, String, StatefulRedisConnection<String, String>> createClient() {
		
		BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
			() -> REDIS_CLIENT.connectAsync(StringCodec.UTF8, RedisURI.create("redis://localhost:6379")), 
			BoundedPoolConfig.create()
		);
		return new PoolRedisClient<>(pool, String.class, String.class);
	}
	
	private static void flushAll() {
		REDIS_CLIENT.connect(RedisURI.create("redis://localhost:6379")).reactive().flushall().block();
	}
	
	public static boolean isEnabled() {
		try (StatefulRedisConnection<String, String> connection = REDIS_CLIENT.connect(RedisURI.create("redis://localhost:6379"))) {
			return true;
		}
		catch (RedisConnectionException e) {
			return false;
		}	
	}
	
	@Test
	public void testVersionedRedisConfigurationSourceRedisClient() throws IllegalArgumentException, URISyntaxException {
		RedisTransactionalClient<String, String> client = createClient();

		try {
			VersionedRedisConfigurationSource source = new VersionedRedisConfigurationSource(client);
			
			source.set("prop1", "abc")
				.and().set("prop2", 42).withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			List<ConfigurationQueryResult> result = source.get("prop1")
				.and().get("prop2").withParameters("env", "production", "customer", "cust1", "application", "app")
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(2, result.size());
			
			Iterator<ConfigurationQueryResult> resultIterator = result.iterator();
			
			ConfigurationQueryResult current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
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
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(1, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(1, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
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
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(1, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(1, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertFalse(current.getResult().isPresent());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(1, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(42, current.getResult().get().asInteger().get());
			
			result = source.get("prop3").atRevision(2)
				.execute()
				.collectList()
				.block();
			
			Assertions.assertEquals(1, result.size());
			
			resultIterator = result.iterator();
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
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
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
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
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
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
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(84, current.getResult().get().asInteger().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop4", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
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
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(1, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abc", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(2, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(84, current.getResult().get().asInteger().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop4", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
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
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abcdef", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(84, current.getResult().get().asInteger().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop4", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
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
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop1", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals("abcdef", current.getResult().get().asString().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(3, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop3", current.getResult().get().getKey().getName());
			Assertions.assertEquals(2, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().isEmpty());
			Assertions.assertEquals(new URI("https://localhost:8443"), current.getResult().get().asURI().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(4, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop2", current.getResult().get().getKey().getName());
			Assertions.assertEquals(4, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals(126, current.getResult().get().asInteger().get());
			
			current = resultIterator.next();
			Assertions.assertFalse(((VersionedRedisConfigurationQueryResult)current).getQueryKey().getRevision().isPresent());
//			Assertions.assertEquals(4, ((VersionedRedisConfigurationQueryResult)current).getQueryKey().getMetaData().get().getActiveRevision().get());
			Assertions.assertTrue(current.getResult().isPresent());
			Assertions.assertEquals("prop4", current.getResult().get().getKey().getName());
			Assertions.assertEquals(3, ((VersionedRedisConfigurationKey)current.getResult().get().getKey()).getRevision().get());
			Assertions.assertTrue(current.getResult().get().getKey().getParameters().containsAll(List.of(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app"))));
			Assertions.assertEquals("Foo Bar", current.getResult().get().asString().get());
			
			Assertions.assertEquals(4, source.getMetaData().block().getWorkingRevision().get());
			Assertions.assertEquals(3, source.getMetaData().block().getActiveRevision().get());
			Assertions.assertEquals(5, source.getMetaData(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app")).block().getWorkingRevision().get());
			Assertions.assertEquals(4, source.getMetaData(Parameter.of("env", "production"), Parameter.of("customer", "cust1"), Parameter.of("application", "app")).block().getActiveRevision().get());
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void testConflictDetection() throws InterruptedException {
		RedisTransactionalClient<String, String> client = createClient();

		try {
			VersionedRedisConfigurationSource source = new VersionedRedisConfigurationSource(client);
			
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
				Assertions.assertEquals(Byte.MAX_VALUE, Byte.MAX_VALUE);
				Assertions.assertTrue(Set.of("MetaData CONF:V:META:[customer=\"cust1\"] is conflicting with CONF:V:META:[app=\"someApp\"] when considering parameters [customer=\"cust1\", app=\"someApp\"]", "MetaData CONF:V:META:[app=\"someApp\"] is conflicting with CONF:V:META:[customer=\"cust1\"] when considering parameters [customer=\"cust1\", app=\"someApp\"]").contains(e.getMessage()));
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
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void testUnset() {
		RedisTransactionalClient<String, String> client = createClient();
		try {
			VersionedRedisConfigurationSource source = new VersionedRedisConfigurationSource(client);
			
			source.set("prop1", SpecialValue.UNSET)
				.execute()
				.collectList()
				.block();

			ConfigurationQueryResult result = source.get("prop1")
				.execute().blockLast();
			
			Assertions.assertTrue(result.getResult().get().isUnset());
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void testNull() {
		RedisTransactionalClient<String, String> client = createClient();

		try {
			VersionedRedisConfigurationSource source = new VersionedRedisConfigurationSource(client);
			
			source.set("prop1", SpecialValue.NULL)
				.execute()
				.collectList()
				.block();

			ConfigurationQueryResult result = source.get("prop1")
				.execute().blockLast();
			
			Assertions.assertFalse(result.getResult().get().isPresent());
		}
		finally {
			client.close().block();
//			flushAll();
		}
	}
	
	@Test
	public void testList() {
		RedisTransactionalClient<String, String> client = createClient();
		
		try {
			VersionedRedisConfigurationSource source = new VersionedRedisConfigurationSource(client);
			
			source
				.set("logging.level", "info").withParameters("environment", "prod", "name", "test1").and()
				.set("logging.level", "debug").withParameters("environment", "dev", "name", "test1").and()
				.set("logging.level", "info").withParameters("environment", "prod", "name", "test2").and()
				.set("logging.level", "error").withParameters("environment", "prod", "name", "test3")
				.execute()
				.collectList()
				.block();

			source.activate().block();
			
			List<ConfigurationProperty> result = source.list("logging.level").executeAll().collectList().block();
			Assertions.assertEquals(4, result.size());
			Assertions.assertEquals(
				Set.of(
					"logging.level[environment=\"prod\",name=\"test1\"] = info", 
					"logging.level[environment=\"dev\",name=\"test1\"] = debug", 
					"logging.level[environment=\"prod\",name=\"test3\"] = error", 
					"logging.level[environment=\"prod\",name=\"test2\"] = info"
				), 
				result.stream().map(p -> p.toString()).collect(Collectors.toSet())
			);
			
			result = source.list("logging.level").execute().collectList().block();
			Assertions.assertEquals(0, result.size());
			
			result = source.list("logging.level").withParameters(Parameter.of("environment", "prod"), Parameter.wildcard("name")).execute().collectList().block();
			Assertions.assertEquals(3, result.size());
			Assertions.assertEquals(
				Set.of(
					"logging.level[environment=\"prod\",name=\"test1\"] = info", 
					"logging.level[environment=\"prod\",name=\"test3\"] = error", 
					"logging.level[environment=\"prod\",name=\"test2\"] = info"
				), 
				result.stream().map(p -> p.toString()).collect(Collectors.toSet())
			);
			
			result = source.list("logging.level").withParameters(Parameter.of("environment", "dev")).execute().collectList().block();
			Assertions.assertEquals(0, result.size());
			
			result = source.list("logging.level").withParameters(Parameter.of("environment", "dev")).executeAll().collectList().block();
			Assertions.assertEquals(1, result.size());
			Assertions.assertEquals(
				Set.of(
					"logging.level[environment=\"dev\",name=\"test1\"] = debug"
				), 
				result.stream().map(p -> p.toString()).collect(Collectors.toSet())
			);
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void testDefaulting() {
		RedisTransactionalClient<String, String> client = createClient();
		
		try {
			VersionedRedisConfigurationSource source = new VersionedRedisConfigurationSource(client);
			source = source.withDefaultingStrategy(DefaultingStrategy.lookup());
			
			source
				.set("log.level", "INFO").and()
				.set("log.level", "WARN").withParameters("environment", "prod").and()
				.set("log.level", "ERROR").withParameters("environment", "prod", "name", "test1")
				.execute().blockLast();
			
			List<ConfigurationQueryResult> results = source
				.get("log.level").withParameters("environment", "prod", "name", "test1").and()
				.get("log.level").withParameters("environment", "prod", "name", "test2").and()
				.get("log.level").withParameters("environment", "dev", "name", "test1").and()
				.get("log.level").withParameters("environment", "prod").and()
				.get("log.level").withParameters("environment", "dev").and()
				.get("log.level")
				.execute()
				.collectList()
				.block();
		
			Assertions.assertEquals(6, results.size());

			Iterator<ConfigurationQueryResult> resultsIterator = results.iterator();

			ConfigurationQueryResult result = resultsIterator.next();
			Assertions.assertEquals("log.level", result.getQueryKey().getName());
			Assertions.assertEquals(List.of(Parameter.of("environment", "prod"), Parameter.of("name", "test1")), new ArrayList<>(result.getQueryKey().getParameters()));
			Assertions.assertEquals("ERROR", result.getResult().get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals("log.level", result.getQueryKey().getName());
			Assertions.assertEquals(List.of(Parameter.of("environment", "prod"), Parameter.of("name", "test2")), new ArrayList<>(result.getQueryKey().getParameters()));
			Assertions.assertEquals("WARN", result.getResult().get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals("log.level", result.getQueryKey().getName());
			Assertions.assertEquals(List.of(Parameter.of("environment", "dev"), Parameter.of("name", "test1")), new ArrayList<>(result.getQueryKey().getParameters()));
			Assertions.assertEquals("INFO", result.getResult().get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals("log.level", result.getQueryKey().getName());
			Assertions.assertEquals(List.of(Parameter.of("environment", "prod")), new ArrayList<>(result.getQueryKey().getParameters()));
			Assertions.assertEquals("WARN", result.getResult().get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals("log.level", result.getQueryKey().getName());
			Assertions.assertEquals(List.of(Parameter.of("environment", "dev")), new ArrayList<>(result.getQueryKey().getParameters()));
			Assertions.assertEquals("INFO", result.getResult().get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals("log.level", result.getQueryKey().getName());
			Assertions.assertTrue(result.getQueryKey().getParameters().isEmpty());
			Assertions.assertEquals("INFO", result.getResult().get().asString().get());
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void testSinglePerf() {
		RedisTransactionalClient<String, String> client = createClient();

		try {
			VersionedRedisConfigurationSource source = new VersionedRedisConfigurationSource(client);
			source.set("prop1", "val").execute().blockLast();
			
			int count = 1000;
			int total = 0;
			for(int i = 0;i < count+1;i++) {
				long t0 = System.nanoTime();
				source.get("prop1").execute().blockLast().getResult().get().asString().get();
				if(i > 0) {
					// Let's ignore warmup
					total += System.nanoTime() - t0;
				}
			}
			double avgPerf = (total / count);
			System.out.println("AVG: " + (total / count));
			
			// This obviously depends on the hardware
			Assertions.assertEquals(750000, avgPerf, 400000); // 0.75ms to fetch 1 property
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
//	@Disabled
	public void testHeavyPerf() throws IllegalArgumentException, URISyntaxException {
		RedisTransactionalClient<String, String> client = createClient();

		try {
			VersionedRedisConfigurationSource source = new VersionedRedisConfigurationSource(client);
			
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
			for(int i=0;i<count+1;i++) {
				long t0 = System.nanoTime();
				
				VersionedRedisExecutableConfigurationQuery query = source.get("prop1")
					.and().get("prop3")
					.and().get("prop2", "prop4").withParameters("env", "production", "customer", "cust1", "application", "app");
				
				for(int j=0;j<999;j++) {
					query.and().get("prop1")
						.and().get("prop3")
						.and().get("prop2", "prop4").withParameters("env", "production", "customer", "cust1", "application", "app");
				}
				
				query.execute()
					.collectList()
					.block();
				if(i > 0) {
					// Let's ignore warmup
					total += (System.nanoTime() - t0);
				}
			}
			double avgPerf = (total / count);
			System.out.println("AVG: " + (total / count));
			
			// This obviously depends on the hardware
			Assertions.assertEquals(65000000, avgPerf, 10000000); // 65ms to fetch 4000 properties
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
}
