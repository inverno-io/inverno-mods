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
package io.inverno.mod.http.server;

import java.net.URI;

import io.inverno.mod.configuration.Configuration;

/**
 * <p>
 * HTTP server module configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Configuration( name = "configuration" )
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
	 * The number of event loops to allocate to the server.
	 * </p>
	 * 
	 * <p>
	 * If not specified, the number of thread allocated to the root event loop group
	 * shall be used.
	 * </p>
	 * 
	 * @return the number of threads to allocate
	 */
	Integer server_event_loop_group_size();

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
	 * Enables/Disables HTTP compression.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if compression is enabled, false otherwise
	 */
	default boolean compression_enabled() {
		return false;
	}
	
	/**
	 * <p>
	 * Enables/Disables HTTP decompression.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true if decompression is enabled, false otherwise
	 */
	default boolean decompression_enabled() {
		return false;
	}
	
	/**
	 * <p>
	 * The threshold beyond which the response body should be compressed.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 0 which means all responses are compressed.
	 * </p>
	 *
	 * @return the compression content size threshold
	 */
	default int compression_contentSizeThreshold() {
		return 0;
	}
	
	// Brotli lib is currently an unnamed module so we can't configure it...
	/**
	 * <p>
	 * Brotly compression quality.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 4.
	 * </p>
	 * 
	 * @return the brotli compression quality
	 */
	/*default int compression_brotli_quality() {
		return 4;
	}*/
	
	/**
	 * <p>
	 * Brotly compression window.
	 * </p>
	 * 
	 * <p>
	 * Defaults to -1.
	 * </p>
	 * 
	 * @return the brotli compression window
	 */
	/*default int compression_brotli_window() {
		return -1;
	}*/

	/**
	 * <p>
	 * Brotly compression mode (0=GENERIC, 1=TEXT, 2=FONT).
	 * </p>
	 * 
	 * <p>
	 * Defaults to 1 (TEXT).
	 * </p>
	 * 
	 * @return the brotli compression mode
	 */
	/*default int compression_brotli_mode() {
		return 1;
	}*/
	
	/**
	 * <p>
	 * Deflate compression level.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 6.
	 * </p>
	 * 
	 * @return the deflate compression level
	 */
	default int compression_deflate_compressionLevel() {
		return 6;
	}
	
	/**
	 * <p>
	 * Deflate compression window bits.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 15.
	 * </p>
	 * 
	 * @return the deflate compression window bits
	 */
	default int compression_deflate_windowBits() {
		return 15;
	}
	
	/**
	 * <p>
	 * Deflate compression memory level.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 8.
	 * </p>
	 * 
	 * @return the deflate compression memory level bits
	 */
	default int compression_deflate_memLevel() {
		return 8;
	}
	
	/**
	 * <p>
	 * Gzip compression level.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 6.
	 * </p>
	 * 
	 * @return the gzip compression level
	 */
	default int compression_gzip_compressionLevel() {
		return 6;
	}
	
	/**
	 * <p>
	 * Gzip compression window bits.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 15.
	 * </p>
	 * 
	 * @return the gzip compression window bits
	 */
	default int compression_gzip_windowBits() {
		return 15;
	}
	
	/**
	 * <p>
	 * Gzip compression memory level.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 8.
	 * </p>
	 * 
	 * @return the gzip compression memory level
	 */
	default int compression_gzip_memLevel() {
		return 8;
	}
	
	/**
	 * <p>
	 * Zstandard compression bock size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 64 KB.
	 * </p>
	 * 
	 * @return the zstd compression block size
	 */
	default int compression_zstd_blockSize() {
		return 1 << 16;
	}
	
	/**
	 * <p>
	 * Zstandard compression level.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 3.
	 * </p>
	 * 
	 * @return the zstd compression level
	 */
	default int compression_zstd_compressionLevel() {
		return 3;
	}
	
	/**
	 * <p>
	 * Zstandard compression max encode size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to 32 MB.
	 * </p>
	 * 
	 * @return the zstd compression max encode size
	 */
	default int compression_zstd_maxEncodeSize() {
		return 1 << 10 + 0x0F;
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
	default boolean tls_enabled() {
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
//	String key_alias();

	/**
	 * <p>
	 * The list of ciphers to include.
	 * </p>
	 * 
	 * @return a list of ciphers
	 */
	String[] tls_ciphers_includes();

	/**
	 * <p>
	 * The list of ciphers to exclude.
	 * </p>
	 * 
	 * @return a list of ciphers
	 */
	String[] tls_ciphers_excludes();

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