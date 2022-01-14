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
import io.inverno.mod.redis.operations.EntryOptional;
import io.inverno.mod.redis.operations.Keys;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisListReactiveCommands;
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
public class ListLmpopBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisListReactiveOperations.ListLmpopBuilder<A, B> {

	private final RedisListReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean left;
	private boolean right;
	private Long count;
	
	/**
	 * 
	 * @param commands 
	 */
	public ListLmpopBuilderImpl(RedisListReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public ListLmpopBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public ListLmpopBuilderImpl<A, B, C> left() {
		this.left = true;
		this.right = false;
		return this;
	}

	@Override
	public ListLmpopBuilderImpl<A, B, C> right() {
		this.left = false;
		this.right = true;
		return this;
	}

	@Override
	public ListLmpopBuilderImpl<A, B, C> count(long count) {
		this.count = count;
		return this;
	}
	
	protected void buildLmpopArgs() {
		throw new UnsupportedOperationException("Implementation doesn't support LMPOP numkeys key [key ...] LEFT|RIGHT [COUNT count]");
	}
	
	@Override
	public Flux<EntryOptional<A, B>> build(A key) {
		if(this.commands != null) {
			return this.build(this.commands, key);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(),key), 
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
	private Flux<EntryOptional<A, B>> build(RedisListReactiveCommands<A, B> localCommands, A key) {
		this.buildLmpopArgs();
		return null;
	}

	@Override
	public Flux<EntryOptional<A, B>> build(Consumer<Keys<A>> keys) {
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
	private Flux<EntryOptional<A, B>> build(RedisListReactiveCommands<A, B> localCommands, Consumer<Keys<A>> keys) {
		this.buildLmpopArgs();
		return null;
	}
}
