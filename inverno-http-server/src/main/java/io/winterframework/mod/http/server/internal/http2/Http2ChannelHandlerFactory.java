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

import java.util.Optional;
import java.util.function.Supplier;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.CompressorHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.internal.http1x.Http1xChannelHandler;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;

/**
 * <p>
 * A factory to create {@link Http1xChannelHandler} when a HTTP2 channel is
 * initialized.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Visibility.PRIVATE)
public class Http2ChannelHandlerFactory implements Supplier<Http2ChannelHandler> {

	private HttpServerConfiguration configuration;
	private ExchangeHandler<Exchange> rootHandler; 
	private ExchangeHandler<ErrorExchange<Throwable>> errorHandler;
	private HeaderService headerService;
	private ObjectConverter<String> parameterConverter;
	private MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	private MultipartDecoder<Part> multipartBodyDecoder;
	
	/**
	 * <p>
	 * Creates a HTTP/2 channel handler factory.
	 * <p>
	 * 
	 * @param configuration         the HTTP server configuration
	 * @param rootHandler           the root exchange handler
	 * @param errorHandler          the error exchange handler
	 * @param headerService         the header service
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 */
	public Http2ChannelHandlerFactory(
			HttpServerConfiguration configuration, 
			ExchangeHandler<Exchange> rootHandler, 
			ExchangeHandler<ErrorExchange<Throwable>> errorHandler, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter,
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder) {
		this.configuration = configuration;
		this.rootHandler = rootHandler;
		this.errorHandler = errorHandler;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
	}

	@Override
	public Http2ChannelHandler get() {
		return new Http2ChannelHandlerBuilder().build();
	}

	private class Http2ChannelHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2ChannelHandler, Http2ChannelHandlerBuilder> implements Supplier<Http2ChannelHandler> {
		
		public Http2ChannelHandlerBuilder() {
			//this.frameLogger(new Http2FrameLogger(LogLevel.INFO, Http2ConnectionAndFrameHandler.class));
			
			Http2Settings initialSettings = this.initialSettings();
			
			Optional.ofNullable(Http2ChannelHandlerFactory.this.configuration.http2_header_table_size()).ifPresent(initialSettings::headerTableSize);
			Optional.ofNullable(Http2ChannelHandlerFactory.this.configuration.http2_max_concurrent_streams()).ifPresent(initialSettings::maxConcurrentStreams);
			Optional.ofNullable(Http2ChannelHandlerFactory.this.configuration.http2_initial_window_size()).ifPresent(initialSettings::initialWindowSize);
			Optional.ofNullable(Http2ChannelHandlerFactory.this.configuration.http2_max_frame_size()).ifPresent(initialSettings::maxFrameSize);
			Optional.ofNullable(Http2ChannelHandlerFactory.this.configuration.http2_max_header_list_size()).ifPresent(initialSettings::maxHeaderListSize);
		}
		
		@Override
		public Http2ChannelHandler get() {
			return this.build();
		}

		@Override
		protected Http2ChannelHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) throws Exception {
			if (Http2ChannelHandlerFactory.this.configuration.compression_enabled()) {
				encoder = new CompressorHttp2ConnectionEncoder(
					encoder, 
					Http2ChannelHandlerFactory.this.configuration.compression_level(),
					CompressorHttp2ConnectionEncoder.DEFAULT_WINDOW_BITS,
					CompressorHttp2ConnectionEncoder.DEFAULT_MEM_LEVEL
				);
			}
			
			Http2ChannelHandler handler = new Http2ChannelHandler(
				Http2ChannelHandlerFactory.this.configuration,
				decoder, 
				encoder, 
				initialSettings,
				Http2ChannelHandlerFactory.this.rootHandler, 
				Http2ChannelHandlerFactory.this.errorHandler,
				Http2ChannelHandlerFactory.this.headerService,
				Http2ChannelHandlerFactory.this.parameterConverter,
				Http2ChannelHandlerFactory.this.urlEncodedBodyDecoder,
				Http2ChannelHandlerFactory.this.multipartBodyDecoder
			);
			this.frameListener(handler);
			return handler;
		}
		
		@Override
		public Http2ChannelHandler build() {
			return super.build();
		}
	}
}
