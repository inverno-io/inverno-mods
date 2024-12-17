/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal.http2;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.internal.CompressionOptionsProvider;
import io.inverno.mod.http.client.internal.EndpointChannelConfigurer;
import io.inverno.mod.http.client.internal.HttpConnectionFactory;
import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.CompressorHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import java.util.Optional;

/**
 * <p>
 * Http/2 {@link HttpConnectionFactory} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class Http2ConnectionFactory implements HttpConnectionFactory<Http2Connection> {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final CompressionOptionsProvider compressionOptionsProvider;

	/**
	 * <p>
	 * Creates an Http/2 connection factory.
	 * </p>
	 * 
	 * @param headerService              the header service
	 * @param parameterConverter         the parameter converter
	 * @param compressionOptionsProvider the compression option provider
	 */
	public Http2ConnectionFactory(HeaderService headerService, ObjectConverter<String> parameterConverter, CompressionOptionsProvider compressionOptionsProvider) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.compressionOptionsProvider = compressionOptionsProvider;
	}
	
	@Override
	public Http2Connection create(HttpClientConfiguration configuration, HttpVersion httpVersion, EndpointChannelConfigurer configurer) {
		return new Http2ConnectionFactory.Http2ChannelHandlerBuilder(configuration).build();
	}

	/**
	 * <p>
	 * HTTP/2 channel handler builder.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.6
	 */
	private class Http2ChannelHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2Connection, Http2ChannelHandlerBuilder> {

		private final HttpClientConfiguration configuration;
		
		/**
		 * <p>
		 * Creates an HTTP/2 channel handler builder.
		 * </p>
		 * 
		 * @param configuration the HTTP client configuration
		 */
		public Http2ChannelHandlerBuilder(HttpClientConfiguration configuration) {
			this.configuration = configuration;
			
			Http2Settings initialSettings = this.initialSettings();
			
			Optional.ofNullable(this.configuration.http2_header_table_size()).ifPresent(initialSettings::headerTableSize);
			Optional.ofNullable(this.configuration.http2_max_concurrent_streams()).ifPresent(initialSettings::maxConcurrentStreams);
			Optional.ofNullable(this.configuration.http2_initial_window_size()).ifPresent(initialSettings::initialWindowSize);
			Optional.ofNullable(this.configuration.http2_max_frame_size()).ifPresent(initialSettings::maxFrameSize);
			Optional.ofNullable(this.configuration.http2_max_header_list_size()).ifPresent(initialSettings::maxHeaderListSize);
			
			this.gracefulShutdownTimeoutMillis(this.configuration.graceful_shutdown_timeout());
			this.validateHeaders(this.configuration.http2_validate_headers());
			this.server(false);
		}

		@Override
		protected Http2Connection build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) throws Exception {
			// compression is done by specifying the content-encoding header, unlike for server we don't need to determine the content encoding from the accept-encoding header
			if(this.configuration.compression_enabled()) {
				encoder = new CompressorHttp2ConnectionEncoder(
					encoder,
					Http2ConnectionFactory.this.compressionOptionsProvider.get()
				);
			}
			
			Http2Connection connection = new Http2Connection(
				decoder, 
				encoder, 
				initialSettings, 
				this.configuration, 
				Http2ConnectionFactory.this.headerService,
				Http2ConnectionFactory.this.parameterConverter
			);
			this.frameListener(connection);
			return connection;
		}

		@Override
		public Http2Connection build() {
			return super.build();
		}
	}
}
