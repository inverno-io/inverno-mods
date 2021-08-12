/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.sql;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * <p>
 * A SQL Client expose reactive method to query a RDBMS using SQL.
 * </p>
 * 
 * <p>
 * Support for transaction is implementation specific and depends on the
 * underlying client.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface SqlClient extends SqlOperations {

	/**
	 * <p>
	 * Returns transactional SQL operations exposing commit() and rollback() method
	 * when the implementation supports it.
	 * </p>
	 * 
	 * <p>
	 * This method should be used when there is a need for explicit transaction
	 * management.
	 * </p>
	 * 
	 * @return a Mono emitting a transactional SQL operations object
	 */
	Mono<TransactionalSqlOperations> transaction();
	
	/**
	 * <p>
	 * Executes the specified function within a transaction.
	 * </p>
	 * 
	 * <p>
	 * All SQL operations performed within the function using the SQL operations
	 * argument will be executed within a transaction.
	 * </p>
	 * 
	 * <p>
	 * The transaction is created when the returned publisher is subscribed and
	 * committed when the publisher returned by the function returned successfully,
	 * it is rolled back otherwise (error or cancel).
	 * </p>
	 * 
	 * @param <T>      The type of results
	 * @param function the transactional function to be run in a transaction
	 * 
	 * @return a publisher of result
	 * @throws UnsupportedOperationException if the implementation does not support transaction
	 */
	<T> Publisher<T> transaction(Function<SqlOperations, Publisher<T>> function); // implicit transaction management => transaction is managed automatically
	
	/**
	 * <p>
	 * Executes the specified function within a single connection.
	 * </p>
	 * 
	 * <p>
	 * All SQL operations performed within the function using the SQL operations
	 * argument will be executed using the same connection.
	 * </p>
	 * 
	 * <p>
	 * The connection is obtained when the returned publisher is subscribed and
	 * closed when it terminates (complete, error or cancel).
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
	<T> Publisher<T> connection(Function<SqlOperations, Publisher<T>> function); // implicit connection management => connection lifecycle is managed automatically
	
	/**
	 * <p>
	 * Closes the SQL client and free resources.
	 * </p>
	 * 
	 * @return a Mono that completes when the client is closed
	 */
	Mono<Void> close();
}
