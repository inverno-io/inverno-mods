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
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import io.vertx.sqlclient.spi.Driver;
import java.util.List;
import java.util.ServiceLoader;

/**
 * <p>
 * A pool SQL client bean exposed on the module.
 * </p>
 * 
 * <p>
 * This wrapper bean creates an SQL client based on the module configuration. It determines the underlying Vert.x SQL client based on the database URI scheme, corresponding client module must be
 * present on the module path.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
@Wrapper @Bean( name = "vertxSqlClient" )
public class PoolSqlClientWrapper implements Supplier<SqlClient> {
	
	private final VertxSqlClientConfiguration configuration;
	private final Vertx vertx;
	
	private SqlClient instance;
	
	/**
	 * <p>
	 * Creates a pool SQL client wrapper.
	 * </p>
	 * 
	 * @param configuration the Vert.x SQL client module configuration
	 * @param vertx         the Vert.x instance to use when creating the Vert.x SQL pool
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
		String db_uri = this.configuration.db_uri();
		
		ServiceLoader<Driver> loader;
		if(PoolSqlClientWrapper.class.getModule().isNamed()) {
			loader = ServiceLoader.load(PoolSqlClientWrapper.class.getModule().getLayer(), Driver.class);
		}
		else {
			loader = ServiceLoader.load(Driver.class, PoolSqlClientWrapper.class.getClassLoader());
		}
		
		for (Driver driver : loader) {
			SqlConnectOptions connectOptions = driver.parseConnectionUri(db_uri);
			if(connectOptions != null) {
				String db_json_options = this.configuration.db_json_options();
				if(db_json_options != null) {
					connectOptions.merge(new JsonObject(this.configuration.db_json_options()));
				}
				connectOptions.setUser(this.configuration.db_user());
				connectOptions.setPassword(this.configuration.db_password());
				
				PoolOptions poolOptions = createPoolOptions(this.configuration);
				
				this.instance = new PoolSqlClient(driver.createPool(this.vertx, List.of(connectOptions), poolOptions));
				return;
			}
		}
		throw new IllegalArgumentException("Unknown DB scheme: " + this.configuration.db_uri());
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
}
