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
package io.inverno.mod.redis;

import reactor.core.publisher.Mono;

/**
 * <p>
 * Redis reactive commands with transaction support.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisTransactionalOperations<A, B> extends RedisOperations<A, B> {

	/**
	 * <p>
	 * Discards all commands issued in the transaction.
	 * </p>
	 * 
	 * <ul>
	 * <li><a href="https://redis.io/commands/discard">DISCARD</a></li>
	 * </ul>
	 * 
	 * @return discard command result (always {@code OK}).
	 */
    Mono<String> discard();

    /**
	 * <p>
	 * Executes all commands issued in the transaction.
	 * </p>
	 * 
	 * <ul>
	 * <li><a href="https://redis.io/commands/exec">EXEC</a></li>
	 * </ul>
	 * 
	 * @return a mono emitting transaction result
	 */
    Mono<RedisTransactionResult> exec();
}
