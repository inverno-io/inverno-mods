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
import io.inverno.mod.redis.operations.Bound;
import io.lettuce.core.Limit;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
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
public class SortedSetZrangestoreBuilderImpl<A, B, C extends StatefulConnection<A, B>, D> implements RedisSortedSetReactiveOperations.SortedSetZrangestoreBuilder<A, B, D> {

	private final RedisSortedSetReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean byScore;
	private boolean byLex;
	private boolean reverse;
	private Long offset;
	private Long count;
	
	/**
	 * 
	 * @param commands 
	 */
	public SortedSetZrangestoreBuilderImpl(RedisSortedSetReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public SortedSetZrangestoreBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}
	
	/**
	 * 
	 * @param parent 
	 */
	private SortedSetZrangestoreBuilderImpl(SortedSetZrangestoreBuilderImpl<A, B, C, ?> parent) {
		this.commands = parent.commands;
		this.connection = parent.connection;
		this.byScore = parent.byScore;
		this.byLex = parent.byLex;
		this.reverse = parent.reverse;
		this.offset = parent.offset;
		this.count = parent.count;
	}
	
	@Override
	public SortedSetZrangestoreBuilderImpl<A, B, C, Number> byScore() {
		this.byScore = true;
		this.byLex = false;
		return new SortedSetZrangestoreBuilderImpl<>(this);
	}

	@Override
	public SortedSetZrangestoreBuilderImpl<A, B, C, B> byLex() {
		this.byScore = false;
		this.byLex = true;
		return new SortedSetZrangestoreBuilderImpl<>(this);
	}

	@Override
	public SortedSetZrangestoreBuilderImpl<A, B, C, D> reverse() {
		this.reverse = true;
		return new SortedSetZrangestoreBuilderImpl<>(this);
	}

	@Override
	public SortedSetZrangestoreBuilderImpl<A, B, C, D> limit(long offset, long count) {
		this.offset = offset;
		this.count = count;
		return this;
	}
	
	@Override
	public <T extends D> Mono<Long> build(A source, A destination, Bound<T> min, Bound<T> max) {
		if(this.commands != null) {
			return this.build(this.commands, source, destination, min, max);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), source, destination, min, max), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param source
	 * @param destination
	 * @param min
	 * @param max
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	private <T extends D> Mono<Long> build(RedisSortedSetReactiveCommands<A, B> localCommands, A source, A destination, Bound<T> min, Bound<T> max) {
		if(this.byScore) {
			// score
			if(this.reverse) {
				if(this.offset != null) {
					return localCommands.zrevrangestorebyscore(destination, source, SortedSetUtils.convertRange((Bound<? extends Number>)min, (Bound<? extends Number>)max), Limit.create(this.offset, this.count));
				}
				else {
					return localCommands.zrevrangestorebyscore(destination, source, SortedSetUtils.convertRange((Bound<? extends Number>)min, (Bound<? extends Number>)max), Limit.unlimited());
				}
			}
			else {
				if(this.offset != null) {
					return localCommands.zrangestorebyscore(destination, source, SortedSetUtils.convertRange((Bound<? extends Number>)min, (Bound<? extends Number>)max), Limit.create(this.offset, this.count));
				}
				else {
					return localCommands.zrangestorebyscore(destination, source, SortedSetUtils.convertRange((Bound<? extends Number>)min, (Bound<? extends Number>)max), Limit.unlimited());
				}
			}
		}
		else if(this.byLex) {
			// lex
			if(this.reverse) {
				if(this.offset != null) {
					return localCommands.zrevrangestorebylex(destination, source, SortedSetUtils.convertRange((Bound<? extends B>)min, (Bound<? extends B>)max), Limit.create(this.offset, this.count));
				}
				else {
					return localCommands.zrevrangestorebylex(destination, source, SortedSetUtils.convertRange((Bound<? extends B>)min, (Bound<? extends B>)max), Limit.unlimited());
				}
			}
			else {
				if(this.offset != null) {
					return localCommands.zrangestorebylex(destination, source, SortedSetUtils.convertRange((Bound<? extends B>)min, (Bound<? extends B>)max), Limit.create(this.offset, this.count));
				}
				else {
					return localCommands.zrangestorebylex(destination, source, SortedSetUtils.convertRange((Bound<? extends B>)min, (Bound<? extends B>)max), Limit.unlimited());
				}
			}
		}
		else {
			// index
			throw new UnsupportedOperationException("Implementation doesn't support ZRANGESTORE dst src min max [REV] [LIMIT offset count] ");
		}
	}
}
