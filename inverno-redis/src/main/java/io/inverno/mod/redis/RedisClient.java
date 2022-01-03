/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis;

import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public interface RedisClient<A, B> extends RedisOperations<A, B> {

	<T> Publisher<T> connection(Function<RedisOperations<A, B>, Publisher<T>> function);
	
	<T> Publisher<T> batch(Function<RedisOperations<A, B>, Publisher<Publisher<T>>> function);
	
	Mono<Void> close();
}
