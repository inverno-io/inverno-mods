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
package io.inverno.mod.redis.lettuce.internal.operations;

import io.inverno.mod.redis.RedisTransactionResult;
import io.inverno.mod.redis.RedisTransactionalOperations;
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
import io.lettuce.core.support.AsyncPool;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 * @param <B>
 * @param <C>
 * @param <D>
 */
public class StatefulRedisConnectionTransactionalOperations<A, B, C extends StatefulConnection<A, B>, D extends BaseRedisReactiveCommands<A, B> & RedisGeoReactiveCommands<A, B> & RedisHashReactiveCommands<A, B> & RedisHLLReactiveCommands<A, B> & RedisKeyReactiveCommands<A, B> & RedisListReactiveCommands<A, B> & RedisScriptingReactiveCommands<A, B> & RedisSetReactiveCommands<A, B> & RedisSortedSetReactiveCommands<A, B> & RedisStreamReactiveCommands<A, B> & RedisStringReactiveCommands<A, B> & RedisTransactionalReactiveCommands<A, B>>
	extends StatefulRedisConnectionOperations<A, B, C, D> implements RedisTransactionalOperations<A, B> {

	/**
	 * 
	 * @param connection
	 * @param commands
	 * @param pool
	 * @param keyType
	 * @param valueType 
	 */
	public StatefulRedisConnectionTransactionalOperations(C connection, D commands, AsyncPool<C> pool, Class<A> keyType, Class<B> valueType) {
		super(connection, commands, pool, keyType, valueType);
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
	public Mono<RedisTransactionResult> exec() {
		return Mono.usingWhen(
			this.commands.exec(),
			r -> Mono.just(r), 
			ign -> this.close()
		).map(RedisTransactionResultImpl::new);
	}
}
