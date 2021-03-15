/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.http.server;

import java.net.URI;

import io.winterframework.mod.configuration.Configuration;

/**
 * <p>
 * HTTP server module configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Configuration
public interface HttpServerConfiguration {

	/**
	 * <p>
	 * The host name of the server socket address.
	 * </p>
	 * 
	 * <p>
	 * Defaults to "0.0.0.0".
	 * </p>
	 * 
	 * @return the server host name
	 */
	default String server_host() {
		return "0.0.0.0";
	}

	/**
	 * <p>
	 * The port of the server socket address.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 8080.
	 * </p>
	 * 
	 * @return the server port
	 */
	default int server_port() {
		return 8080;
	}

	/**
	 * <p>
	 * The type of key store.
	 * </p>
	 * 
	 * <p>
	 * Defaults to "JKS".
	 * </p>
	 * 
	 * @return the key store type
	 */
	default String key_store_type() {
		return "JKS";
	}

	/**
	 * <p>
	 * Enables/Disables HTTPS.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean ssl_enabled() {
		return false;
	}

	/**
	 * <p>
	 * Enables/Disables H2C.
	 * </p>
	 * 
	 * <p>
	 * This only applies when SSL is disabled, otherwise {@link #h2_enabled()} is
	 * considered.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean h2c_enabled() {
		return false;
	}
	
	/**
	 * <p>
	 * Enables/Disables HTTP/2.
	 * </p>
	 * 
	 * <p>
	 * This only applies when SSL is enabled, otherwise {@link #h2c_enabled()} is
	 * considered.
	 * </p>
	 * 
	 * <p>
	 * Defaults to true.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean h2_enabled() {
		return true;
	}

	/**
	 * <p>
	 * The path to the key store.
	 * </p>
	 * 
	 * @return the key store URI
	 */
	URI key_store();

	/**
	 * <p>
	 * The password of the key store.
	 * </p>
	 * 
	 * @return a password
	 */
	String key_store_password();

	/**
	 * <p>
	 * The alias of the key in the key store.
	 * </p>
	 * 
	 * @return a key alias
	 */
	String key_alias();

	/**
	 * <p>
	 * The list of ciphers to include.
	 * </p>
	 * 
	 * @return a list of ciphers
	 */
	String[] ssl_ciphers_includes();

	/**
	 * <p>
	 * The list of ciphers to exclude.
	 * </p>
	 * 
	 * @return a list of ciphers
	 */
	String[] ssl_ciphers_excludes();

	/**
	 * <p>
	 * The HTTP/2 header table size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 4096.
	 * </p>
	 * 
	 * @return the header table size
	 */
	default Long http2_header_table_size() {
		return 4096l;
	}

	/**
	 * <p>
	 * HTTP/2 max concurrent streams.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 100.
	 * </p>
	 * 
	 * @return max concurrent streams
	 */
	default Integer http2_max_concurrent_streams() {
		return 100;
	}

	/**
	 * <p>
	 * HTTP/2 initial window size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 65535.
	 * </p>
	 * 
	 * @return initial window size
	 */
	default Integer http2_initial_window_size() {
		return 65535;
	}

	/**
	 * <p>
	 * HTTP/2 max frame size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 16384.
	 * </p>
	 * 
	 * @return max frame size
	 */
	default Integer http2_max_frame_size() {
		return 16384;
	}

	/**
	 * <p>
	 * HTTP/2 max header list size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link Integer#MAX_VALUE}.
	 * </p>
	 * 
	 * @return max header list size
	 */
	default Integer http2_max_header_list_size() {
		return Integer.MAX_VALUE;
	}
}