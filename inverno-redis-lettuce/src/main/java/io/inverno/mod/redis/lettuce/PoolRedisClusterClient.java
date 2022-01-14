/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.redis.lettuce.internal.AbstractRedisClient;
import io.inverno.mod.redis.lettuce.internal.operations.StatefulRedisConnectionOperations;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.support.BoundedAsyncPool;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A Lettuce cluster pool Redis client wrapper.
 * </p>
 * 
 * <p>
 * This Redis client implementation doesn't support transaction.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 * @param <C> underlying cluster connection type
 */
public class PoolRedisClusterClient<A, B, C extends StatefulRedisClusterConnection<A, B>> extends AbstractRedisClient<A, B, C> {
	
	/**
	 * <p>
	 * Creates a Redis client with the specified Lettuce cluster pool.
	 * </p>
	 *
	 * @param pool      a bounded async pool
	 * @param keyType   the key type
	 * @param valueType the value type
	 */
	public PoolRedisClusterClient(BoundedAsyncPool<C> pool, Class<A> keyType, Class<B> valueType) {
		super(pool, keyType, valueType);
	}

	@Override
	protected Mono<StatefulRedisConnectionOperations<A, B, C, ?>> operations() {
		return Mono.fromCompletionStage(() -> this.pool.acquire()).map(connection -> new StatefulRedisConnectionOperations<>(connection, connection.reactive(), this.pool, this.keyType, this.valueType));
	}
}
