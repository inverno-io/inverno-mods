/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.operations;

import io.inverno.mod.redis.util.AbstractScanBuilder;
import io.inverno.mod.redis.util.EntryOptional;
import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.inverno.mod.redis.util.AbstractScanResult;
import io.inverno.mod.redis.util.Entries;
import io.inverno.mod.redis.util.Keys;

/**
 * 
 * @author jkuhn
 * @param <A>
 * @param <B>
 */
public interface RedisHashReactiveOperations<A, B> /*extends RedisHashReactiveCommands<A, B>*/ {

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
	 * <a href="https://redis.io/commands/hmget">HMGET</a> key field
	 * 
	 * @param key
	 * @param field
	 * @return 
	 */
	Flux<EntryOptional<A, B>> hmget(A key, A field);
	
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
	 * @param <A>
	 * @param <B> 
	 */
	interface HashScanBuilder<A, B> extends AbstractScanBuilder<HashScanBuilder<A, B>> {
		Mono<HashScanResult<A, B>> build(A key, String cursor);
	}
	
	interface HashScanResult<A, B> extends AbstractScanResult {
		Map<A, B> getEntries();
	}
}
