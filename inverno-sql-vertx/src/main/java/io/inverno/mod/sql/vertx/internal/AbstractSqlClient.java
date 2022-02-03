/*
 * Copyright 2022 Jeremy KUHN
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

import io.inverno.mod.sql.SqlClient;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base {@link SqlClient} Vert.x implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public abstract class AbstractSqlClient extends AbstractSqlOperations implements SqlClient {

	/**
	 * <p>
	 * Creates SQL client with the specified Vert.x SQL client.
	 * </p>
	 * 
	 * @param client a Vert.x SQL client
	 */
	public AbstractSqlClient(io.vertx.sqlclient.SqlClient client) {
		super(client);
	}

	@Override
	public Mono<Void> close() {
		return Mono.fromCompletionStage(() -> this.client.close().toCompletionStage());
	}
}
