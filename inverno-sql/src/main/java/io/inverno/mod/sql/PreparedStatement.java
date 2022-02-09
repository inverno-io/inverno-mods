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

/**
 * <p>
 * The prepared statement is pre-compiled, it should be preferred over regular statements as it is more efficient when executed multiple times and it protects against SQL injection attacks.
 * </p>
 *
 * <p>
 * A prepared statement can execute operations in a batch as well by providing multiple bindings before execution.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface PreparedStatement {

	/**
	 * <p>
	 * Appends current arguments binding to the batch and creates another binding.
	 * </p>
	 *
	 * <p>
	 * This method is used to chain arguments bindings in a fluent way.
	 * </p>
	 *
	 * @return this statement
	 */
	PreparedStatement and();
	
	/**
	 * <p>
	 * Binds the specified value at the specified index in the current arguments binding
	 * </p>
	 *
	 * @param index the index of the argument
	 * @param value the argument value
	 *
	 * @return this statement
	 */
	PreparedStatement bindAt(int index, Object value);
	
	/**
	 * <p>
	 * Binds a null value of the specified type at the specified index in the current arguments binding.
	 * </p>
	 *
	 * @param index the index of the argument
	 * @param type  the type of argument
	 *
	 * @return this statement
	 */
	PreparedStatement bindNullAt(int index, Class<?> type);
	
	/**
	 * <p>
	 * Sets the specified list of values as the current arguments binding.
	 * </p>
	 *
	 * <p>
	 * Note that this will replace current arguments binding.
	 * </p>
	 *
	 * @param values the argument values to set
	 *
	 * @return this statement
	 */
	PreparedStatement bind(Object... values);
	
	/**
	 * <p>
	 * Adds multiple arguments bindings at once.
	 * </p>
	 *
	 * <p>
	 * This is is a shortcut for <code>.and().bind(...).and().bind(...)...</code> that appends the specified list of bindings to the current batch.
	 * </p>
	 *
	 * @param values a list of argument bindings
	 *
	 * @return this statement
	 */
	PreparedStatement bind(List<Object[]> values);
	
	/**
	 * <p>
	 * Adds multiple arguments bindings at once.
	 * </p>
	 *
	 * <p>
	 * This is is a shortcut for <code>.and().bind(...).and().bind(...)...</code> that appends the specified list of bindings to the current batch.
	 * </p>
	 *
	 * @param values a stream of argument bindings
	 *
	 * @return this statement
	 */
	PreparedStatement bind(Stream<Object[]> values);
	
	/**
	 * <p>
	 * Specifies the fetch size when rows are retrieved in a stream.
	 * </p>
	 * 
	 * <p>
	 * The fetch size is set to 0 by default and streaming is disabled.
	 * </p>
	 * 
	 * <p>
	 * If streaming is not available, this is a noop method.
	 * </p>
	 * 
	 * @param rows the number of rows to fetch or 0 to disable streaming
	 *
	 * @return this statement
	 */
	PreparedStatement fetchSize(int rows);
	
	/**
	 * <p>
	 * Resets the statement by removing all bindings.
	 * </p>
	 * 
	 * @return this statement
	 */
	PreparedStatement reset();
	
	/**
	 * <p>
	 * Executes the statement and returns a sequence of results corresponding to the bindings.
	 * </p>
	 *
	 * <p>
	 * When multiple parameters bindings are specified, the statement is executed in a batch and one result is emitted per parameters binding.
	 * </p>
	 *
	 * @return a publisher of results
	 */
	Publisher<SqlResult> execute();
	
	/**
	 * <p>
	 * Executes the statement, applies a row mapping function to the resulting rows and returns the results.
	 * </p>
	 *
	 * @param <T>       the type of results
	 * @param rowMapper a row mapping function
	 *
	 * @return a publisher of results
	 */
	<T> Publisher<T> execute(Function<Row, T> rowMapper);
}
