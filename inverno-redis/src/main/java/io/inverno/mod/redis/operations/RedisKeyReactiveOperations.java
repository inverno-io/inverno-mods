/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.operations;

import io.inverno.mod.redis.util.AbstractScanBuilder;
import io.inverno.mod.redis.util.AbstractScanResult;
import io.inverno.mod.redis.util.Keys;
import io.inverno.mod.redis.util.RedisType;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * @author jkuhn
 * @param <A>
 * @param <B>
 */
public interface RedisKeyReactiveOperations<A, B> /*extends RedisKeyReactiveCommands<A, B>*/ {

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
	 * @param <A> 
	 */
	interface KeyCopyBuilder<A> {
		
		KeyCopyBuilder<A> replace();
		KeyCopyBuilder<A> db(long destinationDb);
		
		Mono<Boolean> build(A source, A destination);
	}
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIRE</a> key seconds [NX|XX|GT|LT]
	 * 
	 * @param <A> 
	 */
	interface KeyExpireBuilder<A> {
		KeyExpireBuilder<A> nx();
		KeyExpireBuilder<A> xx();
		KeyExpireBuilder<A> gt();
		KeyExpireBuilder<A> lt();
		
		Mono<Boolean> build(A key, long seconds);	
		Mono<Boolean> build(A key, Duration duration);
	}
	
	/**
	 * <a href="https://redis.io/commands/expire">EXPIREAT</a> key timestamp [NX|XX|GT|LT] 
	 * 
	 * @param <A> 
	 */
	interface KeyExpireatBuilder<A> {
		KeyExpireBuilder<A> nx();
		KeyExpireBuilder<A> xx();
		KeyExpireBuilder<A> gt();
		KeyExpireBuilder<A> lt();
		
		Mono<Boolean> build(A key, long epochSeconds);
		Mono<Boolean> build(A key, ZonedDateTime datetime);
		Mono<Boolean> build(A key, Instant instant);
	}
	
	/**
	 * <a href="https://redis.io/commands/pexpire">PEXPIRE</a> key milliseconds [NX|XX|GT|LT] 
	 * 
	 * @param <A> 
	 */
	interface KeyPexpireBuilder<A> {
		KeyExpireBuilder<A> nx();
		KeyExpireBuilder<A> xx();
		KeyExpireBuilder<A> gt();
		KeyExpireBuilder<A> lt();
		
		Mono<Boolean> build(A key, long milliseconds);	
		Mono<Boolean> build(A key, Duration duration);
	}
	
	/**
	 * <a href="https://redis.io/commands/pexpireat">PEXPIREAT</a> key milliseconds-timestamp [NX|XX|GT|LT]
	 * 
	 * @param <A> 
	 */
	interface KeyPexpireatBuilder<A> {
		KeyExpireBuilder<A> nx();
		KeyExpireBuilder<A> xx();
		KeyExpireBuilder<A> gt();
		KeyExpireBuilder<A> lt();
		
		Mono<Boolean> build(A key, long epochMilliseconds);
		Mono<Boolean> build(A key, ZonedDateTime datetime);
		Mono<Boolean> build(A key, Instant instant);
	}
	
	/**
	 * <a href="https://redis.io/commands/migrate">MIGRATE</a> host port key|"" destination-db timeout [COPY] [REPLACE] [AUTH password] [AUTH2 username password] [KEYS key [key ...]] 
	 * 
	 * @param <A> 
	 */
	interface KeyMigrateBuilder<A> {
		
		KeyMigrateBuilder<A> copy();
		KeyMigrateBuilder<A> replace();
		KeyMigrateBuilder<A> auth(String password);
		KeyMigrateBuilder<A> auth(String username, String password);
		KeyMigrateBuilder<A> keys(Consumer<Keys<A>> keys);
		
		Mono<String> build(String host, int port, A key, int db, long timeout);
	}
	
	/**
	 * <a href="https://redis.io/commands/restore">RESTORE</a> key ttl serialized-value [REPLACE] [ABSTTL] [IDLETIME seconds] [FREQ frequency]
	 * 
	 * @param <A> 
	 */
	interface KeyRestoreBuilder<A> {
		
		KeyRestoreBuilder<A> replace();
		KeyRestoreBuilder<A> absttl();
		KeyRestoreBuilder<A> idletime(long seconds);
		KeyRestoreBuilder<A> freq(long frequency);
		
		Mono<String> build(A key, long ttl, byte[] serializedValue);
	}
	
	/**
	 * <a href="https://redis.io/commands/scan">SCAN</a> cursor [MATCH pattern] [COUNT count] [TYPE type] 
	 * 
	 * @param <A>
	 */
	interface KeyScanBuilder<A> extends AbstractScanBuilder<KeyScanBuilder<A>> {
		
		KeyScanBuilder<A> type(RedisType type);
		
		Mono<KeyScanResult<A>> build(String cursor);
	}
	
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @param <A>
	 * @param <B> 
	 */
	interface AbstractKeySortBuilder<A, B extends AbstractKeySortBuilder<A, B>> {
		
		B by(String pattern);
		B limit(long offset, long count);
		B asc();
		B desc();
		B alpha();
	}
	
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @param <A> 
	 * @param <B> 
	 */
	interface KeySortBuilder<A, B> extends AbstractKeySortBuilder<A, KeySortBuilder<A, B>> {
		
		KeySortBuilder<A, List<Optional<B>>> get(String... patterns);
		
		Flux<B> build(A key);
	}
	
	/**
	 * <a href="https://redis.io/commands/sort">SORT</a> key [BY pattern] [LIMIT offset count] [GET pattern [GET pattern ...]] [ASC|DESC] [ALPHA] [STORE destination] 
	 * 
	 * @param <A> 
	 */
	interface KeySortStoreBuilder<A> extends AbstractKeySortBuilder<A, KeySortStoreBuilder<A>> {
		
		KeySortStoreBuilder<A> store(A destination);
		
		Mono<Long> build(A key);
	}
	
	/**
	 * 
	 * @param <A> 
	 */
	interface KeyScanResult<A> extends AbstractScanResult {
		List<A> getKeys();
	}
	
}
