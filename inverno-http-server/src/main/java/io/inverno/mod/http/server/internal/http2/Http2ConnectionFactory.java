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
package io.inverno.mod.http.server.internal.http2;

import com.aayushatharva.brotli4j.encoder.Encoder;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.compression.Zstd;
import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.CompressorHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>
 * A factory to create {@link Http2Connection} when an HTTP/2 channel is initialized.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class Http2ConnectionFactory implements Supplier<Http2Connection> {
	
	private final HttpServerConfiguration configuration;
	private final ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	private final MultipartDecoder<Part> multipartBodyDecoder;
	
	private final CompressionOptions[] compressionOptions;
	private final Http2ContentEncodingResolver contentEncodingResolver;

	/**
	 * <p>
	 * Creates an Http/2 connection factory.
	 * </p>
	 * 
	 * @param configuration         the server configuration
	 * @param controller            the server controller
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 */
	@SuppressWarnings("unchecked")
	public Http2ConnectionFactory(
			HttpServerConfiguration configuration, ServerController<?, ? extends Exchange<?>, ? extends ErrorExchange<?>> controller, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder
		) {
		this.configuration = configuration;
		this.controller = (ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>>)controller;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		
		if(Http2ConnectionFactory.this.configuration.compression_enabled()) {
			List<CompressionOptions> compressionOptionsList = new ArrayList<>();

			compressionOptionsList.add(StandardCompressionOptions.deflate(this.configuration.compression_deflate_compressionLevel(), this.configuration.compression_deflate_windowBits(), this.configuration.compression_deflate_memLevel()));
			compressionOptionsList.add(StandardCompressionOptions.gzip(this.configuration.compression_gzip_compressionLevel(), this.configuration.compression_gzip_windowBits(), this.configuration.compression_gzip_memLevel()));
			if(Zstd.isAvailable()) {
				compressionOptionsList.add(StandardCompressionOptions.zstd(this.configuration.compression_zstd_compressionLevel(), this.configuration.compression_zstd_blockSize(), this.configuration.compression_zstd_maxEncodeSize()));
			}

			if(Brotli.isAvailable()) {
				compressionOptionsList.add(StandardCompressionOptions.brotli(new Encoder.Parameters().setQuality(this.configuration.compression_brotli_quality()).setMode(Encoder.Mode.of(this.configuration.compression_brotli_mode())).setWindow(this.configuration.compression_brotli_window())));
			}

			this.compressionOptions = compressionOptionsList.toArray(CompressionOptions[]::new);
			this.contentEncodingResolver = new Http2ContentEncodingResolver(Http2ConnectionFactory.this.compressionOptions);
		}
		else {
			this.compressionOptions = null;
			this.contentEncodingResolver = null;
		}
	}
	
	@Override
	public Http2Connection get() {
		return new Http2ChannelHandlerBuilder().build();
	}

	/**
	 * <p>
	 * Http/2 connection builder.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.10
	 */
	private class Http2ChannelHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2Connection, Http2ChannelHandlerBuilder> {
		
		/**
		 * <p>
		 * Creates and Http/2 connection builder.
		 * </p>
		 */
		public Http2ChannelHandlerBuilder() {
			//this.frameLogger(new Http2FrameLogger(LogLevel.INFO, Http2ConnectionAndFrameHandler.class));
			
			Http2Settings initialSettings = this.initialSettings();
			
			Optional.ofNullable(Http2ConnectionFactory.this.configuration.http2_header_table_size()).ifPresent(initialSettings::headerTableSize);
			Optional.ofNullable(Http2ConnectionFactory.this.configuration.http2_max_concurrent_streams()).ifPresent(initialSettings::maxConcurrentStreams);
			Optional.ofNullable(Http2ConnectionFactory.this.configuration.http2_initial_window_size()).ifPresent(initialSettings::initialWindowSize);
			Optional.ofNullable(Http2ConnectionFactory.this.configuration.http2_max_frame_size()).ifPresent(initialSettings::maxFrameSize);
			Optional.ofNullable(Http2ConnectionFactory.this.configuration.http2_max_header_list_size()).ifPresent(initialSettings::maxHeaderListSize);
			
			this.gracefulShutdownTimeoutMillis(Http2ConnectionFactory.this.configuration.graceful_shutdown_timeout());
			this.validateHeaders(Http2ConnectionFactory.this.configuration.http2_validate_headers());
		}
		
		@Override
		protected Http2Connection build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) throws Exception {
			if (Http2ConnectionFactory.this.configuration.compression_enabled()) {
				encoder = new CompressorHttp2ConnectionEncoder(
					encoder,
					Http2ConnectionFactory.this.compressionOptions
				);
			}
			
			Http2Connection connection = new Http2Connection(
				decoder, 
				encoder, 
				initialSettings,
				Http2ConnectionFactory.this.configuration,
				Http2ConnectionFactory.this.controller, 
				Http2ConnectionFactory.this.headerService,
				Http2ConnectionFactory.this.parameterConverter,
				Http2ConnectionFactory.this.urlEncodedBodyDecoder,
				Http2ConnectionFactory.this.multipartBodyDecoder,
				Http2ConnectionFactory.this.contentEncodingResolver
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
