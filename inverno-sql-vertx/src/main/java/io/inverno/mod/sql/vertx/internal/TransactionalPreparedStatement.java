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

import io.inverno.mod.sql.PreparedStatement;
import io.vertx.sqlclient.SqlConnection;

/**
 * <p>
 * A {@link PreparedStatement} implementation that runs in a transaction with
 * support for result streaming.
 * </p>
 * 
 * <p>
 * Note that result streaming doesn't apply to batch operations.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class TransactionalPreparedStatement extends ConnectionPreparedStatement {

	/**
	 * <p>
	 * Creates a transactional prepared statement.
	 * </p>
	 * 
	 * @param connection the underlying Vert.x connection with an opened transaction
	 * @param sql        a SQL operation
	 */
	public TransactionalPreparedStatement(SqlConnection connection, String sql) {
		super(connection, sql);
	}
}
