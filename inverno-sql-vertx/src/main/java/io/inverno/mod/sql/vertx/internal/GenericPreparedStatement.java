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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import io.inverno.mod.sql.PreparedStatement;
import io.inverno.mod.sql.SqlResult;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.data.NullValue;
import io.vertx.sqlclient.impl.ListTuple;
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
public class GenericPreparedStatement implements PreparedStatement {

	private static final NullValue OBJECT_NULL_VALUE = NullValue.of(Object.class);
	
	protected final SqlClient client;
	
	protected final String sql;
	
	protected final List<Tuple> batch;
	
	protected ListTuple currentParameters;
	
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
		this.client = client;
		this.sql = sql;
		this.batch = new LinkedList<>();
	}

	private ListTuple getCurrentParameters() {
		if(this.currentParameters == null) {
			this.currentParameters = new ListTuple(new ArrayList<>());
		}
		return this.currentParameters;
	}
	
	@Override
	public PreparedStatement and() {
		// TODO what happens when a parameter is null
		this.batch.add(this.currentParameters);
		this.currentParameters = null;
		return this;
	}

	@Override
	public PreparedStatement bindAt(int index, Object value) {
		ListTuple current = this.getCurrentParameters();
		
		if(current.size() < index) {
			while(current.size() < index) {
				current.addValue(OBJECT_NULL_VALUE);
			}
			current.addValue(value);
		}
		else {
			current.setValue(index, value);
		}
		return this;
	}

	@Override
	public PreparedStatement bindNullAt(int index, Class<?> type) {
		ListTuple current = this.getCurrentParameters();
		
		if(current.size() < index) {
			while(current.size() < index) {
				current.addValue(OBJECT_NULL_VALUE);
			}
			current.addValue(NullValue.of(type));
		}
		else {
			current.setValue(index, NullValue.of(type));
		}
		return this;
	}

	@Override
	public PreparedStatement bind(Object... values) {
		this.currentParameters = new ListTuple(Arrays.asList(values));
		return this;
	}
	
	@Override
	public PreparedStatement bind(List<Object[]> values) {
		values.forEach(value -> this.batch.add(new ListTuple(Arrays.asList(value))));
		this.currentParameters = null;
		return this;
	}
	
	@Override
	public PreparedStatement bind(Stream<Object[]> values) {
		values.forEach(value -> this.batch.add(new ListTuple(Arrays.asList(value))));
		this.currentParameters = null;
		return this;
	}
	
	@Override
	public PreparedStatement fetchSize(int rows) {
		// Noop since we are not streaming result
		return this;
	}
	
	@Override
	public Publisher<SqlResult> execute() {
		this.batch.add(this.currentParameters);
		
		if(this.batch.size() == 1) {
			 return Mono.fromCompletionStage(() -> this.client
					.preparedQuery(this.sql)
					.execute(this.currentParameters)
					.toCompletionStage()
				)
				.map(GenericSqlResult::new);
		}
		else {
			return Mono.fromCompletionStage(() -> this.client
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
