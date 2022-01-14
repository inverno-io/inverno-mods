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

import io.inverno.mod.redis.operations.RedisSortedSetReactiveOperations;
import io.inverno.mod.redis.operations.EntryOptional;
import io.inverno.mod.redis.operations.Keys;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
import java.util.function.Consumer;
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
public class SortedSetZmpopBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisSortedSetReactiveOperations.SortedSetZmpopBuilder<A, B> {

	private final RedisSortedSetReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	private final Class<A> keyType;

	private boolean min;
	private boolean max;
	private Long count;
	
	/**
	 * 
	 * @param commands
	 * @param keyType 
	 */
	public SortedSetZmpopBuilderImpl(RedisSortedSetReactiveCommands<A, B> commands, Class<A> keyType) {
		this.commands = commands;
		this.connection = null;
		this.keyType = keyType;
	}
	
	/**
	 * 
	 * @param connection
	 * @param keyType 
	 */
	public SortedSetZmpopBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection, Class<A> keyType) {
		this.commands = null;
		this.connection = connection;
		this.keyType = keyType;
	}
	
	@Override
	public SortedSetZmpopBuilderImpl<A, B, C> min() {
		this.min = true;
		this.max = false;
		return this;
	}

	@Override
	public SortedSetZmpopBuilderImpl<A, B, C> max() {
		this.min = false;
		this.max = true;
		return this;
	}

	@Override
	public SortedSetZmpopBuilderImpl<A, B, C> count(long count) {
		this.count = count;
		return this;
	}
	
	/**
	 * 
	 */
	protected void buildZmpopArgs() {
		throw new UnsupportedOperationException("Implementation doesn't support ZMPOP numkeys key [key ...] MIN|MAX [COUNT count]");
	}
	
	@Override
	public Flux<EntryOptional<A, RedisSortedSetReactiveOperations.SortedSetScoredMember<B>>> build(A key) {
		if(this.commands != null) {
			return this.build(this.commands, key);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @return 
	 */
	private Flux<EntryOptional<A, RedisSortedSetReactiveOperations.SortedSetScoredMember<B>>> build(RedisSortedSetReactiveCommands<A, B> localCommands, A key) {
		this.buildZmpopArgs();
		return null;
	}

	@Override
	public Flux<EntryOptional<A, RedisSortedSetReactiveOperations.SortedSetScoredMember<B>>> build(Consumer<Keys<A>> keys) {
		if(this.commands != null) {
			return this.build(this.commands, keys);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), keys), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param keys
	 * @return 
	 */
	private Flux<EntryOptional<A, RedisSortedSetReactiveOperations.SortedSetScoredMember<B>>> build(RedisSortedSetReactiveCommands<A, B> localCommands, Consumer<Keys<A>> keys) {
		this.buildZmpopArgs();
		return null;
	}
}
