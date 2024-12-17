/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.discovery.http.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.inverno.mod.base.Settable;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.LeastRequestTrafficLoadBalancer;
import io.inverno.mod.discovery.http.MinLoadFactorTrafficLoadBalancer;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Describes an HTTP meta service including: network configuration, traffic policy, routes and destinations.
 * </p>
 *
 * <p>
 * An HTTP service descriptor is typically resolved by service name from a configuration source or any other source in a discovery service in order to create a meta HTTP service capable of rewriting,
 * routing and/or load balancing requests to one or more destinations.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpMetaServiceDescriptor {

	private final HttpClientConfiguration configuration;

	private final LoadBalancerDescriptor loadBalancer;

	private final List<RouteDescriptor> routes;

	/**
	 * <p>
	 * Creates an HTTP meta service descriptor.
	 * </p>
	 *
	 * @param configuration the HTTP client configuration applying to all routes and destinations
	 * @param loadBalancer  the load balancer configuration applying to all routes and destinations
	 * @param routes        the service routes
	 */
	@JsonCreator
	public HttpMetaServiceDescriptor(@JsonProperty("configuration") HttpClientConfiguration configuration, @JsonProperty("loadBalancer") LoadBalancerDescriptor loadBalancer, @JsonProperty("routes") List<RouteDescriptor> routes) {
		this.configuration = configuration;
		this.loadBalancer = loadBalancer;
		this.routes = routes;
	}

	/**
	 * <p>
	 * Returns the HTTP client configuration.
	 * </p>
	 *
	 * @return the HTTP client configuration
	 */
	@JsonProperty("configuration")
	public HttpClientConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * <p>
	 * Returns the load balancer configuration.
	 * </p>
	 *
	 * @return the load balancer configuration
	 */
	@JsonProperty("loadBalancer")
	public LoadBalancerDescriptor getLoadBalancer() {
		return loadBalancer;
	}

	/**
	 * <p>
	 * Returns the service routes.
	 * </p>
	 *
	 * @return the service routes
	 */
	@JsonProperty("routes")
	public List<RouteDescriptor> getRoutes() {
		return routes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HttpMetaServiceDescriptor that = (HttpMetaServiceDescriptor) o;
		return Objects.equals(configuration, that.configuration) && Objects.equals(loadBalancer, that.loadBalancer) && Objects.equals(routes, that.routes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(configuration, loadBalancer, routes);
	}

	/**
	 * <p>
	 * Describes HTTP client configuration.
	 * </p>
	 *
	 * <p>
	 * They can be defined at different level of the HTTP meta service descriptor:
	 * </p>
	 *
	 * <ul>
	 * <li>at service level to override a setting for every destination defined in the service</li>
	 * <li>at route level to override a setting for every destination defined in the route</li>
	 * <li>at destination level to override a setting for a particular destination</li>
	 * </ul>
	 *
	 * <p>
	 * The HTTP client configuration used to create the endpoint(s) in a given destination is then obtained from the initial configuration coming from the HTTP client overridden by the configuration
	 * defined at service level, overridden by the configuration defined at route level, overridden by the configuration defined at destination level.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public static class HttpClientConfiguration {

		private Settable<Integer> client_event_loop_group_size = Settable.undefined();
		private Settable<Integer> pool_max_size = Settable.undefined();
		private Settable<Long> pool_clean_period = Settable.undefined();
		private Settable<Integer> pool_buffer_size = Settable.undefined();
		private Settable<Long> pool_keep_alive_timeout = Settable.undefined();
		private Settable<Long> pool_connect_timeout = Settable.undefined();
		private Settable<Long> request_timeout = Settable.undefined();
		private Settable<Long> graceful_shutdown_timeout = Settable.undefined();
		private Settable<Set<HttpVersion>> http_protocol_versions = Settable.undefined();
		private Settable<Boolean> send_user_agent = Settable.undefined();
		private Settable<String> user_agent = Settable.undefined();
		private Settable<Boolean> compression_enabled = Settable.undefined();
		private Settable<Boolean> decompression_enabled = Settable.undefined();
		private Settable<Integer> compression_contentSizeThreshold = Settable.undefined();
		private Settable<Integer> compression_brotli_quality = Settable.undefined();
		private Settable<Integer> compression_brotli_window = Settable.undefined();
		private Settable<Integer> compression_brotli_mode = Settable.undefined();
		private Settable<Integer> compression_deflate_compressionLevel = Settable.undefined();
		private Settable<Integer> compression_deflate_windowBits = Settable.undefined();
		private Settable<Integer> compression_deflate_memLevel = Settable.undefined();
		private Settable<Integer> compression_gzip_compressionLevel = Settable.undefined();
		private Settable<Integer> compression_gzip_windowBits = Settable.undefined();
		private Settable<Integer> compression_gzip_memLevel = Settable.undefined();
		private Settable<Integer> compression_zstd_blockSize = Settable.undefined();
		private Settable<Integer> compression_zstd_compressionLevel = Settable.undefined();
		private Settable<Integer> compression_zstd_maxEncodeSize = Settable.undefined();
		private Settable<URI> tls_key_store = Settable.undefined();
		private Settable<String> tls_key_store_type = Settable.undefined();
		private Settable<String> tls_key_store_password = Settable.undefined();
		private Settable<String> tls_key_alias = Settable.undefined();
		private Settable<String> tls_key_alias_password = Settable.undefined();
		private Settable<URI> tls_trust_store = Settable.undefined();
		private Settable<String> tls_trust_store_type = Settable.undefined();
		private Settable<String> tls_trust_store_password = Settable.undefined();
		private Settable<Boolean> tls_trust_all = Settable.undefined();
		private Settable<Boolean> tls_send_sni = Settable.undefined();
		private Settable<String[]> tls_ciphers_includes = Settable.undefined();
		private Settable<String[]> tls_ciphers_excludes = Settable.undefined();
		private Settable<Integer> http1x_initial_buffer_size = Settable.undefined();
		private Settable<Integer> http1x_max_initial_line_length = Settable.undefined();
		private Settable<Integer> http1x_max_chunk_size = Settable.undefined();
		private Settable<Integer> http1x_max_header_size = Settable.undefined();
		private Settable<Boolean> http1x_validate_headers = Settable.undefined();
		private Settable<Long> http1_max_concurrent_requests = Settable.undefined();
		private Settable<Long> http2_header_table_size = Settable.undefined();
		private Settable<Long> http2_max_concurrent_streams = Settable.undefined();
		private Settable<Integer> http2_initial_window_size = Settable.undefined();
		private Settable<Integer> http2_max_frame_size = Settable.undefined();
		private Settable<Integer> http2_max_header_list_size = Settable.undefined();
		private Settable<Boolean> http2_validate_headers = Settable.undefined();
		private Settable<Integer> ws_max_frame_size = Settable.undefined();
		private Settable<Boolean> ws_frame_compression_enabled = Settable.undefined();
		private Settable<Integer> ws_frame_compression_level = Settable.undefined();
		private Settable<Boolean> ws_message_compression_enabled = Settable.undefined();
		private Settable<Integer> ws_message_compression_level = Settable.undefined();
		private Settable<Boolean> ws_message_allow_client_window_size = Settable.undefined();
		private Settable<Integer> ws_message_requested_server_window_size = Settable.undefined();
		private Settable<Boolean> ws_message_allow_client_no_context = Settable.undefined();
		private Settable<Boolean> ws_message_requested_server_no_context = Settable.undefined();
		private Settable<Boolean> ws_close_on_outbound_complete = Settable.undefined();
		private Settable<Long> ws_inbound_close_frame_timeout = Settable.undefined();
		private Settable<String> proxy_host = Settable.undefined();
		private Settable<Integer> proxy_port = Settable.undefined();
		private Settable<String> proxy_username = Settable.undefined();
		private Settable<String> proxy_password = Settable.undefined();
		private Settable<io.inverno.mod.http.client.HttpClientConfiguration.ProxyProtocol> proxy_protocol = Settable.undefined();

		/**
		 * <p>
		 * Returns a configurer used to override an HTTP client configuration.
		 * </p>
		 *
		 * @return an HTTP client configuration configurer
		 */
		@JsonIgnore
		public Consumer<HttpClientConfigurationLoader.Configurator> getConfigurer() {
			return configurator -> {
				this.client_event_loop_group_size.ifSet(configurator::client_event_loop_group_size);
				this.pool_max_size.ifSet(configurator::client_event_loop_group_size);
				this.pool_clean_period.ifSet(configurator::pool_clean_period);
				this.pool_buffer_size.ifSet(configurator::pool_buffer_size);
				this.pool_keep_alive_timeout.ifSet(configurator::pool_keep_alive_timeout);
				this.pool_connect_timeout.ifSet(configurator::pool_connect_timeout);
				this.request_timeout.ifSet(configurator::request_timeout);
				this.graceful_shutdown_timeout.ifSet(configurator::graceful_shutdown_timeout);
				this.http_protocol_versions.ifSet(configurator::http_protocol_versions);
				this.send_user_agent.ifSet(configurator::send_user_agent);
				this.user_agent.ifSet(configurator::user_agent);
				this.compression_enabled.ifSet(configurator::compression_enabled);
				this.decompression_enabled.ifSet(configurator::decompression_enabled);
				this.compression_contentSizeThreshold.ifSet(configurator::compression_contentSizeThreshold);
				this.compression_brotli_quality.ifSet(configurator::compression_brotli_quality);
				this.compression_brotli_window.ifSet(configurator::compression_brotli_window);
				this.compression_brotli_mode.ifSet(configurator::compression_brotli_mode);
				this.compression_deflate_compressionLevel.ifSet(configurator::compression_deflate_compressionLevel);
				this.compression_deflate_windowBits.ifSet(configurator::compression_deflate_windowBits);
				this.compression_deflate_memLevel.ifSet(configurator::compression_deflate_memLevel);
				this.compression_gzip_compressionLevel.ifSet(configurator::compression_gzip_compressionLevel);
				this.compression_gzip_windowBits.ifSet(configurator::compression_gzip_windowBits);
				this.compression_gzip_memLevel.ifSet(configurator::compression_gzip_memLevel);
				this.compression_zstd_blockSize.ifSet(configurator::compression_zstd_blockSize);
				this.compression_zstd_compressionLevel.ifSet(configurator::compression_zstd_compressionLevel);
				this.compression_zstd_maxEncodeSize.ifSet(configurator::compression_zstd_maxEncodeSize);
				this.tls_key_store.ifSet(configurator::tls_key_store);
				this.tls_key_store_type.ifSet(configurator::tls_key_store_type);
				this.tls_key_store_password.ifSet(configurator::tls_key_store_password);
				this.tls_key_alias.ifSet(configurator::tls_key_alias);
				this.tls_key_alias_password.ifSet(configurator::tls_key_alias_password);
				this.tls_trust_store.ifSet(configurator::tls_trust_store);
				this.tls_trust_store_type.ifSet(configurator::tls_trust_store_type);
				this.tls_trust_store_password.ifSet(configurator::tls_trust_store_password);
				this.tls_trust_all.ifSet(configurator::tls_trust_all);
				this.tls_send_sni.ifSet(configurator::tls_send_sni);
				this.tls_ciphers_includes.ifSet(configurator::tls_ciphers_includes);
				this.tls_ciphers_excludes.ifSet(configurator::tls_ciphers_excludes);
				this.http1x_initial_buffer_size.ifSet(configurator::http1x_initial_buffer_size);
				this.http1x_max_initial_line_length.ifSet(configurator::http1x_max_initial_line_length);
				this.http1x_max_chunk_size.ifSet(configurator::http1x_max_chunk_size);
				this.http1x_max_header_size.ifSet(configurator::http1x_max_header_size);
				this.http1x_validate_headers.ifSet(configurator::http1x_validate_headers);
				this.http1_max_concurrent_requests.ifSet(configurator::http1_max_concurrent_requests);
				this.http2_header_table_size.ifSet(configurator::http2_header_table_size);
				this.http2_max_concurrent_streams.ifSet(configurator::http2_max_concurrent_streams);
				this.http2_initial_window_size.ifSet(configurator::http2_initial_window_size);
				this.http2_max_frame_size.ifSet(configurator::http2_max_frame_size);
				this.http2_max_header_list_size.ifSet(configurator::http2_max_header_list_size);
				this.http2_validate_headers.ifSet(configurator::http2_validate_headers);
				this.ws_max_frame_size.ifSet(configurator::ws_max_frame_size);
				this.ws_frame_compression_enabled.ifSet(configurator::ws_frame_compression_enabled);
				this.ws_frame_compression_level.ifSet(configurator::ws_frame_compression_level);
				this.ws_message_compression_enabled.ifSet(configurator::ws_message_compression_enabled);
				this.ws_message_compression_level.ifSet(configurator::ws_message_compression_level);
				this.ws_message_allow_client_window_size.ifSet(configurator::ws_message_allow_client_window_size);
				this.ws_message_requested_server_window_size.ifSet(configurator::ws_message_requested_server_window_size);
				this.ws_message_allow_client_no_context.ifSet(configurator::ws_message_allow_client_no_context);
				this.ws_message_requested_server_no_context.ifSet(configurator::ws_message_requested_server_no_context);
				this.ws_close_on_outbound_complete.ifSet(configurator::ws_close_on_outbound_complete);
				this.ws_inbound_close_frame_timeout.ifSet(configurator::ws_inbound_close_frame_timeout);
				this.proxy_host.ifSet(configurator::proxy_host);
				this.proxy_port.ifSet(configurator::proxy_port);
				this.proxy_username.ifSet(configurator::proxy_username);
				this.proxy_password.ifSet(configurator::proxy_password);
				this.proxy_protocol.ifSet(configurator::proxy_protocol);
			};
		}

		/**
		 * <p>
		 * Returns the client event loop group size.
		 * </p>
		 *
		 * @return a settable returning the client event loop group size or an unset settable if no value was set
		 */
		public Settable<Integer> getClient_event_loop_group_size() {
			return this.client_event_loop_group_size;
		}

		/**
		 * <p>
		 * Sets the client event loop group size.
		 * </p>
		 *
		 * @param client_event_loop_group_size the client event lookp group size
		 */
		public void setClient_event_loop_group_size(Integer client_event_loop_group_size) {
			this.client_event_loop_group_size = Settable.of(client_event_loop_group_size);
		}

		/**
		 * <p>
		 * Returns the pool max size.
		 * </p>
		 *
		 * @return a settable returning the pool max size or an unset settable if no value was set
		 */
		public Settable<Integer> getPool_max_size() {
			return this.pool_max_size;
		}

		/**
		 * <p>
		 * Sets the pool max size.
		 * </p>
		 *
		 * @param pool_max_size the pool max size
		 */
		public void setPool_max_size(Integer pool_max_size) {
			this.pool_max_size = Settable.of(pool_max_size);
		}

		/**
		 * <p>
		 * Returns the pool clean period.
		 * </p>
		 *
		 * @return a settable returning the pool clean period or an unset settable if no value was set
		 */
		public Settable<Long> getPool_clean_period() {
			return this.pool_clean_period;
		}

		/**
		 * <p>
		 * Sets the pool clean period
		 * </p>
		 *
		 * @param pool_clean_period the pool clean period
		 */
		public void setPool_clean_period(Long pool_clean_period) {
			this.pool_clean_period = Settable.of(pool_clean_period);
		}

		/**
		 * <p>
		 * Returns the pool buffer size.
		 * </p>
		 *
		 * @return a settable returning the pool buffer size or an unset settable if no value was set
		 */
		public Settable<Integer> getPool_buffer_size() {
			return this.pool_buffer_size;
		}

		/**
		 * <p>
		 * Sets the pool buffer size.
		 * </p>
		 *
		 * @param pool_buffer_size the pool buffer size
		 */
		public void setPool_buffer_size(Integer pool_buffer_size) {
			this.pool_buffer_size = Settable.of(pool_buffer_size);
		}

		/**
		 * <p>
		 * Returns the pool keep alive timeout.
		 * </p>
		 *
		 * @return a settable returning the pool clean period or an unset settable if no value was set
		 */
		public Settable<Long> getPool_keep_alive_timeout() {
			return this.pool_keep_alive_timeout;
		}

		/**
		 * <p>
		 * Sets the pool keep alive timeout.
		 * </p>
		 *
		 * @param pool_keep_alive_timeout the pool keep alive timeout
		 */
		public void setPool_keep_alive_timeout(Long pool_keep_alive_timeout) {
			this.pool_keep_alive_timeout = Settable.of(pool_keep_alive_timeout);
		}

		/**
		 * <p>
		 * Returns the pool connect timeout.
		 * </p>
		 *
		 * @return a settable returning the pool connect timeout or an unset settable if no value was set
		 */
		public Settable<Long> getPool_connect_timeout() {
			return pool_connect_timeout;
		}

		/**
		 * <p>
		 * Sets the pool connect timeout.
		 * </p>
		 *
		 * @param pool_connect_timeout the pool connect timeout
		 */
		public void setPool_connect_timeout(Long pool_connect_timeout) {
			this.pool_connect_timeout = Settable.of(pool_connect_timeout);
		}

		/**
		 * <p>
		 * Returns the request timeout.
		 * </p>
		 *
		 * @return a settable returning the request timeout or an unset settable if no value was set
		 */
		public Settable<Long> getRequest_timeout() {
			return request_timeout;
		}

		/**
		 * <p>
		 * Sets the request timeout.
		 * </p>
		 *
		 * @param request_timeout the request timeout
		 */
		public void setRequest_timeout(Long request_timeout) {
			this.request_timeout = Settable.of(request_timeout);
		}

		/**
		 * <p>
		 * Returns the graceful shutdown timeout.
		 * </p>
		 *
		 * @return a settable returning the graceful shutdown timeout or an unset settable if no value was set
		 */
		public Settable<Long> getGraceful_shutdown_timeout() {
			return graceful_shutdown_timeout;
		}

		/**
		 * <p>
		 * Sets the graceful shutdown timeout.
		 * </p>
		 *
		 * @param graceful_shutdown_timeout the graceful shutdown timeout
		 */
		public void setGraceful_shutdown_timeout(Long graceful_shutdown_timeout) {
			this.graceful_shutdown_timeout = Settable.of(graceful_shutdown_timeout);
		}

		/**
		 * <p>
		 * Returns the HTTP protocol versions.
		 * </p>
		 *
		 * @return a settable returning the HTTP protocol versions or an unset settable if no value was set
		 */
		public Settable<Set<HttpVersion>> getHttp_protocol_versions() {
			return this.http_protocol_versions;
		}

		/**
		 * <p>
		 * Sets the HTTP protocol versions.
		 * </p>
		 *
		 * @param http_protocol_versions the HTTP protocol versions
		 */
		public void setHttp_protocol_versions(Set<HttpVersion> http_protocol_versions) {
			this.http_protocol_versions = Settable.of(http_protocol_versions);
		}

		/**
		 * <p>
		 * Returns the send user agent flag.
		 * </p>
		 *
		 * @return a settable returning the send user agent flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getSend_user_agent() {
			return send_user_agent;
		}

		/**
		 * <p>
		 * Sets the send user agent flag.
		 * </p>
		 *
		 * @param send_user_agent the send user agent flag
		 */
		public void setSend_user_agent(Boolean send_user_agent) {
			this.send_user_agent = Settable.of(send_user_agent);
		}

		/**
		 * <p>
		 * Returns the user agent.
		 * </p>
		 *
		 * @return a settable returning the user agent or an unset settable if no value was set
		 */
		public Settable<String> getUser_agent() {
			return user_agent;
		}

		/**
		 * <p>
		 * Sets the user agent.
		 * </p>
		 *
		 * @param user_agent the user agent
		 */
		public void setUser_agent(String user_agent) {
			this.user_agent = Settable.of(user_agent);
		}

		/**
		 * <p>
		 * Returns the compression enabled flag.
		 * </p>
		 *
		 * @return a settable returning the compression enabled flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getCompression_enabled() {
			return compression_enabled;
		}

		/**
		 * <p>
		 * Sets the compression enabled flag.
		 * </p>
		 *
		 * @param compression_enabled the compression enabled flag
		 */
		public void setCompression_enabled(Boolean compression_enabled) {
			this.compression_enabled = Settable.of(compression_enabled);
		}

		/**
		 * <p>
		 * Returns the decompression enabled flag.
		 * </p>
		 *
		 * @return the decompression enabled flag
		 */
		public Settable<Boolean> getDecompression_enabled() {
			return decompression_enabled;
		}

		/**
		 * <p>
		 * Sets the decompression enabled flag.
		 * </p>
		 *
		 * @param decompression_enabled the decompression enabled flag
		 */
		public void setDecompression_enabled(Boolean decompression_enabled) {
			this.decompression_enabled = Settable.of(decompression_enabled);
		}

		/**
		 * <p>
		 * Returns the compression content size threshold.
		 * </p>
		 *
		 * @return a settable returning the compression content size threshold or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_contentSizeThreshold() {
			return compression_contentSizeThreshold;
		}

		/**
		 * <p>
		 * Sets the compression content size threshold.
		 * </p>
		 *
		 * @param compression_contentSizeThreshold the compression content size threshold
		 */
		public void setCompression_contentSizeThreshold(Integer compression_contentSizeThreshold) {
			this.compression_contentSizeThreshold = Settable.of(compression_contentSizeThreshold);
		}

		/**
		 * <p>
		 * Returns Brotli compression quality.
		 * </p>
		 *
		 * @return a settable returning Brotli compression quality or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_brotli_quality() {
			return compression_brotli_quality;
		}

		/**
		 * <p>
		 * Sets Brotli compression quality.
		 * </p>
		 *
		 * @param compression_brotli_quality Brotli compression quality
		 */
		public void setCompression_brotli_quality(Integer compression_brotli_quality) {
			this.compression_brotli_quality = Settable.of(compression_brotli_quality);
		}

		/**
		 * <p>
		 * Returns Brotli compression window.
		 * </p>
		 *
		 * @return a settable returning Brotli compression window or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_brotli_window() {
			return compression_brotli_window;
		}

		/**
		 * <p>
		 * Sets Brotli compression window.
		 * </p>
		 *
		 * @param compression_brotli_window Brotli compression window
		 */
		public void setCompression_brotli_window(Integer compression_brotli_window) {
			this.compression_brotli_window = Settable.of(compression_brotli_window);
		}

		/**
		 * <p>
		 * Returns Brotli compression mode.
		 * </p>
		 *
		 * @return a settable returning Brotli compression mode  or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_brotli_mode() {
			return compression_brotli_mode;
		}

		/**
		 * <p>
		 * Sets Brotli compression mode.
		 * </p>
		 *
		 * @param compression_brotli_mode Brotli compression mode
		 */
		public void setCompression_brotli_mode(Integer compression_brotli_mode) {
			this.compression_brotli_mode = Settable.of(compression_brotli_mode);
		}

		/**
		 * <p>
		 * Returns Deflate compression level.
		 * </p>
		 *
		 * @return a settable returning Deflate compression level or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_deflate_compressionLevel() {
			return compression_deflate_compressionLevel;
		}

		/**
		 * <p>
		 * Sets Deflate compression level.
		 * </p>
		 *
		 * @param compression_deflate_compressionLevel Deflate compression level
		 */
		public void setCompression_deflate_compressionLevel(Integer compression_deflate_compressionLevel) {
			this.compression_deflate_compressionLevel = Settable.of(compression_deflate_compressionLevel);
		}

		/**
		 * <p>
		 * Returns Deflate window bits.
		 * </p>
		 *
		 * @return a settable returning Deflate window bits or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_deflate_windowBits() {
			return compression_deflate_windowBits;
		}

		/**
		 * <p>
		 * Sets Deflate window its.
		 * </p>
		 *
		 * @param compression_deflate_windowBits Deflate window bits
		 */
		public void setCompression_deflate_windowBits(Integer compression_deflate_windowBits) {
			this.compression_deflate_windowBits = Settable.of(compression_deflate_windowBits);
		}

		/**
		 * <p>
		 * Returns Deflate memory level.
		 * </p>
		 *
		 * @return a settable returning Deflate memory level or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_deflate_memLevel() {
			return compression_deflate_memLevel;
		}

		/**
		 * <p>
		 * Sets Deflate memory level.
		 * </p>
		 *
		 * @param compression_deflate_memLevel Deflate memory level
		 */
		public void setCompression_deflate_memLevel(Integer compression_deflate_memLevel) {
			this.compression_deflate_memLevel = Settable.of(compression_deflate_memLevel);
		}

		/**
		 * <p>
		 * Returns Gzip compression level.
		 * </p>
		 *
		 * @return a settable returning Gzip compression level or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_gzip_compressionLevel() {
			return compression_gzip_compressionLevel;
		}

		/**
		 * <p>
		 * Sets Gzip compression level.
		 * </p>
		 *
		 * @param compression_gzip_compressionLevel Gzip compression level
		 */
		public void setCompression_gzip_compressionLevel(Integer compression_gzip_compressionLevel) {
			this.compression_gzip_compressionLevel = Settable.of(compression_gzip_compressionLevel);
		}

		/**
		 * <p>
		 * Returns Gzip window bits.
		 * </p>
		 *
		 * @return a settable returning Gzip window bits or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_gzip_windowBits() {
			return compression_gzip_windowBits;
		}

		/**
		 * <p>
		 * Sets Gzip window bits.
		 * </p>
		 *
		 * @param compression_gzip_windowBits Gzip window bits
		 */
		public void setCompression_gzip_windowBits(Integer compression_gzip_windowBits) {
			this.compression_gzip_windowBits = Settable.of(compression_gzip_windowBits);
		}

		/**
		 * <p>
		 * Returns Gzip memory level.
		 * </p>
		 *
		 * @return a settable returning Gzip memory level or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_gzip_memLevel() {
			return compression_gzip_memLevel;
		}

		/**
		 * <p>
		 * Sets Gzip memory level
		 * </p>
		 *
		 * @param compression_gzip_memLevel Gzip memory level
		 */
		public void setCompression_gzip_memLevel(Integer compression_gzip_memLevel) {
			this.compression_gzip_memLevel = Settable.of(compression_gzip_memLevel);
		}

		/**
		 * <p>
		 * Returns Zstd block size.
		 * </p>
		 *
		 * @return a settable returning Zstd block size or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_zstd_blockSize() {
			return compression_zstd_blockSize;
		}

		/**
		 * <p>
		 * Sets Zstd block size.
		 * </p>
		 *
		 * @param compression_zstd_blockSize Zstd block size
		 */
		public void setCompression_zstd_blockSize(Integer compression_zstd_blockSize) {
			this.compression_zstd_blockSize = Settable.of(compression_zstd_blockSize);
		}

		/**
		 * <p>
		 * Returns Zstd compression level.
		 * </p>
		 *
		 * @return a settable returning Zstd compression level or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_zstd_compressionLevel() {
			return compression_zstd_compressionLevel;
		}

		/**
		 * <p>
		 * Sets Zstd compression level.
		 * </p>
		 *
		 * @param compression_zstd_compressionLevel Zstd compression level
		 */
		public void setCompression_zstd_compressionLevel(Integer compression_zstd_compressionLevel) {
			this.compression_zstd_compressionLevel = Settable.of(compression_zstd_compressionLevel);
		}

		/**
		 * <p>
		 * Returns Zstd max encode size.
		 * </p>
		 *
		 * @return a settable returning Zstd ma encode size or an unset settable if no value was set
		 */
		public Settable<Integer> getCompression_zstd_maxEncodeSize() {
			return compression_zstd_maxEncodeSize;
		}

		/**
		 * <p>
		 * Sets Zstd max encode size.
		 * </p>
		 *
		 * @param compression_zstd_maxEncodeSize Zstd max encode size
		 */
		public void setCompression_zstd_maxEncodeSize(Integer compression_zstd_maxEncodeSize) {
			this.compression_zstd_maxEncodeSize = Settable.of(compression_zstd_maxEncodeSize);
		}

		/**
		 * <p>
		 * Returns the TLS keystore URI.
		 * </p>
		 *
		 * @return a settable returning the TLS keystore URI or an unset settable if no value was set
		 */
		public Settable<URI> getTls_key_store() {
			return tls_key_store;
		}

		/**
		 * <p>
		 * Sets the TLS keystore URI.
		 * </p>
		 *
		 * @param tls_key_store the TLS keystore URI
		 */
		public void setTls_key_store(URI tls_key_store) {
			this.tls_key_store = Settable.of(tls_key_store);
		}

		/**
		 * <p>
		 * Returns the TLS keystore type.
		 * </p>
		 *
		 * @return a settable returning the TLS keystore type or an unset settable if no value was set
		 */
		public Settable<String> getTls_key_store_type() {
			return tls_key_store_type;
		}

		/**
		 * <p>
		 * Sets the TLS keystore type.
		 * </p>
		 *
		 * @param tls_key_store_type the TLS keystore type
		 */
		public void setTls_key_store_type(String tls_key_store_type) {
			this.tls_key_store_type = Settable.of(tls_key_store_type);
		}

		/**
		 * <p>
		 * Returns the TLS keystore password.
		 * </p>
		 *
		 * @return a settable returning the TLS keystore password or an unset settable if no value was set
		 */
		public Settable<String> getTls_key_store_password() {
			return tls_key_store_password;
		}

		/**
		 * <p>
		 * Sets the TLS keystore password.
		 * </p>
		 *
		 * @param tls_key_store_password the TLS keystore password
		 */
		public void setTls_key_store_password(String tls_key_store_password) {
			this.tls_key_store_password = Settable.of(tls_key_store_password);
		}

		/**
		 * <p>
		 * Returns the TLS key alias.
		 * </p>
		 *
		 * @return a settable returning the TLS key alis or an unset settable if no value was set
		 */
		public Settable<String> getTls_key_alias() {
			return tls_key_alias;
		}

		/**
		 * <p>
		 * Sets the TLS key alias.
		 * </p>
		 *
		 * @param tls_key_alias the TLS key alias
		 */
		public void setTls_key_alias(String tls_key_alias) {
			this.tls_key_alias = Settable.of(tls_key_alias);
		}

		/**
		 * <p>
		 * Returns the TLS key alias password.
		 * </p>
		 *
		 * @return a settable returning the TLS key alias password or an unset settable if no value was set
		 */
		public Settable<String> getTls_key_alias_password() {
			return tls_key_alias_password;
		}

		/**
		 * <p>
		 * Sets the TLS key alias password.
		 * </p>
		 *
		 * @param tls_key_alias_password the TLS key alias password
		 */
		public void setTls_key_alias_password(String tls_key_alias_password) {
			this.tls_key_alias_password = Settable.of(tls_key_alias_password);
		}

		/**
		 * <p>
		 * Returns the TLS truststore URI.
		 * </p>
		 *
		 * @return a settable returning the TLS truststore URI or an unset settable if no value was set
		 */
		public Settable<URI> getTls_trust_store() {
			return tls_trust_store;
		}

		/**
		 * <p>
		 * Sets the TLS truststore URI.
		 * </p>
		 *
		 * @param tls_trust_store the TLS truststore URI
		 */
		public void setTls_trust_store(URI tls_trust_store) {
			this.tls_trust_store = Settable.of(tls_trust_store);
		}

		/**
		 * <p>
		 * Returns the TLS truststore type.
		 * </p>
		 *
		 * @return a settable returning the TLS truststore type or an unset settable if no value was set
		 */
		public Settable<String> getTls_trust_store_type() {
			return tls_trust_store_type;
		}

		/**
		 * <p>
		 * Sets the TLS truststore type.
		 * </p>
		 *
		 * @param tls_trust_store_type the TLS truststore type
		 */
		public void setTls_trust_store_type(String tls_trust_store_type) {
			this.tls_trust_store_type = Settable.of(tls_trust_store_type);
		}

		/**
		 * <p>
		 * Returns the TLS truststore password.
		 * </p>
		 *
		 * @return a settable returning the TLS truststore password or an unset settable if no value was set
		 */
		public Settable<String> getTls_trust_store_password() {
			return tls_trust_store_password;
		}

		/**
		 * <p>
		 * Sets the TLS truststore password.
		 * </p>
		 *
		 * @param tls_trust_store_password the TLS truststore password
		 */
		public void setTls_trust_store_password(String tls_trust_store_password) {
			this.tls_trust_store_password = Settable.of(tls_trust_store_password);
		}

		/**
		 * <p>
		 * Returns the TLS trust all flag.
		 * </p>
		 *
		 * @return a settable returning the TLS trust all flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getTls_trust_all() {
			return tls_trust_all;
		}

		/**
		 * <p>
		 * Sets the TLS trust all flag.
		 * </p>
		 *
		 * @param tls_trust_all the TLS trust all flag.
		 */
		public void setTls_trust_all(Boolean tls_trust_all) {
			this.tls_trust_all = Settable.of(tls_trust_all);
		}

		/**
		 * <p>
		 * Returns the TLS send SNI flag.
		 * </p>
		 *
		 * @return a settable returning the TLS send SNI flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getTls_send_sni() {
			return tls_send_sni;
		}

		/**
		 * <p>
		 * Sets the TLS send SNI flag.
		 * </p>
		 *
		 * @param tls_send_sni the TLS send SNI flag.
		 */
		public void setTls_send_sni(Boolean tls_send_sni) {
			this.tls_send_sni = Settable.of(tls_send_sni);
		}

		/**
		 * <p>
		 * Returns the TLS ciphers to include.
		 * </p>
		 *
		 * @return a settable returning the TLS ciphers to include or an unset settable if no value was set
		 */
		public Settable<String[]> getTls_ciphers_includes() {
			return tls_ciphers_includes;
		}

		/**
		 * <p>
		 * Sets the TLS ciphers to include.
		 * </p>
		 *
		 * @param tls_ciphers_includes the TLS ciphers to include
		 */
		public void setTls_ciphers_includes(String[] tls_ciphers_includes) {
			this.tls_ciphers_includes = Settable.of(tls_ciphers_includes);
		}

		/**
		 * <p>
		 * Returns the TLS cipher to excludes.
		 * </p>
		 *
		 * @return a settable returning the TLS ciphers to exclude or an unset settable if no value was set
		 */
		public Settable<String[]> getTls_ciphers_excludes() {
			return tls_ciphers_excludes;
		}

		/**
		 * <p>
		 * Sets the TLS ciphers to excludes
		 * </p>
		 *
		 * @param tls_ciphers_excludes the TLS ciphers to exclude
		 */
		public void setTls_ciphers_excludes(String[] tls_ciphers_excludes) {
			this.tls_ciphers_excludes = Settable.of(tls_ciphers_excludes);
		}

		/**
		 * <p>
		 * Returns the HTTP/1.x initial buffer size.
		 * </p>
		 *
		 * @return a settable returning the HTTP/1.x initial buffer size or an unset settable if no value was set
		 */
		public Settable<Integer> getHttp1x_initial_buffer_size() {
			return http1x_initial_buffer_size;
		}

		/**
		 * <p>
		 * Sets the HTTP/1.x initial buffer size.
		 * </p>
		 *
		 * @param http1x_initial_buffer_size the initial HTTP/1.x initial buffer size
		 */
		public void setHttp1x_initial_buffer_size(Integer http1x_initial_buffer_size) {
			this.http1x_initial_buffer_size = Settable.of(http1x_initial_buffer_size);
		}

		/**
		 * <p>
		 * Returns the HTTP/1.x max initial line length.
		 * </p>
		 *
		 * @return the HTTP/1.x max initial line length
		 */
		public Settable<Integer> getHttp1x_max_initial_line_length() {
			return http1x_max_initial_line_length;
		}

		/**
		 * <p>
		 * Sets the HTTP/1.x max initial line length.
		 * </p>
		 *
		 * @param http1x_max_initial_line_length HTTP/1.x max initial line length
		 */
		public void setHttp1x_max_initial_line_length(Integer http1x_max_initial_line_length) {
			this.http1x_max_initial_line_length = Settable.of(http1x_max_initial_line_length);
		}

		/**
		 * <p>
		 * Returns the HTTP/1.x max chunk size.
		 * </p>
		 *
		 * @return a settable returning the HTTP/1.x max chunk size or an unset settable if no value was set
		 */
		public Settable<Integer> getHttp1x_max_chunk_size() {
			return http1x_max_chunk_size;
		}

		/**
		 * <p>
		 * Sets the HTTP/1.x max chunk size.
		 * </p>
		 *
		 * @param http1x_max_chunk_size the HTTP/1.x max chunk size
		 */
		public void setHttp1x_max_chunk_size(Integer http1x_max_chunk_size) {
			this.http1x_max_chunk_size = Settable.of(http1x_max_chunk_size);
		}

		/**
		 * <p>
		 * Returns the HTTP/1.x max header size.
		 * </p>
		 *
		 * @return a settable returning the HTTP/1.x max header size or an unset settable if no value was set
		 */
		public Settable<Integer> getHttp1x_max_header_size() {
			return http1x_max_header_size;
		}

		/**
		 * <p>
		 * Sets the HTTP/1.x max header size.
		 * </p>
		 *
		 * @param http1x_max_header_size the HTTP/1.x max header size
		 */
		public void setHttp1x_max_header_size(Integer http1x_max_header_size) {
			this.http1x_max_header_size = Settable.of(http1x_max_header_size);
		}

		/**
		 * <p>
		 * Returns the HTTP/1.x validate header flag.
		 * </p>
		 *
		 * @return a settable returning the HTTP/1.x validate header flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getHttp1x_validate_headers() {
			return http1x_validate_headers;
		}

		/**
		 * <p>
		 * Sets the HTTP/1.x validate header flag.
		 * </p>
		 *
		 * @param http1x_validate_headers the HTTP/1.x validate header flag
		 */
		public void setHttp1x_validate_headers(Boolean http1x_validate_headers) {
			this.http1x_validate_headers = Settable.of(http1x_validate_headers);
		}

		/**
		 * <p>
		 * Returns the HTTP/1.x max concurrent requests.
		 * </p>
		 *
		 * @return a settable returning the HTTP/1.x max concurrent requests or an unset settable if no value was set
		 */
		public Settable<Long> getHttp1_max_concurrent_requests() {
			return http1_max_concurrent_requests;
		}

		/**
		 * <p>
		 * Sets the HTTP/1.x max concurrent requests.
		 * </p>
		 *
		 * @param http1_max_concurrent_requests the HTTP/1.x max concurrent requests
		 */
		public void setHttp1_max_concurrent_requests(Long http1_max_concurrent_requests) {
			this.http1_max_concurrent_requests = Settable.of(http1_max_concurrent_requests);
		}

		/**
		 * <p>
		 * Returns the HTTP/2 header table size.
		 * </p>
		 *
		 * @return a settable returning the HTTP/2 header table size or an unset settable if no value was set
		 */
		public Settable<Long> getHttp2_header_table_size() {
			return http2_header_table_size;
		}

		/**
		 * <p>
		 * Sets the HTTP/2 header table size.
		 * </p>
		 *
		 * @param http2_header_table_size the HTTP/2 header table size
		 */
		public void setHttp2_header_table_size(Long http2_header_table_size) {
			this.http2_header_table_size = Settable.of(http2_header_table_size);
		}

		/**
		 * <p>
		 * Returns the HTTP/2 max concurrent streams.
		 * </p>
		 *
		 * @return a settable returning the HTTP/2 max concurrent streams or an unset settable if no value was set
		 */
		public Settable<Long> getHttp2_max_concurrent_streams() {
			return http2_max_concurrent_streams;
		}

		/**
		 * <p>
		 * Sets the HTTP/2 max concurrent streams.
		 * </p>
		 *
		 * @param http2_max_concurrent_streams the HTTP/2 max concurrent streams
		 */
		public void setHttp2_max_concurrent_streams(Long http2_max_concurrent_streams) {
			this.http2_max_concurrent_streams = Settable.of(http2_max_concurrent_streams);
		}

		/**
		 * <p>
		 * Returns the HTTP/2 initial window size.
		 * </p>
		 *
		 * @return a settable returning the HTTP/2 initial window size or an unset settable if no value was set
		 */
		public Settable<Integer> getHttp2_initial_window_size() {
			return http2_initial_window_size;
		}

		/**
		 * <p>
		 * Sets the HTTP/2 initial window size.
		 * </p>
		 *
		 * @param http2_initial_window_size the HTTP/2 initial window size
		 */
		public void setHttp2_initial_window_size(Integer http2_initial_window_size) {
			this.http2_initial_window_size = Settable.of(http2_initial_window_size);
		}

		/**
		 * <p>
		 * Returns the HTTP/2 max frame size.
		 * </p>
		 *
		 * @return a settable returning the HTTP/2 max frame size or an unset settable if no value was set
		 */
		public Settable<Integer> getHttp2_max_frame_size() {
			return http2_max_frame_size;
		}

		/**
		 * <p>
		 * Sets the HTTP/2 max frame size.
		 * </p>
		 *
		 * @param http2_max_frame_size the HTTP/2 max frame size
		 */
		public void setHttp2_max_frame_size(Integer http2_max_frame_size) {
			this.http2_max_frame_size = Settable.of(http2_max_frame_size);
		}

		/**
		 * <p>
		 * Returns the HTTP/2 max header list size.
		 * </p>
		 *
		 * @return a settable returning the HTTP/2 max header list size or an unset settable if no value was set
		 */
		public Settable<Integer> getHttp2_max_header_list_size() {
			return http2_max_header_list_size;
		}

		/**
		 * <p>
		 * Sets the HTTP/2 max header list size.
		 * </p>
		 *
		 * @param http2_max_header_list_size the HTTP/2 max header list size
		 */
		public void setHttp2_max_header_list_size(Integer http2_max_header_list_size) {
			this.http2_max_header_list_size = Settable.of(http2_max_header_list_size);
		}

		/**
		 * <p>
		 * Returns the HTTP/2 validate headers flag.
		 * </p>
		 *
		 * @return a settable returning the HTTP/2 validate headers flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getHttp2_validate_headers() {
			return http2_validate_headers;
		}

		/**
		 * <p>
		 * Sets the HTTP/2 validate headers flag.
		 * </p>
		 *
		 * @param http2_validate_headers the HTTP/2 validate headers flag
		 */
		public void setHttp2_validate_headers(Boolean http2_validate_headers) {
			this.http2_validate_headers = Settable.of(http2_validate_headers);
		}

		/**
		 * <p>
		 * Returns the WeSocket max frame size.
		 * </p>
		 *
		 * @return a settable returning the WebSocket max frame size or an unset settable if no value was set
		 */
		public Settable<Integer> getWs_max_frame_size() {
			return ws_max_frame_size;
		}

		/**
		 * <p>
		 * Sets the WebSocket max frame size.
		 * </p>
		 *
		 * @param ws_max_frame_size the WebSocket max frame size
		 */
		public void setWs_max_frame_size(Integer ws_max_frame_size) {
			this.ws_max_frame_size = Settable.of(ws_max_frame_size);
		}

		/**
		 * <p>
		 * Returns the WebSocket frame compression enabled flag.
		 * </p>
		 *
		 * @return a settable returning the WebSocket frame compression enabled flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getWs_frame_compression_enabled() {
			return ws_frame_compression_enabled;
		}

		/**
		 * <p>
		 * Sets the WebSocket frame compression enabled flag.
		 * </p>
		 *
		 * @param ws_frame_compression_enabled the WebSocket frame compression enabled flag
		 */
		public void setWs_frame_compression_enabled(Boolean ws_frame_compression_enabled) {
			this.ws_frame_compression_enabled = Settable.of(ws_frame_compression_enabled);
		}

		/**
		 * <p>
		 * Returns the WebSocket frame compression level.
		 * </p>
		 *
		 * @return a settable returning the WebSocket frame compression level or an unset settable if no value was set
		 */
		public Settable<Integer> getWs_frame_compression_level() {
			return ws_frame_compression_level;
		}

		/**
		 * <p>
		 * Sets the WebSocket frame compression level
		 * </p>
		 *
		 * @param ws_frame_compression_level the WebSocket frame compression level
		 */
		public void setWs_frame_compression_level(Integer ws_frame_compression_level) {
			this.ws_frame_compression_level = Settable.of(ws_frame_compression_level);
		}

		/**
		 * <p>
		 * Returns the WebSocket message compression enabled flag.
		 * </p>
		 *
		 * @return a settable returning the WebSocket message compression enabled flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getWs_message_compression_enabled() {
			return ws_message_compression_enabled;
		}

		/**
		 * <p>
		 * Sets the WebSocket message compression enabled flag.
		 * </p>
		 *
		 * @param ws_message_compression_enabled the WebSocket message compression enabled flag
		 */
		public void setWs_message_compression_enabled(Boolean ws_message_compression_enabled) {
			this.ws_message_compression_enabled = Settable.of(ws_message_compression_enabled);
		}

		/**
		 * <p>
		 * Returns the WebSocket message compression level.
		 * </p>
		 *
		 * @return a settable returning the WebSocket message compression level or an unset settable if no value was set
		 */
		public Settable<Integer> getWs_message_compression_level() {
			return ws_message_compression_level;
		}

		/**
		 * <p>
		 * Sets the WebSocket message compression level.
		 * </p>
		 *
		 * @param ws_message_compression_level the WebSocket message compression level
		 */
		public void setWs_message_compression_level(Integer ws_message_compression_level) {
			this.ws_message_compression_level = Settable.of(ws_message_compression_level);
		}

		/**
		 * <p>
		 * Returns the WebSocket message allow client window size flag.
		 * </p>
		 *
		 * @return a settable returning the WebSocket message allow client window size flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getWs_message_allow_client_window_size() {
			return ws_message_allow_client_window_size;
		}

		/**
		 * <p>
		 * Sets the WebSocket message allow client window size flag.
		 * </p>
		 *
		 * @param ws_message_allow_client_window_size the WebSocket message allow client window size flag
		 */
		public void setWs_message_allow_client_window_size(Boolean ws_message_allow_client_window_size) {
			this.ws_message_allow_client_window_size = Settable.of(ws_message_allow_client_window_size);
		}

		/**
		 * <p>
		 * Returns the WebSocket message requested server window size.
		 * </p>
		 *
		 * @return a settable returning the Websocket message requested server window size or an unset settable if no value was set
		 */
		public Settable<Integer> getWs_message_requested_server_window_size() {
			return ws_message_requested_server_window_size;
		}

		/**
		 * <p>
		 * Sets the WebSocket message requested server window size
		 * </p>
		 *
		 * @param ws_message_requested_server_window_size the WebSocket message requested server window size
		 */
		public void setWs_message_requested_server_window_size(Integer ws_message_requested_server_window_size) {
			this.ws_message_requested_server_window_size = Settable.of(ws_message_requested_server_window_size);
		}

		/**
		 * <p>
		 * Returns the WebSocket allow client no context flag.
		 * </p>
		 *
		 * @return a settable returning the WebSocket allow client no context flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getWs_message_allow_client_no_context() {
			return ws_message_allow_client_no_context;
		}

		/**
		 * <p>
		 * Sets the WebSocket allow client no context flag.
		 * </p>
		 *
		 * @param ws_message_allow_client_no_context the WebSocket allow client no context flag
		 */
		public void setWs_message_allow_client_no_context(Boolean ws_message_allow_client_no_context) {
			this.ws_message_allow_client_no_context = Settable.of(ws_message_allow_client_no_context);
		}

		/**
		 * <p>
		 * Returns WebSocket message requested server no context flag.
		 * </p>
		 *
		 * @return a settable returning the WebSocket message requested server no context flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getWs_message_requested_server_no_context() {
			return ws_message_requested_server_no_context;
		}

		/**
		 * <p>
		 * Sets the WebSocket message requested server no context flag.
		 * </p>
		 *
		 * @param ws_message_requested_server_no_context the WebSocket message requested no context flag
		 */
		public void setWs_message_requested_server_no_context(Boolean ws_message_requested_server_no_context) {
			this.ws_message_requested_server_no_context = Settable.of(ws_message_requested_server_no_context);
		}

		/**
		 * <p>
		 * Returns the WebSocket close on outbound complete flag.
		 * </p>
		 *
		 * @return a settable returning the WebSocket close on outbound complete flag or an unset settable if no value was set
		 */
		public Settable<Boolean> getWs_close_on_outbound_complete() {
			return ws_close_on_outbound_complete;
		}

		/**
		 * <p>
		 * Sets the WebSocket close on outbound complete flag.
		 * </p>
		 *
		 * @param ws_close_on_outbound_complete the WebSocket close on outbound complete flag
		 */
		public void setWs_close_on_outbound_complete(Boolean ws_close_on_outbound_complete) {
			this.ws_close_on_outbound_complete = Settable.of(ws_close_on_outbound_complete);
		}

		/**
		 * <p>
		 * Returns the WebSocket inbound close frame timeout.
		 * </p>
		 *
		 * @return a settable returning the WebSocket inbound close frame timeout or an unset settable if no value was set
		 */
		public Settable<Long> getWs_inbound_close_frame_timeout() {
			return ws_inbound_close_frame_timeout;
		}

		/**
		 * <p>
		 * Sets the WebSocket inbound close frame timeout.
		 * </p>
		 *
		 * @param ws_inbound_close_frame_timeout the WebSocket inbound close frame timeout
		 */
		public void setWs_inbound_close_frame_timeout(Long ws_inbound_close_frame_timeout) {
			this.ws_inbound_close_frame_timeout = Settable.of(ws_inbound_close_frame_timeout);
		}

		/**
		 * <p>
		 * Returns the proxy host.
		 * </p>
		 *
		 * @return a settable returning the proxy host or an unset settable if no value was set
		 */
		public Settable<String> getProxy_host() {
			return proxy_host;
		}

		/**
		 * <p>
		 * Sets the proxy host.
		 * </p>
		 *
		 * @param proxy_host the proxy host
		 */
		public void setProxy_host(String proxy_host) {
			this.proxy_host = Settable.of(proxy_host);
		}

		/**
		 * <p>
		 * Returns the proxy port.
		 * </p>
		 *
		 * @return a settable returning the proxy port or an unset settable if no value was set
		 */
		public Settable<Integer> getProxy_port() {
			return proxy_port;
		}

		/**
		 * <p>
		 * Sets the proxy port.
		 * </p>
		 *
		 * @param proxy_port the proxy port
		 */
		public void setProxy_port(Integer proxy_port) {
			this.proxy_port = Settable.of(proxy_port);
		}

		/**
		 * <p>
		 * Returns the proxy username.
		 * </p>
		 *
		 * @return a settable returning the proxy username or an unset settable if no value was set
		 */
		public Settable<String> getProxy_username() {
			return proxy_username;
		}

		/**
		 * <p>
		 * Sets the proxy username.
		 * </p>
		 *
		 * @param proxy_username the proxy username
		 */
		public void setProxy_username(String proxy_username) {
			this.proxy_username = Settable.of(proxy_username);
		}

		/**
		 * <p>
		 * Returns the proxy password.
		 * </p>
		 *
		 * @return a settable returning the proxy password or an unset settable if no value was set
		 */
		public Settable<String> getProxy_password() {
			return proxy_password;
		}

		/**
		 * <p>
		 * Sets the proxy password.
		 * </p>
		 *
		 * @param proxy_password the proxy password
		 */
		public void setProxy_password(String proxy_password) {
			this.proxy_password = Settable.of(proxy_password);
		}

		/**
		 * <p>
		 * Returns the proxy protocol.
		 * </p>
		 *
		 * @return a settable returning the proxy protocol or an unset settable if no value was set
		 */
		public Settable<io.inverno.mod.http.client.HttpClientConfiguration.ProxyProtocol> getProxy_protocol() {
			return proxy_protocol;
		}

		/**
		 * <p>
		 * Sets the proxy protocol.
		 * </p>
		 *
		 * @param proxy_protocol the proxy protocol
		 */
		public void setProxy_protocol(io.inverno.mod.http.client.HttpClientConfiguration.ProxyProtocol proxy_protocol) {
			this.proxy_protocol = Settable.of(proxy_protocol);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			HttpClientConfiguration that = (HttpClientConfiguration) o;
			return Objects.equals(client_event_loop_group_size, that.client_event_loop_group_size) && Objects.equals(pool_max_size, that.pool_max_size) && Objects.equals(pool_clean_period, that.pool_clean_period) && Objects.equals(pool_buffer_size, that.pool_buffer_size) && Objects.equals(pool_keep_alive_timeout, that.pool_keep_alive_timeout) && Objects.equals(pool_connect_timeout, that.pool_connect_timeout) && Objects.equals(request_timeout, that.request_timeout) && Objects.equals(graceful_shutdown_timeout, that.graceful_shutdown_timeout) && Objects.equals(http_protocol_versions, that.http_protocol_versions) && Objects.equals(send_user_agent, that.send_user_agent) && Objects.equals(user_agent, that.user_agent) && Objects.equals(compression_enabled, that.compression_enabled) && Objects.equals(decompression_enabled, that.decompression_enabled) && Objects.equals(compression_contentSizeThreshold, that.compression_contentSizeThreshold) && Objects.equals(compression_brotli_quality, that.compression_brotli_quality) && Objects.equals(compression_brotli_window, that.compression_brotli_window) && Objects.equals(compression_brotli_mode, that.compression_brotli_mode) && Objects.equals(compression_deflate_compressionLevel, that.compression_deflate_compressionLevel) && Objects.equals(compression_deflate_windowBits, that.compression_deflate_windowBits) && Objects.equals(compression_deflate_memLevel, that.compression_deflate_memLevel) && Objects.equals(compression_gzip_compressionLevel, that.compression_gzip_compressionLevel) && Objects.equals(compression_gzip_windowBits, that.compression_gzip_windowBits) && Objects.equals(compression_gzip_memLevel, that.compression_gzip_memLevel) && Objects.equals(compression_zstd_blockSize, that.compression_zstd_blockSize) && Objects.equals(compression_zstd_compressionLevel, that.compression_zstd_compressionLevel) && Objects.equals(compression_zstd_maxEncodeSize, that.compression_zstd_maxEncodeSize) && Objects.equals(tls_key_store, that.tls_key_store) && Objects.equals(tls_key_store_type, that.tls_key_store_type) && Objects.equals(tls_key_store_password, that.tls_key_store_password) && Objects.equals(tls_key_alias, that.tls_key_alias) && Objects.equals(tls_key_alias_password, that.tls_key_alias_password) && Objects.equals(tls_trust_store, that.tls_trust_store) && Objects.equals(tls_trust_store_type, that.tls_trust_store_type) && Objects.equals(tls_trust_store_password, that.tls_trust_store_password) && Objects.equals(tls_trust_all, that.tls_trust_all) && Objects.equals(tls_send_sni, that.tls_send_sni) && Objects.equals(tls_ciphers_includes, that.tls_ciphers_includes) && Objects.equals(tls_ciphers_excludes, that.tls_ciphers_excludes) && Objects.equals(http1x_initial_buffer_size, that.http1x_initial_buffer_size) && Objects.equals(http1x_max_initial_line_length, that.http1x_max_initial_line_length) && Objects.equals(http1x_max_chunk_size, that.http1x_max_chunk_size) && Objects.equals(http1x_max_header_size, that.http1x_max_header_size) && Objects.equals(http1x_validate_headers, that.http1x_validate_headers) && Objects.equals(http1_max_concurrent_requests, that.http1_max_concurrent_requests) && Objects.equals(http2_header_table_size, that.http2_header_table_size) && Objects.equals(http2_max_concurrent_streams, that.http2_max_concurrent_streams) && Objects.equals(http2_initial_window_size, that.http2_initial_window_size) && Objects.equals(http2_max_frame_size, that.http2_max_frame_size) && Objects.equals(http2_max_header_list_size, that.http2_max_header_list_size) && Objects.equals(http2_validate_headers, that.http2_validate_headers) && Objects.equals(ws_max_frame_size, that.ws_max_frame_size) && Objects.equals(ws_frame_compression_enabled, that.ws_frame_compression_enabled) && Objects.equals(ws_frame_compression_level, that.ws_frame_compression_level) && Objects.equals(ws_message_compression_enabled, that.ws_message_compression_enabled) && Objects.equals(ws_message_compression_level, that.ws_message_compression_level) && Objects.equals(ws_message_allow_client_window_size, that.ws_message_allow_client_window_size) && Objects.equals(ws_message_requested_server_window_size, that.ws_message_requested_server_window_size) && Objects.equals(ws_message_allow_client_no_context, that.ws_message_allow_client_no_context) && Objects.equals(ws_message_requested_server_no_context, that.ws_message_requested_server_no_context) && Objects.equals(ws_close_on_outbound_complete, that.ws_close_on_outbound_complete) && Objects.equals(ws_inbound_close_frame_timeout, that.ws_inbound_close_frame_timeout) && Objects.equals(proxy_host, that.proxy_host) && Objects.equals(proxy_port, that.proxy_port) && Objects.equals(proxy_username, that.proxy_username) && Objects.equals(proxy_password, that.proxy_password) && Objects.equals(proxy_protocol, that.proxy_protocol);
		}

		@Override
		public int hashCode() {
			return Objects.hash(client_event_loop_group_size, pool_max_size, pool_clean_period, pool_buffer_size, pool_keep_alive_timeout, pool_connect_timeout, request_timeout, graceful_shutdown_timeout, http_protocol_versions, send_user_agent, user_agent, compression_enabled, decompression_enabled, compression_contentSizeThreshold, compression_brotli_quality, compression_brotli_window, compression_brotli_mode, compression_deflate_compressionLevel, compression_deflate_windowBits, compression_deflate_memLevel, compression_gzip_compressionLevel, compression_gzip_windowBits, compression_gzip_memLevel, compression_zstd_blockSize, compression_zstd_compressionLevel, compression_zstd_maxEncodeSize, tls_key_store, tls_key_store_type, tls_key_store_password, tls_key_alias, tls_key_alias_password, tls_trust_store, tls_trust_store_type, tls_trust_store_password, tls_trust_all, tls_send_sni, tls_ciphers_includes, tls_ciphers_excludes, http1x_initial_buffer_size, http1x_max_initial_line_length, http1x_max_chunk_size, http1x_max_header_size, http1x_validate_headers, http1_max_concurrent_requests, http2_header_table_size, http2_max_concurrent_streams, http2_initial_window_size, http2_max_frame_size, http2_max_header_list_size, http2_validate_headers, ws_max_frame_size, ws_frame_compression_enabled, ws_frame_compression_level, ws_message_compression_enabled, ws_message_compression_level, ws_message_allow_client_window_size, ws_message_requested_server_window_size, ws_message_allow_client_no_context, ws_message_requested_server_no_context, ws_close_on_outbound_complete, ws_inbound_close_frame_timeout, proxy_host, proxy_port, proxy_username, proxy_password, proxy_protocol);
		}
	}

	/**
	 * <p>
	 * Describes an HTTP traffic load balancer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "strategy", visible = true)
	@JsonSubTypes({
//		@JsonSubTypes.Type(value = ConsistentHashLoadBalancerDescriptor.class, name = "CONSISTENT_HASH"),
		@JsonSubTypes.Type(value = LoadBalancerDescriptor.class, names = { "RANDOM", "ROUND_ROBIN" }),
		@JsonSubTypes.Type(value = LeastRequestLoadBalancerDescriptor.class, names = { "LEAST_REQUEST" }),
		@JsonSubTypes.Type(value = MinLoadFactorLoadBalancerDescriptor.class, names = { "MIN_LOAD_FACTOR" })
	})
	public static class LoadBalancerDescriptor {

		private final HttpTrafficPolicy.LoadBalancingStrategy strategy;

		/**
		 * <p>
		 * Creates HTTP traffic load balancer descriptor.
		 * </p>
		 *
		 * @param strategy a load balancing strategy
		 */
		@JsonCreator
		public LoadBalancerDescriptor(@JsonProperty("strategy") HttpTrafficPolicy.LoadBalancingStrategy strategy) {
			this.strategy = strategy;
		}

		/**
		 * <p>
		 * Returns the load balancing strategy.
		 * </p>
		 *
		 * @return the load balancing strategy
		 */
		@JsonProperty("strategy")
		public HttpTrafficPolicy.LoadBalancingStrategy getStrategy() {
			return strategy;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			LoadBalancerDescriptor that = (LoadBalancerDescriptor) o;
			return strategy == that.strategy;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(strategy);
		}
	}

	/**
	 * <p>
	 * Describes least request traffic load balancer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public static class LeastRequestLoadBalancerDescriptor extends LoadBalancerDescriptor {

		private int choiceCount = LeastRequestTrafficLoadBalancer.DEFAULT_CHOICE_COUNT;
		private int bias = LeastRequestTrafficLoadBalancer.DEFAULT_BIAS;

		/**
		 * <p>
		 * Creates a least request load balancer descriptor.
		 * </p>
		 */
		public LeastRequestLoadBalancerDescriptor() {
			super(HttpTrafficPolicy.LoadBalancingStrategy.LEAST_REQUEST);
		}

		/**
		 * <p>
		 * Returns the choice count.
		 * </p>
		 *
		 * @return the choice count
		 */
		@JsonProperty("choiceCount")
		public int getChoiceCount() {
			return choiceCount;
		}

		/**
		 * <p>
		 * Sets the choice count.
		 * </p>
		 *
		 * @param choiceCount the choice count
		 */
		@JsonProperty("choiceCount")
		public void setChoiceCount(int choiceCount) {
			this.choiceCount = choiceCount;
		}

		/**
		 * <p>
		 * Returns the active requests bias.
		 * </p>
		 *
		 * @return the active requests bias
		 */
		@JsonProperty("bias")
		public int getBias() {
			return bias;
		}

		/**
		 * <p>
		 * Sets the active requests bias.
		 * </p>
		 *
		 * @param bias the active requests bias
		 */
		@JsonProperty("bias")
		public void setBias(int bias) {
			this.bias = bias;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			LeastRequestLoadBalancerDescriptor that = (LeastRequestLoadBalancerDescriptor) o;
			return choiceCount == that.choiceCount && bias == that.bias;
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), choiceCount, bias);
		}
	}

	/**
	 * <p>
	 * Describes min load factor traffic load balancer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public static class MinLoadFactorLoadBalancerDescriptor extends LoadBalancerDescriptor {

		private int choiceCount = MinLoadFactorTrafficLoadBalancer.DEFAULT_CHOICE_COUNT;
		private int bias = LeastRequestTrafficLoadBalancer.DEFAULT_BIAS;

		/**
		 * <p>
		 * Creates min load factory load balancer descriptor.
		 * </p>
		 */
		public MinLoadFactorLoadBalancerDescriptor() {
			super(HttpTrafficPolicy.LoadBalancingStrategy.MIN_LOAD_FACTOR);
		}

		/**
		 * <p>
		 * Returns the choice count.
		 * </p>
		 *
		 * @return the choice count
		 */
		@JsonProperty("choiceCount")
		public int getChoiceCount() {
			return choiceCount;
		}

		/**
		 * <p>
		 * Sets the choice count.
		 * </p>
		 *
		 * @param choiceCount the choice count
		 */
		@JsonProperty("choiceCount")
		public void setChoiceCount(int choiceCount) {
			this.choiceCount = choiceCount;
		}

		/**
		 * <p>
		 * Returns the load factor bias.
		 * </p>
		 *
		 * @return the load factor bias
		 */
		@JsonProperty("bias")
		public int getBias() {
			return bias;
		}

		/**
		 * <p>
		 * Sets the load factor bias.
		 * </p>
		 *
		 * @param bias the load factor bias
		 */
		@JsonProperty("bias")
		public void setBias(int bias) {
			this.bias = bias;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			MinLoadFactorLoadBalancerDescriptor that = (MinLoadFactorLoadBalancerDescriptor) o;
			return choiceCount == that.choiceCount && bias == that.bias;
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), choiceCount, bias);
		}
	}

	/*public static class ConsistentHashLoadBalancerDescriptor extends LoadBalancerDescriptor {

		public ConsistentHashLoadBalancerDescriptor() {
			super(HttpTrafficPolicy.LoadBalancingStrategy.CONSISTENT_HASH);
		}
	}*/

	/**
	 * <p>
	 * Describes an HTTP route.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class RouteDescriptor {

		private final HttpClientConfiguration configuration;

		private final LoadBalancerDescriptor loadBalancer;

		private final Set<? extends ValueMatcher> authorityMatchers;
		private final Set<PathMatcher> pathMatchers;
		private final Set<Method> methodMatchers;
		private final Set<String> contentTypeMatchers;
		private final Set<String> acceptMatchers;
		private final Set<String> languageMatchers;
		private final Map<String, Set<? extends ValueMatcher>> headersMatchers;
		private final Map<String, Set<? extends ValueMatcher>> queryParameterMatchers;

		private final RequestTransformer transformRequest;
		private final ResponseTransformer transformResponse;
		private final List<DestinationDescriptor> destinations;

		/**
		 * <p>
		 * Creates an HTTP route descriptor.
		 * </p>
		 *
		 * @param configuration          the HTTP client configuration
		 * @param loadBalancer           the load balancer descriptor
		 * @param authorityMatchers      the authority matchers
		 * @param pathMatchers           the path matchers
		 * @param methodMatchers         the method matchers
		 * @param contentTypeMatchers    the content type matchers
		 * @param acceptMatchers         the accept matchers
		 * @param languageMatchers       the language matchers
		 * @param headersMatchers        the headers matchers
		 * @param queryParameterMatchers the query parameters matchers
		 * @param transformRequest       the request transformer
		 * @param transformResponse      the response transformer
		 * @param destinations           the route destinations
		 */
		@JsonCreator
		public RouteDescriptor(
				@JsonProperty("configuration") HttpClientConfiguration configuration,
				@JsonProperty("loadBalancer") LoadBalancerDescriptor loadBalancer,
				@JsonProperty("authority") Set<? extends ValueMatcher> authorityMatchers,
				@JsonProperty("path") Set<PathMatcher> pathMatchers,
				@JsonProperty("method") Set<Method> methodMatchers,
				@JsonProperty("contentType") Set<String> contentTypeMatchers,
				@JsonProperty("accept") Set<String> acceptMatchers,
				@JsonProperty("language") Set<String> languageMatchers,
				@JsonProperty("headers") Map<String, Set<? extends ValueMatcher>> headersMatchers,
				@JsonProperty("queryParameters") Map<String, Set<? extends ValueMatcher>> queryParameterMatchers,
				@JsonProperty("transformRequest") RequestTransformer transformRequest,
				@JsonProperty("transformResponse") ResponseTransformer transformResponse,
				@JsonProperty("destinations") List<DestinationDescriptor> destinations
			) {
			this.configuration = configuration;
			this.loadBalancer = loadBalancer;

			this.authorityMatchers = authorityMatchers;
			this.pathMatchers = pathMatchers;
			this.methodMatchers = methodMatchers;
			this.contentTypeMatchers = contentTypeMatchers;
			this.acceptMatchers = acceptMatchers;
			this.languageMatchers = languageMatchers;
			this.headersMatchers = headersMatchers;
			this.queryParameterMatchers = queryParameterMatchers;

			this.transformRequest = transformRequest;
			this.transformResponse = transformResponse;
			this.destinations = destinations;
		}

		/**
		 * <p>
		 * Returns the HTTP client configuration.
		 * </p>
		 *
		 * @return the HTTP client configuration
		 */
		@JsonProperty("configuration")
		public HttpClientConfiguration getConfiguration() {
			return configuration;
		}

		/**
		 * <p>
		 * Returns the load balancer descriptor.
		 * </p>
		 *
		 * @return the load balancer descriptor
		 */
		@JsonProperty("loadBalancer")
		public LoadBalancerDescriptor getLoadBalancer() {
			return loadBalancer;
		}

		/**
		 * <p>
		 * Returns the authority matchers.
		 * </p>
		 *
		 * @return the authority matchers
		 */
		@JsonProperty("authority")
		public Set<? extends ValueMatcher> getAuthorityMatchers() {
			return authorityMatchers;
		}

		/**
		 * <p>
		 * Returns the path matchers.
		 * </p>
		 *
		 * @return the path matchers
		 */
		@JsonProperty("path")
		public Set<PathMatcher> getPathMatchers() {
			return pathMatchers;
		}

		/**
		 * <p>
		 * Returns the method matchers.
		 * </p>
		 *
		 * @return the method matchers
		 */
		@JsonProperty("method")
		public Set<Method> getMethodMatchers() {
			return methodMatchers;
		}

		/**
		 * <p>
		 * Returns the content type matchers.
		 * </p>
		 *
		 * @return the content type matchers
		 */
		@JsonProperty("contentType")
		public Set<String> getContentTypeMatchers() {
			return contentTypeMatchers;
		}

		/**
		 * <p>
		 * Returns the accept matchers.
		 * </p>
		 *
		 * @return the accept matchers
		 */
		@JsonProperty("accept")
		public Set<String> getAcceptMatchers() {
			return acceptMatchers;
		}

		/**
		 * <p>
		 * Returns the language matchers.
		 * </p>
		 *
		 * @return the language matchers
		 */
		@JsonProperty("language")
		public Set<String> getLanguageMatchers() {
			return languageMatchers;
		}

		/**
		 * <p>
		 * Returns the headers matchers.
		 * </p>
		 *
		 * @return the headers matchers
		 */
		@JsonProperty("headers")
		public Map<String, Set<? extends ValueMatcher>> getHeadersMatchers() {
			return headersMatchers;
		}

		/**
		 * <p>
		 * Returns the query parameter matchers.
		 * </p>
		 *
		 * @return the query parameter matchers
		 */
		@JsonProperty("queryParameters")
		public Map<String, Set<? extends ValueMatcher>> getQueryParameterMatchers() {
			return queryParameterMatchers;
		}

		/**
		 * <p>
		 * Returns the request transformer.
		 * </p>
		 *
		 * @return the request transformer
		 */
		@JsonProperty("transformRequest")
		public RequestTransformer getTransformRequest() {
			return transformRequest;
		}

		/**
		 * <p>
		 * Returns the response transformer.
		 * </p>
		 *
		 * @return the response transformer
		 */
		@JsonProperty("transformResponse")
		public ResponseTransformer getTransformResponse() {
			return transformResponse;
		}

		/**
		 * <p>
		 * Returns the route destinations.
		 * </p>
		 *
		 * @return the route destinations
		 */
		@JsonProperty("destinations")
		public List<DestinationDescriptor> getDestinations() {
			return destinations;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RouteDescriptor that = (RouteDescriptor) o;
			return Objects.equals(configuration, that.configuration) && Objects.equals(loadBalancer, that.loadBalancer) && Objects.equals(authorityMatchers, that.authorityMatchers) && Objects.equals(pathMatchers, that.pathMatchers) && Objects.equals(methodMatchers, that.methodMatchers) && Objects.equals(contentTypeMatchers, that.contentTypeMatchers) && Objects.equals(acceptMatchers, that.acceptMatchers) && Objects.equals(languageMatchers, that.languageMatchers) && Objects.equals(headersMatchers, that.headersMatchers) && Objects.equals(queryParameterMatchers, that.queryParameterMatchers) && Objects.equals(transformRequest, that.transformRequest) && Objects.equals(transformResponse, that.transformResponse) && Objects.equals(destinations, that.destinations);
		}

		@Override
		public int hashCode() {
			return Objects.hash(configuration, loadBalancer, authorityMatchers, pathMatchers, methodMatchers, contentTypeMatchers, acceptMatchers, languageMatchers, headersMatchers, queryParameterMatchers, transformRequest, transformResponse, destinations);
		}
	}

	/**
	 * <p>
	 * Describes a path matcher.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class PathMatcher {

		private final String path;
		private final boolean matchTrailingSlash;

		/**
		 * <p>
		 * Creates a path matcher.
		 * </p>
		 *
		 * @param path               a static path or a path pattern
		 * @param matchTrailingSlash true to match trailing slash, false otherwise
		 */
		@JsonCreator
		public PathMatcher(@JsonProperty("path") String path, @JsonProperty("matchTrailingSlash") boolean matchTrailingSlash) {
			this.path = path;
			this.matchTrailingSlash = matchTrailingSlash;
		}

		/**
		 * <p>
		 * Returns the path.
		 * </p>
		 *
		 * @return the path
		 */
		@JsonProperty("path")
		public String getPath() {
			return path;
		}

		/**
		 * <p>
		 * Determines whether the trailing slash must be matched.
		 * </p>
		 *
		 * @return true if the trailing slash is matched, false otherwise
		 */
		@JsonProperty("matchTrailingSlash")
		public boolean isMatchTrailingSlash() {
			return matchTrailingSlash;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			PathMatcher that = (PathMatcher) o;
			return matchTrailingSlash == that.matchTrailingSlash && Objects.equals(path, that.path);
		}

		@Override
		public int hashCode() {
			return Objects.hash(path, matchTrailingSlash);
		}
	}

	/**
	 * <p>
	 * Describes a value matcher.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
	@JsonSubTypes({
		@JsonSubTypes.Type(value = StaticValueMatcher.class),
		@JsonSubTypes.Type(value = RegexValueMatcher.class)
	})
	public static abstract class ValueMatcher {

		/**
		 * <p>
		 * Designates a kind of match.
		 * </p>
		 *
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.12
		 */
		public enum Kind {
			/**
			 * <p>
			 * Static match.
			 * </p>
			 */
			STATIC,
			/**
			 * <p>
			 * Regex match.
			 * </p>
			 */
			REGEX
		}

		private final Kind kind;

		/**
		 * <p>
		 * Creates a value matcher.
		 * </p>
		 *
		 * @param kind the match kind
		 */
		protected ValueMatcher(Kind kind) {
			this.kind = kind;
		}

		/**
		 * <p>
		 * Returns the value matcher kind.
		 * </p>
		 *
		 * @return the value matcher kind
		 */
		@JsonIgnore
		public Kind getKind() {
			return kind;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ValueMatcher that = (ValueMatcher) o;
			return kind == that.kind;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(kind);
		}
	}

	/**
	 * <p>
	 * Describes a static value matcher.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class StaticValueMatcher extends ValueMatcher {

		private final String value;

		/**
		 * <p>
		 * Creates a static value matcher.
		 * </p>
		 *
		 * @param value a static value
		 */
		@JsonCreator
		public StaticValueMatcher(@JsonProperty("value") String value) {
			super(Kind.STATIC);
			this.value = value;
		}

		@JsonProperty("value")
		public String getValue() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			StaticValueMatcher that = (StaticValueMatcher) o;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), value);
		}
	}

	/**
	 * <p>
	 * Describes a regex value matcher.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class RegexValueMatcher extends ValueMatcher {

		private final String regex;

		/**
		 * <p>
		 * Creates a regex value matcher.
		 * </p>
		 *
		 * @param regex a regex
		 */
		@JsonCreator
		public RegexValueMatcher(@JsonProperty("regex") String regex) {
			super(Kind.REGEX);
			this.regex = regex;
		}

		@JsonProperty("regex")
		public String getRegex() {
			return regex;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			RegexValueMatcher that = (RegexValueMatcher) o;
			return Objects.equals(regex, that.regex);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), regex);
		}
	}

	/**
	 * <p>
	 * Describes a route destination.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class DestinationDescriptor {

		private final HttpClientConfiguration configuration;
		private final LoadBalancerDescriptor loadBalancer;
		private final URI uri;
		private final Integer weight;
		private final RequestTransformer transformRequest;
		private final ResponseTransformer transformResponse;

		/**
		 * <p>
		 * Creates a route destination.
		 * </p>
		 *
		 * @param configuration     the HTTP client configuration
		 * @param loadBalancer      the load balancer descriptor
		 * @param uri               the destination URI
		 * @param weight            the destination weight
		 * @param transformRequest  the request transformer
		 * @param transformResponse the response transformer
		 *
		 * @throws IllegalArgumentException if the specified weight is invalid or if the specified URI is not a valid service URI
		 */
		@JsonCreator
		public DestinationDescriptor(
				@JsonProperty("configuration") HttpClientConfiguration configuration,
				@JsonProperty("loadBalancer") LoadBalancerDescriptor loadBalancer,
				@JsonProperty("uri") URI uri,
				@JsonProperty("weight") Integer weight,
				@JsonProperty("transformRequest") RequestTransformer transformRequest,
				@JsonProperty("transformResponse") ResponseTransformer transformResponse
			) throws IllegalArgumentException {
			this.configuration = configuration;
			this.loadBalancer = loadBalancer;
			this.uri = uri;
			this.weight = weight;
			this.transformRequest = transformRequest;
			this.transformResponse = transformResponse;

			// Validate that URI is a valid service request URI
			ServiceID.of(this.uri);

			if(this.weight != null && this.weight <= 0) {
				throw new IllegalArgumentException("weight must be a positive integer");
			}
		}

		/**
		 * <p>
		 * Returns the HTTP client configuration.
		 * </p>
		 *
		 * @return the HTTP client configuration
		 */
		@JsonProperty("configuration")
		public HttpClientConfiguration getConfiguration() {
			return configuration;
		}

		/**
		 * <p>
		 * Returns the load balancer descriptor.
		 * </p>
		 *
		 * @return the load balancer descriptor
		 */
		@JsonProperty("loadBalancer")
		public LoadBalancerDescriptor getLoadBalancer() {
			return loadBalancer;
		}

		/**
		 * <p>
		 * Returns the destination URI.
		 * </p>
		 *
		 * @return the destination URI
		 */
		@JsonProperty("uri")
		public URI getURI() {
			return uri;
		}

		/**
		 * <p>
		 * Returns the destination weight.
		 * </p>
		 *
		 * @return the destination weight
		 */
		@JsonProperty("weight")
		public Integer getWeight() {
			return weight != null ? weight : 1;
		}

		/**
		 * <p>
		 * Returns the request transformer.
		 * </p>
		 *
		 * @return the request transformer
		 */
		@JsonProperty("transformRequest")
		public RequestTransformer getTransformRequest() {
			return transformRequest;
		}

		/**
		 * <p>
		 * Returns the response transformer.
		 * </p>
		 *
		 * @return the response transformer
		 */
		@JsonProperty("transformResponse")
		public ResponseTransformer getTransformResponse() {
			return transformResponse;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			DestinationDescriptor that = (DestinationDescriptor) o;
			return Objects.equals(configuration, that.configuration) && Objects.equals(loadBalancer, that.loadBalancer) && Objects.equals(uri, that.uri) && Objects.equals(weight, that.weight) && Objects.equals(transformRequest, that.transformRequest) && Objects.equals(transformResponse, that.transformResponse);
		}

		@Override
		public int hashCode() {
			return Objects.hash(configuration, loadBalancer, uri, weight, transformRequest, transformResponse);
		}
	}

	/**
	 * <p>
	 * Describes a request transformer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class RequestTransformer {

		private final Map<String, String> translatePath;
		private final String setAuthority;
		private final Map<String, String> addHeaders;
		private final Map<String, String> setHeaders;
		private final Set<String> removeHeaders;

		/**
		 * <p>
		 * Creates a request transformer.
		 * </p>
		 *
		 * @param translatePath the path translators
		 * @param setAuthority  the request authority to set
		 * @param addHeaders    the headers to add
		 * @param setHeaders    the headers to set
		 * @param removeHeaders the headers to remove
		 */
		@JsonCreator
		public RequestTransformer(
				@JsonProperty("translatePath") Map<String, String> translatePath,
				@JsonProperty("setAuthority") String setAuthority,
				@JsonProperty("addHeaders") Map<String, String> addHeaders,
				@JsonProperty("setHeaders") Map<String, String> setHeaders,
				@JsonProperty("removeHeaders") Set<String> removeHeaders
			) {
			this.translatePath = translatePath;
			this.setAuthority = setAuthority;
			this.addHeaders = addHeaders;
			this.setHeaders = setHeaders;
			this.removeHeaders = removeHeaders;
		}

		/**
		 * <p>
		 * Returns the path translators.
		 * </p>
		 *
		 * @return the path translators
		 */
		@JsonProperty("translatePath")
		public Map<String, String> getTranslatePath() {
			return translatePath;
		}

		/**
		 * <p>
		 * Returns the request authority to set.
		 * </p>
		 *
		 * @return the request authority to set
		 */
		@JsonProperty("setAuthority")
		public String getSetAuthority() {
			return setAuthority;
		}

		/**
		 * <p>
		 * Returns the headers to add.
		 * </p>
		 *
		 * @return the headers to add
		 */
		@JsonProperty("addHeaders")
		public Map<String, String> getAddHeaders() {
			return addHeaders;
		}

		/**
		 * <p>
		 * Returns the headers to set.
		 * </p>
		 *
		 * @return the headers to set
		 */
		@JsonProperty("setHeaders")
		public Map<String, String> getSetHeaders() {
			return setHeaders;
		}

		/**
		 * <p>
		 * Returns the headers to remove.
		 * </p>
		 *
		 * @return the headers ro remove
		 */
		@JsonProperty("removeHeaders")
		public Set<String> getRemoveHeaders() {
			return removeHeaders;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RequestTransformer that = (RequestTransformer) o;
			return Objects.equals(translatePath, that.translatePath) && Objects.equals(setAuthority, that.setAuthority) && Objects.equals(addHeaders, that.addHeaders) && Objects.equals(setHeaders, that.setHeaders) && Objects.equals(removeHeaders, that.removeHeaders);
		}

		@Override
		public int hashCode() {
			return Objects.hash(translatePath, setAuthority, addHeaders, setHeaders, removeHeaders);
		}
	}

	/**
	 * <p>
	 * Describes a response transformer.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ResponseTransformer {

		private final Map<String, String> addHeaders;
		private final Map<String, String> setHeaders;
		private final Set<String> removeHeaders;

		/**
		 * <p>
		 * Creates a response transformer.
		 * </p>
		 *
		 * @param addHeaders    the headers to add
		 * @param setHeaders    the headers to set
		 * @param removeHeaders the headers to remove
		 */
		@JsonCreator
		public ResponseTransformer(
			@JsonProperty("addHeaders") Map<String, String> addHeaders,
			@JsonProperty("setHeaders") Map<String, String> setHeaders,
			@JsonProperty("removeHeaders") Set<String> removeHeaders
		) {
			this.addHeaders = addHeaders;
			this.setHeaders = setHeaders;
			this.removeHeaders = removeHeaders;
		}

		/**
		 * <p>
		 * Returns the headers to add.
		 * </p>
		 *
		 * @return the headers to add
		 */
		@JsonProperty("addHeaders")
		public Map<String, String> getAddHeaders() {
			return addHeaders;
		}

		/**
		 * <p>
		 * Returns the headers to set.
		 * </p>
		 *
		 * @return the headers to set
		 */
		@JsonProperty("setHeaders")
		public Map<String, String> getSetHeaders() {
			return setHeaders;
		}

		/**
		 * <p>
		 * Returns the headers to remove.
		 * </p>
		 *
		 * @return the headers to remove
		 */
		@JsonProperty("removeHeaders")
		public Set<String> getRemoveHeaders() {
			return removeHeaders;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ResponseTransformer that = (ResponseTransformer) o;
			return Objects.equals(addHeaders, that.addHeaders) && Objects.equals(setHeaders, that.setHeaders) && Objects.equals(removeHeaders, that.removeHeaders);
		}

		@Override
		public int hashCode() {
			return Objects.hash(addHeaders, setHeaders, removeHeaders);
		}
	}
}
