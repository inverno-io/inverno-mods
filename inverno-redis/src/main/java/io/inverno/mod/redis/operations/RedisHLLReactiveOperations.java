/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.operations;

import io.inverno.mod.redis.util.Keys;
import io.inverno.mod.redis.util.Values;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 * @param <A>
 * @param <B>
 */
public interface RedisHLLReactiveOperations<A, B> {

	/**
	 * <a href="https://redis.io/commands/pfadd">PFADD</a> key element
	 * 
	 * @param k
	 * @param value
	 * @return 
	 */
	Mono<Long> pfadd(A k, B value);
	
	/**
	 * <a href="https://redis.io/commands/pfadd">PFADD</a> key [element [element ...]] 
	 * 
	 * @param k
	 * @param values
	 * @return 
	 */
	Mono<Long> pfadd(A k, Consumer<Values<B>> values);
	
	/**
	 * <a href="https://redis.io/commands/pfcount">PFCOUNT</a> key
	 * 
	 * @param key
	 * @return 
	 */
	Mono<Long> pfcount(A key);
	
	/**
	 * <a href="https://redis.io/commands/pfcount">PFCOUNT</a> key [key ...]
	 * 
	 * @param keys
	 * @return 
	 */
	Mono<Long> pfcount(Consumer<Keys<A>> keys);

	/**
	 * <a href="https://redis.io/commands/pfmerge">PFMERGE</a> destkey sourcekey
	 * 
	 * @param destkey
	 * @param sourcekey
	 * @return 
	 */
	Mono<String> pfmerge(A destkey, A sourcekey);
	
	/**
	 * <a href="https://redis.io/commands/pfmerge">PFMERGE</a> destkey sourcekey [sourcekey ...] 
	 * 
	 * @param destkey
	 * @param sourcekeys
	 * @return 
	 */
	Mono<String> pfmerge(A destkey, Consumer<Keys<A>> sourcekeys);
}
