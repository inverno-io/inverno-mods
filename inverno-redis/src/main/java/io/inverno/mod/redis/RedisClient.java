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
 * A Redis Client exposes reactive methods to query a Redis datastore.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @param <A> key type
 * @param <B> value type
 */
public interface RedisClient<A, B> extends RedisOperations<A, B> {

	/**
	 * <p>
	 * Executes queries on a single connection.
	 * </p>
	 *
	 * <p>
	 * All Redis operations performed within the function using the Redis operations argument will be executed on the same connection.
	 * </p>
	 *
	 * <p>
	 * The connection is obtained when the returned publisher is subscribed and closed when it terminates (complete, error or cancel).
	 * </p>
	 *
	 * <p>
	 * Whether connections are reused (pool) or created is implementation specific.
	 * </p>
	 *
	 * @param <T>      The type of results
	 * @param function the function to be run using a single connection
	 *
	 * @return a publisher
	 */
	<T> Publisher<T> connection(Function<RedisOperations<A, B>, Publisher<T>> function);
	
	/**
	 * <p>
	 * Executes multiple queries in a batch on a single connection.
	 * </p>
	 *
	 * <p>
	 * The specified function shall return queries publishers created from the Redis operations argument, these queries are then pipelined on a single Redis connection, defering the flush of queries
	 * over the network.
	 * </p>
	 *
	 * <p>
	 * A connection is obtained when the returned publisher is subscribed and closed when it terminates (complete, error or cancel).
	 * </p>
	 *
	 * <p>
	 * Whether connections are reused (pool) or created is implementation specific.
	 * </p>
	 *
	 * @param <T>      the type of results
	 * @param function a function returning queries to execute in a batch
	 *
	 * @return a publisher of results
	 */
	<T> Publisher<T> batch(Function<RedisOperations<A, B>, Publisher<Publisher<T>>> function);
	
	/**
	 * <p>
	 * Closes the Redis client and free resources.
	 * </p>
	 *
	 * @return a Mono that completes when the client is closed
	 */
	Mono<Void> close();
}
