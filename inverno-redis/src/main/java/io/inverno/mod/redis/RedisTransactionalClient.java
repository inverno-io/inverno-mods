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

import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A Redis Client exposes reactive method to query a Redis datastore with transaction support.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisTransactionalClient<A, B> extends RedisClient<A, B> {

	/**
	 * <p>
	 * Starts a transaction block with the specified watches.
	 * </p>
	 * 
	 * <p>
	 * All Redis operations performed within the function using the Redis operations argument will be executed on a single connection.
	 * </p>
	 * 
	 * <p>
	 * The connection is obtained and the transaction started when the returned publisher is subscribed. The transaction MUST be explicitly executed or discarded in order to free resources.
	 * </p>
	 * 
	 * <p>
	 * Whether connections are reused (pool) or created is implementation specific.
	 * </p>
	 * 
	 * <ul>
	 * <li><a href="https://redis.io/commands/multi">MULTI</a></li>
	 * <li><a href="https://redis.io/commands/exec">EXEC</a></li>
	 * <li><a href="https://redis.io/commands/discard">DISCARD</a></li>
	 * </ul>
	 * 
	 * @param watches a list of watches
	 * 
	 * @return a mono emitting a transactional operations object
	 */
	Mono<RedisTransactionalOperations<A, B>> multi(A... watches);
	
	/**
	 * <p>
	 * Executes queries in a transaction on a single connection.
	 * </p>
	 * 
	 * <p>
	 * The specified function shall return queries publishers created from the Redis operations argument, these queries are then executed within a transaction on a single Redis connection.
	 * </p>
	 * 
	 * <p>
	 * The connection is obtained and the transaction started when the returned publisher is subscribed. The transaction is executed when the returned operations publisher successfully completes or
	 * discarded when it completes with errors. The connection is eventually closed once the EXEC/DISCARD operation terminates.
	 * </p>
	 * 
	 * <p>
	 * Whether connections are reused (pool) or created is implementation specific.
	 * </p>
	 * 
	 * <ul>
	 * <li><a href="https://redis.io/commands/multi">MULTI</a></li>
	 * <li><a href="https://redis.io/commands/exec">EXEC</a></li>
	 * <li><a href="https://redis.io/commands/discard">DISCARD</a></li>
	 * </ul>
	 * 
	 * @param function a function returning queries to execute in a transaction
	 * @param watches a list of watches
	 * 
	 * @return a mono emitting transaction result
	 */
	Mono<RedisTransactionResult> multi(Function<RedisOperations<A, B>, Publisher<Publisher<?>>> function, A... watches);
}
