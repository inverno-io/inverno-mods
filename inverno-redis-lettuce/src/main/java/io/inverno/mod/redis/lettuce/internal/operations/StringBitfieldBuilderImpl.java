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
import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import java.util.Objects;
import java.util.Optional;
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
public class StringBitfieldBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisStringReactiveOperations.StringBitfieldBuilder<A, B> {

	private final RedisStringReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private String getEncoding;
	private Integer getOffset;
	
	private String setEncoding;
	private Integer setOffset;
	private Long setValue;
	
	private String incrbyEncoding;
	private Integer incrbyOffset;
	private Long incrbyIncrement;
	
	private boolean wrap;
	private boolean sat;
	private boolean fail;
	
	/**
	 * 
	 * @param commands 
	 */
	public StringBitfieldBuilderImpl(RedisStringReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public StringBitfieldBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}
	
	@Override
	public StringBitfieldBuilderImpl<A, B, C> get(String encoding, int offset) {
		Objects.requireNonNull(encoding);
		this.getEncoding = encoding;
		this.getOffset = offset;
		return this;
	}

	@Override
	public StringBitfieldBuilderImpl<A, B, C> set(String encoding, int offset, long value) {
		Objects.requireNonNull(encoding);
		this.setEncoding = encoding;
		this.setOffset = offset;
		this.setValue = value;
		return this;
	}

	@Override
	public StringBitfieldBuilderImpl<A, B, C> incrby(String encoding, int offset, long increment) {
		Objects.requireNonNull(encoding);
		this.incrbyEncoding = encoding;
		this.incrbyOffset = offset;
		this.incrbyIncrement = increment;
		return this;
	}

	@Override
	public StringBitfieldBuilderImpl<A, B, C> wrap() {
		this.wrap = true;
		this.sat = false;
		this.fail = false;
		return this;
	}

	@Override
	public StringBitfieldBuilderImpl<A, B, C> sat() {
		this.wrap = false;
		this.sat = true;
		this.fail = false;
		return this;
	}

	@Override
	public StringBitfieldBuilderImpl<A, B, C> fail() {
		this.wrap = false;
		this.sat = false;
		this.fail = true;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected BitFieldArgs buildBitFieldArgs() {
		BitFieldArgs bitFieldArgs = new BitFieldArgs();
		if(this.getEncoding != null) {
			bitFieldArgs.get(this.convertBitFieldType(this.getEncoding), this.getOffset);
		}
		if(this.setEncoding != null) {
			bitFieldArgs.set(this.convertBitFieldType(this.setEncoding), this.setOffset, this.setValue);
		}
		if(this.incrbyEncoding != null) {
			bitFieldArgs.incrBy(this.convertBitFieldType(this.incrbyEncoding), this.incrbyOffset, this.incrbyIncrement);
		}
		
		if(this.wrap) {
			bitFieldArgs.overflow(BitFieldArgs.OverflowType.WRAP);
		}
		else if(this.sat) {
			bitFieldArgs.overflow(BitFieldArgs.OverflowType.SAT);
		}
		else if(this.fail) {
			bitFieldArgs.overflow(BitFieldArgs.OverflowType.FAIL);
		}
		return bitFieldArgs;
	}

	/**
	 * 
	 * @param encoding
	 * @return 
	 */
	private BitFieldArgs.BitFieldType convertBitFieldType(String encoding) {
		char sign;
		int bits;
		try {
			sign = encoding.charAt(0);
			bits = Integer.parseInt(encoding.substring(1));
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid encoding: " + encoding, e);
		}

		switch (sign) {
			case 'i':
				return BitFieldArgs.signed(bits);
			case 'u':
				return BitFieldArgs.unsigned(bits);
			default:
				throw new IllegalArgumentException("Invalid encoding: " + encoding);
		}
	}
	
	@Override
	public Flux<Optional<Long>> build(A key) {
		if(this.commands != null) {
			return this.build(this.commands, key);
		}
		else {
			return Flux.usingWhen(
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
	private Flux<Optional<Long>> build(RedisStringReactiveCommands<A, B> localCommands, A key) {
		return localCommands.bitfield(key, this.buildBitFieldArgs()).map(v -> Optional.ofNullable(v.getValueOrElse(null)));
	}
}
