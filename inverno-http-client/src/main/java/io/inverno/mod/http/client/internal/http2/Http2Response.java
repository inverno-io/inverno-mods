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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Response;
import io.inverno.mod.http.client.internal.AbstractResponse;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.function.Consumer;

/**
 * <p>
 * Http/2 {@link Response} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http2Response extends AbstractResponse<Http2ResponseHeaders, Http2ResponseBody, Http2ResponseTrailers, Http2Headers> {
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	/**
	 * <p>
	 * Creates an HTTP/2 response.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headers            the originating headers
	 */
	public Http2Response(HeaderService headerService, ObjectConverter<String> parameterConverter, Http2Headers headers) {
		super(new Http2ResponseHeaders(headerService, parameterConverter, headers), new Http2ResponseBody());
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}

	@Override
	public Http2Response configureInterceptedHeaders(Consumer<OutboundResponseHeaders> headersConfigurer) {
		this.headers().configureInterceptedHeaders(headersConfigurer);
		return this;
	}

	@Override
	protected Http2ResponseTrailers createTrailers(Http2Headers trailers) {
		return new Http2ResponseTrailers(this.headerService, this.parameterConverter, trailers);
	}
}
