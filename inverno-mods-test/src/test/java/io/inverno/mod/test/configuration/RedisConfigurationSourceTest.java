/*
 * Copyright 2022 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.test.configuration;

import io.inverno.mod.configuration.ConfigurationKey;
import io.inverno.mod.configuration.ConfigurationQueryResult;
import io.inverno.mod.configuration.ConfigurationUpdate;
import io.inverno.mod.configuration.DefaultingStrategy;
import io.inverno.mod.configuration.source.RedisConfigurationSource;
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
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@EnabledIf( value = "isEnabled", disabledReason = "Failed to connect to test Redis datastore" )
public class RedisConfigurationSourceTest {
	
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
	public void testRedisConfigurationSource() throws URISyntaxException {
		
//		[test=5, tutu="plop"] {
//			tata.toto="563"
//		}
//
//		# Comment on tata
//		tata {
//			[tutu="plop"] {
//				toto = 65432	
//			}
//		}
//
//		url="https://localhost:8443"
//		table="a,b,c"
//		some_string="abc\ndef"
//
//		[ context="text_block" ] {
//			text_block = """
//				Hey 
//				   This is 
//						a 
//					text 		block
//			"""
//		}
//
//		plip.plap {
//			json = """
//				{
//					"title":"Some json",
//					table = ["abc,"bcd"]
//				}
//			"""
//		}
		
		RedisTransactionalClient<String, String> client = createClient();
		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			
			source
				.set("tata.toto", "563").withParameters("test", 5, "tutu", "plop").and()
				.set("tata.toto", 65432).withParameters("tutu", "plop").and()
				.set("url", "https://localhost:8443").and()
				.set("table", "a,b,c").and()
				.set("some_string", "abc\ndef").and()
				.set("text_block", "\n" +
					"		Hey \n" +
					"		   This is \n" +
					"				a \n" +
					"			text 		block\n" +
					"	"
				).withParameters("context", "text_block").and()
				.set("plip.plap.json", "\n" +
					"		{\n" +
					"			\"title\":\"Some json\",\n" +
					"			table = [\"abc,\"bcd\"]\n" +
					"		}\n" +
					"	"
				)
				.execute().blockLast();
			
			
			List<ConfigurationQueryResult> results = source
				.get("tata.toto").withParameters("tutu", "plop","test", 5).and()
				.get("tata.toto").withParameters("tutu", "plop").and()
				.get("url", "table").and()
				.get("text_block").withParameters("context", "text_block").and()
				.get("plip.plap.json").and()
				.get("some_string")
				.execute()
				.collectList()
				.block();
		
//			results.stream().forEach(queryResult -> {
//				System.out.println(queryResult.getQueryKey() + " -> " + queryResult.getResult().orElse(null));
//			});

			Assertions.assertEquals(7, results.size());

			Iterator<ConfigurationQueryResult> resultIterator = results.iterator();

			ConfigurationQueryResult current = resultIterator.next();
			Assertions.assertTrue(current.isPresent());
			Assertions.assertEquals("tata.toto", current.get().getKey().getName());
			Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(ConfigurationKey.Parameter.of("test", 5), ConfigurationKey.Parameter.of("tutu", "plop"))));
			Assertions.assertTrue(current.get().isPresent());
			Assertions.assertEquals(563, current.get().asInteger().get());

			current = resultIterator.next();
			Assertions.assertTrue(current.isPresent());
			Assertions.assertEquals("tata.toto", current.get().getKey().getName());
			Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(ConfigurationKey.Parameter.of("tutu", "plop"))));
			Assertions.assertTrue(current.get().isPresent());
			Assertions.assertEquals(65432, current.get().asInteger().get());

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

			current = resultIterator.next();
			Assertions.assertTrue(current.get().isPresent());
			Assertions.assertEquals("text_block", current.get().getKey().getName());
			Assertions.assertTrue(current.get().getKey().getParameters().containsAll(List.of(ConfigurationKey.Parameter.of("context", "text_block"))));
			Assertions.assertTrue(current.get().isPresent());
			Assertions.assertEquals("\n" + 
				"		Hey \n" + 
				"		   This is \n" + 
				"				a \n" + 
				"			text 		block\n" + 
				"	", current.get().asString().get());

			current = resultIterator.next();
			Assertions.assertTrue(current.get().isPresent());
			Assertions.assertEquals("plip.plap.json", current.get().getKey().getName());
			Assertions.assertTrue(current.get().getKey().getParameters().isEmpty());
			Assertions.assertTrue(current.get().isPresent());
			Assertions.assertEquals("\n" + 
				"		{\n" + 
				"			\"title\":\"Some json\",\n" + 
				"			table = [\"abc,\"bcd\"]\n" + 
				"		}\n" + 
				"	", current.get().asString().get());

			current = resultIterator.next();
			Assertions.assertTrue(current.get().isPresent());
			Assertions.assertEquals("some_string", current.get().getKey().getName());
			Assertions.assertTrue(current.get().getKey().getParameters().isEmpty());
			Assertions.assertTrue(current.get().isPresent());
			Assertions.assertEquals("abc\ndef", current.get().asString().get());
			
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void testNull() throws URISyntaxException {
//		testNull = null
		RedisTransactionalClient<String, String> client = createClient();
		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			
			source.set("testNull", ConfigurationUpdate.SpecialValue.NULL).execute().blockLast();
			
			List<ConfigurationQueryResult> results = source
				.get("testNull")
				.execute()
				.collectList()
				.block();
		
			Assertions.assertEquals(1, results.size());
			Assertions.assertTrue(results.get(0).isPresent());
			Assertions.assertFalse(results.get(0).get().asString().isPresent());
			
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void testUnset() throws URISyntaxException {
//		testUnset = unset
		RedisTransactionalClient<String, String> client = createClient();
		try {
			RedisConfigurationSource source = new RedisConfigurationSource(client);
			
			source.set("testUnset", ConfigurationUpdate.SpecialValue.UNSET).execute().blockLast();
			
			List<ConfigurationQueryResult> results = source
				.get("testUnset")
				.execute()
				.collectList()
				.block();
		
			Assertions.assertEquals(1, results.size());
			Assertions.assertTrue(results.get(0).isPresent());
			Assertions.assertTrue(results.get(0).get().isUnset());
			
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
			RedisConfigurationSource source = new RedisConfigurationSource(client);
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
			Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "prod", "name", "test1"), result.getQueryKey());
			Assertions.assertEquals("ERROR", result.get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "prod", "name", "test2"), result.getQueryKey());
			Assertions.assertEquals("WARN", result.get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "dev", "name", "test1"), result.getQueryKey());
			Assertions.assertEquals("INFO", result.get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "prod"), result.getQueryKey());
			Assertions.assertEquals("WARN", result.get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals(ConfigurationKey.of("log.level", "environment", "dev"), result.getQueryKey());
			Assertions.assertEquals("INFO", result.get().asString().get());

			result = resultsIterator.next();
			Assertions.assertEquals(ConfigurationKey.of("log.level"), result.getQueryKey());
			Assertions.assertEquals("INFO", result.get().asString().get());
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
}
