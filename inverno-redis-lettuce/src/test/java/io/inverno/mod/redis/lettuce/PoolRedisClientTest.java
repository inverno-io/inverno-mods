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
package io.inverno.mod.redis.lettuce;

import io.inverno.mod.redis.RedisTransactionResult;
import io.inverno.mod.redis.operations.RedisGeoReactiveOperations;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.BoundedAsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
@EnabledIf( value = "isEnabled", disabledReason = "Failed to connect to test Redis database" )
public class PoolRedisClientTest {
	
	private static final RedisClient REDIS_CLIENT = io.lettuce.core.RedisClient.create();
	
	private static PoolRedisClient<String, String, StatefulRedisConnection<String, String>> createClient() {
		BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
			() -> REDIS_CLIENT.connectAsync(StringCodec.UTF8, RedisURI.create("redis://localhost:6379")), 
			BoundedPoolConfig.create()
		);
		
		return new PoolRedisClient<>(pool, String.class, String.class);
	}
	
	public static boolean isEnabled() {
		try (StatefulRedisConnection<String, String> connection = REDIS_CLIENT.connect(RedisURI.create("redis://localhost:6379"))) {
			return true;
		}
		catch (RedisConnectionException e) {
			return false;
		}	
	}
	
	private static void flushAll() {
		REDIS_CLIENT.connect(RedisURI.create("redis://localhost:6379")).reactive().flushall().block();
	}
	
	private static PoolRedisClient<String, String, StatefulRedisConnection<String, String>> createClientWithAuthentication() {
		RedisClient redisClient = io.lettuce.core.RedisClient.create();
		
		BoundedAsyncPool<StatefulRedisConnection<String, String>> pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
			() -> redisClient.connectAsync(StringCodec.UTF8, RedisURI.create("redis://localhost:6379"))
				.thenCompose(connection -> connection.async().auth("user", "password")
				.thenApply(result -> {
						if(!result.equals("OK")) {
							throw new RuntimeException("Authentication error");
						}
						return connection;
					})
				),
			BoundedPoolConfig.create()
		);
		
		return new PoolRedisClient<>(pool, String.class, String.class);
	}
	
	@Test
	public void simpleTest() {
		var client = createClient();
		try {
			Assertions.assertEquals("OK", client.set("key", "value").block());
			Assertions.assertEquals("value", client.get("key").block());
			Assertions.assertEquals(1l, client.del("key").block());
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void connectionTest() {
		var client = createClient();
		try {
			Flux<String> connection = Flux.from(client.connection(ops -> {
				return ops.set("key_1", "value_1")
					.then(ops.set("key_2", "value_2"))
					.thenMany(Flux.mergeSequential(ops.get("key_1"), ops.get("key_2")));
			}));
			
			List<String> connection_result = connection.collectList().block();
			
			Assertions.assertEquals(2, connection_result.size());
			
			Assertions.assertEquals("value_1", connection_result.get(0));
			Assertions.assertEquals("value_2", connection_result.get(1));
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void batchTest() {
		var client = createClient();
		try {
			Flux<String> batch1 = Flux.from(client.batch(ops -> {
				return Flux.range(0, 10).map(i -> {
					return ops.set("key_" + i, "value_" + i);
				});
			}));

			List<String> batch1_result = batch1.collectList().block();
			Assertions.assertEquals(10, batch1_result.size());
			Assertions.assertEquals(IntStream.range(0, 10).mapToObj(i -> "OK").collect(Collectors.toList()), batch1_result);

			Flux<String> batch2 = Flux.from(client.batch(ops -> {
				return Flux.range(0, 10).map(i -> {
					return ops.get("key_" + i);
				});
			}));

			List<String> batch2_result = batch2.collectList().block();
			Assertions.assertEquals(10, batch2_result.size());
			Assertions.assertEquals(IntStream.range(0, 10).mapToObj(i -> "value_" + i).collect(Collectors.toList()), batch2_result);

			Flux<Object> batch3 = Flux.from(client.batch(ops -> {
				return Flux.just(ops.get("key_1").cast(Object.class), ops.strlen("key_5").cast(Object.class), ops.keys("*").collectSortedList().cast(Object.class));
			}));

			List<Object> batch3_result = batch3.collectList().block();
			Assertions.assertEquals(3, batch3_result.size());

			Assertions.assertEquals("value_1", batch3_result.get(0));
			Assertions.assertEquals(7l, batch3_result.get(1));
			Assertions.assertEquals(IntStream.range(0, 10).mapToObj(i -> "key_" + i).collect(Collectors.toList()), batch3_result.get(2));
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void multiTest() {
		var client = createClient();
		try {
			Mono<RedisTransactionResult> multi1 = client.multi()
				.flatMap(ops -> {
					ops.set("key_1", "value_1").subscribe();
					ops.set("key_2", "value_2").subscribe();
					
					return ops.exec();
				});
			
			RedisTransactionResult multi1_result = multi1.block();
			
			Assertions.assertFalse(multi1_result.wasDiscarded());
			Assertions.assertEquals(2, multi1_result.size());
			Assertions.assertEquals("OK", multi1_result.get(0));
			Assertions.assertEquals("OK", multi1_result.get(1));
			
			Mono<RedisTransactionResult> multi2 = client.multi("key_3")
				.doOnNext(ign -> client.set("key_3", "value_3").block())
				.flatMap(ops -> {
					ops.set("key_3", "value_3").subscribe();
					
					return ops.exec();
				});
			
			RedisTransactionResult multi2_result = multi2.block();
			
			Assertions.assertTrue(multi2_result.wasDiscarded());
			Assertions.assertTrue(multi2_result.isEmpty());

			flushAll();
			
			Mono<RedisTransactionResult> multi3 = client.multi(ops -> {
				return Flux.just(ops.set("key_1", "value_1"), ops.set("key_2", "value_2"));
			});
			
			RedisTransactionResult multi3_result = multi3.block();
			
			Assertions.assertFalse(multi3_result.wasDiscarded());
			Assertions.assertEquals(2, multi3_result.size());
			Assertions.assertEquals("OK", multi3_result.get(0));
			Assertions.assertEquals("OK", multi3_result.get(1));
			
			Mono<RedisTransactionResult> multi4 = client.multi("key_3")
				.doOnNext(ign -> client.set("key_3", "value_3").block())
				.flatMap(ops -> {
					ops.set("key_3", "value_3").subscribe();
					
					return ops.exec();
				});
			
			RedisTransactionResult multi4_result = multi4.block();
			
			Assertions.assertTrue(multi4_result.wasDiscarded());
			Assertions.assertTrue(multi4_result.isEmpty());
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
	
	@Test
	public void testBuilders() {
		var client = createClient();
		try {
//			GEOADD Sicily 13.361389 38.115556 "Palermo" 15.087269 37.502669 "Catania"
			Long geoadd_result = client.geoadd().build("Sicily", items -> items.item(13.361389, 38.115556, "Palermo").item(15.087269, 37.502669, "Catania")).block();
			
			Assertions.assertEquals(2, geoadd_result);
			
//			 GEORADIUS Sicily 15 37 200 km WITHCOORD
			List<RedisGeoReactiveOperations.GeoWithin<String>> georadius_result = client.georadiusExtended().withcoord().build("Sicily", 15, 37, 200, RedisGeoReactiveOperations.GeoUnit.km).collectList().block();

			Assertions.assertEquals(2, georadius_result.size());
			
			RedisGeoReactiveOperations.GeoWithin<String> geoWithin = georadius_result.get(0);
			
			Assertions.assertEquals("Palermo", geoWithin.getMember());
			Assertions.assertTrue(geoWithin.getCoordinates().isPresent());
			Assertions.assertEquals(13.36138933897018433, geoWithin.getCoordinates().get().getLongitude());
			Assertions.assertEquals(38.11555639549629859, geoWithin.getCoordinates().get().getLatitude());
			
			geoWithin = georadius_result.get(1);
			
			Assertions.assertEquals("Catania", geoWithin.getMember());
			Assertions.assertTrue(geoWithin.getCoordinates().isPresent());
			Assertions.assertEquals(15.08726745843887329, geoWithin.getCoordinates().get().getLongitude());
			Assertions.assertEquals(37.50266842333162032, geoWithin.getCoordinates().get().getLatitude());
		}
		finally {
			client.close().block();
			flushAll();
		}
	}
}
