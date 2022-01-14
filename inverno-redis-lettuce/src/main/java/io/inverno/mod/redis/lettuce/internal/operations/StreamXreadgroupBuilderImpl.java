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
import io.lettuce.core.XReadArgs;
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
public class StreamXreadgroupBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStreamReactiveOperations.StreamXreadgroupBuilder<A, B> {

	private final RedisStreamReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private Long count;
	private Long block;
	private boolean noack;
	
	/**
	 * 
	 * @param commands 
	 */
	public StreamXreadgroupBuilderImpl(RedisStreamReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StreamXreadgroupBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public StreamXreadgroupBuilderImpl<A, B, C> count(long count) {
		this.count = count;
		return this;
	}

	@Override
	public StreamXreadgroupBuilderImpl<A, B, C> block(long milliseconds) {
		this.block = milliseconds;
		return this;
	}
	
	@Override
	public StreamXreadgroupBuilderImpl<A, B, C> noack() {
		this.noack = true;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected XReadArgs buildXReadArgs() {
		XReadArgs xreadArgs = new XReadArgs();
		if(this.count != null) {
			xreadArgs.count(this.count);
		}
		if(this.block != null) {
			xreadArgs.block(this.block);
		}
		xreadArgs.noack(this.noack);
		return xreadArgs;
	}

	@Override
	public Flux<RedisStreamReactiveOperations.StreamMessage<A, B>> build(A group, A consumer, A key, String messageId) {
		if(this.commands != null) {
			return this.build(this.commands, group, consumer, key, messageId);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), group, consumer, key, messageId), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param group
	 * @param consumer
	 * @param key
	 * @param messageId
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	private Flux<RedisStreamReactiveOperations.StreamMessage<A, B>> build(RedisStreamReactiveCommands<A, B> localCommands, A group, A consumer, A key, String messageId) {
		return localCommands.xreadgroup(io.lettuce.core.Consumer.from(group, consumer), this.buildXReadArgs(), XReadArgs.StreamOffset.from(key, messageId)).map(StreamMessageImpl::new);
	}

	@Override
	public Flux<RedisStreamReactiveOperations.StreamMessage<A, B>> build(A group, A consumer, Consumer<RedisStreamReactiveOperations.StreamStreams<A>> streams) {
		if(this.commands != null) {
			return this.build(this.commands, group, consumer, streams);
		}
		else {
			return Flux.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), group, consumer, streams), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param group
	 * @param consumer
	 * @param streams
	 * @return 
	 */
	private Flux<RedisStreamReactiveOperations.StreamMessage<A, B>> build(RedisStreamReactiveCommands<A, B> localCommands, A group, A consumer, Consumer<RedisStreamReactiveOperations.StreamStreams<A>> streams) {
		StreamStreamsImpl<A> streamsConfigurator = new StreamStreamsImpl<>();
		streams.accept(streamsConfigurator);
		return localCommands.xreadgroup(io.lettuce.core.Consumer.from(group, consumer), this.buildXReadArgs(), streamsConfigurator.getStreams()).map(StreamMessageImpl::new);
	}
}
