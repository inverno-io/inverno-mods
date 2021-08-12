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

import java.util.function.Supplier;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.sql.SqlClient;
import io.inverno.mod.sql.vertx.PoolSqlClient;
import io.inverno.mod.sql.vertx.VertxSqlClientConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.db2client.DB2ConnectOptionsConverter;
import io.vertx.db2client.DB2Pool;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mssqlclient.MSSQLConnectOptionsConverter;
import io.vertx.mssqlclient.MSSQLPool;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLConnectOptionsConverter;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgConnectOptionsConverter;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

/**
 * <p>
 * A pool SQL client bean exposed on the module.
 * </p>
 * 
 * <p>
 * This wrapper bean creates an SQL client based on the module configuration. It
 * determines the underlying Vert.x SQL client based on the database URI scheme,
 * corresponding client module must be present on the module path.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
@Wrapper @Bean( name = "sqlClient" )
public class PoolSqlClientWrapper implements Supplier<SqlClient> {

	private static final String DB2_SCHEME = "db2";
	
	private static final String MSSQL_SCHEME = "sqlserver";
	
	private static final String MYSQL_SCHEME = "mysql";
	private static final String MARIADB_SCHEME = "mariadb";
	
	private static final String POSTGRES_SCHEME = "postgres";
	
	private final VertxSqlClientConfiguration configuration;
	private final Vertx vertx;
	
	private SqlClient instance;
	
	/**
	 * <p>
	 * Creates a pool SQL client wrapper.
	 * </p>
	 * 
	 * @param configuration the Vert.x SQL client module configuration
	 * @param vertx         the Vert.x instance to use when creating the Vert.x SQL
	 *                      pool
	 */
	public PoolSqlClientWrapper(VertxSqlClientConfiguration configuration, Vertx vertx) {
		this.configuration = configuration;
		this.vertx = vertx;
	}

	@Override
	public SqlClient get() {
		return this.instance;
	}
	
	@Init
	public void init() {
		if(this.configuration.db_uri().startsWith(DB2_SCHEME)) {
			this.instance = createDB2Client(this.configuration, this.vertx);
		}
		else if(this.configuration.db_uri().startsWith(MSSQL_SCHEME)) {
			this.instance = createMSSQLClient(this.configuration, this.vertx);
		}
		else if(this.configuration.db_uri().startsWith(MYSQL_SCHEME) || this.configuration.db_uri().startsWith(MARIADB_SCHEME)) {
			this.instance = createMySQLClient(this.configuration, this.vertx);
		}
		else if(this.configuration.db_uri().startsWith(POSTGRES_SCHEME)) {
			this.instance = createPgClient(this.configuration, this.vertx);
		}
		else {
			throw new IllegalArgumentException("Unknown DB scheme: " + this.configuration.db_uri());
		}
	}
	
	@Destroy
	public void destroy() {
		this.instance.close().subscribe();
	}
	
	/**
	 * <p>
	 * Extracts the Vert.x pool options from the module's configuration.
	 * </p>
	 * 
	 * @param configuration the Vert.x SQL client module configuration
	 * 
	 * @return pool options
	 */
	private static PoolOptions createPoolOptions(VertxSqlClientConfiguration configuration) {
		PoolOptions options = new PoolOptions();
		
		options.setConnectionTimeout(configuration.pool_connectionTimeout());
		options.setConnectionTimeoutUnit(configuration.pool_connectionTimeoutUnit());
		options.setIdleTimeout(configuration.pool_idleTimeout());
		options.setIdleTimeoutUnit(configuration.pool_idleTimeoutUnit());
		options.setMaxSize(configuration.pool_maxSize());
		options.setMaxWaitQueueSize(configuration.pool_maxWaitQueueSize());
		options.setPoolCleanerPeriod(configuration.pool_poolCleanerPeriod());
		
		return options;
	}
	
	/**
	 * <p>
	 * Creates a DB2 pool SQL client.
	 * </p>
	 * 
	 * @param configuration the Vert.x SQL client module configuration
	 * @param vertx         the Vert.x instance to use
	 * 
	 * @return a pool SQL client
	 */
	private static SqlClient createDB2Client(VertxSqlClientConfiguration configuration, Vertx vertx) {
		DB2ConnectOptions connectOptions = DB2ConnectOptions.fromUri(configuration.db_uri());
		
		String json_options = configuration.db_json_options();
		if(json_options != null) {
			DB2ConnectOptionsConverter.fromJson(new JsonObject(configuration.db_json_options()), connectOptions);
		}
		
		PoolOptions poolOptions = createPoolOptions(configuration);
		
		return new PoolSqlClient(DB2Pool.pool(vertx, connectOptions, poolOptions));
	}
	
	/**
	 * <p>
	 * Creates a MSSQL pool SQL client.
	 * </p>
	 * 
	 * @param configuration the Vert.x SQL client module configuration
	 * @param vertx         the Vert.x instance to use
	 * 
	 * @return a pool SQL client
	 */
	private static SqlClient createMSSQLClient(VertxSqlClientConfiguration configuration, Vertx vertx) {
		MSSQLConnectOptions connectOptions = MSSQLConnectOptions.fromUri(configuration.db_uri());
		
		String json_options = configuration.db_json_options();
		if(json_options != null) {
			MSSQLConnectOptionsConverter.fromJson(new JsonObject(configuration.db_json_options()), connectOptions);
		}
		
		PoolOptions poolOptions = createPoolOptions(configuration);
		
		return new PoolSqlClient(MSSQLPool.pool(vertx, connectOptions, poolOptions));
	}
	
	/**
	 * <p>
	 * Creates a MySQL pool SQL client.
	 * </p>
	 * 
	 * @param configuration the Vert.x SQL client module configuration
	 * @param vertx         the Vert.x instance to use
	 * 
	 * @return a pool SQL client
	 */
	private static SqlClient createMySQLClient(VertxSqlClientConfiguration configuration, Vertx vertx) {
		MySQLConnectOptions connectOptions = MySQLConnectOptions.fromUri(configuration.db_uri());
		
		String json_options = configuration.db_json_options();
		if(json_options != null) {
			MySQLConnectOptionsConverter.fromJson(new JsonObject(configuration.db_json_options()), connectOptions);
		}
		
		PoolOptions poolOptions = createPoolOptions(configuration);
		
		return new PoolSqlClient(MySQLPool.pool(vertx, connectOptions, poolOptions));
	}
	
	/**
	 * <p>
	 * Creates a Postgres pool SQL client.
	 * </p>
	 * 
	 * @param configuration the Vert.x SQL client module configuration
	 * @param vertx         the Vert.x instance to use
	 * 
	 * @return a pool SQL client
	 */
	private static SqlClient createPgClient(VertxSqlClientConfiguration configuration, Vertx vertx) {
		PgConnectOptions connectOptions = PgConnectOptions.fromUri(configuration.db_uri());
		
		String json_options = configuration.db_json_options();
		if(json_options != null) {
			PgConnectOptionsConverter.fromJson(new JsonObject(configuration.db_json_options()), connectOptions);
		}
		
		PoolOptions poolOptions = createPoolOptions(configuration);
		
		return new PoolSqlClient(PgPool.pool(vertx, connectOptions, poolOptions));
	}
}
