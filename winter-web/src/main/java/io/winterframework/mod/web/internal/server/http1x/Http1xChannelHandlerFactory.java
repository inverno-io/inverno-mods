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
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class Http1xChannelHandlerFactory implements Supplier<Http1xChannelHandler> {

	private ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler;
	private ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler;
	private HeaderService headerService;
	private RequestBodyDecoder<Parameter> urlEncodedBodyDecoder; 
	private RequestBodyDecoder<Part> multipartBodyDecoder;
	
	public Http1xChannelHandlerFactory(
			ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, 
			ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler, 
			HeaderService headerService, 
			RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, 
			RequestBodyDecoder<Part> multipartBodyDecoder) {
		
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
