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

import io.inverno.mod.sql.SqlClient;
import io.inverno.mod.sql.SqlOperations;
import io.inverno.mod.sql.TransactionalSqlOperations;
import io.inverno.mod.sql.vertx.internal.AbstractSqlOperations;
import io.inverno.mod.sql.vertx.internal.TransactionalSqlConnection;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.TransactionRollbackException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A Vert.x connection SQL client wrapper.
 * </p>
 * 
 * <p>
 * This SQL client implementation supports transaction.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class ConnectionSqlClient extends AbstractSqlOperations implements SqlClient {

	/**
	 * <p>
	 * Creates a SQL client with the specified Vert.x SQL connection.
	 * </p>
	 * 
	 * @param connection a Vert.x SQL connection
	 */
	public ConnectionSqlClient(SqlConnection connection) {
		super(connection);
	}

	@Override
	public Mono<TransactionalSqlOperations> transaction() {
		return Mono.fromCompletionStage(((SqlConnection)this.client)
			.begin()
			.map(transaction -> new TransactionalSqlConnection(((SqlConnection)this.client), transaction, false))
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

	/**
	 * <p>
	 * Executes the specified function in the scope of the connection but does not
	 * close the connection when the returned publisher terminates.
	 * </p>
	 */
	@Override
	public <T> Publisher<T> connection(Function<SqlOperations, Publisher<T>> function) {
		return function.apply(this);
	}

	@Override
	public Mono<Void> close() {
		return Mono.fromCompletionStage(() -> this.client.close().toCompletionStage());
	}
}
