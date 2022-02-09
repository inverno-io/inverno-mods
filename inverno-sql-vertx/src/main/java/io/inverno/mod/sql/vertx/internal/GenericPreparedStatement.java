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

import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.inverno.mod.sql.PreparedStatement;
import io.inverno.mod.sql.Row;
import io.inverno.mod.sql.SqlResult;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link PreparedStatement} implementation.
 * </p>
 * 
 * <p>
 * This implementation does not support result streaming.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericPreparedStatement extends AbstractPreparedStatement {

	protected Mono<PreparedQuery<RowSet<io.vertx.sqlclient.Row>>> preparedQuery;
	
	/**
	 * <p>
	 * Creates a prepared statement with the specified Vert.x SQL client and SQL
	 * operation.
	 * </p>
	 * 
	 * @param client a Vert.x SQL client
	 * @param sql a SQL operation
	 */
	public GenericPreparedStatement(SqlClient client, String sql) {
		this.preparedQuery = Mono.fromSupplier(() -> client.preparedQuery(sql)).cache();
	}
	
	@Override
	public Publisher<SqlResult> execute() {
		if(this.batch.size() == 1) {
			return this.preparedQuery.flatMapMany(query -> 
				Mono.fromCompletionStage(query.execute(this.currentParameters).toCompletionStage())
					.map(GenericSqlResult::new)
			);
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
	
	public <T> Publisher<T> execute(Function<Row, T> rowMapper) {
		if(this.batch.size() == 1) {
			return this.preparedQuery.flatMapMany(query -> 
				Mono.fromCompletionStage(query.execute(this.currentParameters).toCompletionStage())
					.flatMapMany(rowSet -> Flux.fromIterable(rowSet).map(row -> rowMapper.apply(new GenericRow(row))))
			);
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
