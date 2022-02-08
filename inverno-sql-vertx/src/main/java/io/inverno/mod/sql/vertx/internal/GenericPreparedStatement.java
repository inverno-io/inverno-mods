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
import io.inverno.mod.sql.Row;
import io.inverno.mod.sql.SqlResult;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.data.NullValue;
import io.vertx.sqlclient.impl.ListTuple;
import java.util.function.Function;
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

	protected final Mono<PreparedQuery<RowSet<io.vertx.sqlclient.Row>>> preparedQuery;
	
	protected final LinkedList<Tuple> batch;
	
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
		this.preparedQuery = Mono.fromSupplier(() -> this.client.preparedQuery(this.sql)).cache();
		this.currentParameters = new ListTuple(new ArrayList<>());
		this.batch.add(this.currentParameters);
	}
	
	@Override
	public synchronized PreparedStatement and() {
		this.currentParameters = new ListTuple(new ArrayList<>());
		this.batch.add(this.currentParameters);
		return this;
	}

	@Override
	public synchronized PreparedStatement bindAt(int index, Object value) {
		if(this.currentParameters.size() < index) {
			while(this.currentParameters.size() < index) {
				this.currentParameters.addValue(OBJECT_NULL_VALUE);
			}
			this.currentParameters.addValue(value);
		}
		else {
			this.currentParameters.setValue(index, value);
		}
		return this;
	}

	@Override
	public synchronized PreparedStatement bindNullAt(int index, Class<?> type) {
		if(this.currentParameters.size() < index) {
			while(this.currentParameters.size() < index) {
				this.currentParameters.addValue(OBJECT_NULL_VALUE);
			}
			this.currentParameters.addValue(NullValue.of(type));
		}
		else {
			this.currentParameters.setValue(index, NullValue.of(type));
		}
		return this;
	}

	@Override
	public synchronized PreparedStatement bind(Object... values) {
		this.batch.pollLast();
		this.currentParameters = new ListTuple(Arrays.asList(values));
		this.batch.add(this.currentParameters);
		return this;
	}
	
	@Override
	public synchronized PreparedStatement bind(List<Object[]> values) {
		if(values == null || values.isEmpty()) {
			throw new IllegalArgumentException("Bindings list is null or empty");
		}
		this.batch.pollLast();
		values.forEach(value -> this.batch.add(new ListTuple(Arrays.asList(value))));
		this.currentParameters = (ListTuple)this.batch.getLast();
		return this;
	}
	
	@Override
	public PreparedStatement bind(Stream<Object[]> values) {
		if(values == null) {
			throw new IllegalArgumentException("Bindings stream is null");
		}
		this.batch.pollLast();
		values.forEach(value -> this.batch.add(new ListTuple(Arrays.asList(value))));
		this.currentParameters = (ListTuple)this.batch.getLast();;
		return this;
	}
	
	@Override
	public PreparedStatement fetchSize(int rows) {
		// Noop since we are not streaming result
		return this;
	}

	@Override
	public synchronized PreparedStatement reset() {
		this.batch.clear();
		this.currentParameters = new ListTuple(new ArrayList<>());
		this.batch.add(this.currentParameters);
		
		return this;
	}
	
	@Override
	public Publisher<SqlResult> execute() {
		if(this.batch.size() == 1) {
			return this.preparedQuery
				.flatMap(query -> Mono.fromCompletionStage(query.execute(this.currentParameters).toCompletionStage()))
				.map(GenericSqlResult::new);
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
	
	public <T> Publisher<T> execute(Function<Row, T> rowMapper) {
		if(this.batch.size() == 1) {
			return this.preparedQuery
				.flatMap(query -> Mono.fromCompletionStage(query.execute(this.currentParameters).toCompletionStage()))
				.flatMapMany(rowSet -> Flux.fromIterable(rowSet).map(row -> rowMapper.apply(new GenericRow(row))));
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
