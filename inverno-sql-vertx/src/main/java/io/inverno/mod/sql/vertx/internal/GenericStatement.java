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

import io.inverno.mod.sql.SqlResult;
import io.inverno.mod.sql.Statement;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>Generic {@link Statement} implementation.</p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericStatement implements Statement {

	protected final SqlClient client;
	
	protected final StringBuilder sqlBuilder;
	
	/**
	 * <p>
	 * Creates a generic statement with the specified Vert.x SQL client and SQL
	 * operation.
	 * </p>
	 * 
	 * @param client a Vert.x SQL client
	 * @param sql    a static SQL operation
	 */
	public GenericStatement(SqlClient client, String sql) {
		this.client = client;
		this.sqlBuilder = new StringBuilder(sql);
	}

	@Override
	public Statement and(String sql) {
		this.sqlBuilder.append(";").append(sql);
		return this;
	}

	@Override
	public Publisher<SqlResult> execute() {
		return Mono
			.fromCompletionStage(() -> this.client
				.query(this.sqlBuilder.toString())
				.execute()
				.toCompletionStage()
			)
			.flatMapMany(rowSet -> Flux.create(sink -> {
				RowSet<io.vertx.sqlclient.Row> current = rowSet;
				do {
					sink.next(new GenericSqlResult(current));
				} while( (current = current.next()) != null);
				sink.complete();
			}));
	}
}
