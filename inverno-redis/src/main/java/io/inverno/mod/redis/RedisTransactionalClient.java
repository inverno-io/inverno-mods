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
 * @param <A>
 * @param <B>
 */
public interface RedisTransactionalClient<A, B> extends RedisClient<A, B> {

	Mono<RedisTransactionalOperations<A, B>> multi(A... watches);
	
	Mono<RedisTransactionResult> multi(Function<RedisOperations<A, B>, Publisher<Publisher<Object>>> function, A... watches);
}
