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
package io.inverno.mod.sql.vertx;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.inverno.mod.sql.SqlOperations;
import io.inverno.mod.sql.TransactionalSqlOperations;
import io.inverno.mod.sql.vertx.internal.AbstractSqlClient;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A Vert.x pooled client SQL client wrapper.
 * </p>
 * 
 * <p>
 * This SQL client implementation supports operations pipelining but does not support transaction.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
public class PooledClientSqlClient extends AbstractSqlClient {

	/**
	 * <p>
	 * Creates a SQL client wrapper with the specified Vert.x pooled client.
	 * </p>
	 * 
	 * @param client a Vert.x pooled client
	 */
	public PooledClientSqlClient(io.vertx.sqlclient.SqlClient client) {
		super(client);
	}

	@Override
	public Mono<TransactionalSqlOperations> transaction() {
		throw new UnsupportedOperationException("Pooled clients don't support transactions");
	}

	@Override
	public <T> Publisher<T> transaction(Function<SqlOperations, Publisher<T>> function) {
		throw new UnsupportedOperationException("Pooled clients don't support transactions");
	}

	@Override
	public <T> Publisher<T> connection(Function<SqlOperations, Publisher<T>> function) {
		return function.apply(this);
	}
}
