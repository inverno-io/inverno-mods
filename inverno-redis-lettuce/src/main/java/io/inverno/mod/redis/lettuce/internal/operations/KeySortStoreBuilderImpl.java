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
import io.lettuce.core.SortArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import java.util.Objects;
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
public class KeySortStoreBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisKeyReactiveOperations.KeySortStoreBuilder<A> {

	private final RedisKeyReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private String by;
	private Long offset;
	private Long count;
	private String[] patterns;
	private boolean asc;
	private boolean desc;
	private boolean alpha;
	
	/**
	 * 
	 * @param commands 
	 */
	public KeySortStoreBuilderImpl(RedisKeyReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public KeySortStoreBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}
	
	@Override
	public KeySortStoreBuilderImpl<A, B, C> by(String pattern) {
		this.by = Objects.requireNonNull(pattern, "pattern");
		return this;
	}
	
	@Override
	public KeySortStoreBuilderImpl<A, B, C> limit(long offset, long count) {
		this.offset = offset;
		this.count = count;
		return this;
	}

	@Override
	public KeySortStoreBuilderImpl<A, B, C> get(String... patterns) {
		this.patterns = Objects.requireNonNull(patterns, "patterns");
		return this;
	}

	@Override
	public KeySortStoreBuilderImpl<A, B, C> asc() {
		this.asc = true;
		this.desc = false;
		return this;
	}

	@Override
	public KeySortStoreBuilderImpl<A, B, C> desc() {
		this.asc = false;
		this.desc = true;
		return this;
	}

	@Override
	public KeySortStoreBuilderImpl<A, B, C> alpha() {
		this.alpha = true;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected SortArgs buildSortArgs() {
		SortArgs sortArgs = new SortArgs();
		if(this.by != null) {
			sortArgs.by(this.by);
		}
		if(this.offset != null) {
			sortArgs.limit(this.offset, this.count);
		}
		if(this.patterns != null) {
			if(this.patterns.length > 1) {
				throw new UnsupportedOperationException("Implementation doesn't support multiple GET pattern");
			}
			sortArgs.get(this.patterns[0]);
		}
		if(this.asc) {
			sortArgs.asc();
		}
		if(this.desc) {
			sortArgs.desc();
		}
		if(this.alpha) {
			sortArgs.alpha();
		}
		return sortArgs;
	}
	
	@Override
	public Mono<Long> build(A source, A destination) {
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
	private Mono<Long> build(RedisKeyReactiveCommands<A, B> localCommands, A source, A destination) {
		return localCommands.sortStore(source, this.buildSortArgs(), destination);
	}
}
