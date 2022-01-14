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

import io.inverno.mod.base.Charsets;
import io.inverno.mod.redis.operations.RedisHashReactiveOperations;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisHashReactiveCommands;
import java.nio.charset.Charset;
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
public class HashScanBuilderImpl<A, B, C extends StatefulConnection<A, B>> implements RedisHashReactiveOperations.HashScanBuilder<A, B> {

	private final RedisHashReactiveCommands<A, B> commands;
	private final Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection;
	
	private Long count;
	private String pattern;
	private Charset patternCharset;
	
	/**
	 * 
	 * @param commands 
	 */
	public HashScanBuilderImpl(RedisHashReactiveCommands<A, B> commands) {
		this.commands = commands;
		this.connection = null;
	}
	
	/**
	 * 
	 * @param connection 
	 */
	public HashScanBuilderImpl(Mono<StatefulRedisConnectionOperations<A, B, C, ?>> connection) {
		this.commands = null;
		this.connection = connection;
	}

	@Override
	public HashScanBuilderImpl<A, B, C> count(long count) {
		this.count = count;
		return this;
	}

	@Override
	public HashScanBuilderImpl<A, B, C> pattern(String pattern) {
		this.pattern = pattern;
		this.patternCharset = Charsets.DEFAULT;
		return this;
	}

	@Override
	public HashScanBuilderImpl<A, B, C> pattern(String pattern, Charset charset) {
		this.pattern = pattern;
		this.patternCharset = charset;
		return this;
	}
	
	/**
	 * 
	 * @return 
	 */
	protected ScanArgs buildScanArgs() {
		ScanArgs scanArgs = new ScanArgs();
		if(this.count != null) {
			scanArgs.limit(this.count);
		}
		
		if(this.pattern != null) {
			scanArgs.match(this.pattern, this.patternCharset);
		}
		return scanArgs;
	}
	
	@Override
	public Mono<RedisHashReactiveOperations.HashScanResult<A, B>> build(A key, String cursor) {
		if(this.commands != null) {
			return this.build(this.commands, key, cursor);
		}
		else {
			return Mono.usingWhen(
				this.connection, 
				c -> this.build(c.getCommands(), key, cursor), 
				c -> c.close()
			);
		}
	}
	
	/**
	 * 
	 * @param localCommands
	 * @param key
	 * @param cursor
	 * @return 
	 */
	private Mono<RedisHashReactiveOperations.HashScanResult<A, B>> build(RedisHashReactiveCommands<A, B> localCommands, A key, String cursor) {
		return localCommands.hscan(key, ScanCursor.of(cursor), this.buildScanArgs()).map(HashScanResultImpl::new);
	}
}
