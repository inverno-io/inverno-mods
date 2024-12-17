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

import java.util.stream.Stream;

/**
 * <p>
 * Redis MULTI/EXEC sequence result.
 * </p>
 * 
 * <p>
 * It indicates whether a Redis transaction was discarded and exposes commands results.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface RedisTransactionResult {

	/**
	 * <p>
	 * Indicates whether the transaction was discarded.
	 * </p>
	 * 
     * @return true if the transaction batch was discarded.
     */
    boolean wasDiscarded();
	
	/**
	 * <p>
	 * Returns the number of commands executed within the transaction.
	 * </p>
     *
     * @return the number of commands
     */
    int size();
	
	/**
	 * <p>
	 * Returns true if the transaction result contains no elements.
	 * </p>
     *
     * @return true if the transaction result contains no elements, false otherwise
     */
    boolean isEmpty();
	
	/**
	 * <p>
	 * Returns the command result at the specified index.
	 * </p>
     *
     * @param index index of the result to return
     * @param <T> inferred type
	 * 
     * @return the command result at the specified index
	 *
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    <T> T get(int index) throws IndexOutOfBoundsException;

    /**
	 * <p>
	 * Returns commands results as stream.
	 * </p>
     *
     * @return a stream of commands results
     */
    Stream<Object> stream();
}
