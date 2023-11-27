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
package io.inverno.mod.redis.lettuce;

import io.inverno.mod.configuration.Configuration;

/**
 * <p>
 * Lettuce Redis client module configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
@Configuration( name = "configuration" )
public interface LettuceRedisClientConfiguration {

	/**
	 * <p>
	 * The number of reactor threads to allocate to the Redis client event loop group.
	 * </p>
	 * 
	 * <p>
	 * Defaults to number of processors available to the JVM.
	 * </p>
	 * 
	 * @return the number of threads to allocate
	 */
	default int event_loop_group_size() {
		return Runtime.getRuntime().availableProcessors();
	}
	
	/**
	 * <p>
	 * The Redis datastore URI.
	 * </p>
	 * 
	 * @return a Redis datastore URI
	 */
	String uri();

	/**
	 * <p>
	 * Redis datastore user name.
	 * </p>
	 * 
	 * @return a user name
	 */
	String username();
	
	/**
	 * <p>
	 * Redis datastore password.
	 * </p>
	 * 
	 * @return a password
	 */
	String password();
	
	/**
	 * <p>
	 * Redis datastore host.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code localhost}.
	 * </p>
	 * 
	 * @return a host
	 */
	default String host() {
		return "localhost";
	}
	
	/**
	 * <p>
	 * Redis datastore port.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 6379}.
	 * </p>
	 * 
	 * @return a host
	 */
	default int port() {
		return 6379;
	}
	
	/**
	 * <p>
	 * Indicates whether connection is secured.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true to activate TLS, false otherwise
	 */
	default boolean tls() {
		return false;
	}
	
	/**
	 * <p>
	 * The database index.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 0}.
	 * </p>
	 * 
	 * @return a database index
	 */
	default int database() {
		return 0;
	}
	
	/**
	 * <p>
	 * The Redis client name.
	 * </p>
	 * 
	 * @return a client name
	 */
	String client_name();
	
	/**
	 * <p>
	 * Redis connection timeout in milliseconds.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 60000}.
	 * </p>
	 * 
	 * @return the connection timeout
	 */
	default long timeout() {
		return 60000l;
	}
	
	/**
	 * <p>
	 * Maximum active connection in the Redis client connection pool.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 8}.
	 * </p>
	 * 
	 * @return the maximum active connection in the pool
	 */
	default int pool_max_active() {
		return 8;
	}
	
	/**
	 * <p>
	 * Minimum idle connection in the Redis client connection pool.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 0}.
	 * </p>
	 * 
	 * @return the minimum idle connection in the pool
	 */
	default int pool_min_idle() {
		return 0;
	}
	
	/**
	 * <p>
	 * Maximum idle connection in the Redis client connection pool.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 8}.
	 * </p>
	 * 
	 * @return the maximum idle connection in the pool
	 */
	default int pool_max_idle() {
		return 8;
	}
}
