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
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import java.time.Instant;
import java.time.ZonedDateTime;
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
public class KeyPexpireatBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisKeyReactiveOperations.KeyPexpireatBuilder<A> {

	private final RedisKeyReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private boolean nx;
	private boolean xx;
	private boolean gt;
	private boolean lt;
	
	/**
	 * 
	 * @param commands 
	 */
	public KeyPexpireatBuilderImpl(RedisKeyReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public KeyPexpireatBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public KeyPexpireatBuilderImpl<A, B, C> nx() {
		this.nx = true;
		this.xx = false;
		this.gt = false;
		this.lt = false;
		return this;
	}

	@Override
	public KeyPexpireatBuilderImpl<A, B, C> xx() {
		this.nx = false;
		this.xx = true;
		this.gt = false;
		this.lt = false;
		return this;
	}

	@Override
	public KeyPexpireatBuilderImpl<A, B, C> gt() {
		this.nx = false;
		this.xx = false;
		this.gt = true;
		this.lt = false;
		return this;
	}

	@Override
	public KeyPexpireatBuilderImpl<A, B, C> lt() {
		this.nx = false;
		this.xx = false;
		this.gt = false;
		this.lt = true;
		return this;
	}
	
	/**
	 * 
	 */
	protected void buildExpireatArgs() {
		if(this.nx || this.xx || this.gt || this.lt) {
			throw new UnsupportedOperationException("Implementation doesn't support EXPIRE key seconds NX|XX|GT|LT");
		}
	}
	
	@Override
	public Mono<Boolean> build(A key, long epochSeconds) {
		if(this.commands != null) {
			return this.build(this.commands, key, epochSeconds);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, epochSeconds), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param epochSeconds
	 * @return 
	 */
	private Mono<Boolean> build(RedisKeyReactiveCommands<A, B> localCommands, A key, long epochSeconds) {
		// this will throw an UnsupportedOperationException when one of nx, xx, gt and lt is specified as it is not supported yet
		this.buildExpireatArgs();
		return localCommands.pexpireat(key, epochSeconds);
	}

	@Override
	public Mono<Boolean> build(A key, ZonedDateTime datetime) {
		if(this.commands != null) {
			return this.build(this.commands, key, datetime);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, datetime), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param datetime
	 * @return 
	 */
	private Mono<Boolean> build(RedisKeyReactiveCommands<A, B> localCommands, A key, ZonedDateTime datetime) {
		// this will throw an UnsupportedOperationException when one of nx, xx, gt and lt is specified as it is not supported yet
		this.buildExpireatArgs();
		return localCommands.pexpireat(key, datetime.toInstant());
	}

	@Override
	public Mono<Boolean> build(A key, Instant instant) {
		if(this.commands != null) {
			return this.build(this.commands, key, instant);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, instant), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param instant
	 * @return 
	 */
	private Mono<Boolean> build(RedisKeyReactiveCommands<A, B> localCommands, A key, Instant instant) {
		// this will throw an UnsupportedOperationException when one of nx, xx, gt and lt is specified as it is not supported yet
		this.buildExpireatArgs();
		return localCommands.pexpireat(key, instant);
	}
}
