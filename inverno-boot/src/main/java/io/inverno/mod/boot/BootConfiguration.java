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

import io.inverno.mod.configuration.Configuration;

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
	 * Net client global configuration.
	 * </p>
	 * 
	 * @return the Net client global configuration
	 */
	BootNetClientConfiguration net_client();
	
	/**
	 * <p>
	 * Net server global configuration.
	 * </p>
	 * 
	 * @return the Net server global configuration
	 */
	BootNetServerConfiguration net_server();

	/**
	 * <p>
	 * Address resolver configuration.
	 * </p>
	 *
	 * @return the address resolver configuration
	 */
	BootNetAddressResolverConfiguration address_resolver();

	/**
	 * <p>
	 * Enables/Disables native transport when available.
	 * </p>
	 * 
	 * <p>
	 * Note that this settings impact both {@link io.inverno.mod.base.concurrent.Reactor Reactor} and {@link io.inverno.mod.base.net.NetService NetService} implementations.
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
	 * If sets to true and Vert.x core is on the module path, the reactor is backed by a {@link io.vertx.core.Vertx Vertx} instance.
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
