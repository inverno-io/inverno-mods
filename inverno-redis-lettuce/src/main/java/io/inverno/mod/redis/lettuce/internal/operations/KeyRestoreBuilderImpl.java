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
import io.lettuce.core.RestoreArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
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
public class KeyRestoreBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisKeyReactiveOperations.KeyRestoreBuilder<A> {

	private final RedisKeyReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean replace;
	private boolean absttl;
	private Long idletime;
	private Long freq;
	
	/**
	 * 
	 * @param commands 
	 */
	public KeyRestoreBuilderImpl(RedisKeyReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public KeyRestoreBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public KeyRestoreBuilderImpl<A, B, C> replace() {
		this.replace = true;
		return this;
	}

	@Override
	public KeyRestoreBuilderImpl<A, B, C> absttl() {
		this.absttl = true;
		return this;
	}

	@Override
	public KeyRestoreBuilderImpl<A, B, C> idletime(long seconds) {
		this.idletime = seconds;
		return this;
	}

	@Override
	public KeyRestoreBuilderImpl<A, B, C> freq(long frequency) {
		this.freq = frequency;
		return this;
	}

	/**
	 * 
	 * @param ttl
	 * @return 
	 */
	protected RestoreArgs buildRestoreArgs(long ttl) {
		RestoreArgs restoreArgs = RestoreArgs.Builder.ttl(ttl);
		restoreArgs.replace(this.replace);
		restoreArgs.absttl(this.absttl);
		if(this.idletime != null) {
			restoreArgs.idleTime(this.idletime);
		}
		if(this.freq != null) {
			restoreArgs.frequency(this.freq);
		}
		return restoreArgs;
	}
	
	@Override
	public Mono<String> build(A key, long ttl, byte[] serializedValue) {
		if(this.commands != null) {
			return this.build(this.commands, key, ttl, serializedValue);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, ttl, serializedValue), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param ttl
	 * @param serializedValue
	 * @return 
	 */
	private Mono<String> build(RedisKeyReactiveCommands<A, B> localCommands, A key, long ttl, byte[] serializedValue) {
		return localCommands.restore(key, serializedValue, this.buildRestoreArgs(ttl));
	}
}
