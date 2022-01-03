/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.redis.lettuce.internal;

import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.BaseRedisReactiveCommands;
import io.lettuce.core.api.reactive.RedisGeoReactiveCommands;
import io.lettuce.core.api.reactive.RedisHLLReactiveCommands;
import io.lettuce.core.api.reactive.RedisHashReactiveCommands;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import io.lettuce.core.api.reactive.RedisListReactiveCommands;
import io.lettuce.core.api.reactive.RedisScriptingReactiveCommands;
import io.lettuce.core.api.reactive.RedisSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.api.reactive.RedisTransactionalReactiveCommands;
import io.lettuce.core.support.BoundedAsyncPool;
import reactor.core.publisher.Mono;
import io.inverno.mod.redis.lettuce.RedisTransactionalOperations;

/**
 *
 * @author jkuhn
 */
public class StatefulRedisConnectionTransactionalOperations<A, B, C extends StatefulConnection<A, B>, D extends BaseRedisReactiveCommands<A, B> & RedisGeoReactiveCommands<A, B> & RedisHashReactiveCommands<A, B> & RedisHLLReactiveCommands<A, B> & RedisKeyReactiveCommands<A, B> & RedisListReactiveCommands<A, B> & RedisScriptingReactiveCommands<A, B> & RedisSetReactiveCommands<A, B> & RedisSortedSetReactiveCommands<A, B> & RedisStreamReactiveCommands<A, B> & RedisStringReactiveCommands<A, B> & RedisTransactionalReactiveCommands<A, B>> 
	extends StatefulRedisConnectionOperations<A, B, C, D> implements RedisTransactionalOperations<A, B> {
	
	public StatefulRedisConnectionTransactionalOperations(C connection, D commands, BoundedAsyncPool<C> pool) {
		super(connection, commands, pool);
	}

	@Override
	public Mono<String> discard() {
		return Mono.usingWhen(
			this.commands.discard(), 
			r -> Mono.just(r), 
			ign -> this.close()
		);
	}

	@Override
	public Mono<TransactionResult> exec() {
		return Mono.usingWhen(
			this.commands.exec(), 
			r -> Mono.just(r), 
			ign -> this.close()
		);
	}
}
