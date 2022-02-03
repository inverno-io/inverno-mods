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

/**
 * <p>
 * SQL unsafe or experimental operations.
 * </p>
 * 
 * <p>
 * The methods exposed in this inferface should be considered as unstable and should be used with care. Implementations might not fully support this interface which can also change over time.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface UnsafeSqlOperations {

	/**
	 * <p>
	 * Executes multiple queries in a batch.
	 * </p>
	 *
	 * <p>
	 * The specified function shall return queries publishers created from the SQL operations argument, these queries are then pipelined, defering the flush of queries over the network.
	 * </p>
	 *
	 * @param <T>      the type of results
	 * @param function a function returning queries to execute in a batch
	 *
	 * @return a publisher of results
	 */
	<T> Publisher<T> batchQueries(Function<SqlOperations, Publisher<Publisher<T>>> function);
}
