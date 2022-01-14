import io.vertx.core.Vertx;

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

/**
 * <p>
 * The Inverno Vert.x SQL Client module which provides a SQL client based on
 * Vert.x.
 * </p>
 * 
 * <p>
 * This module exposes a pool based SQL client which is automatically created
 * using the module's configuration which provides connection and pooling
 * options. This client can then be used within an application to execute SQL
 * operations on the RDBMS.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt>vertxSqlClientConfiguration</dt>
 * <dd>the Vert.x SQL client module configuration</dd>
 * <dt>reactor</dt>
 * <dd>the Inverno reactor (required)</dd>
 * <dt>vertx</dt>
 * <dd>A {@link Vertx} instance that overrides the module's internal
 * instance</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt>vertxSqlClientConfiguration</dt>
 * <dd>the Vert.x SQL client module configuration</dd>
 * <dt>vertxSqlClient</dt>
 * <dd>the Vert.x  pool SQL client to execute SQL operations on the RDBMS</dd>
 * </dl>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.sql.vertx {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	requires io.inverno.mod.base;
	requires transitive io.inverno.mod.configuration;
	requires transitive io.inverno.mod.sql;
	
	requires transitive reactor.core;
	requires transitive org.reactivestreams;

	requires java.sql;
	requires io.vertx.core;
	requires transitive io.vertx.client.sql;
	
	exports io.inverno.mod.sql.vertx;
	
	uses io.vertx.sqlclient.spi.Driver;
}