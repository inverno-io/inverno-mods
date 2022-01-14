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

import io.inverno.mod.redis.operations.RedisStringReactiveOperations;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
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
public class StringGetexBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStringReactiveOperations.StringGetexBuilder<A, B> {

	private final RedisStringReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private Long ex;
	private Long px;
	private Long exat;
	private Long pxat;
	private boolean persist;
	
	/**
	 * 
	 * @param commands 
	 */
	public StringGetexBuilderImpl(RedisStringReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StringGetexBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public StringGetexBuilderImpl<A, B, C> ex(long seconds) {
		this.ex = seconds;
		this.px = null;
		this.exat = null;
		this.pxat = null;
		this.persist = false;
		return this;
	}

	@Override
	public StringGetexBuilderImpl<A, B, C> px(long milliseconds) {
		this.ex = null;
		this.px = milliseconds;
		this.exat = null;
		this.pxat = null;
		this.persist = false;
		return this;
	}

	@Override
	public StringGetexBuilderImpl<A, B, C> exat(long unixTime) {
		this.ex = null;
		this.px = null;
		this.exat = unixTime;
		this.pxat = null;
		this.persist = false;
		return this;
	}

	@Override
	public StringGetexBuilderImpl<A, B, C> pxat(long unixTime) {
		this.ex = null;
		this.px = null;
		this.exat = null;
		this.pxat = unixTime;
		this.persist = false;
		return this;
	}

	@Override
	public StringGetexBuilderImpl<A, B, C> persist() {
		this.ex = null;
		this.px = null;
		this.exat = null;
		this.pxat = null;
		this.persist = true;
		return this;
	}

	/**
	 * 
	 * @return 
	 */
	protected GetExArgs buildGetExArgs() {
		GetExArgs getExArgs = new GetExArgs();
		if(this.ex != null) {
			getExArgs.ex(this.ex);
		}
		else if(this.px != null) {
			getExArgs.px(this.px);
		}
		else if(this.exat != null) {
			getExArgs.exAt(this.exat);
		}
		else if(this.pxat != null) {
			getExArgs.pxAt(this.pxat);
		}
		else if(this.persist) {
			getExArgs.persist();
		}
		return getExArgs;
	}
	
	@Override
	public Mono<B> build(A key) {
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
	private Mono<B> build(RedisStringReactiveCommands<A, B> localCommands, A key) {
		return localCommands.getex(key, this.buildGetExArgs());
	}
}
