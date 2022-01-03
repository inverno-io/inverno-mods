/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.redis.lettuce;

import io.inverno.mod.redis.lettuce.internal.StatefulRedisConnectionOperations;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.support.BoundedAsyncPool;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class PoolRedisClusterClient<A, B, C extends StatefulRedisClusterConnection<A, B>> extends AbstractRedisClient<A, B, C> {

	public PoolRedisClusterClient(BoundedAsyncPool<C> pool) {
		super(pool);
	}
	
	protected Mono<StatefulRedisConnectionOperations<A, B, C, ?>> operations() {
		return Mono.fromCompletionStage(() -> this.pool.acquire()).map(connection -> new StatefulRedisConnectionOperations<>(connection, connection.reactive(), this.pool));
	}
}
