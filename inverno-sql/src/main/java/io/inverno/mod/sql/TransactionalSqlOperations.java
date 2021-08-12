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

import reactor.core.publisher.Mono;

/**
 * <p>
 * SQL operations with support for transactions.
 * </p>
 * 
 * <p>All SQL operations are executed within a transaction which is eventually commited or rolled back.</p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public interface TransactionalSqlOperations extends SqlOperations {

	/**
	 * <p>
	 * Commits the transaction.
	 * </p>
	 * 
	 * @return a Mono that completes when the transaction is commited
	 */
	Mono<Void> commit();

	/**
	 * <p>
	 * Rolls back the transaction.
	 * </p>
	 * 
	 * @return a Mono that completes when the transaction is rolled back
	 */
	Mono<Void> rollback();
}
