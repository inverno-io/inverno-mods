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
package io.inverno.mod.http.client.internal.v2.http2;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.handler.codec.http2.Http2Headers;

/**
 * <p>
 * Http/2 {@link Response} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http2ResponseV2 implements HttpConnectionResponse {
	
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Http2ResponseHeadersV2 headers;
	private final Http2ResponseBodyV2 body;
	
	private Http2ResponseTrailersV2 trailers;

	/**
	 * <p>
	 * Creates an HTTP/2 response.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param headers            the originating headers
	 */
	public Http2ResponseV2(HeaderService headerService, ObjectConverter<String> parameterConverter, Http2Headers headers) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.headers = new Http2ResponseHeadersV2(headerService, parameterConverter, headers);
		this.body = new Http2ResponseBodyV2();
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
	public Http2ResponseBodyV2 body() {
		return this.body;
	}

	@Override
	public Http2ResponseHeadersV2 headers() {
		return this.headers;
	}

	@Override
	public Http2ResponseTrailersV2 trailers() {
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
	void setTrailers(Http2Headers trailers) {
		this.trailers = new Http2ResponseTrailersV2(this.headerService, this.parameterConverter, trailers);
	}
}
