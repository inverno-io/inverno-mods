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
public class SortedSetZrangeBuilderImpl<A, B, C extends StatefulConnection<A, B>, D> implements RedisSortedSetReactiveOperations.SortedSetZrangeBuilder<A, B, D> {

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
	public SortedSetZrangeBuilderImpl(RedisSortedSetReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public SortedSetZrangeBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}
	
	/**
	 * 
	 * @param parent 
	 */
	private SortedSetZrangeBuilderImpl(SortedSetZrangeBuilderImpl<A, B, C, ?> parent) {
		this.commands = parent.commands;
		this.connection = parent.connection;
		this.byScore = parent.byScore;
		this.byLex = parent.byLex;
		this.reverse = parent.reverse;
		this.offset = parent.offset;
		this.count = parent.count;
	}
	
	@Override
	public SortedSetZrangeBuilderImpl<A, B, C, ? extends Number> byScore() {
		this.byScore = true;
		this.byLex = false;
		return new SortedSetZrangeBuilderImpl<A, B, C, Long>(this);
	}

	@Override
	public SortedSetZrangeBuilderImpl<A, B, C, ? extends B> byLex() {
		this.byScore = false;
		this.byLex = true;
		return new SortedSetZrangeBuilderImpl<>(this);
	}

	@Override
	public SortedSetZrangeBuilderImpl<A, B, C, D> reverse() {
		this.reverse = true;
		return new SortedSetZrangeBuilderImpl<>(this);
	}

	@Override
	public SortedSetZrangeBuilderImpl<A, B, C, D> limit(long offset, long count) {
		this.offset = offset;
		this.count = count;
		return this;
	}

	@Override
	public Flux<B> build(A key, Bound<D> min, Bound<D> max) {
		if(this.commands != null) {
			return this.build(this.commands, key, min, max);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, min, max), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param min
	 * @param max
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	private Flux<B> build(RedisSortedSetReactiveCommands<A, B> localCommands, A key, Bound<D> min, Bound<D> max) {
		if(this.byScore) {
			// score
			if(this.reverse) {
				if(this.offset != null) {
					return localCommands.zrevrangebyscore(key, SortedSetUtils.convertRange((Bound<? extends Number>)min, (Bound<? extends Number>)max), Limit.create(this.offset, this.count));
				}
				else {
					return localCommands.zrevrangebyscore(key, SortedSetUtils.convertRange((Bound<? extends Number>)min, (Bound<? extends Number>)max));
				}
			}
			else {
				if(this.offset != null) {
					return localCommands.zrangebyscore(key, SortedSetUtils.convertRange((Bound<? extends Number>)min, (Bound<? extends Number>)max), Limit.create(this.offset, this.count));
				}
				else {
					return localCommands.zrangebyscore(key, SortedSetUtils.convertRange((Bound<? extends Number>)min, (Bound<? extends Number>)max));
				}
			}
		}
		else if(this.byLex) {
			// lex
			if(this.reverse) {
				if(this.offset != null) {
					return localCommands.zrevrangebylex(key, SortedSetUtils.convertRange((Bound<? extends B>)min, (Bound<? extends B>)max), Limit.create(this.offset, this.count));
				}
				else {
					return localCommands.zrevrangebylex(key, SortedSetUtils.convertRange((Bound<? extends B>)min, (Bound<? extends B>)max));
				}
			}
			else {
				if(this.offset != null) {
					return localCommands.zrangebylex(key, SortedSetUtils.convertRange((Bound<? extends B>)min, (Bound<? extends B>)max), Limit.create(this.offset, this.count));
				}
				else {
					return localCommands.zrangebylex(key, SortedSetUtils.convertRange((Bound<? extends B>)min, (Bound<? extends B>)max));
				}
			}
		}
		else {
			// index
			if(this.offset != null) {
				throw new UnsupportedOperationException("Implementation doesn't support ZRANGE key min max [REV] LIMIT offset count");
			}
			
			if(this.reverse) {
				return localCommands.zrevrange(key, (Long)min.getValue(), (Long)max.getValue());
			}
			else {
				return localCommands.zrange(key, (Long)min.getValue(), (Long)max.getValue());
			}
		}
	}
}
