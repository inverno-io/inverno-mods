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

import org.reactivestreams.Publisher;

import io.inverno.mod.sql.PreparedStatement;
import io.inverno.mod.sql.SqlResult;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.SqlConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link PreparedStatement} implementation that runs in a transaction with
 * support for result streaming.
 * </p>
 * 
 * <p>
 * Note that result streaming doesn't apply to batch operations.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class TransactionalPreparedStatement extends GenericPreparedStatement {

	public static final int DEFAULT_FETCH_SIZE = 50;
	
	private int fetchSize = DEFAULT_FETCH_SIZE;
	
	/**
	 * <p>
	 * Creates a transactional prepared statement.
	 * </p>
	 * 
	 * @param connection the underlying Vert.x connection with an opened transaction
	 * @param sql        a SQL operation
	 */
	public TransactionalPreparedStatement(SqlConnection connection, String sql) {
		super(connection, sql);
	}
	
	@Override
	public PreparedStatement fetchSize(int rows) {
		this.fetchSize = rows;
		return this;
	}

	@Override
	public Publisher<SqlResult> execute() {
		this.batch.add(this.currentParameters);
		
		if(this.batch.size() == 1) {
			// We can do streaming
			return Mono.fromCompletionStage(() -> ((SqlConnection)this.client)
					.prepare(this.sql)
					.toCompletionStage()
				)
				.map(preparedStatement -> {
					Mono<RowSet<io.vertx.sqlclient.Row>> rowSet = Mono.fromCompletionStage(() -> ((SqlConnection)this.client)
							.preparedQuery(this.sql)
							.execute(this.currentParameters)
							.toCompletionStage()
						);
					
					Mono<RowStream<io.vertx.sqlclient.Row>> rowStream = Mono.fromSupplier(() -> preparedStatement.createStream(this.fetchSize));
					
					return new StreamSqlResult(rowSet, rowStream);
				});
		}
		else {
			return Mono.fromCompletionStage(() -> ((SqlConnection)this.client)
					.preparedQuery(this.sql)
					.executeBatch(this.batch)
					.toCompletionStage()
				)
				.flatMapMany(rowSet -> {
					return Flux.create(sink -> {
						RowSet<io.vertx.sqlclient.Row> current = rowSet;
						do {
							sink.next(new GenericSqlResult(current));
						} while( (current = current.next()) != null);
						sink.complete();
					});
				});
		}
	}
}
