/*
 * Copyright 2022 Jeremy KUHN
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

import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.inverno.mod.sql.PreparedStatement;
import io.inverno.mod.sql.Row;
import io.inverno.mod.sql.SqlResult;
import io.vertx.sqlclient.Cursor;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link PreparedStatement} implementation that runs in a connection with support for result streaming.
 * </p>
 *
 * <p>
 * Note that result streaming doesn't apply to batch operations.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class ConnectionPreparedStatement extends AbstractPreparedStatement {

	protected int fetchSize = 0;
	
	protected final Mono<io.vertx.sqlclient.PreparedStatement> preparedStatement;
	protected final Mono<PreparedQuery<RowSet<io.vertx.sqlclient.Row>>> preparedQuery;
	
	/**
	 * <p>
	 * Creates a connection prepared statement.
	 * </p>
	 * 
	 * @param connection the underlying Vert.x connection
	 * @param sql        a SQL operation
	 */
	public ConnectionPreparedStatement(SqlConnection connection, String sql) {
		this.preparedStatement = Mono.fromCompletionStage(connection.prepare(sql).toCompletionStage()).cache();
		this.preparedQuery = this.preparedStatement.map(io.vertx.sqlclient.PreparedStatement::query).cache();
	}
	
	@Override
	public PreparedStatement fetchSize(int rows) {
		this.fetchSize = rows;
		return this;
	}

	@Override
	public Publisher<SqlResult> execute() {
		if(this.batch.size() == 1) {
			if(this.fetchSize > 0) {
				// Streaming
				return this.preparedStatement
					.map(statement -> new StreamSqlResult(statement.cursor(this.currentParameters), this.fetchSize));
			}
			else {
				return this.preparedQuery.flatMap(query ->
					Mono.fromCompletionStage(query.execute(this.currentParameters).toCompletionStage())
						.map(GenericSqlResult::new)
				);
			}
		}
		else {
			return this.preparedQuery.flatMapMany(query -> 
				Mono.fromCompletionStage(query.executeBatch(this.batch).toCompletionStage())
					.flatMapMany(rowSet -> 
						Mono.just(rowSet)
							.expand(current -> Mono.justOrEmpty(current.next()))
							.map(GenericSqlResult::new)
					)
			);
		}
	}

	@Override
	public <T> Publisher<T> execute(Function<Row, T> rowMapper) {
		if(this.batch.size() == 1) {
			if(this.fetchSize > 0) {
				// Streaming
				return this.preparedStatement
					.flatMapMany(statement -> {
						Cursor cursor = statement.cursor(this.currentParameters);
						return Mono.fromCompletionStage(() -> cursor.read(this.fetchSize).toCompletionStage())
							.repeat(cursor::hasMore)
							.map(rowSet -> rowSet.size() > 0 ? rowMapper.apply(new GenericRow(rowSet.iterator().next())) : null)
							.doFinally(ign -> cursor.close());
					});
			}
			else {
				return this.preparedQuery.flatMapMany(query -> 
					Mono.fromCompletionStage(query.execute(this.currentParameters).toCompletionStage())
						.flatMapMany(rowSet -> Flux.fromIterable(rowSet).map(row -> rowMapper.apply(new GenericRow(row))))
				);
			}
		}
		else {
			return this.preparedQuery.flatMapMany(query -> 
				Mono.fromCompletionStage(query.executeBatch(this.batch).toCompletionStage())
					.flatMapMany(rowSet -> 
						Mono.just(rowSet)
							.expand(current -> Mono.justOrEmpty(current.next()))
							.flatMapIterable(Function.identity())
							.map(row -> rowMapper.apply(new GenericRow(row)))
					)
			);
		}
	}
}
