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

import io.inverno.mod.sql.Row;
import io.inverno.mod.sql.RowMetadata;
import io.inverno.mod.sql.SqlResult;
import io.vertx.sqlclient.Cursor;
import reactor.core.publisher.Mono;

/**
 * <p>
 * {@link SqlResult} implementation extracting SQL operation results from a Vert.x {@link Cursor}.
 * </p>
 * 
 * <p>
 * Unlike {@link GenericSqlResult}, this implementation supports results streaming. It uses a {@link Cursor} to create a {@link Publisher} of rows which must be subscribed to be able to access row
 * metadata.
 * </p>
 * 
 * <p>
 * Both {@link #rowsUpdated()} and {@link #rows()} methods return a publisher that consumes the stream and as result it is not possible to invoke both.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class StreamSqlResult implements SqlResult {

	private final Cursor cursor;
	private final int fetchSize;
	
	private RowMetadata rowMetadata;

	/**
	 * <p>
	 * Creates a stream SQL result.
	 * </p>
	 * 
	 * @param cursor the cursor
	 */
	public StreamSqlResult(Cursor cursor, int fetchSize) {
		this.cursor = cursor;
		this.fetchSize = fetchSize;
	}
	
	/**
	 * <p>
	 * Returns null if the rows publisher hasn't been subscribed. 
	 * </p>
	 */
	@Override
	public RowMetadata getRowMetadata() {
		return this.rowMetadata;
	}

	/**
	 * <p>
	 * The returned Mono actually consumed the stream, as a result it is not possible to invoke both {@link #rows()} and {@code rowsUpdated()}.
	 * </p>
	 */
	@Override
	public Mono<Integer> rowsUpdated() {
		return Mono.fromCompletionStage(this.cursor.read(this.fetchSize).toCompletionStage())
			.map(rowSet -> {
				if(this.rowMetadata == null) {
					this.rowMetadata = new GenericRowMetadata(rowSet.columnsNames());
				}
				return rowSet.rowCount();
			})
			.repeat(this.cursor::hasMore)
			.reduce(0, Integer::sum)
			.doFinally(ign -> this.cursor.close());
	}

	@Override
	public Publisher<Row> rows() {
		return Mono.fromCompletionStage(() -> this.cursor.read(this.fetchSize).toCompletionStage())
			.repeat(this.cursor::hasMore)
			.flatMapIterable(rowSet -> {
				if(this.rowMetadata == null) {
					this.rowMetadata = new GenericRowMetadata(rowSet.columnsNames());
				}
				return rowSet;
			})
			.map(row -> (Row)new GenericRow(row))
			.doFinally(ign -> this.cursor.close());
	}
}
