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

import io.inverno.mod.redis.lettuce.internal.AbstractRedisClient;
import io.inverno.mod.redis.RedisOperations;
import io.inverno.mod.redis.RedisTransactionResult;
import io.inverno.mod.redis.RedisTransactionalClient;
import io.inverno.mod.redis.RedisTransactionalOperations;
import io.inverno.mod.redis.lettuce.internal.operations.StatefulRedisConnectionOperations;
import io.inverno.mod.redis.lettuce.internal.operations.StatefulRedisConnectionTransactionalOperations;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.BoundedAsyncPool;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A Lettuce pool Redis client wrapper.
 * </p>
 * 
 * <p>
 * This Redis client implementation supports transaction.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 * @param <C> underlying connection type
 */
public class PoolRedisClient<A, B, C extends StatefulRedisConnection<A, B>> extends AbstractRedisClient<A, B, C> implements RedisTransactionalClient<A, B> {
	
	/**
	 * <p>
	 * Creates a Redis client with the specified Lettuce pool.
	 * </p>
	 *
	 * @param pool      a bounded async pool
	 * @param keyType   the key type
	 * @param valueType the value type
	 */
	public PoolRedisClient(BoundedAsyncPool<C> pool, Class<A> keyType, Class<B> valueType) {
		super(pool, keyType, valueType);
	}

	@Override
	protected Mono<StatefulRedisConnectionOperations<A, B, C, ?>> operations() {
		return Mono.fromCompletionStage(() -> this.pool.acquire()).map(connection -> new StatefulRedisConnectionOperations<>(connection, connection.reactive(), this.pool, this.keyType, this.valueType));
	}

	/**
	 * <p>
	 * Returns Redis transactional operations.
	 * </p>
	 * 
	 * @return a mono emitting RedisTransactionalOperations object
	 */
	protected Mono<StatefulRedisConnectionTransactionalOperations<A, B, C, ?>> transactionalOperations() {
		return Mono.fromCompletionStage(() -> this.pool.acquire()).map(connection -> new StatefulRedisConnectionTransactionalOperations<>(connection, connection.reactive(), this.pool, this.keyType, this.valueType));
	}
	
	@Override
	public Mono<RedisTransactionalOperations<A, B>> multi(A... watches) {
		return this.transactionalOperations().flatMap(operations -> {
			if(watches != null && watches.length > 0) {
				return operations.getCommands().watch(watches).then(operations.getCommands().multi().map(r -> {
					if(r.equals("OK")) {
						return operations;
					}
					else {
						throw new IllegalStateException(r);
					}
				}));
			}
			else {
				return operations.getCommands().multi().map(r -> {
					if(r.equals("OK")) {
						return operations;
					}
					else {
						throw new IllegalStateException(r);
					}
				});
			}
		});
	}

	@Override
	public Mono<RedisTransactionResult> multi(Function<RedisOperations<A, B>, Publisher<Publisher<Object>>> function, A... watches) {
		// commands must be subscibed in the function: .set(...).subscribe() which basically returns QUEUED
		return Mono.usingWhen(
			this.multi(watches),
			toperations -> Flux.merge(Flux.from(function.apply(toperations)).concatWithValues(toperations.exec().cast(Object.class))).last().cast(RedisTransactionResult.class),
			toperations -> Mono.empty(),
			(toperations, ex) -> {
				return toperations.discard().then(Mono.error(ex));
			},
			RedisTransactionalOperations::discard
		);
	}
}
