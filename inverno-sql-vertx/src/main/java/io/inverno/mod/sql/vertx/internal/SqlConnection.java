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

import reactor.core.publisher.Mono;

/**
 * <p>
 * SQL operations executed on a single SQL connection.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class SqlConnection extends AbstractSqlOperations {

	/**
	 * <p>
	 * Creates a SQL connection with the specified underlying Vert.x SQL connection.
	 * </p>
	 * 
	 * @param connection a Vert.x SQL connection
	 */
	public SqlConnection(io.vertx.sqlclient.SqlConnection connection) {
		super(connection);
	}

	/**
	 * <p>
	 * Closes the connection.
	 * </p>
	 *
	 * <p>
	 * If the connection was retrieved from a pool, this simply returns the
	 * connection to the pool.
	 * </p>
	 * 
	 * @return a Mono that completes when the connection is closed
	 */
	public Mono<Void> close() {
		return Mono.fromCompletionStage(() -> this.client.close().toCompletionStage());
	}
}
