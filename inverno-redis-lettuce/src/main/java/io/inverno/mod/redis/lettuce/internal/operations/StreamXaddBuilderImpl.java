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

import io.inverno.mod.redis.operations.RedisStreamReactiveOperations;
import io.lettuce.core.XAddArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
import java.util.function.Consumer;
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
public class StreamXaddBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStreamReactiveOperations.StreamXaddBuilder<A, B> {

	private final RedisStreamReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean nomkstream;
	private Long threshold;
	private String minid;
	private boolean exact;
	private boolean approximate;
	private Long limit;
	private String id;
	
	/**
	 * 
	 * @param commands 
	 */
	public StreamXaddBuilderImpl(RedisStreamReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StreamXaddBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public StreamXaddBuilderImpl<A, B, C> nomkstream() {
		this.nomkstream = true;
		return this;
	}

	@Override
	public StreamXaddBuilderImpl<A, B, C> maxlen(long threshold) {
		this.threshold = threshold;
		this.minid = null;
		return this;
	}

	@Override
	public StreamXaddBuilderImpl<A, B, C> minid(String minid) {
		this.threshold = null;
		this.minid = minid;
		return this;
	}

	@Override
	public StreamXaddBuilderImpl<A, B, C> exact() {
		this.exact = true;
		this.approximate = false;
		return this;
	}

	@Override
	public StreamXaddBuilderImpl<A, B, C> approximate() {
		this.exact = false;
		this.approximate = true;
		return this;
	}

	@Override
	public StreamXaddBuilderImpl<A, B, C> limit(long limit) {
		this.limit = limit;
		return this;
	}
	
	@Override
	public StreamXaddBuilderImpl<A, B, C> id(String id) {
		this.id = id;
		return this;
	}

	/**
	 * 
	 * @return 
	 */
	protected XAddArgs buildXAddArgs() {
		XAddArgs xaddArgs = new XAddArgs();
		
		if(this.nomkstream) {
			xaddArgs.nomkstream();
		}
		
		if(this.threshold != null) {
			// maxlen
			xaddArgs.maxlen(this.threshold);
		}
		else if(this.minid != null) {
			// minid
			xaddArgs.minId(this.minid);
		}
		
		if(this.exact) {
			xaddArgs.exactTrimming();
		}
		else if(this.approximate) {
			xaddArgs.approximateTrimming();
		}
		
		if(this.limit != null) {
			xaddArgs.limit(this.limit);
		}
		
		if(this.id != null) {
			xaddArgs.id(this.id);
		}
		
		return xaddArgs;
	}
	
	@Override
	public Mono<String> build(A key, A field, B value) {
		if(this.commands != null) {
			return this.build(this.commands, key, field, value);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, field, value), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param field
	 * @param value
	 * @return 
	 */
	private Mono<String> build(RedisStreamReactiveCommands<A, B> localCommands, A key, A field, B value) {
		return localCommands.xadd(key, this.buildXAddArgs(), field, value);
	}

	@Override
	public Mono<String> build(A key, Consumer<RedisStreamReactiveOperations.StreamEntries<A, B>> entries) {
		if(this.commands != null) {
			return this.build(this.commands, key, entries);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, entries), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param entries
	 * @return 
	 */
	private Mono<String> build(RedisStreamReactiveCommands<A, B> localCommands, A key, Consumer<RedisStreamReactiveOperations.StreamEntries<A, B>> entries) {
		StreamEntriesImpl<A, B> entriesConfigurator = new StreamEntriesImpl<>();
		entries.accept(entriesConfigurator);
		return localCommands.xadd(key, this.buildXAddArgs(), entriesConfigurator.getEntries());
	}
}

