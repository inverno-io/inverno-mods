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

import java.util.concurrent.TimeUnit;

import io.inverno.mod.configuration.Configuration;
import io.vertx.sqlclient.PoolOptions;

/**
 * <p>
 * Vert.x SQL client module configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
@Configuration
public interface VertxSqlClientConfiguration {

	/**
	 * <p>
	 * The URI to connect to the RDBMS.
	 * </p>
	 * 
	 * @return a database URI
	 */
	String db_uri();
	
	/**
	 * <p>
	 * A JSON containing database connection options.
	 * </p>
	 * 
	 * @return database connection options
	 */
	String db_json_options();
	
	/**
	 * <p>
	 * The maximum pool size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link PoolOptions#DEFAULT_MAX_SIZE}
	 * </p>
	 * 
	 * @return the maximum pool size
	 */
	default int pool_maxSize() {
		return PoolOptions.DEFAULT_MAX_SIZE;
	}
	
	/**
	 * <p>
	 * The maximum wait queue size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link PoolOptions#DEFAULT_MAX_WAIT_QUEUE_SIZE}
	 * </p>
	 * 
	 * @return the maximum wait queue size
	 */
	default int pool_maxWaitQueueSize() {
		return PoolOptions.DEFAULT_MAX_WAIT_QUEUE_SIZE;
	}
	
	/**
	 * <p>
	 * The idle timeout of a pooled connection.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link PoolOptions#DEFAULT_IDLE_TIMEOUT}
	 * </p>
	 * 
	 * @return the pooled connection idle timeout
	 */
	default int pool_idleTimeout() {
		return PoolOptions.DEFAULT_IDLE_TIMEOUT;
	}
	
	/**
	 * <p>
	 * The idle timeout unit.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link PoolOptions#DEFAULT_IDLE_TIMEOUT_TIME_UNIT}
	 * </p>
	 * 
	 * @return the idle timeout unit
	 */
	default TimeUnit pool_idleTimeoutUnit() {
		return PoolOptions.DEFAULT_IDLE_TIMEOUT_TIME_UNIT;
	}
	
	/**
	 * <p>
	 * The connection pool cleaner period in ms.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link PoolOptions#DEFAULT_POOL_CLEANER_PERIOD}
	 * </p>
	 * 
	 * @return the pool cleaner period in ms
	 */
	default int pool_poolCleanerPeriod() {
		return PoolOptions.DEFAULT_POOL_CLEANER_PERIOD;
	}
	
	/**
	 * <p>
	 * The amount of time a client will wait for a connection from the pool.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link PoolOptions#DEFAULT_CONNECTION_TIMEOUT}
	 * </p>
	 * 
	 * @return the connection timeout
	 */
	default int pool_connectionTimeout() {
		return PoolOptions.DEFAULT_CONNECTION_TIMEOUT;
	}
	
	/**
	 * <p>
	 * The connection timeout unit.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link PoolOptions#DEFAULT_CONNECTION_TIMEOUT_TIME_UNIT}
	 * </p>
	 * 
	 * @return the connection timeout unit
	 */
	default TimeUnit pool_connectionTimeoutUnit() {
		return PoolOptions.DEFAULT_CONNECTION_TIMEOUT_TIME_UNIT;
	}
}
