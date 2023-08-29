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

package io.inverno.mod.http.client;

import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.http.base.HttpVersion;
import java.lang.module.ModuleDescriptor;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Configuration
public interface HttpClientConfiguration {
	
	static final Set<HttpVersion> DEFAULT_HTTP_PROTOCOL_VERSIONS = Set.of(HttpVersion.HTTP_2_0, HttpVersion.HTTP_1_1);
	
	static final String USER_AGENT = "Inverno" + Optional.ofNullable(HttpClientConfiguration.class.getModule().getDescriptor()).flatMap(ModuleDescriptor::version).map(version -> "/" + version).orElse("");
	
	/**
	 * <p>
	 * The set of HTTP protocols accepted by the client.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code HttpVersion.HTTP_2_0, HttpVersion.HTTP_1_1}
	 * </p>
	 * 
	 * @return a set of HTTP versions
	 */
	default Set<HttpVersion> http_protocol_versions() {
		return DEFAULT_HTTP_PROTOCOL_VERSIONS;
	}
	
	default boolean send_user_agent() {
		return true;
	}
	
	default String user_agent() {
		return USER_AGENT;
	}
	
	/**
	 * <p>
	 * Enables/Disables HTTPS.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean tls_enabled() {
		return false;
	}
	
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
	 * Indicates whether all server certificates should be trusted (including self-signed certificates).
	 * </p>
	 * 
	 * <p>
	 * Note that this overrides {@link #tls_trust_manager_factory()}.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}
	 * </p>
	 * 
	 * @return true to trust all server certificates
	 */
	default boolean tls_trust_all() {
		return false;
	}
	
	/**
	 * <p>
	 * The trust manager factory.
	 * </p>
	 * 
	 * <p>
	 * Note that this is overridden by {@link #tls_trust_all()}.
	 * </p>
	 * 
	 * @return the trust manager factory
	 */
	TrustManagerFactory tls_trust_manager_factory();
	
	/**
	 * <p>
	 * Sends Server Name Indication parameter during TLS handshake.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true if the client must send the hostname to the server during TLS handshake
	 */
	default boolean tls_send_sni() {
		return false;
	}
	
	/**
	 * <p>
	 * Enables/Disables HTTP compression.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
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
	 * Defaults to {@code false}.
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
	 * Defaults to {@code 0} which means all responses are compressed.
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
	 * Defaults to {@code 4}.
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
	 * Defaults to {@code -1}.
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
	 * Defaults to {@code 1} (TEXT).
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
	 * Defaults to {@code 6}.
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
	 * Defaults to {@code 15}.
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
	 * Defaults to {@code 8}.
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
	 * Defaults to {@code 6}.
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
	 * Defaults to {@code 15}.
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
	 * Defaults to {@code 8}.
	 * </p>
	 * 
	 * @return the gzip compression memory level
	 */
	default int compression_gzip_memLevel() {
		return 8;
	}
	
	/**
	 * <p>
	 * Zstandard compression block size (in bytes).
	 * </p>
	 * 
	 * <p>
	 * Defaults to 64KB.
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
	 * Defaults to {@code 3}.
	 * </p>
	 * 
	 * @return the zstd compression level
	 */
	default int compression_zstd_compressionLevel() {
		return 3;
	}
	
	/**
	 * <p>
	 * Zstandard compression max encode size (in bytes).
	 * </p>
	 * 
	 * <p>
	 * Defaults to 32MB.
	 * </p>
	 * 
	 * @return the zstd compression max encode size
	 */
	default int compression_zstd_maxEncodeSize() {
		return 1 << 10 + 0x0F;
	}
	
	/**
	 * <p>
	 * The HTTP/1.1 pipelining limit which corresponds to the maximum concurrent requests on a single connection.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 10}.
	 * </p>
	 * 
	 * @return 
	 */
	default Long http1_max_concurrent_requests() {
		return 10l;
	}
	
	/**
	 * <p>
	 * The HTTP/2 header table size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 4096}.
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
	 * Defaults to {@code 100}.
	 * </p>
	 * 
	 * @return max concurrent streams
	 */
	default Long http2_max_concurrent_streams() {
		return 100l;
	}

	/**
	 * <p>
	 * HTTP/2 initial window size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 65535}.
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
	 * Defaults to {@code 16384}.
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
	
	default Integer pool_max_size() {
		return 2;
	}
	
	default long pool_clean_period() {
		return 1000l;
	}
	
	Integer pool_buffer_size();
	
	Long pool_keep_alive_timeout();
	
	Long connect_timeout();
	
	Long request_timeout();
	
	/**
	 * <p>
	 * The number of event loops to allocate to the client.
	 * </p>
	 * 
	 * <p>
	 * If not specified, the number of thread allocated to the root event loop group
	 * shall be used.
	 * </p>
	 * 
	 * @return the number of threads to allocate
	 */
	Integer client_event_loop_group_size();
	
}