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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.client.Response;
import io.inverno.mod.http.client.internal.AbstractResponse;
import io.netty.handler.codec.http.HttpResponse;

/**
 * <p>
 * Http/1.x {@link Response} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xResponse extends AbstractResponse<Http1xResponseHeaders, Http1xResponseBody, Http1xResponseTrailers, LinkedHttpHeaders> {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	/**
	 * <p>
	 * Creates an Http/1.x response.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param response           the originating Http response
	 */
	public Http1xResponse(
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter,
			HttpResponse response
		) {
		super(new Http1xResponseHeaders(headerService, parameterConverter, (LinkedHttpHeaders)response.headers(), response.status().code()), new Http1xResponseBody());
		
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}

	@Override
	protected Http1xResponseTrailers createTrailers(LinkedHttpHeaders trailers) {
		return new Http1xResponseTrailers(this.headerService, this.parameterConverter, trailers);
	}
}
