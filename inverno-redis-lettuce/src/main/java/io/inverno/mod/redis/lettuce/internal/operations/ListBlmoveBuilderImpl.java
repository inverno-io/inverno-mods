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
import io.lettuce.core.LMoveArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisListReactiveCommands;
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
public class ListBlmoveBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisListReactiveOperations.ListBlmoveBuilder<A, B> {

	private final RedisListReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean leftLeft;
	private boolean leftRight;
	private boolean rightLeft;
	private boolean rightRight;
	
	/**
	 * 
	 * @param commands 
	 */
	public ListBlmoveBuilderImpl(RedisListReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public ListBlmoveBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public ListBlmoveBuilderImpl<A, B, C> leftLeft() {
		this.leftLeft = true;
		this.leftRight = false;
		this.rightLeft = false;
		this.rightRight = false;
		return this;
	}

	@Override
	public ListBlmoveBuilderImpl<A, B, C> leftRight() {
		this.leftLeft = false;
		this.leftRight = true;
		this.rightLeft = false;
		this.rightRight = false;
		return this;
	}

	@Override
	public ListBlmoveBuilderImpl<A, B, C> rightLeft() {
		this.leftLeft = false;
		this.leftRight = false;
		this.rightLeft = true;
		this.rightRight = false;
		return this;
	}

	@Override
	public ListBlmoveBuilderImpl<A, B, C> rightRight() {
		this.leftLeft = false;
		this.leftRight = false;
		this.rightLeft = false;
		this.rightRight = true;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected LMoveArgs buildLMoveArgs() {
		if(this.leftLeft) {
			return LMoveArgs.Builder.leftLeft();
		}
		if(this.leftRight) {
			return LMoveArgs.Builder.leftRight();
		}
		if(this.rightLeft) {
			return LMoveArgs.Builder.rightLeft();
		}
		if(this.rightRight) {
			return LMoveArgs.Builder.rightRight();
		}
		throw new IllegalStateException("Missing LEFT|RIGHT LEFT|RIGHT");
	}
	
	@Override
	public Mono<B> build(A source, A destination, double timeout) {
		if(this.commands != null) {
			return this.build(this.commands, source, destination, timeout);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), source, destination, timeout), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param source
	 * @param destination
	 * @param timeout
	 * @return 
	 */
	private Mono<B> build(RedisListReactiveCommands<A, B> localCommands, A source, A destination, double timeout) {
		return localCommands.blmove(source, destination, this.buildLMoveArgs(), timeout);
	}
}
