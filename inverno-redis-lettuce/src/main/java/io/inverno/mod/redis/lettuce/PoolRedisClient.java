/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.redis.lettuce;

import io.inverno.mod.redis.lettuce.internal.StatefulRedisConnectionOperations;
import io.inverno.mod.redis.lettuce.internal.StatefulRedisConnectionTransactionalOperations;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.BoundedAsyncPool;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class PoolRedisClient<A, B, C extends StatefulRedisConnection<A, B>> extends AbstractRedisClient<A, B, C> implements RedisTransactionalClient<A, B> {

	public PoolRedisClient(BoundedAsyncPool<C> pool) {
		super(pool);
	}
	
	@Override
	protected Mono<StatefulRedisConnectionOperations<A, B, C, ?>> operations() {
		return Mono.fromCompletionStage(() -> this.pool.acquire()).map(connection -> new StatefulRedisConnectionOperations<>(connection, connection.reactive(), this.pool));
	}
	
	protected Mono<StatefulRedisConnectionTransactionalOperations<A, B, C, ?>> transactionalOperations() {
		return Mono.fromCompletionStage(() -> this.pool.acquire()).map(connection -> new StatefulRedisConnectionTransactionalOperations<>(connection, connection.reactive(), this.pool));
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
	public Mono<TransactionResult> multi(Function<RedisOperations<A, B>, Publisher<Publisher<Object>>> function, A... watches) {
		// commands must be subscibed in the function: .set(...).subscribe() which basically returns QUEUED
		return Mono.usingWhen(
			this.multi(watches),
			toperations -> Flux.merge(Flux.from(function.apply(toperations)).concatWithValues(toperations.exec().cast(Object.class))).last().cast(TransactionResult.class),
			toperations -> Mono.empty(),
			(toperations, ex) -> {
				return toperations.discard().then(Mono.error(ex));
			},
			RedisTransactionalOperations::discard
		);
	}	
}
