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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketFrame;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketMessage;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import java.util.function.Supplier;

/**
 * <p>
 * A factory to create {@link Http1xConnection} when an HTTP/1.x channel is initialized.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class Http1xConnectionFactory implements Supplier<Http1xConnection> {
	
	private final HttpServerConfiguration configuration;
	private final ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartDecoder<Parameter> urlEncodedBodyDecoder; 
	private final MultipartDecoder<Part> multipartBodyDecoder;
	
	private final HeadersValidator headersValidator;
	private final GenericWebSocketFrame.GenericFactory webSocketFrameFactory;
	private final GenericWebSocketMessage.GenericFactory webSocketMessageFactory;

	/**
	 * <p>
	 * Creates an Http/1.x connection factory.
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
	public Http1xConnectionFactory(
			HttpServerConfiguration configuration,
			ServerController<?, ? extends Exchange<?>, ? extends ErrorExchange<?>> controller,
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
		this.headersValidator = configuration.http1x_validate_headers() ? HeadersValidator.DEFAULT_HTTP1X_HEADERS_VALIDATOR : null;
		if(configuration.ws_enabled()) {
			this.webSocketFrameFactory = new GenericWebSocketFrame.GenericFactory(configuration.ws_max_frame_size());
			this.webSocketMessageFactory = new GenericWebSocketMessage.GenericFactory(configuration.ws_max_frame_size());
		}
		else {
			this.webSocketFrameFactory = null;
			this.webSocketMessageFactory = null;
		}
	}
	
	@Override
	public Http1xConnection get() {
		return new Http1xConnection(
			this.configuration, 
			this.controller, 
			this.headerService, 
			this.parameterConverter, 
			this.urlEncodedBodyDecoder, 
			this.multipartBodyDecoder, 
			this.webSocketFrameFactory, 
			this.webSocketMessageFactory, 
			this.headersValidator
		);
	}
}