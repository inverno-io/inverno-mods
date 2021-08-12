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
import io.vertx.sqlclient.RowSet;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link SqlResult} implementation extracting SQL operation results
 * from a Vert.x {@link RowSet}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class GenericSqlResult implements SqlResult {

	private final RowSet<io.vertx.sqlclient.Row> rowSet;
	
	private RowMetadata rowMetadata;
	
	/**
	 * <p>
	 * Creates a generic SQL result.
	 * </p>
	 * 
	 * @param rowSet a Vert.x row set
	 */
	public GenericSqlResult(RowSet<io.vertx.sqlclient.Row> rowSet) {
		this.rowSet = rowSet;
	}

	@Override
	public Mono<Integer> rowsUpdated() {
		return Mono.fromSupplier(() -> this.rowSet.rowCount());
	}
	
	@Override
	public RowMetadata getRowMetadata() {
		if(this.rowMetadata == null) {
			this.rowMetadata = new GenericRowMetadata(this.rowSet.columnsNames());
		}
		return this.rowMetadata;
	}
	
	@Override
	public Publisher<Row> rows() {
		return Flux.fromIterable(this.rowSet).map(row -> new GenericRow(row));
	}
}
