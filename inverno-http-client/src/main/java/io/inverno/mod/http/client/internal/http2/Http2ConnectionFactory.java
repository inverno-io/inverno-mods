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
package io.inverno.mod.http.client.internal.http2;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.CompressionOptionsProvider;
import io.inverno.mod.http.client.internal.EndpointChannelConfigurer;
import io.inverno.mod.http.client.internal.HttpConnectionFactory;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.CompressorHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import java.util.Optional;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class Http2ConnectionFactory implements HttpConnectionFactory<Http2Connection> {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	private final MultipartEncoder<Part<?>> multipartBodyEncoder;
	private final Part.Factory partFactory;
	
	private final CompressionOptionsProvider compressionOptionsProvider;
	
	public Http2ConnectionFactory(
			CompressionOptionsProvider compressionOptionsProvider,
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartEncoder<Parameter> urlEncodedBodyEncoder, 
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory) {
		this.compressionOptionsProvider = compressionOptionsProvider;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
	}

	@Override
	public Http2Connection create(HttpClientConfiguration configuration, HttpVersion httpVersion, EndpointChannelConfigurer configurer) {
		return new Http2ConnectionFactory.Http2ChannelHandlerBuilder(configuration).build();
	}
	
	private class Http2ChannelHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2Connection, Http2ChannelHandlerBuilder> {

		private final HttpClientConfiguration configuration;
		
		public Http2ChannelHandlerBuilder(HttpClientConfiguration configuration) {
			this.configuration = configuration;
			
			Http2Settings initialSettings = this.initialSettings();
			
			Optional.ofNullable(this.configuration.http2_header_table_size()).ifPresent(initialSettings::headerTableSize);
			Optional.ofNullable(this.configuration.http2_max_concurrent_streams()).ifPresent(initialSettings::maxConcurrentStreams);
			Optional.ofNullable(this.configuration.http2_initial_window_size()).ifPresent(initialSettings::initialWindowSize);
			Optional.ofNullable(this.configuration.http2_max_frame_size()).ifPresent(initialSettings::maxFrameSize);
			Optional.ofNullable(this.configuration.http2_max_header_list_size()).ifPresent(initialSettings::maxHeaderListSize);
			
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
				this.configuration, 
				decoder, 
				encoder, 
				initialSettings, 
				Http2ConnectionFactory.this.headerService,
				Http2ConnectionFactory.this.parameterConverter, 
				Http2ConnectionFactory.this.urlEncodedBodyEncoder, 
				Http2ConnectionFactory.this.multipartBodyEncoder, 
				Http2ConnectionFactory.this.partFactory
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
