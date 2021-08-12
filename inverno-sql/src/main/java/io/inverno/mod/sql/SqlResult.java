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

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * <p>
 * The SQL result of a single SQL statement.
 * </p>
 * 
 * <p>
 * Depending on the nature of the statement operation, a SQL result might expose
 * rows or the number of rows affected by the operation but not both.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface SqlResult {

	/**
	 * <p>
	 * Returns the row metadata describing the resulting rows if any.
	 * </p>
	 * 
	 * <p>
	 * Implementations might return null if row metadata are not available.
	 * </p>
	 * 
	 * @return row metadata or null
	 */
	RowMetadata getRowMetadata();
	
	/**
	 * <p>
	 * Returns the number of rows affected by the operation.
	 * </p>
	 * 
	 * @return a Mono emitting the number of rows affected by the operation
	 */
	Mono<Integer> rowsUpdated();

	/**
	 * <p>
	 * Returns the resulting rows.
	 * </p>
	 * 
	 * @return a publisher of rows
	 */
	Publisher<Row> rows();
}
