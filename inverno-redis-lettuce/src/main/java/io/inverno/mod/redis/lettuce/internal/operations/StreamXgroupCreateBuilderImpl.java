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
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
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
public class StreamXgroupCreateBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStreamReactiveOperations.StreamXgroupCreateBuilder<A> {

	private final RedisStreamReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean mkstream;
	
	/**
	 * 
	 * @param commands 
	 */
	public StreamXgroupCreateBuilderImpl(RedisStreamReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StreamXgroupCreateBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public StreamXgroupCreateBuilderImpl<A, B, C> mkstream() {
		this.mkstream = true;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected XGroupCreateArgs buildXGroupCreateArgs() {
		XGroupCreateArgs xgroupCreateArgs = new XGroupCreateArgs();
		xgroupCreateArgs.mkstream(this.mkstream);
		return xgroupCreateArgs;
	}

	@Override
	public Mono<String> build(A key, A groupname, String id) {
		if(this.commands != null) {
			return this.build(this.commands, key, groupname, id);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, groupname, id), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param groupname
	 * @param id
	 * @return 
	 */
	private Mono<String> build(RedisStreamReactiveCommands<A, B> localCommands, A key, A groupname, String id) {
		return localCommands.xgroupCreate(XReadArgs.StreamOffset.from(key, id), groupname, this.buildXGroupCreateArgs());
	}
}