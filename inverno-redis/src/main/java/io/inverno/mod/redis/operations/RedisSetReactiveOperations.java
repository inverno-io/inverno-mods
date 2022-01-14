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

import java.util.List;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Sets reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisSetReactiveOperations<A, B> {

	/**
	 * <a href="https://redis.io/commands/sadd">SADD</a> key member
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Mono<Long> sadd(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/sadd">SADD</a> key member [member ...] 
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Mono<Long> sadd(A key, Consumer<Values<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/scard">SCARD</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> scard(A key);
	
	/**
	 * <a href="https://redis.io/commands/sdiff">SDIFF</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> sdiff(A key);
	
	/**
	 * <a href="https://redis.io/commands/sdiff">SDIFF</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<B> sdiff(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/sdiff">SDIFF</a> destination key
	 * 
	 * @param destination
	 * @param key
	 * @return 
	 */
	Mono<Long> sdiffstore(A destination, A key);
	
	/**
	 * <a href="https://redis.io/commands/sdiff">SDIFF</a> destination key [key ...]
	 * 
	 * @param destination
	 * @param keys
	 * @return 
	 */
	Mono<Long> sdiffstore(A destination, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/sinter">SINTER</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> sinter(A key);
	
	/**
	 * <a href="https://redis.io/commands/sinter">SINTER</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<B> sinter(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/sintercard">SINTERCARD</a> numkeys key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> sintercard(A key);
	
	/**
	 * <a href="https://redis.io/commands/sintercard">SINTERCARD</a> numkeys key [LIMIT limit]
	 * 
	 * @param key
	 * @param limit
	 * @return 
	 */
	Mono<Long> sintercard(A key, long limit);
	
	/**
	 * <a href="https://redis.io/commands/sintercard">SINTERCARD</a> numkeys key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Mono<Long> sintercard(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/sintercard">SINTERCARD</a> numkeys key [key ...] [LIMIT limit]
	 * 
	 * @param keys
	 * @param limit
	 * @return 
	 */
	Mono<Long> sintercard(Consumer<Keys<A>> keys, long limit);
	
	/**
	 * <a href="https://redis.io/commands/sinter">SINTERSTORE</a> destination key
	 * 
	 * @param destination
	 * @param key
	 * @return 
	 */
	Mono<Long> sinterstore(A destination, A key);
	
	/**
	 * <a href="https://redis.io/commands/sinterstore">SINTERSTORE</a> destination key [key ...]
	 * 
	 * @param destination
	 * @param keys
	 * @return 
	 */
	Mono<Long> sinterstore(A destination, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/sismember">SISMEMBER</a> key member
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Mono<Boolean> sismember(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/smembers">SMEMBERS</a> key 
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> smembers(A key);
	
	/**
	 * <a href="https://redis.io/commands/smismember">SMISMEMBER</a> key member [member ...] 
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Flux<Boolean> smismember(A key, Consumer<Values<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/smove">SMOVE</a> source destination member
	 * 
	 * @param source
	 * @param destination
	 * @param member
	 * @return 
	 */
	Mono<Boolean> smove(A source, A destination, B member);
	
	/**
	 * <a href="https://redis.io/commands/spop">SPOP</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<B> spop(A key);
	
	/**
	 * <a href="https://redis.io/commands/spop">SPOP</a> key [count]
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<B> spop(A key, long count);

	/**
	 * <a href="https://redis.io/commands/srandmember">SRANDMEMBER</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<B> srandmember(A key);
	
	/**
	 * <a href="https://redis.io/commands/srandmember">SRANDMEMBER</a> key [count]
	 * 
	 * @param key
	 * @param count
	 * @return 
	 */
	Flux<B> srandmember(A key, long count);

	/**
	 * <a href="https://redis.io/commands/srem">SREM</a> key member
	 * 
	 * @param key
	 * @param member
	 * @return 
	 */
	Mono<Long> srem(A key, B member);
	
	/**
	 * <a href="https://redis.io/commands/srem">SREM</a> key member [member ...]
	 * 
	 * @param key
	 * @param members
	 * @return 
	 */
	Mono<Long> srem(A key, Consumer<Values<B>> members);
	
	/**
	 * <a href="https://redis.io/commands/sscan">SSCAN</a> key cursor
	 * 
	 * @param key
	 * @param cursor
	 * @return 
	 */
	Mono<SetScanResult<B>> sscan(A key, String cursor);

	/**
	 * <a href="https://redis.io/commands/sscan">SSCAN</a> key cursor [MATCH pattern] [COUNT count]
	 * 
	 * @return 
	 */
	SetScanBuilder<A, B> sscan();

	/**
	 * <a href="https://redis.io/commands/sunion">SUNION</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> sunion(A key);
	
	/**
	 * <a href="https://redis.io/commands/sunion">SUNION</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<B> sunion(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/sunionstore">SUNIONSTORE</a> destination key
	 * 
	 * @param destination
	 * @param key
	 * @return 
	 */
	Mono<Long> sunionstore(A destination, A key);
	
	/**
	 * <a href="https://redis.io/commands/sunionstore">SUNIONSTORE</a> destination key [key ...]
	 * 
	 * @param destination
	 * @param keys
	 * @return 
	 */
	Mono<Long> sunionstore(A destination, Consumer<Keys<A>> keys);

	/**
	 * <a href="https://redis.io/commands/sscan">SSCAN</a> key cursor [MATCH pattern] [COUNT count]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface SetScanBuilder<A, B> extends AbstractScanBuilder<SetScanBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param cursor
		 * @return 
		 */
		Mono<SetScanResult<B>> build(A key, String cursor);
	}

	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <B> value type
	 */
	interface SetScanResult<B> extends AbstractScanResult {
		
		/**
		 * 
		 * @return 
		 */
		List<B> getMembers();
	}
}
