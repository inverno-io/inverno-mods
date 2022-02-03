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

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import io.inverno.mod.sql.PreparedStatement;
import io.inverno.mod.sql.Row;
import io.inverno.mod.sql.SqlOperations;
import io.inverno.mod.sql.Statement;
import io.inverno.mod.sql.UnsafeSqlOperations;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.impl.SqlClientInternal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;

/**
 * <p>
 * Base {@link SqlOperations} Vert.x implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public abstract class AbstractSqlOperations implements SqlOperations, UnsafeSqlOperations {

	/**
	 * The underlying Vertx SQL client
	 */
	protected SqlClient client;
	
	/**
	 * <p>
	 * Creates SQL operations with the specified Vert.x SQL client.
	 * </p>
	 * 
	 * @param client a Vert.x SQL client
	 */
	public AbstractSqlOperations(SqlClient client) {
		this.client = client;
	}
	
	@Override
	public Statement statement(String sql) {
		return new GenericStatement(this.client, sql);
	}

	@Override
	public PreparedStatement preparedStatement(String sql) {
		return new GenericPreparedStatement(this.client, sql);
	}
	
	@Override
	public Publisher<Row> query(String sql, Object... args) {
		return Mono
			.fromCompletionStage(() -> this.client
				.preparedQuery(sql)
				.execute(Tuple.from(args))
				.toCompletionStage()
			)
			.flatMapMany(rowSet -> Flux.fromIterable(rowSet).map(row -> new GenericRow(row)));
	}
	
	@Override
	public <T> Publisher<T> query(String sql, Function<Row, T> rowMapper, Object... args) {
		return Mono
			.fromCompletionStage(() -> this.client
				.preparedQuery(sql)
				.execute(Tuple.from(args))
				.toCompletionStage()
			)
			.flatMapMany(rowSet -> Flux.fromIterable(rowSet).map(row -> rowMapper.apply(new GenericRow(row))));
	}
	
	@Override
	public <T> Mono<T> queryForObject(String sql, Function<Row, T> rowMapper, Object... args) {
		return Mono
			.fromCompletionStage(() -> this.client
				.preparedQuery(sql)
				.execute(Tuple.from(args))
				.map(rowSet -> rowSet.size() > 0 ? rowMapper.apply(new GenericRow(rowSet.iterator().next())) : null)
				.toCompletionStage()
			)
			.filter(Objects::nonNull);
	}
	
	@Override
	public <T> Publisher<T> batchQueries(Function<SqlOperations, Publisher<Publisher<T>>> function) {
		if(!(this.client instanceof SqlClientInternal)) {
			throw new UnsupportedOperationException("Implementation doesn't support batch operations");
		}

		BatchedSqlOperations batchedOperations = new BatchedSqlOperations();
		return Flux.from(function.apply(batchedOperations))
			.buffer(Queues.SMALL_BUFFER_SIZE) // use default mergeSequential() concurrency size: 256
			.flatMap(queries -> {
				return Flux.create(sink -> {
					// group is not always implemented in Vertx which is why this is considered unsafe operation
					((SqlClientInternal)this.client).group(c -> {
						batchedOperations.setClient(c);
						// We must subscribe here and return the result
						Flux.mergeSequential(queries).subscribe(sink::next, sink::error, sink::complete);
					});
				});
			});
	}
	
	@Override
	public Mono<Integer> update(String sql, Object... args) {
		return Mono
			.fromCompletionStage(() -> this.client
				.preparedQuery(sql)
				.execute(Tuple.from(args))
				.toCompletionStage()
			)
			.map(rowSet -> rowSet.rowCount());
	}
	
	@Override
	public Mono<Integer> batchUpdate(String sql, List<Object[]> args) {
		return Mono
			.fromCompletionStage(() -> this.client
				.preparedQuery(sql)
				.executeBatch(args.stream().map(Tuple::from).collect(Collectors.toList()))
				.toCompletionStage()
			)
			.map(rowSet -> rowSet.rowCount());
	}
	
	@Override
	public Mono<Integer> batchUpdate(String sql, Stream<Object[]> args) {
		return Mono
			.fromCompletionStage(() -> this.client
				.preparedQuery(sql)
				.executeBatch(args.map(Tuple::from).collect(Collectors.toList()))
				.toCompletionStage()
			)
			.map(rowSet -> rowSet.rowCount());
	}
	
	/**
	 * <p>
	 * Batched SQL operations created within {@link #batchQueries(java.util.function.Function)} to be able to set the group SqlClient provided by {@link SqlClientInternal#group(io.vertx.core.Handler)}
	 * when the batch published is subscribed.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	private static class BatchedSqlOperations extends AbstractSqlOperations {

		public BatchedSqlOperations() {
			super(null);
		}

		/**
		 * <p>
		 * Sets the group SqlClient.
		 * </p>
		 * 
		 * @param client the group SqlClient to set
		 */
		public void setClient(SqlClient client) {
			this.client = client;
		}
		
		@Override
		public <T> Publisher<T> batchQueries(Function<SqlOperations, Publisher<Publisher<T>>> function) {
			throw new IllegalStateException("Already in a batch");
		}
	}
}
