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

import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.ws.WebSocketExchange;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.TrustManagerFactory;

/**
 * <p>
 * HTTP client module configuration.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
@Configuration
public interface HttpClientConfiguration {
	
	/**
	 * Default HTTP versions accepted by the client.
	 */
	static final Set<HttpVersion> DEFAULT_HTTP_PROTOCOL_VERSIONS = Set.of(HttpVersion.HTTP_2_0, HttpVersion.HTTP_1_1);
	
	/**
	 * Default user agent.
	 */
	static final String USER_AGENT = "Inverno" + Optional.ofNullable(HttpClientConfiguration.class.getModule().getDescriptor()).flatMap(ModuleDescriptor::version).map(version -> "/" + version).orElse("");
	
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
	
	/**
	 * <p>
	 * The maximum size of the client connection pool.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 2}.
	 * </p>
	 * 
	 * @return the maximum size of the pool
	 */
	default Integer pool_max_size() {
		return 2;
	}
	
	/**
	 * <p>
	 * The pool's clean period in milliseconds.
	 * </p>
	 * 
	 * <p>
	 * This specifies the frequency at which unnecessary connections are removed from the active pool and parked until either a request burst requires the connection to be reinstated or until the keep
	 * alive timeout is reached in which case the connection is closed and definitely removed from the pool.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 1000} (1 seconds).
	 * </p>
	 * 
	 * @return the pool's clean period
	 */
	default long pool_clean_period() {
		return 1000l;
	}
	
	/**
	 * <p>
	 * The pool's request buffer size. 
	 * </p>
	 * 
	 * <p>
	 * This represents the limit beyond which requests can't be buffered and are rejectded ({@code null} means no limit).
	 * </p>
	 * 
	 * @return the pool's request buffer size
	 */
	Integer pool_buffer_size();
	
	/**
	 * <p>
	 * The pool's connection keep alive timeout in milliseconds.
	 * </p>
	 * 
	 * <p>
	 * The represents the time beyond which an inactive connection is closed and removed from the pool ({@code null} means no timeout).
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 60000}.
	 * </p>
	 * 
	 * @return the pool
	 */
	default Long pool_keep_alive_timeout() {
		return 60000l;
	}
	
	/**
	 * <p>
	 * The pool connection timeout in milliseconds.
	 * </p>
	 * 
	 * <p>
	 * Note that this timeout specifies the maximum time to wait for the connection pool to return a connection, this differs from {@link NetClientConfiguration#connect_timeout() } which specifies the
	 * connection timeout at the socket level.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 60000}.
	 * </p>
	 * 
	 * @return the pool connection timeout
	 */
	default long pool_connect_timeout() {
		return 60000l;
	}
	
	/**
	 * <p>
	 * The request timeout in milliseconds.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 60000}.
	 * </p>
	 * 
	 * @return the connection timeout
	 */
	default long request_timeout() {
		return 60000l;
	}
	
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
	
	/**
	 * <p>
	 * Sends {@code user-agent} header.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code true}
	 * </p>
	 * 
	 * @return true to send the user agent, false otherwise.
	 */
	default boolean send_user_agent() {
		return true;
	}
	
	/**
	 * <p>
	 * The value of the {@code user-agent} header to send.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code Inverno/x.y}
	 * </p>
	 * 
	 * @return the user agent
	 */
	default String user_agent() {
		return USER_AGENT;
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
	default int compression_brotli_quality() {
		return 4;
	}
	
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
	default int compression_brotli_window() {
		return -1;
	}

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
	default int compression_brotli_mode() {
		return 1;
	}
	
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
	 * The path to the key store.
	 * </p>
	 * 
	 * <p>
	 * Note that this overrides both {@link #tls_trust_manager_factory()} and {@link #tls_trust_all()}.
	 * </p>
	 * 
	 * @return the key store URI
	 */
	URI tls_key_store();
	
	/**
	 * <p>
	 * The type of key store.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code JKS}.
	 * </p>
	 * 
	 * @return the key store type
	 */
	default String tls_key_store_type() {
		return "JKS";
	}

	/**
	 * <p>
	 * The password of the key store.
	 * </p>
	 * 
	 * @return a password
	 */
	String tls_key_store_password();
	
	/**
	 * <p>
	 * The alias of the key in the key store.
	 * </p>
	 * 
	 * @return a key alias
	 */
	String tls_key_alias();
	
	/**
	 * <p>
	 * The password for the alias of the key in the key store.
	 * </p>
	 * 
	 * @return a password
	 */
	String tls_key_alias_password();
	
	/**
	 * <p>
	 * The path to the key store.
	 * </p>
	 * 
	 * @return the key store URI
	 */
	URI tls_trust_store();
	
	/**
	 * <p>
	 * The type of key store.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code JKS}.
	 * </p>
	 * 
	 * @return the key store type
	 */
	default String tls_trust_store_type() {
		return "JKS";
	}
	
	/**
	 * <p>
	 * The password of the key store.
	 * </p>
	 * 
	 * @return a password
	 */
	String tls_trust_store_password();
	
	/**
	 * <p>
	 * The trust manager factory.
	 * </p>
	 * 
	 * <p>
	 * Note that this overrides {@link #tls_trust_all()}.
	 * </p>
	 * 
	 * @return the trust manager factory
	 */
	TrustManagerFactory tls_trust_manager_factory();
	
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
	 * The HTTP/1.1 pipelining limit which corresponds to the maximum concurrent requests on a single connection.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 10} ({@code null} means no limit).
	 * </p>
	 * 
	 * @return the maximum HTTP/1.1 concurrent requests on a single connection
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
	
	/**
	 * <p>
	 * WebSocket max frame size in bytes.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 65536}.
	 * </p>
	 * 
	 * @return the WebSocket max frame size
	 */
	default Integer ws_max_frame_size() {
		return 65536;
	}
	
	/**
	 * <p>
	 * Enables/Disables WebSocket per frame compression.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true if WebSocket per frame is enabled, false otherwise
	 */
	default boolean ws_frame_compression_enabled() {
		return false;
	}
	
	/**
	 * <p>
	 * WebSocket per frame compression level.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 6}.
	 * </p>
	 * 
	 * @return the WebSocket frame compression level
	 */
	default int ws_frame_compression_level() {
		return 6;
	}
	
	/**
	 * <p>
	 * Enables/Disables WebSocket per message compression.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true if WebSocket per message is enabled, false otherwise
	 */
	default boolean ws_message_compression_enabled() {
		return false;
	}
	
	/**
	 * <p>
	 * WebSocket per message compression level.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 6}.
	 * </p>
	 * 
	 * @return the WebSocket message compression level
	 */
	default int ws_message_compression_level() {
		return 6;
	}
	
	/**
	 * <p>
	 * Allows WebSocket server to customize the client inflater window size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true to allow WebSocket server to customize the client inflater window size, false otherwise
	 */
	default boolean ws_message_allow_client_window_size() {
		return false;
	}
	/**
	 * <p>
	 * Indicates the requested sever window size to use if server inflater is customizable.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 15}.
	 * </p>
	 * 
	 * @return the requested sever window size to use if server inflater is customizable
	 */
	default int ws_message_requested_server_window_size() {
		return 15;
	}
	
	/**
	 * <p>
	 * Allows WebSocket server to activate client_no_context_takeover.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true to allow WebSocket server to activate client_no_context_takeover, false otherwise
	 */
	default boolean ws_message_allow_client_no_context() {
		return false;
	}
	
	/**
	 * <p>
	 * Indicates whether client needs to activate server_no_context_takeover if server is compatible with.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}
	 * </p>
	 * 
	 * @return true to activate server_no_context_takeover if server is compatible with, false otherwise
	 */
	default boolean ws_message_requested_server_no_context() {
		return false;
	}
	
	/**
	 * <p>
	 * Indicates whether a WebSocket should be closed when the outbound frames publisher completes.
	 * </p>
	 * 
	 * <p>
	 * Note that this behaviour can be overridden by invoking {@link WebSocketExchange.Outbound#closeOnComplete(boolean) } on the WebSocket exchange's outbound.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}
	 * </p>
	 * 
	 * @return true to close the WebSocket when the outbound frames publisher completes, false otherwise.
	 */
	default boolean ws_close_on_outbound_complete() {
		return true;
	}
	
	/**
	 * <p>
	 * The time in milliseconds to wait after a close frame has been sent for a close frame to be received before closing the WebSocket unilaterally.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 60000}.
	 * </p>
	 * 
	 * @return the inbound close frame timeout
	 */
	default long ws_inbound_close_frame_timeout() {
		return 60000l;
	}
}
