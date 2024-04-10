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

import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import java.net.URI;
import javax.net.ssl.TrustManagerFactory;

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
	 * The HTTP client authentication type.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.7
	 */
	public static enum ClientAuth {
		/**
		 * Indicates that client authentication will not be requested during TLS handshake.
		 */
		NONE,
		/**
		 * Indicates that client authentication will be requested during TLS handshake but that it is not required for the handshake to succeed.
		 */
		REQUESTED,
		/**
		 * Indicates that client authentication will be requested during TLS handshake and that the handshake will fail if the client does not present authentication.
		 */
		REQUIRED;
	}
	
	/**
	 * <p>
	 * The host name of the server socket address.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 0.0.0.0}.
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
	 * Defaults to {@code 8080}.
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
	 * If not specified, the number of thread allocated to the root event loop group shall be used.
	 * </p>
	 * 
	 * @return the number of threads to allocate
	 */
	Integer server_event_loop_group_size();

	/**
	 * <p>
	 * Tries to gracefully shutdown active connections when stopping the server.
	 * </p>
	 * 
	 * <p>
	 * Note that a connection is always gracefully shutdown when receiving a {@code GO_AWAY} HTTP/2 frame.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true to try to gracefully shutdown the server, false otherwise
	 */
	default boolean graceful_shutdown() {
		return false;
	}
	
	/**
	 * <p>
	 * The graceful shutdown timeout in milliseconds after which a connection is closed even if there are still active exchanges.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 30000}.
	 * </p>
	 * 
	 * @return the graceful shutdown timeout
	 */
	default long graceful_shutdown_timeout() {
		return 30000l;
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
	 * The client authentication type.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link ClientAuth#NONE}.
	 * </p>
	 * 
	 * @return 
	 */
	default ClientAuth tls_client_auth() {
		return ClientAuth.NONE;
	}
	
	/**
	 * <p>
	 * The path to the key store.
	 * </p>
	 * 
	 * <p>
	 * Note that this overrides {@link #tls_trust_manager_factory()}.
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
	 * @return the trust manager factory
	 */
	TrustManagerFactory tls_trust_manager_factory();
	
	/**
	 * <p>
	 * TLS handshake timeout (ms).
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 10000}.
	 * </p>
	 * 
	 * @return the TLS handshake timeout
	 */
	default long tls_handshake_timeout() {
		return 10000;
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
	 * Enables/Disables H2C.
	 * </p>
	 *
	 * <p>
	 * This only applies when SSL is disabled, otherwise {@link #h2_enabled()} is considered.
	 * </p>
	 *
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 *
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean h2c_enabled() {
		return false;
	}
	
	/**
	 * <p>
	 * The maximum length in bytes of the content of an H2C upgrade request.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 8192}.
	 * </p>
	 * 
	 * @return the maximum content length of an H2C upgrade request
	 */
	default int h2c_max_content_length() {
		return 8192;
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
	 * Defaults to {@code true}.
	 * </p>
	 * 
	 * @return true if the option is enabled, false otherwise
	 */
	default boolean h2_enabled() {
		return true;
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
	default Integer http2_max_concurrent_streams() {
		return 100;
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
	 * WebSocket handshake timeout (ms).
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code -1} (i.e. no timeout).
	 * </p>
	 * 
	 * @return the WebSocket handshake timeout
	 */
	default long ws_handshake_timeout() {
		return -1l;
	}
	
	/**
	 * <p>
	 * WebSocket close timeout (ms).
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code -1} (i.e. no timeout).
	 * </p>
	 * 
	 * @return the WebSocket close timeout
	 */
	default long ws_close_timeout() {
		return -1l;
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
	 * Accepts/Rejects unproperly masked WebSocket frames.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true to allow unproperly masked frames, false otherwise
	 */
	default boolean ws_allow_mask_mismatch() {
		return false;
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
	 * Allows WebSocket client to customize the server inflater window size.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true to allow clients to customize the server inflater window size, false otherwise
	 */
	default boolean ws_message_allow_server_window_size() {
		return false;
	}
	
	/**
	 * <p>
	 * The prefered client window size to use if client inflater is customizable.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code 15}.
	 * </p>
	 * 
	 * @return the prefered client window size
	 */
	default int ws_message_prefered_client_window_size() {
		return 15;
	}
	
	/**
	 * <p>
	 * Allows WebSocket client to activate server_no_context_takeover.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true to allow clients to activate server_no_context_takeover, false otherwise
	 */
	default boolean ws_message_allow_server_no_context() {
		return false;
	}
	
	/**
	 * <p>
	 * Indicates if server prefers to activate client_no_context_takeover if client supports it.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@code false}.
	 * </p>
	 * 
	 * @return true to activate client_no_context_takeover if client supports it, false otherwise
	 */
	default boolean ws_message_preferred_client_no_context() {
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