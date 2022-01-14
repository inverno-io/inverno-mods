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
import io.lettuce.core.SetArgs;
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
public class StringSetBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStringReactiveOperations.StringSetBuilder<A, B> {

	private final RedisStringReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private Long ex;
	private Long px;
	private Long exat;
	private Long pxat;
	private boolean keepttl;
	private boolean nx;
	private boolean xx;
	
	/**
	 * 
	 * @param commands 
	 */
	public StringSetBuilderImpl(RedisStringReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StringSetBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}
	
	@Override
	public StringSetBuilderImpl<A, B, C> ex(long seconds) {
		this.ex = seconds;
		this.px = null;
		this.exat = null;
		this.pxat = null;
		this.keepttl = false;
		return this;
	}

	@Override
	public StringSetBuilderImpl<A, B, C> px(long milliseconds) {
		this.ex = null;
		this.px = milliseconds;
		this.exat = null;
		this.pxat = null;
		this.keepttl = false;
		return this;
	}

	@Override
	public StringSetBuilderImpl<A, B, C> exat(long unixTime) {
		this.ex = null;
		this.px = null;
		this.exat = unixTime;
		this.pxat = null;
		this.keepttl = false;
		return this;
	}

	@Override
	public StringSetBuilderImpl<A, B, C> pxat(long unixTime) {
		this.ex = null;
		this.px = null;
		this.exat = null;
		this.pxat = unixTime;
		this.keepttl = false;
		return this;
	}

	@Override
	public StringSetBuilderImpl<A, B, C> keepttl() {
		this.ex = null;
		this.px = null;
		this.exat = null;
		this.pxat = null;
		this.keepttl = true;
		return this;
	}

	@Override
	public StringSetBuilderImpl<A, B, C> nx() {
		this.nx = true;
		this.xx = false;
		return this;
	}

	@Override
	public StringSetBuilderImpl<A, B, C> xx() {
		this.nx = false;
		this.xx = true;
		return this;
	}

	/**
	 * 
	 * @return 
	 */
	protected SetArgs buildSetArgs() {
		SetArgs setArgs = new SetArgs();
		
		if(this.ex != null) {
			setArgs.ex(this.ex);
		}
		else if(this.px != null) {
			setArgs.px(this.px);
		}
		else if(this.exat != null) {
			setArgs.exAt(this.exat);
		}
		else if(this.pxat != null) {
			setArgs.pxAt(this.pxat);
		}
		else if(this.keepttl) {
			setArgs.keepttl();
		}
		
		if(this.nx) {
			setArgs.nx();
		}
		else if(this.xx) {
			setArgs.xx();
		}
		
		return setArgs;
	}
	
	@Override
	public Mono<String> build(A key, B value) {
		if(this.commands != null) {
			return this.build(this.commands, key, value);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, value), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param value
	 * @return 
	 */
	private Mono<String> build(RedisStringReactiveCommands<A, B> localCommands, A key, B value) {
		return localCommands.set(key, value, this.buildSetArgs());
	}
}
