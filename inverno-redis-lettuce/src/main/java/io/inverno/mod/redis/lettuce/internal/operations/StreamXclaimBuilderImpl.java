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
import io.lettuce.core.XClaimArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands;
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
public class StreamXclaimBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStreamReactiveOperations.StreamXclaimBuilder<A, B> {

	private final RedisStreamReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private Long idle;
	private Long time;
	private Long retrycount;
	private boolean force;
	private boolean justid;
	
	/**
	 * 
	 * @param commands 
	 */
	public StreamXclaimBuilderImpl(RedisStreamReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StreamXclaimBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public StreamXclaimBuilderImpl<A, B, C> idle(long ms) {
		this.idle = ms;
		return this;
	}

	@Override
	public StreamXclaimBuilderImpl<A, B, C> time(long msUnixTime) {
		this.time = msUnixTime;
		return this;
	}

	@Override
	public StreamXclaimBuilderImpl<A, B, C> retrycount(long count) {
		this.retrycount = count;
			return this;
	}

	@Override
	public StreamXclaimBuilderImpl<A, B, C> force() {
		this.force = true;
		return this;
	}

	@Override
	public StreamXclaimBuilderImpl<A, B, C> justid() {
		this.justid = true;
		return this;
	}

	/**
	 * 
	 * @return 
	 */
	protected XClaimArgs buildXClaimArgs() {
		XClaimArgs xclaimArgs = new XClaimArgs();
		if(this.idle != null) {
			xclaimArgs.idle(this.idle);
		}
		if(this.time != null) {
			xclaimArgs.time(this.time);
		}
		if(this.retrycount != null) {
			xclaimArgs.retryCount(this.retrycount);
		}

		if(this.force) {
			xclaimArgs.force(this.force);
		}
		if(this.justid) {
			xclaimArgs.justid();
		}
		return xclaimArgs;
	}
	
	@Override
	public Flux<RedisStreamReactiveOperations.StreamMessage<A, B>> build(A key, A group, A consumer, long minIdleTime, String messageId) {
		if(this.commands != null) {
			return this.build(this.commands, key, group, consumer, minIdleTime, messageId);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, group, consumer, minIdleTime, messageId), 
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
	 * @param messageId
	 * @return 
	 */
	private Flux<RedisStreamReactiveOperations.StreamMessage<A, B>> build(RedisStreamReactiveCommands<A, B> localCommands, A key, A group, A consumer, long minIdleTime, String messageId) {
		return localCommands.xclaim(key, io.lettuce.core.Consumer.from(group, consumer), this.buildXClaimArgs(), messageId).map(StreamMessageImpl::new);
	}

	@Override
	public Flux<RedisStreamReactiveOperations.StreamMessage<A, B>> build(A key, A group, A consumer, long minIdleTime, Consumer<RedisStreamReactiveOperations.StreamMessageIds> messageIds) {
		if(this.commands != null) {
			return this.build(this.commands, key, group, consumer, minIdleTime, messageIds);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, group, consumer, minIdleTime, messageIds), 
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
	 * @param messageIds
	 * @return 
	 */
	private Flux<RedisStreamReactiveOperations.StreamMessage<A, B>> build(RedisStreamReactiveCommands<A, B> localCommands, A key, A group, A consumer, long minIdleTime, Consumer<RedisStreamReactiveOperations.StreamMessageIds> messageIds) {
		StreamMessageIdsImpl messageIdsConfigurator = new StreamMessageIdsImpl();
		messageIds.accept(messageIdsConfigurator);
		return localCommands.xclaim(key, io.lettuce.core.Consumer.from(group, consumer), this.buildXClaimArgs(), messageIdsConfigurator.getIds()).map(StreamMessageImpl::new);
	}
}
