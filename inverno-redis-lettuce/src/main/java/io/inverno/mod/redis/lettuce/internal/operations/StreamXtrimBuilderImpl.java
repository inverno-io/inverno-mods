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
import io.lettuce.core.XTrimArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
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
public class StreamXtrimBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStreamReactiveOperations.StreamXtrimBuilder<A> {

	private final RedisStreamReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private Long maxlen;
	private String minid;
	private boolean exact;
	private boolean approximate;
	private Long limit;
	
	/**
	 * 
	 * @param commands 
	 */
	public StreamXtrimBuilderImpl(RedisStreamReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StreamXtrimBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public StreamXtrimBuilderImpl<A, B, C> maxlen(long threshold) {
		this.maxlen = threshold;
		this.minid = null;
		return this;
	}

	@Override
	public StreamXtrimBuilderImpl<A, B, C> minid(String streamId) {
		this.maxlen = null;
		this.minid = streamId;
		return this;
	}

	@Override
	public StreamXtrimBuilderImpl<A, B, C> exact() {
		this.exact = true;
		this.approximate = false;
		return this;
	}

	@Override
	public StreamXtrimBuilderImpl<A, B, C> approximate() {
		this.exact = false;
		this.approximate = true;
		return this;
	}

	@Override
	public StreamXtrimBuilderImpl<A, B, C> limit(long limit) {
		this.limit = limit;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected XTrimArgs buildXTrimArgs() {
		XTrimArgs xtrimArgs = new XTrimArgs();
		
		if(this.maxlen != null) {
			xtrimArgs.maxlen(this.maxlen);
		}
		else if(this.minid != null) {
			xtrimArgs.minId(this.minid);
		}
		else {
			throw new IllegalStateException("Missing MAXLEN|MINID");
		}
		
		if(this.exact) {
			xtrimArgs.exactTrimming();
		}
		else if(this.approximate) {
			xtrimArgs.approximateTrimming();
		}
		
		if(this.limit != null) {
			xtrimArgs.limit(this.limit);
		}
		
		return xtrimArgs;
	}

	@Override
	public Mono<Long> build(A key) {
		if(this.commands != null) {
			return this.build(this.commands, key);
		}
		else {
			return Mono.usingWhen(
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
	private Mono<Long> build(RedisStreamReactiveCommands<A, B> localCommands, A key) {
		return localCommands.xtrim(key, this.buildXTrimArgs());
	}
}
