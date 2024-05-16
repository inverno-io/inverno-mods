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
package io.inverno.mod.http.client.internal.v2.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.handler.codec.http.HttpResponse;

/**
 * <p>
 * Http/1.x {@link Response} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xResponseV2 implements HttpConnectionResponse {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Http1xResponseHeadersV2 headers;
	private final Http1xResponseBodyV2 body;
	
	private Http1xResponseTrailersV2 trailers;
	
	/**
	 * <p>
	 * Creates an Http/1.x response.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param response           the originating Http response
	 */
	public Http1xResponseV2(
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter,
			HttpResponse response
		) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.headers = new Http1xResponseHeadersV2(headerService, parameterConverter, (LinkedHttpHeaders)response.headers(), response.status().code());
		this.body = new Http1xResponseBodyV2();
	}
	
	/**
	 * <p>
	 * Disposes the response.
	 * </p>
	 * 
	 * <p>
	 * This method disposes the response body.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	final void dispose(Throwable cause) {
		this.body.dispose(cause);
	}

	@Override
	public Http1xResponseHeadersV2 headers() {
		return this.headers;
	}
	
	@Override
	public Http1xResponseBodyV2 body() {
		return this.body;
	}

	@Override
	public Http1xResponseTrailersV2 trailers() {
		return this.trailers;
	}

	/**
	 * <p>
	 * Sets the response trailers.
	 * </p>
	 * 
	 * <p>
	 * This is invoked by the connection when response trailers are received.
	 * </p>
	 * 
	 * @param trailers the originating trailers
	 */
	void setTrailers(LinkedHttpHeaders trailers) {
		this.trailers = new Http1xResponseTrailersV2(this.headerService, this.parameterConverter, trailers);
	}
}
