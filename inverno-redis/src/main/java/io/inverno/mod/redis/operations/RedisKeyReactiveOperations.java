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

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis Keys reactive commands.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisKeyReactiveOperations<A, B> {

	/**
	 * <a href="https://redis.io/commands/copy">COPY</a> source destination
	 * 
	 * @param source
	 * @param destination
	 * @return 
	 */
	Mono<Boolean> copy(A source, A destination);
	
	/**
	 * <a href="https://redis.io/commands/copy">COPY</a> source destination [DB destination-db] [REPLACE]
	 * 
	 * @return 
	 */
	KeyCopyBuilder<A> copy();

	/**
	 * <a href="https://redis.io/commands/key">DEL</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> del(A key);
	
	/**
	 * <a href="https://redis.io/commands/key">DEL</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Mono<Long> del(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/dump">DUMP</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<byte[]> dump(A key);
	
	/**
	 * <a href="https://redis.io/commands/exists">EXISTS</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> exists(A key);
	
	/**
	 * <a href="https://redis.io/commands/exists">EXISTS</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Mono<Long> exists(Consumer<Keys<A>> keys);

	/**
	 * <a href="https://redis.io/commands/expire">EXPIRE</a> key seconds
	 * 
	 * @param key
	 * @param seconds
	 * @return 
	 */
	Mono<Boolean> expire(A key, long seconds);
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIRE</a> key seconds
	 * 
	 * @param key
	 * @param duration
	 * @return 
	 */
	Mono<Boolean> expire(A key, Duration duration);
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIRE</a> key seconds [NX|XX|GT|LT]
	 * 
	 * @return 
	 */
	KeyExpireBuilder<A> expire();
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIREAT</a> key timestamp
	 * 
	 * @param key
	 * @param epochSeconds
	 * @return 
	 */
	Mono<Boolean> expireat(A key, long epochSeconds);
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIREAT</a> key timestamp
	 * 
	 * @param key
	 * @param datetime
	 * @return 
	 */
	Mono<Boolean> expireat(A key, ZonedDateTime datetime);
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIREAT</a> key timestamp
	 * 
	 * @param key
	 * @param instant
	 * @return 
	 */
	Mono<Boolean> expireat(A key, Instant instant);
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIREAT</a> key timestamp [NX|XX|GT|LT] 
	 * 
	 * @return 
	 */
	KeyExpireatBuilder<A> expireat();
	
	/**
	 * <a href="https://redis.io/commands/expiretime">EXPIRETIME</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> expiretime(A key);
	
	
	/**
	 * <a href="https://redis.io/commands/keys">KEYS</a> pattern
	 * 
	 * @param pattern
	 * @return 
	 */
	Flux<A> keys(A pattern); // it should be a pattern as string...
	
	/**
	 * <a href="https://redis.io/commands/migrate">MIGRATE</a> host port key|"" destination-db timeout
	 * 
	 * @param host
	 * @param port
	 * @param key
	 * @param db
	 * @param timeout
	 * @return 
	 */
	Mono<String> migrate(String host, int port, A key, int db, long timeout);
	
	/**
	 * <a href="https://redis.io/commands/migrate">MIGRATE</a> host port key|"" destination-db timeout [COPY] [REPLACE] [AUTH password] [AUTH2 username password] [KEYS key [key ...]] 
	 * 
	 * @return 
	 */
	KeyMigrateBuilder<A> migrate();

	/**
	 * <a href="MOVE">https://redis.io/commands/move</a> key db
	 * 
	 * @param key
	 * @param db
	 * @return 
	 */
	Mono<Boolean> move(A key, int db);
	
	/**
	 * <a href="https://redis.io/commands/object-encoding">OBJECT ENCODING</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<String> objectEncoding(A key);
	
	/**
	 * <a href="https://redis.io/commands/object-freq">OBJECT FREQ</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> objectFreq(A key);
	
	/**
	 * <a href="https://redis.io/commands/object-idletime">OBJECT IDLETIME</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> objectIdletime(A key);
	
	/**
	 * <a href="https://redis.io/commands/object-refcount">OBJECT REFCOUNT</a> key 
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> objectRefcount(A key);

	/**
	 * <a href="https://redis.io/commands/persist">PERSIST</a> key 
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Boolean> persist(A key);

	/**
	 * <a href="https://redis.io/commands/pexpire">PEXPIRE</a> key milliseconds [NX|XX|GT|LT] 
	 * 
	 * @param key
	 * @param milliseconds
	 * @return 
	 */
	Mono<Boolean> pexpire(A key, long milliseconds);
	/**
	 * <a href="https://redis.io/commands/pexpire">PEXPIRE</a> key milliseconds [NX|XX|GT|LT] 
	 * 
	 * @param key
	 * @param duration
	 * @return 
	 */
	Mono<Boolean> pexpire(A key, Duration duration);
	/**
	 * <a href="https://redis.io/commands/pexpire">PEXPIRE</a> key milliseconds [NX|XX|GT|LT] 
	 * 
	 * @return 
	 */
	KeyPexpireBuilder<A> pexpire();
	
	/**
	 * <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a> key milliseconds-timestamp [NX|XX|GT|LT]
	 * 
	 * @param key
	 * @param epochMilliseconds
	 * @return 
	 */
	Mono<Boolean> pexpireat(A key, long epochMilliseconds);
	/**
	 * <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a> key milliseconds-timestamp [NX|XX|GT|LT]
	 * 
	 * @param key
	 * @param datetime
	 * @return 
	 */
	Mono<Boolean> pexpireat(A key, ZonedDateTime datetime);
	/**
	 * <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a> key milliseconds-timestamp [NX|XX|GT|LT]
	 * 
	 * @param key
	 * @param instant
	 * @return 
	 */
	Mono<Boolean> pexpireat(A key, Instant instant);
	/**
	 * <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a> key milliseconds-timestamp [NX|XX|GT|LT]
	 * 
	 * @return 
	 */
	KeyPexpireatBuilder<A> pexpireat();
	
	/**
	 * <a href="https://redis.io/commands/pexpiretime"> PEXPIRETIME</a> key 
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> pexpiretime(A key);
	
	/**
	 * <a href="https://redis.io/commands/pttl">PTTL</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> pttl(A key);
	
	/**
	 * <a href="https://redis.io/commands/randomkey">RANDOMKEY</a>
	 * 
	 * @return 
	 */
	Mono<A> randomkey();
	
	/**
	 * <a href="https://redis.io/commands/rename">RENAME</a> key newkey 
	 * 
	 * @param key
	 * @param newkey
	 * @return 
	 */
	Mono<String> rename(A key, A newkey);
	
	/**
	 * <a href="https://redis.io/commands/renamenx">RENAMENX</a> key newkey 
	 * 
	 * @param key
	 * @param newkey
	 * @return 
	 */
	Mono<Boolean> renamenx(A key, A newkey);
	
	/**
	 * <a href="https://redis.io/commands/restore">RESTORE</a> key ttl serialized-value [REPLACE] [ABSTTL] [IDLETIME seconds] [FREQ frequency]
	 * 
	 * @param key
	 * @param ttl
	 * @param serializedValue
	 * @return 
	 */
	Mono<String> restore(A key, long ttl, byte[] serializedValue);
	/**
	 * <a href="https://redis.io/commands/restore">RESTORE</a> key ttl serialized-value [REPLACE] [ABSTTL] [IDLETIME seconds] [FREQ frequency]
	 * 
	 * @return 
	 */
	KeyRestoreBuilder<A> restore();

	/**
	 * <a href="https://redis.io/commands/scan">SCAN</a> cursor [MATCH pattern] [COUNT count] [TYPE type] 
	 * 
	 * @param cursor
	 * @return 
	 */
	Mono<KeyScanResult<A>> scan(String cursor);
	/**
	 * <a href="https://redis.io/commands/scan">SCAN</a> cursor [MATCH pattern] [COUNT count] [TYPE type] 
	 * 
	 * @return 
	 */
	KeyScanBuilder<A> scan();

	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @param key
	 * @return 
	 */
	Flux<B> sort(A key);
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @return 
	 */
	KeySortBuilder<A, B> sort();
	
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @param key
	 * @param destination
	 * @return 
	 */
	Mono<Long> sortStore(A key, A destination);
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @return 
	 */
	KeySortStoreBuilder<A> sortStore();
	
	/**
	 * <a href="https://redis.io/commands/touch">TOUCH</a> key [key ...]
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> touch(A key);
	/**
	 * <a href="https://redis.io/commands/touch">TOUCH</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Mono<Long> touch(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/ttl">TTL</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> ttl(A key);
	
	/**
	 * <a href="https://redis.io/commands/type">TYPE</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<String> type(A key);

	/**
	 * <a href="https://redis.io/commands/unlink">UNLINK</a> key [key ...]
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> unlink(A key);
	/**
	 * <a href="https://redis.io/commands/unlink">UNLINK</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Mono<Long> unlink(Consumer<Keys<A>> keys);
	
	/**
	 * <a href="https://redis.io/commands/wait">WAIT</a> numreplicas timeout
	 * 
	 * @param replicas
	 * @param timeout
	 * @return 
	 */
	Mono<Long> waitForReplication(int replicas, long timeout);

	/**
	 * <a href="https://redis.io/commands/copy">COPY</a> source destination [DB destination-db] [REPLACE] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyCopyBuilder<A> {
		
		/**
		 * 
		 * @return 
		 */
		KeyCopyBuilder<A> replace();
		
		/**
		 * 
		 * @param destinationDb
		 * @return 
		 */
		KeyCopyBuilder<A> db(long destinationDb);
		
		/**
		 * 
		 * @param source
		 * @param destination
		 * @return 
		 */
		Mono<Boolean> build(A source, A destination);
	}
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIRE</a> key seconds [NX|XX|GT|LT]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyExpireBuilder<A> {
		
		/**
		 * 
		 * @return 
		 */
		KeyExpireBuilder<A> nx();
		
		/**
		 * 
		 * @return 
		 */
		KeyExpireBuilder<A> xx();
		
		/**
		 * 
		 * @return 
		 */
		KeyExpireBuilder<A> gt();
		
		/**
		 * 
		 * @return 
		 */
		KeyExpireBuilder<A> lt();
		
		/**
		 * 
		 * @param key
		 * @param seconds
		 * @return 
		 */
		Mono<Boolean> build(A key, long seconds);
		
		/**
		 * 
		 * @param key
		 * @param duration
		 * @return 
		 */
		Mono<Boolean> build(A key, Duration duration);
	}
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIREAT</a> key timestamp [NX|XX|GT|LT] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyExpireatBuilder<A> {
		
		/**
		 * 
		 * @return 
		 */
		KeyExpireatBuilder<A> nx();
		
		/**
		 * 
		 * @return 
		 */
		KeyExpireatBuilder<A> xx();
		
		/**
		 * 
		 * @return 
		 */
		KeyExpireatBuilder<A> gt();
		
		/**
		 * 
		 * @return 
		 */
		KeyExpireatBuilder<A> lt();
		
		/**
		 * 
		 * @param key
		 * @param epochSeconds
		 * @return 
		 */
		Mono<Boolean> build(A key, long epochSeconds);
		
		/**
		 * 
		 * @param key
		 * @param datetime
		 * @return 
		 */
		Mono<Boolean> build(A key, ZonedDateTime datetime);
		
		/**
		 * 
		 * @param key
		 * @param instant
		 * @return 
		 */
		Mono<Boolean> build(A key, Instant instant);
	}
	
	/**
	 * <a href="https://redis.io/commands/pexpire">PEXPIRE</a> key milliseconds [NX|XX|GT|LT] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyPexpireBuilder<A> {
		
		/**
		 * 
		 * @return 
		 */
		KeyPexpireBuilder<A> nx();
		
		/**
		 * 
		 * @return 
		 */
		KeyPexpireBuilder<A> xx();
		
		/**
		 * 
		 * @return 
		 */
		KeyPexpireBuilder<A> gt();
		
		/**
		 * 
		 * @return 
		 */
		KeyPexpireBuilder<A> lt();
		
		/**
		 * 
		 * @param key
		 * @param milliseconds
		 * @return 
		 */
		Mono<Boolean> build(A key, long milliseconds);
		
		/**
		 * 
		 * @param key
		 * @param duration
		 * @return 
		 */
		Mono<Boolean> build(A key, Duration duration);
	}
	
	/**
	 * <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a> key milliseconds-timestamp [NX|XX|GT|LT]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyPexpireatBuilder<A> {
		
		/**
		 * 
		 * @return 
		 */
		KeyPexpireatBuilder<A> nx();
		
		/**
		 * 
		 * @return 
		 */
		KeyPexpireatBuilder<A> xx();
		
		/**
		 * 
		 * @return 
		 */
		KeyPexpireatBuilder<A> gt();
		
		/**
		 * 
		 * @return 
		 */
		KeyPexpireatBuilder<A> lt();
		
		/**
		 * 
		 * @param key
		 * @param epochMilliseconds
		 * @return 
		 */
		Mono<Boolean> build(A key, long epochMilliseconds);
		
		/**
		 * 
		 * @param key
		 * @param datetime
		 * @return 
		 */
		Mono<Boolean> build(A key, ZonedDateTime datetime);
		
		/**
		 * 
		 * @param key
		 * @param instant
		 * @return 
		 */
		Mono<Boolean> build(A key, Instant instant);
	}
	
	/**
	 * <a href="https://redis.io/commands/migrate">MIGRATE</a> host port key|"" destination-db timeout [COPY] [REPLACE] [AUTH password] [AUTH2 username password] [KEYS key [key ...]] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyMigrateBuilder<A> {
		
		/**
		 * 
		 * @return 
		 */
		KeyMigrateBuilder<A> copy();
		
		/**
		 * 
		 * @return 
		 */
		KeyMigrateBuilder<A> replace();
		
		/**
		 * 
		 * @param password
		 * @return 
		 */
		KeyMigrateBuilder<A> auth(String password);
		
		/**
		 * 
		 * @param username
		 * @param password
		 * @return 
		 */
		KeyMigrateBuilder<A> auth(String username, String password);
		
		/**
		 * 
		 * @param keys
		 * @return 
		 */
		KeyMigrateBuilder<A> keys(Consumer<Keys<A>> keys);
		
		/**
		 * 
		 * @param host
		 * @param port
		 * @param key
		 * @param db
		 * @param timeout
		 * @return 
		 */
		Mono<String> build(String host, int port, A key, int db, long timeout);
	}
	
	/**
	 * <a href="https://redis.io/commands/restore">RESTORE</a> key ttl serialized-value [REPLACE] [ABSTTL] [IDLETIME seconds] [FREQ frequency]
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyRestoreBuilder<A> {
		
		/**
		 * 
		 * @return 
		 */
		KeyRestoreBuilder<A> replace();
		
		/**
		 * 
		 * @return 
		 */
		KeyRestoreBuilder<A> absttl();
		
		/**
		 * 
		 * @param seconds
		 * @return 
		 */
		KeyRestoreBuilder<A> idletime(long seconds);
		
		/**
		 * 
		 * @param frequency
		 * @return 
		 */
		KeyRestoreBuilder<A> freq(long frequency);
		
		/**
		 * 
		 * @param key
		 * @param ttl
		 * @param serializedValue
		 * @return 
		 */
		Mono<String> build(A key, long ttl, byte[] serializedValue);
	}
	
	/**
	 * <a href="https://redis.io/commands/scan">SCAN</a> cursor [MATCH pattern] [COUNT count] [TYPE type] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyScanBuilder<A> extends AbstractScanBuilder<KeyScanBuilder<A>> {
		
		/**
		 * 
		 * @param type
		 * @return 
		 */
		KeyScanBuilder<A> type(RedisType type);
		
		/**
		 * 
		 * @param cursor
		 * @return 
		 */
		Mono<KeyScanResult<A>> build(String cursor);
	}
	
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <C> builder type
	 */
	interface AbstractKeySortBuilder<A, C extends AbstractKeySortBuilder<A, C>> {
		
		/**
		 * 
		 * @param pattern
		 * @return 
		 */
		C by(String pattern);
		
		/**
		 * 
		 * @param offset
		 * @param count
		 * @return 
		 */
		C limit(long offset, long count);
		
		/**
		 * 
		 * @param patterns
		 * @return 
		 */
		C get(String... patterns);
		
		/**
		 * 
		 * @return 
		 */
		C asc();
		
		/**
		 * 
		 * @return 
		 */
		C desc();
		
		/**
		 * 
		 * @return 
		 */
		C alpha();
	}
	
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 * @param <B> value type
	 */
	interface KeySortBuilder<A, B> extends AbstractKeySortBuilder<A, KeySortBuilder<A, B>> {
		
		/**
		 * 
		 * @param key
		 * @return 
		 */
		Flux<B> build(A key);
	}
	
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeySortStoreBuilder<A> extends AbstractKeySortBuilder<A, KeySortStoreBuilder<A>> {
		
		/**
		 * 
		 * @param source
		 * @param destination
		 * @return 
		 */
		Mono<Long> build(A source, A destination);
	}
	
	/**
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 * 
	 * @param <A> key type
	 */
	interface KeyScanResult<A> extends AbstractScanResult {
		
		/**
		 * 
		 * @return 
		 */
		List<A> getKeys();
	}
}
