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
package io.winterframework.mod.web.internal.server.http2;

import java.util.Optional;
import java.util.function.Supplier;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.WebConfiguration;
import io.winterframework.mod.web.internal.RequestBodyDecoder;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class Http2ChannelHandlerFactory implements Supplier<Http2ChannelHandler> {

	private WebConfiguration configuration;
	private ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler; 
	private ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler;
	private HeaderService headerService;
	private RequestBodyDecoder<Parameter> urlEncodedBodyDecoder;
	private RequestBodyDecoder<Part> multipartBodyDecoder;
	
	public Http2ChannelHandlerFactory(
			WebConfiguration configuration, 
			ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, 
			ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler, 
			HeaderService headerService, 
			RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, 
			RequestBodyDecoder<Part> multipartBodyDecoder) {
		this.configuration = configuration;
		this.rootHandler = rootHandler;
		this.errorHandler = errorHandler;
		this.headerService = headerService;
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
			Optional.ofNullable(Http2ChannelHandlerFactory.this.configuration.http2_push_enabled()).ifPresent(initialSettings::pushEnabled);
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
			Http2ChannelHandler handler = new Http2ChannelHandler(
				decoder, 
				encoder, 
				initialSettings,
				Http2ChannelHandlerFactory.this.rootHandler, Http2ChannelHandlerFactory.this.errorHandler,
				Http2ChannelHandlerFactory.this.headerService,
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
