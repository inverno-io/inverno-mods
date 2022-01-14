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
package io.inverno.mod.redis.operations;

import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Hashes reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisHashReactiveOperations<A, B> {

	/**
	 * <a href="https://redis.io/commands/hdel">HDEL</a> key field
	 * 
	 * @param key
	 * @param field
	 * @return 
	 */
	Mono<Long> hdel(A key, A field);
	
	/**
	 * <a href="https://redis.io/commands/hdel">HDEL</a> key field [field ...] 
	 * 
	 * @param key
	 * @param fields
	 * @return 
	 */
	Mono<Long> hdel(A key, Consumer<Keys<A>> fields);
	
	/**
	 * <a href="https://redis.io/commands/hexists">HEXISTS</a> key field 
	 * 
	 * @param key
	 * @param field
	 * @return 
	 */
	Mono<Boolean> hexists(A key, A field);
	
	/**
	 * <a href="https://redis.io/commands/hget">HGET</a> key field 
	 * 
	 * @param key
	 * @param field
	 * @return 
	 */
	Mono<B> hget(A key, A field);
	
	/**
	 * <a href="https://redis.io/commands/hgetall">HGETALL</a> key field 
	 * 
	 * @param key
	 * @return 
	 */
	Flux<EntryOptional<A, B>> hgetall(A key);
	
	/**
	 * <a href="https://redis.io/commands/hincrby">HINCRBY</a> key field increment 
	 * 
	 * @param key
	 * @param field
	 * @param increment
	 * @return 
	 */
	Mono<Long> hincrby(A key, A field, long increment);
	
	/**
	 * <a href="https://redis.io/commands/hincrbyfloat">HINCRBYFLOAT</a> key field increment 
	 * 
	 * @param key
	 * @param field
	 * @param increment
	 * @return 
	 */
	Mono<Double> hincrbyfloat(A key, A field, double increment);
	
	/**
	 * <a href="https://redis.io/commands/hkeys">HKEYS</a> key 
	 * 
	 * @param key
	 * @return 
	 */
	Flux<A> hkeys(A key);
	
	/**
	 * <a href="https://redis.io/commands/hkeys">HLEN</a> key 
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> hlen(A key);
	
	/**
	 * <a href="https://redis.io/commands/hmget">HMGET</a> key field [field ...] 
	 * 
	 * @param key
	 * @param fields
	 * @return 
	 */
	Flux<EntryOptional<A, B>> hmget(A key, Consumer<Keys<A>> fields);
	
	/**
	 * <a href="https://redis.io/commands/hmset">HMSET</a> key field value [field value ...]
	 * 
	 * @param key
	 * @param entries
	 * @return 
	 */
	Mono<String> hmset(A key, Consumer<Entries<A, B>> entries);
	
	/**
	 * <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<A> hrandfield(A key);
	
	/**
	 * <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a> key count
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<A> hrandfield(A key, long count);
	
	/**
	 * <a href="https://redis.io/commands/hrandfield">HRANDFIELD</a> key count WITHVALUES
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<EntryOptional<A, B>> hrandfieldWithvalues(A key, long count);
	
	/**
	 * <a href="https://redis.io/commands/hscan">HSCAN</a> key cursor
	 * 
	 * @param key
	 * @param cursor
	 * @return 
	 */
	Mono<HashScanResult<A, B>> hscan(A key, String cursor);
	
	/**
	 * <a href="https://redis.io/commands/hscan">HSCAN</a> key cursor [MATCH pattern] [COUNT count] 
	 * 
	 * @return 
	 */
	HashScanBuilder<A, B> hscan();
	
	/**
	 * <a href="https://redis.io/commands/hset">HSET</a> key field value
	 * 
	 * @param key
	 * @param field
	 * @param value
	 * @return 
	 */
	Mono<Boolean> hset(A key, A field, B value);
	
	/**
	 * <a href="https://redis.io/commands/hset">HSET</a> key field value [field value ...] 
	 * 
	 * @param key
	 * @param entries
	 * @return 
	 */
	Mono<Long> hset(A key, Consumer<Entries<A, B>> entries);
	
	/**
	 * <a href="https://redis.io/commands/hsetnx">HSETNX</a> key field value 
	 * 
	 * @param key
	 * @param field
	 * @param value
	 * @return 
	 */
	Mono<Boolean> hsetnx(A key, A field, B value);
	
	/**
	 * <a href="https://redis.io/commands/hstrlen">HSTRLEN </a> key field
	 * 
	 * @param key
	 * @param field
	 * @return 
	 */
	Mono<Long> hstrlen(A key, A field);
	
	/**
	 * <a href="https://redis.io/commands/hvals">HVALS</a> key 
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> hvals(A key);

	/**
	 * <a href="https://redis.io/commands/hscan">HSCAN</a> key cursor [MATCH pattern] [COUNT count]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface HashScanBuilder<A, B> extends AbstractScanBuilder<HashScanBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param cursor
		 * @return 
		 */
		Mono<HashScanResult<A, B>> build(A key, String cursor);
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface HashScanResult<A, B> extends AbstractScanResult {
		
		/**
		 * 
		 * @return 
		 */
		Map<A, B> getEntries();
	}
}
