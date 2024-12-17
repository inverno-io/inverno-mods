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
package io.inverno.mod.sql.vertx.internal;

import io.inverno.mod.sql.PreparedStatement;
import io.inverno.mod.sql.TransactionalSqlOperations;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Transactional SQL operations executed on a single connection.
 * </p>
 * 
 * <p>
 * Committing or rolling back the transaction also closes the underlying Vert.x connection.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class TransactionalSqlConnection extends AbstractSqlOperations implements TransactionalSqlOperations {

	private final Transaction transaction;
	
	private final boolean closeConnection;
	
	/**
	 * <p>
	 * Creates transactional operations with the specified underlying Vert.x SQL connection and transaction.
	 * </p>
	 * 
	 * @param connection a Vert.x SQL connection with an opened transaction
	 * @param transaction the transaction
	 * @param closeConnection true to close the connection on commit/rollback, false otherwise
	 */
	public TransactionalSqlConnection(SqlConnection connection, Transaction transaction, boolean closeConnection) {
		super(connection);
		this.transaction = transaction;
		this.closeConnection = closeConnection;
	}

	@Override
	public PreparedStatement preparedStatement(String sql) {
		return new TransactionalPreparedStatement(((SqlConnection)this.client), sql);
	}
	
	@Override
	public Mono<Void> commit() {
		Mono<Void> result = Mono.fromCompletionStage(() -> this.transaction.commit().toCompletionStage());
		if(this.closeConnection) {
			result = result.then(this.close());
		}
		return result;
	}
	
	@Override
	public Mono<Void> rollback() {
		Mono<Void> result = Mono.fromCompletionStage(() -> this.transaction.rollback().toCompletionStage());
		if(this.closeConnection) {
			result = result.then(this.close());
		}
		return result;
	}

	/**
	 * <p>
	 * Closes the underlying SQL connection.
	 * </p>
	 * 
	 * @return a Mono that completes when the connection is closed
	 */
	public Mono<Void> close() {
		return Mono.fromCompletionStage(() -> this.client.close().toCompletionStage());
	}
}
