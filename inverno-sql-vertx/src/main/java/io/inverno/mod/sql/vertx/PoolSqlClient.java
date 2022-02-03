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
package io.inverno.mod.sql.vertx;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.inverno.mod.sql.SqlOperations;
import io.inverno.mod.sql.TransactionalSqlOperations;
import io.inverno.mod.sql.vertx.internal.AbstractSqlClient;
import io.inverno.mod.sql.vertx.internal.SqlConnection;
import io.inverno.mod.sql.vertx.internal.TransactionalSqlConnection;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.TransactionRollbackException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A Vert.x pool SQL client wrapper.
 * </p>
 * 
 * <p>
 * This SQL client implementation supports transaction but does not support
 * operations pipelining.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class PoolSqlClient extends AbstractSqlClient {

	/**
	 * <p>
	 * Creates a SQL client with the specified Vert.x pool.
	 * </p>
	 * 
	 * @param pool a Vert.x pool
	 */
	public PoolSqlClient(Pool pool) {
		super(pool);
	}

	/**
	 * <p>
	 * Gets a connection from the underlying pool.
	 * </p>
	 * 
	 * @return a Mono emitting a connection
	 */
	private Mono<SqlConnection> connection() {
		return Mono.fromCompletionStage(() -> ((Pool)this.client)
			.getConnection()
			.map(SqlConnection::new)
			.toCompletionStage()
		);
	}
	
	@Override
	public Mono<TransactionalSqlOperations> transaction() {
		return Mono.fromCompletionStage(() -> ((Pool)this.client)
			.getConnection()
			.flatMap(sqlConnection -> sqlConnection
				.begin()
				.map(transaction -> new TransactionalSqlConnection(sqlConnection, transaction, true))
			)
			.toCompletionStage()
		);
	}

	@Override
	public <T> Publisher<T> transaction(Function<SqlOperations, Publisher<T>> function) {
		return Flux.usingWhen(
			this.transaction(), 
			function, 
			TransactionalSqlOperations::commit, 
			(transaction, ex) -> {
				if(!(ex instanceof TransactionRollbackException)) {
					return transaction.rollback();
				}
				return Mono.error(() -> ex);
			}, 
			TransactionalSqlOperations::rollback
		);
	}

	@Override
	public <T> Publisher<T> connection(Function<SqlOperations, Publisher<T>> function) {
		return Flux.usingWhen(
			this.connection(),
			function,
			SqlConnection::close
		);
	}
}
