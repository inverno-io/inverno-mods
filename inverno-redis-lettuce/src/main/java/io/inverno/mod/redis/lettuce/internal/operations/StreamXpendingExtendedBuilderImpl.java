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
import io.lettuce.core.Consumer;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.XPendingArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
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
public class StreamXpendingExtendedBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStreamReactiveOperations.StreamXpendingExtendedBuilder<A> {

	private final RedisStreamReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;

	private Long minIdleTime;
	private A consumer;
	
	/**
	 * 
	 * @param commands 
	 */
	public StreamXpendingExtendedBuilderImpl(RedisStreamReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StreamXpendingExtendedBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public StreamXpendingExtendedBuilderImpl<A, B, C> idle(long minIdleTime) {
		this.minIdleTime =  minIdleTime;
		return this;
	}

	@Override
	public StreamXpendingExtendedBuilderImpl<A, B, C> consumer(A consumer) {
		this.consumer = consumer;
		return this;
	}
	
	/**
	 * 
	 * @param group
	 * @param start
	 * @param end
	 * @param count
	 * @return 
	 */
	protected XPendingArgs<A> buildXPendingArgs(A group, String start, String end, long count) {
		XPendingArgs<A> xpendingArgs = new XPendingArgs<>();
		
		xpendingArgs.limit(Limit.from(count));
		xpendingArgs.range(Range.create(start, end));
		
		if(this.minIdleTime != null) {
			xpendingArgs.idle(count);
		}
		if(this.consumer != null) {
			xpendingArgs.consumer(Consumer.from(group, this.consumer));
		}
		return xpendingArgs;
	}

	@Override
	public Flux<RedisStreamReactiveOperations.StreamPendingMessage> build(A key, A group, String start, String end, long count) {
		if(this.commands != null) {
			return this.build(this.commands, key, group, start, end, count);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, group, start, end, count), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param group
	 * @param start
	 * @param end
	 * @param count
	 * @return 
	 */
	private Flux<RedisStreamReactiveOperations.StreamPendingMessage> build(RedisStreamReactiveCommands<A, B> localCommands, A key, A group, String start, String end, long count) {
		return localCommands.xpending(key, this.buildXPendingArgs(group, start, end, count)).map(StreamPendingMessageImpl::new);
	}
}
