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

import java.util.Optional;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Strings reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisStringReactiveOperations<A, B> {

	/**
	 * <a href="https://redis.io/commands/append">APPEND</a> key value
	 * 
	 * @param key
	 * @param value
	 * @return 
	 */
	Mono<Long> append(A key, B value);
	
	/**
	 * <a href="https://redis.io/commands/decr">DECR</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> decr(A key);
	
	/**
	 * <a href="https://redis.io/commands/decrby">DECRBY</a> key decrement
	 * 
	 * @param key
	 * @param decrement
	 * @return 
	 */
	Mono<Long> decrby(A key, long decrement);
	
	/**
	 * <a href="https://redis.io/commands/get">GET</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<B> get(A key);
	
	/**
	 * <a href="https://redis.io/commands/getdel">GETDEL</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<B> getdel(A key);
	
	/**
	 * <a href="https://redis.io/commands/getex">GETEX</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<B> getex(A key);
	
	/**
	 * <a href="https://redis.io/commands/getex">GETEX</a> key [EX seconds|PX milliseconds|EXAT unix-time|PXAT unix-time|PERSIST] 
	 * 
	 * @return 
	 */
	StringGetexBuilder<A, B> getex();
	
	/**
	 * <a href="https://redis.io/commands/getrange">GETRANGE</a> key start end
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return 
	 */
	Mono<B> getrange(A key, long start, long end);
	
	/**
	 * <a href="https://redis.io/commands/getset">GETSET</a> key value
	 * 
	 * @param key
	 * @param value
	 * @return 
	 */
	Mono<B> getset(A key, B value);
	
	/**
	 * <a href="https://redis.io/commands/incr">INCR</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> incr(A key);
	
	/**
	 * <a href="https://redis.io/commands/incrby">INCRBY</a> key increment
	 * 
	 * @param key
	 * @param increment
	 * @return 
	 */
	Mono<Long> incrby(A key, long increment);

	/**
	 * <a href="https://redis.io/commands/incrbyfloat">INCRBYFLOAT</a> key increment
	 * 
	 * @param key
	 * @param increment
	 * @return 
	 */
	Mono<Double> incrbyfloat(A key, double increment);
	
	/**
	 * <a href="https://redis.io/commands/mget">MGET</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Flux<EntryOptional<A, B>> mget(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/mset">MSET</a> key value [key value ...] 
	 * 
	 * @param entries
	 * @return 
	 */
	Mono<String> mset(Consumer<Entries<A, B>> entries);
	
	/**
	 * <a href="https://redis.io/commands/msetnx">MSETNX</a> key value [key value ...] 
	 * 
	 * @param entries
	 * @return 
	 */
	Mono<Boolean> msetnx(Consumer<Entries<A, B>> entries);
	
	/**
	 * <a href="https://redis.io/commands/psetex">PSETEX</a> key milliseconds value
	 * 
	 * @param key
	 * @param milliseconds
	 * @param value
	 * @return 
	 */
	Mono<String> psetex(A key, long milliseconds, B value);
	
	/**
	 * <a href="https://redis.io/commands/set">SET</a> key value
	 * 
	 * @param key
	 * @param value
	 * @return 
	 */
	Mono<String> set(A key, B value);
	
	/**
	 * <a href="https://redis.io/commands/set">SET</a> key value [EX seconds|PX milliseconds|EXAT unix-time-seconds|PXAT unix-time-milliseconds|KEEPTTL] [NX|XX]
	 * 
	 * @return 
	 */
	StringSetBuilder<A, B> set();
	
	/**
	 * <a href="https://redis.io/commands/set">SET</a> key value GET
	 * 
	 * @param key
	 * @param value
	 * @return 
	 */
	Mono<B> setGet(A key, B value);
	
	/**
	 * <a href="https://redis.io/commands/set">SET</a> key value [EX seconds|PX milliseconds|EXAT unix-time-seconds|PXAT unix-time-milliseconds|KEEPTTL] [NX|XX] GET
	 * 
	 * @return 
	 */
	StringSetGetBuilder<A, B> setGet();

	/**
	 * <a href="https://redis.io/commands/setex">SETEX</a> key seconds value 
	 * 
	 * @param key
	 * @param seconds
	 * @param value
	 * @return 
	 */
	Mono<String> setex(A key, long seconds, B value);
	
	/**
	 * <a href="https://redis.io/commands/setnx">SETNX</a> key value 
	 * 
	 * @param key
	 * @param value
	 * @return 
	 */
	Mono<Boolean> setnx(A key, B value);
	
	/**
	 * <a href="https://redis.io/commands/setrange">SETRANGE</a> key offset value
	 * 
	 * @param key
	 * @param offset
	 * @param value
	 * @return 
	 */
	Mono<Long> setrange(A key, long offset, B value);
	
	/**
	 * <a href="https://redis.io/commands/strlen">STRLEN</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> strlen(A key);
	
	/**
	 * <a href="https://redis.io/commands/bitcount">BITCOUT</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> bitcount(A key);
	
	/**
	 * <a href="https://redis.io/commands/bitcount">BITCOUT</a> key start end
	 * 
	 * @param key
	 * @param start
	 * @param end
	 * @return 
	 */
	Mono<Long> bitcount(A key, long start, long end);

	/**
	 * <a href="https://redis.io/commands/bitfield">BITFIELD</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Flux<Optional<Long>> bitfield(A key);
	
	/**
	 * <a href="https://redis.io/commands/bitfield">BITFIELD</a> key [GET encoding offset] [SET encoding offset value] [INCRBY encoding offset increment] [OVERFLOW WRAP|SAT|FAIL] 
	 * 
	 * @return 
	 */
	StringBitfieldBuilder<A, B> bitfield();
	
	/**
	 * <a href="https://redis.io/commands/bitop">BITOP</a> AND destkey key
	 * 
	 * @param destKey
	 * @param key
	 * @return 
	 */
	Mono<Long> bitopAnd(A destKey, A key);
	
	/**
	 * <a href="https://redis.io/commands/bitop">BITOP</a> AND destkey key [key ...] 
	 * 
	 * @param destKey
	 * @param keys
	 * @return 
	 */
	Mono<Long> bitopAnd(A destKey, Consumer<Keys<A>> keys);

	/**
	 * <a href="https://redis.io/commands/bitop">BITOP</a> OR destkey key
	 * 
	 * @param destKey
	 * @param key
	 * @return 
	 */
	Mono<Long> bitopOr(A destKey, A key);
	
	/**
	 * <a href="https://redis.io/commands/bitop">BITOP</a> OR destkey key [key ...] 
	 * 
	 * @param destKey
	 * @param keys
	 * @return 
	 */
	Mono<Long> bitopOr(A destKey, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/bitop">BITOP</a> XOR destkey key
	 * 
	 * @param destKey
	 * @param key
	 * @return 
	 */
	Mono<Long> bitopXor(A destKey, A key);
	
	/**
	 * <a href="https://redis.io/commands/bitop">BITOP</a> XOR destkey key [key ...] 
	 * 
	 * @param destKey
	 * @param keys
	 * @return 
	 */
	Mono<Long> bitopXor(A destKey, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/bitop">BITOP</a> NOT destkey key
	 * 
	 * @param destKey
	 * @param key
	 * @return 
	 */
	Mono<Long> bitopNot(A destKey, A key);
	
	/**
	 * <a href="https://redis.io/commands/bitop">BITOP</a> NOT destkey key [key ...] 
	 * 
	 * @param destKey
	 * @param keys
	 * @return 
	 */
	Mono<Long> bitopNot(A destKey, Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/bitpos">BITPOS</a> key bit [start [end [BYTE|BIT]]] 
	 * 
	 * @param key
	 * @param bit
	 * @return 
	 */
	Mono<Long> bitpos(A key, boolean bit);
	
	/**
	 * <a href="https://redis.io/commands/bitpos">BITPOS</a> key bit [start [end [BYTE|BIT]]] 
	 * 
	 * @param key
	 * @param bit
	 * @param start
	 * @return 
	 */
	Mono<Long> bitpos(A key, boolean bit, long start);
	
	/**
	 * <a href="https://redis.io/commands/bitpos">BITPOS</a> key bit [start [end [BYTE|BIT]]] 
	 * 
	 * @param key
	 * @param bit
	 * @param start
	 * @param end
	 * @return 
	 */
	Mono<Long> bitpos(A key, boolean bit, long start, long end);
	 
	/**
	 * <a href="https://redis.io/commands/getbit">GETBIT</a> key offset
	 * 
	 * @param key
	 * @param offset
	 * @return 
	 */
	Mono<Long> getbit(A key, long offset);
	
	/**
	 * <a href="https://redis.io/commands/getbit">SETBIT</a> key offset value
	 * 
	 * @param key
	 * @param offset
	 * @param value
	 * @return 
	 */
	Mono<Long> setbit(A key, long offset, int value);

	/**
	 * <a href="https://redis.io/commands/getex">GETEX</a> key [EX seconds|PX milliseconds|EXAT unix-time|PXAT unix-time|PERSIST]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StringGetexBuilder<A, B> {
		
		/**
		 * 
		 * @param seconds
		 * @return 
		 */
		StringGetexBuilder<A, B> ex(long seconds);
		
		/**
		 * 
		 * @param milliseconds
		 * @return 
		 */
		StringGetexBuilder<A, B> px(long milliseconds);
		
		/**
		 * 
		 * @param unixTime
		 * @return 
		 */
		StringGetexBuilder<A, B> exat(long unixTime);
		
		/**
		 * 
		 * @param unixTime
		 * @return 
		 */
		StringGetexBuilder<A, B> pxat(long unixTime);
		
		/**
		 * 
		 * @return 
		 */
		StringGetexBuilder<A, B> persist();
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Mono<B> build(A key);
	}
	
	/**
	 * <a href="https://redis.io/commands/set">SET</a> key value [EX seconds|PX milliseconds|EXAT unix-time-seconds|PXAT unix-time-milliseconds|KEEPTTL] [NX|XX] [GET]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 * @param <C> builder type
	 */
	interface AbstractStringSetBuilder<A, B, C extends AbstractStringSetBuilder<A, B, C>> {
		
		/**
		 * 
		 * @param seconds
		 * @return 
		 */
		C ex(long seconds);
		
		/**
		 * 
		 * @param milliseconds
		 * @return 
		 */
		C px(long milliseconds);
		
		/**
		 * 
		 * @param unixTime
		 * @return 
		 */
		C exat(long unixTime);
		
		/**
		 * 
		 * @param unixTime
		 * @return 
		 */
		C pxat(long unixTime);
		
		/**
		 * 
		 * @return 
		 */
		C keepttl();
		
		/**
		 * 
		 * @return 
		 */
		C nx();
		
		/**
		 * 
		 * @return 
		 */
		C xx();
	}
	
	/**
	 * <a href="https://redis.io/commands/set">SET</a> key value [EX seconds|PX milliseconds|EXAT unix-time-seconds|PXAT unix-time-milliseconds|KEEPTTL] [NX|XX]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StringSetBuilder<A, B> extends AbstractStringSetBuilder<A, B, StringSetBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param value
		 * @return 
		 */
		Mono<String> build(A key, B value);
	}
	
	/**
	 * <a href="https://redis.io/commands/set">SET</a> key value [EX seconds|PX milliseconds|EXAT unix-time-seconds|PXAT unix-time-milliseconds|KEEPTTL] [NX|XX] [GET]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StringSetGetBuilder<A, B> extends AbstractStringSetBuilder<A, B, StringSetGetBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @param value
		 * @return 
		 */
		Mono<B> build(A key, B value);
	}
	
	/**
	 * <a href="https://redis.io/commands/bitfield">BITFIELD</a> key [GET encoding offset] [SET encoding offset value] [INCRBY encoding offset increment] [OVERFLOW WRAP|SAT|FAIL] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface StringBitfieldBuilder<A, B> {
		
		/**
		 * 
		 * @param encoding
		 * @param offset
		 * @return 
		 */
		StringBitfieldBuilder<A, B> get(String encoding, int offset);
		
		/**
		 * 
		 * @param encoding
		 * @param offset
		 * @param value
		 * @return 
		 */
		StringBitfieldBuilder<A, B> set(String encoding, int offset, long value);
		
		/**
		 * 
		 * @param encoding
		 * @param offset
		 * @param increment
		 * @return 
		 */
		StringBitfieldBuilder<A, B> incrby(String encoding, int offset, long increment);
		
		/**
		 * 
		 * @return 
		 */
		StringBitfieldBuilder<A, B> wrap();
		
		/**
		 * 
		 * @return 
		 */
		StringBitfieldBuilder<A, B> sat();
		
		/**
		 * 
		 * @return 
		 */
		StringBitfieldBuilder<A, B> fail();
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<Optional<Long>> build(A key);
	}
}
