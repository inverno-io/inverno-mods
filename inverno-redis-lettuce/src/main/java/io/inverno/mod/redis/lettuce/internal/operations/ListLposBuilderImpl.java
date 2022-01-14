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

import io.inverno.mod.redis.operations.RedisListReactiveOperations;
import io.lettuce.core.LPosArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisListReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class ListLposBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisListReactiveOperations.ListLposBuilder<A, B> {

	private final RedisListReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private Long rank;
	private Long maxlen;
	
	/**
	 * 
	 * @param commands 
	 */
	public ListLposBuilderImpl(RedisListReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public ListLposBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public ListLposBuilderImpl<A, B, C> rank(long rank) {
		this.rank = rank;
		return this;
	}

	@Override
	public ListLposBuilderImpl<A, B, C> maxlen(long maxlen) {
		this.maxlen = maxlen;
		return this;
	}

	/**
	 * 
	 * @return 
	 */
	protected LPosArgs buildLPosArgs() {
		LPosArgs lposArgs = new LPosArgs();
		if(this.rank != null) {
			lposArgs.rank(this.rank);
		}
		if(this.maxlen != null) {
			lposArgs.maxlen(this.maxlen);
		}
		return lposArgs;
	}
	
	@Override
	public Mono<Long> build(A key, B element) {
		if(this.commands != null) {
			return this.build(this.commands, key, element);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, element), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param element
	 * @return 
	 */
	private Mono<Long> build(RedisListReactiveCommands<A, B> localCommands, A key, B element) {
		return localCommands.lpos(key, element, this.buildLPosArgs());
	}

	@Override
	public Flux<Long> build(A key, B element, long count) {
		if(this.commands != null) {
			return this.build(this.commands, key, element, count);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, element, count), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param element
	 * @param count
	 * @return 
	 */
	private Flux<Long> build(RedisListReactiveCommands<A, B> localCommands, A key, B element, long count) {
		return localCommands.lpos(key, element, (int)count ,this.buildLPosArgs());
	}
}
