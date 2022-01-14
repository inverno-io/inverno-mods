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

import io.inverno.mod.redis.operations.RedisKeyReactiveOperations;
import io.lettuce.core.CopyArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
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
public class KeyCopyBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisKeyReactiveOperations.KeyCopyBuilder<A> {

	private final RedisKeyReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean replace;
	private Long destinationDb;
	
	/**
	 * 
	 * @param commands 
	 */
	public KeyCopyBuilderImpl(RedisKeyReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public KeyCopyBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public KeyCopyBuilderImpl<A, B, C> replace() {
		this.replace = true;
		return this;
	}

	@Override
	public KeyCopyBuilderImpl<A, B, C> db(long destinationDb) {
		this.destinationDb = destinationDb;
		return this;
	}

	/**
	 * 
	 * @return 
	 */
	protected CopyArgs buildCopyArgs() {
		CopyArgs copyArgs = CopyArgs.Builder.replace(this.replace);
		if(this.destinationDb != null) {
			copyArgs.destinationDb(this.destinationDb);
		}
		return copyArgs;
	}
	
	@Override
	public Mono<Boolean> build(A source, A destination) {
		if(this.commands != null) {
			return this.build(this.commands, source, destination);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), source, destination), 
				c -> c.close()
			);
		}
	}

	/**
	 * 
	 * @param localCommands
	 * @param source
	 * @param destination
	 * @return 
	 */
	private Mono<Boolean> build(RedisKeyReactiveCommands<A, B> localCommands, A source, A destination) {
		return localCommands.copy(source, destination, this.buildCopyArgs());
	}
}
