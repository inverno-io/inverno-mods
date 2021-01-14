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
package io.winterframework.mod.web.internal.server.http1x;

import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.internal.server.multipart.MultipartDecoder;
import io.winterframework.mod.web.server.ErrorExchange;
import io.winterframework.mod.web.server.Exchange;
import io.winterframework.mod.web.server.ExchangeHandler;
import io.winterframework.mod.web.server.Part;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class Http1xChannelHandlerFactory implements Supplier<Http1xChannelHandler> {

	private ExchangeHandler<Exchange> rootHandler;
	private ExchangeHandler<ErrorExchange<Throwable>> errorHandler;
	private HeaderService headerService;
	private MultipartDecoder<Parameter> urlEncodedBodyDecoder; 
	private MultipartDecoder<Part> multipartBodyDecoder;
	
	public Http1xChannelHandlerFactory(
			ExchangeHandler<Exchange> rootHandler, 
			ExchangeHandler<ErrorExchange<Throwable>> errorHandler, 
			HeaderService headerService, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder) {
		
		this.rootHandler = rootHandler;
		this.errorHandler = errorHandler;
		this.headerService = headerService;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
	}

	@Override
	public Http1xChannelHandler get() {
		return new Http1xChannelHandler(this.rootHandler, this.errorHandler, this.headerService, this.urlEncodedBodyDecoder, this.multipartBodyDecoder);
	}
}
