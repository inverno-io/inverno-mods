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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * <p>
 * Specifies basic reactive SQL operations
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface SqlOperations {

	/**
	 * <p>
	 * Creates a SQL statement.
	 * </p>
	 * 
	 * @param sql the static SQL to execute
	 * 
	 * @return a new statement
	 */
	Statement statement(String sql);

	/**
	 * <p>
	 * Creates a prepared SQL statement.
	 * </p>
	 * 
	 * <p>
	 * Prepared statements are pre-compiled and protect against SQL injection
	 * attacks.
	 * </p>
	 * 
	 * @param sql the SQL to execute
	 * 
	 * @return a new prepared statement
	 */
	PreparedStatement preparedStatement(String sql);
	
	/**
	 * <p>
	 * Executes a query operation using a prepared statement with the specified
	 * arguments and returns the resulting rows.
	 * </p>
	 * 
	 * @param sql  the SQL query to execute
	 * @param args the query arguments
	 * 
	 * @return a publisher of rows
	 */
	Publisher<Row> query(String sql, Object... args);
	
	/**
	 * <p>
	 * Executes a query operation using a prepared statement with the specified
	 * arguments, applies a row mapping function to the resulting rows and returns
	 * the results.
	 * </p>
	 * 
	 * @param <T>       the type of results
	 * @param sql       the SQL query to execute
	 * @param rowMapper a row mapping function
	 * @param args      the query arguments
	 * 
	 * @return a publisher of results
	 */
	<T> Publisher<T> query(String sql, Function<Row, T> rowMapper, Object... args);

	/**
	 * <p>
	 * Executes a query operation using a prepared statement with the specified
	 * arguments, maps a single row to an object using a row mapping function and
	 * return that object.
	 * </p>
	 * 
	 * @param <T>       The type of the resulting object
	 * @param sql       the SQL query to execute
	 * @param rowMapper a row mapping function
	 * @param args      the query arguments
	 * 
	 * @return a Mono emitting the result
	 */
	<T> Mono<T> queryForObject(String sql, Function<Row, T> rowMapper, Object... args);
	
	/**
	 * <p>
	 * Executes an update operation such as insert, update or delete using a
	 * prepared statement with the specified arguments and returns the number rows
	 * affected by the operation.
	 * </p>
	 * 
	 * @param sql  the SQL update to execute
	 * @param args the update arguments
	 * 
	 * @return a Mono emitting the number of rows affected by the operation
	 */
	Mono<Integer> update(String sql, Object... args);

	/**
	 * <p>
	 * Executes multiple update operations in a batch using the specified list of
	 * arguments and returns the number rows affected by the operation.
	 * </p>
	 * 
	 * @param sql  the SQL update to execute
	 * @param args a list of arguments
	 * 
	 * @return a Mono emitting the number of rows affected by the batch operation
	 */
	Mono<Integer> batchUpdate(String sql, List<Object[]> args);
	
	/**
	 * <p>
	 * Executes multiple update operations in a batch using the specified stream of
	 * arguments and returns the number rows affected by the operation.
	 * </p>
	 * 
	 * @param sql  the SQL update to execute
	 * @param args a stream of arguments
	 * 
	 * @return a Mono emitting the number of rows affected by the batch operation
	 */
	Mono<Integer> batchUpdate(String sql, Stream<Object[]> args);
}
