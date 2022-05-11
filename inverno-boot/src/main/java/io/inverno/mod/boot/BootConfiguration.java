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
package io.inverno.mod.boot;

import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.configuration.Configuration;
import io.vertx.core.Vertx;

/**
 * <p>
 * Boot module configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Configuration( name = "configuration" )
public interface BootConfiguration {

	/**
	 * <p>
	 * Enables/Disables socket address re-use .
	 * </p>
	 * 
	 * <p>
	 * Defaults to true.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean reuse_address() {
		return true;
	};

	/**
	 * <p>
	 * Enables/Disables socket port re-use.
	 * </p>
	 * 
	 * <p>
	 * Defaults to true.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean reuse_port() {
		return true;
	}

	/**
	 * <p>
	 * Enables/Disables socket keep alive.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean keep_alive() {
		return false;
	}

	/**
	 * <p>
	 * The accept backlog.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 1024.
	 * </p>
	 * 
	 * @return the accept backlog
	 */
	default Integer accept_backlog() {
		return 1024;
	}
	
	/**
	 * <p>
	 * Enables/Disables TCP no delay.
	 * </p>
	 * 
	 * <p>
	 * Defaults to true.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean tcp_no_delay() {
		return true;
	}

	/**
	 * <p>
	 * Enables/Disables TCP quick ack.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean tcp_quickack() {
		return false;
	}

	/**
	 * <p>
	 * Enables/Disables TCP cork.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean tcp_cork() {
		return false;
	}

	/**
	 * <p>
	 * Enables/Disables native transport when available.
	 * </p>
	 * 
	 * <p>
	 * Note that this settings impact both {@link Reactor} and {@link NetService}
	 * implementations.
	 * </p>
	 * 
	 * <p>
	 * Defaults to true.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean prefer_native_transport() {
		return true;
	}
	
	/**
	 * <p>
	 * Enables/Disables Vert.x reactor when available.
	 * </p>
	 * 
	 * <p>
	 * If sets to true and Vert.x core is on the module path, the reactor is backed
	 * by a {@link Vertx} instance.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean reactor_prefer_vertx() {
		return false;
	}

	/**
	 * <p>
	 * The number of threads to allocate to the reactor event loop group.
	 * </p>
	 * 
	 * <p>
	 * Defaults to twice the number of processors available to the JVM.
	 * </p>
	 * 
	 * @return the number of threads to allocate
	 */
	default int reactor_event_loop_group_size() {
		return Runtime.getRuntime().availableProcessors() * 2;
	}
}
