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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.inverno.mod.sql.Row;
import io.inverno.mod.sql.RowMetadata;
import io.inverno.mod.sql.SqlResult;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.RowStream;
import reactor.core.publisher.Mono;

/**
 * <p>
 * {@link SqlResult} implementation extracting SQL operation results from a
 * Vert.x {@link RowStream}.
 * </p>
 * 
 * <p>
 * Unlike {@link GenericSqlResult}, this implementation supports results
 * streaming. It wraps a {@link RowStream} in a {@link Publisher} of rows but
 * still uses a {@link RowSet} to expose the number of rows affected by the
 * operation, as a result it is not possible to invoke both
 * {@link StreamSqlResult#rows()} and {@link StreamSqlResult#rowsUpdated()}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class StreamSqlResult implements SqlResult {

	private final Mono<RowSet<io.vertx.sqlclient.Row>> rowSet;
	private final Mono<RowStream<io.vertx.sqlclient.Row>> rowStream;
	
	/**
	 * <p>
	 * Creates a stream SQL result.
	 * </p>
	 * 
	 * @param rowSet    a Mono emitting a result row set
	 * @param rowStream a Mono emitting a result stream
	 */
	public StreamSqlResult(Mono<RowSet<io.vertx.sqlclient.Row>> rowSet, Mono<RowStream<io.vertx.sqlclient.Row>> rowStream) {
		this.rowSet = rowSet;
		this.rowStream = rowStream;
	}
	
	@Override
	public RowMetadata getRowMetadata() {
		// TODO if we use for the rowSet we probably won't be able to create a stream afterwards... 
		return null;
	}

	@Override
	public Mono<Integer> rowsUpdated() {
		return this.rowSet.map(RowSet::rowCount);
	}

	@Override
	public Publisher<Row> rows() {
		// TODO the transaction should be committed when the stream is closed
		return this.rowStream.flatMapMany(RowStreamPublisher::new);
	}

	/**
	 * <p>
	 * A Publisher of rows that wraps a {@link RowStream}.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.2
	 */
	static class RowStreamPublisher implements Publisher<Row>, Subscription {

		private final RowStream<io.vertx.sqlclient.Row> rowStream;
		
		private int subscriptions;
		
		/**
		 * <p>
		 * Creates a row stream publisher.
		 * </p>
		 * 
		 * @param rowStream the underlying row stream
		 */
		public RowStreamPublisher(RowStream<io.vertx.sqlclient.Row> rowStream) {
			this.rowStream = rowStream;
		}
		
		@Override
		public void request(long n) {
			synchronized(this) {
				this.rowStream.pause();
				this.rowStream.fetch(n);
			}
		}

		@Override
		public void cancel() {
			synchronized(this) {
				this.rowStream.close();
			}
		}
		
		@Override
		public void subscribe(Subscriber<? super Row> subscriber) {
			synchronized(this) {
				if(this.subscriptions > 0) {
					subscriber.onError(new IllegalStateException("ReadStreamPublisher allows only a single Subscriber"));
				}
				else {
					this.subscriptions++;
					// we can create the stream now and set the handler
					this.rowStream.pause(); // switch to fetch mode
					
					this.rowStream.exceptionHandler(subscriber::onError);
					this.rowStream.endHandler(ign -> subscriber.onComplete());
					this.rowStream.handler(row -> subscriber.onNext(new GenericRow(row)));
					
					// GO
					subscriber.onSubscribe(this);
				}
			}
		}
	}
}
