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
import io.lettuce.core.XAutoClaimArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
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
public class StreamXautoclaimBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStreamReactiveOperations.StreamXautoclaimBuilder<A, B> {
	
	private final RedisStreamReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean justid;
	private Long count;
	
	/**
	 * 
	 * @param commands 
	 */
	public StreamXautoclaimBuilderImpl(RedisStreamReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StreamXautoclaimBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public StreamXautoclaimBuilderImpl<A, B, C> justid() {
		this.justid = true;
		return this;
	}

	@Override
	public StreamXautoclaimBuilderImpl<A, B, C> count(long count) {
		this.count = count;
		return this;
	}
	
	/**
	 * 
	 * @param group
	 * @param consumer
	 * @param minIdleTime
	 * @param start
	 * @return 
	 */
	protected XAutoClaimArgs<A> buildXAutoClaimArgs(A group, A consumer, long minIdleTime, String start) {
		XAutoClaimArgs<A> xautoClaimArgs = new XAutoClaimArgs<>();
		xautoClaimArgs.consumer(io.lettuce.core.Consumer.from(group, consumer));
		xautoClaimArgs.minIdleTime(minIdleTime);
		xautoClaimArgs.startId(start);
		
		if(this.justid) {
			xautoClaimArgs.justid();
		}
		if(this.count != null) {
			xautoClaimArgs.count(this.count);
		}
		return xautoClaimArgs;
	}

	@Override
	public Mono<RedisStreamReactiveOperations.StreamClaimedMessages<A, B>> build(A key, A group, A consumer, long minIdleTime, String start) {
		if(this.commands != null) {
			return this.build(this.commands, key, group, consumer, minIdleTime, start);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, group, consumer, minIdleTime, start), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param group
	 * @param consumer
	 * @param minIdleTime
	 * @param start
	 * @return 
	 */
	private Mono<RedisStreamReactiveOperations.StreamClaimedMessages<A, B>> build(RedisStreamReactiveCommands<A, B> localCommands, A key, A group, A consumer, long minIdleTime, String start) {
		Objects.requireNonNull(group);
		Objects.requireNonNull(consumer);
		Objects.requireNonNull(start);
		return localCommands.xautoclaim(key, this.buildXAutoClaimArgs(group, consumer, minIdleTime, start)).map(StreamClaimedMessagesImpl::new);
	}
}
