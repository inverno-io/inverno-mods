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

import java.util.function.Supplier;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ErrorExchangeHandler;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.RootExchangeHandler;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;

/**
 * <p>
 * A factory to create {@link Http1xChannelHandler} when a HTTP1.x channel is
 * initialized.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Visibility.PRIVATE)
public class Http1xChannelHandlerFactory implements Supplier<Http1xChannelHandler> {

	private RootExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> rootHandler;
	private ErrorExchangeHandler<Throwable, ErrorExchange<Throwable>> errorHandler;
	private HeaderService headerService;
	private ObjectConverter<String> parameterConverter;
	private MultipartDecoder<Parameter> urlEncodedBodyDecoder; 
	private MultipartDecoder<Part> multipartBodyDecoder;
	
	/**
	 * <p>
	 * Creates a HTTP1.x channel handler factory.
	 * <p>
	 * 
	 * @param rootHandler           the root exchange handler
	 * @param errorHandler          the error exchange handler
	 * @param headerService         the header service
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 */
	@SuppressWarnings({ "unchecked" })
	public Http1xChannelHandlerFactory(
			RootExchangeHandler<?, ? extends Exchange<?>> rootHandler, 
			ErrorExchangeHandler<Throwable, ErrorExchange<Throwable>> errorHandler, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter,
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder) {
		
		this.rootHandler = (RootExchangeHandler<ExchangeContext, Exchange<ExchangeContext>>)rootHandler;
		this.errorHandler = errorHandler;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
	}

	@Override
	public Http1xChannelHandler get() {
		return new Http1xChannelHandler(this.rootHandler, this.errorHandler, this.headerService, this.parameterConverter, this.urlEncodedBodyDecoder, this.multipartBodyDecoder);
	}
}
