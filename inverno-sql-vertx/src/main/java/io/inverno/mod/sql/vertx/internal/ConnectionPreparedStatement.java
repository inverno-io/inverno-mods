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

import io.inverno.mod.sql.PreparedStatement;
import io.inverno.mod.sql.Row;
import io.inverno.mod.sql.SqlResult;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.SqlConnection;
import java.util.function.Function;
import org.reactivestreams.Publisher;
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
public class ConnectionPreparedStatement extends GenericPreparedStatement {

	public static final int DEFAULT_FETCH_SIZE = 50;
	
	private int fetchSize = DEFAULT_FETCH_SIZE;
	
	protected final Mono<io.vertx.sqlclient.PreparedStatement> preparedStatement;
	
	/**
	 * <p>
	 * Creates a connection prepared statement.
	 * </p>
	 * 
	 * @param connection the underlying Vert.x connection
	 * @param sql        a SQL operation
	 */
	public ConnectionPreparedStatement(SqlConnection connection, String sql) {
		super(connection, sql);
		this.preparedStatement = Mono.fromCompletionStage(connection.prepare(sql).toCompletionStage());
	}
	
	@Override
	public PreparedStatement fetchSize(int rows) {
		this.fetchSize = rows;
		return this;
	}

	@Override
	public Publisher<SqlResult> execute() {
		if(this.batch.size() == 1) {
			// We can do streaming
			return this.preparedStatement
				.map(statement -> {
					Mono<RowSet<io.vertx.sqlclient.Row>> rowSet = this.preparedQuery.flatMap(query -> Mono.fromCompletionStage(query.execute(this.currentParameters).toCompletionStage()));
					Mono<RowStream<io.vertx.sqlclient.Row>> rowStream = Mono.fromSupplier(() -> statement.createStream(this.fetchSize));
					
					return new StreamSqlResult(rowSet, rowStream);
				});
		}
		else {
			return this.preparedQuery
				.flatMap(query -> Mono.fromCompletionStage(query.executeBatch(this.batch).toCompletionStage()))
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

	@Override
	public <T> Publisher<T> execute(Function<Row, T> rowMapper) {
		if(this.batch.size() == 1) {
			// We can do streaming
			return this.preparedStatement
				.flatMapMany(statement -> {
					return Flux.from(new StreamSqlResult.RowStreamPublisher(statement.createStream(this.fetchSize, this.currentParameters)))
						.map(rowMapper);
				});
		}
		else {
			return this.preparedQuery
				.flatMap(query -> Mono.fromCompletionStage(query.executeBatch(this.batch).toCompletionStage()))
				.flatMapMany(rowSet -> {
					return Flux.create(sink -> {
						RowSet<io.vertx.sqlclient.Row> current = rowSet;
						do {
							current.iterator().forEachRemaining(row -> {
								sink.next(rowMapper.apply(new GenericRow(row)));
							});
						} while( (current = current.next()) != null);
						sink.complete();
					});
				});
		}
	}
}
