/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package io.inverno.mod.redis.lettuce;

import io.lettuce.core.TransactionResult;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public interface RedisTransactionalOperations<A, B> extends RedisOperations<A, B> {

	/**
	 * <p>
	 * Discards all commands issued in the transaction.
	 * </p>
	 * 
	 * @return String simple-string-reply always {@code OK}.
	 */
    Mono<String> discard();

    /**
	 * <p>
	 * Executes all commands issued in the transaction.
	 * </p>
	 * 
	 * @return Object array-reply each element being the reply to each of the commands in the atomic transaction.
	 *
	 * @see TransactionResult#wasDiscarded
	 */
    Mono<TransactionResult> exec();
}
