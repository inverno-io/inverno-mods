/*
 * Copyright 2022 Jeremy KUHN
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
import io.inverno.mod.http.client.Response;
import io.inverno.mod.http.client.internal.AbstractResponse;
import io.netty.handler.codec.http.HttpResponse;

/**
 * <p>
 * HTTP/1.x {@link Response} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xResponse extends AbstractResponse {

	/**
	 * <p>
	 * Creates HTTP/1.x response.
	 * </p>
	 *
	 * @param httpResponse       the underlying Netty's HTTP response
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public Http1xResponse(HttpResponse httpResponse, HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(new Http1xResponseHeaders(httpResponse.headers(), httpResponse.status().code(), headerService, parameterConverter));
	}
	
	/**
	 * <p>
	 * Sets the response trailers.
	 * </p>
	 * 
	 * @param responseTrailers the response trailers to set
	 */
	public void setResponseTrailers(Http1xResponseTrailers responseTrailers) {
		this.responseTrailers = responseTrailers;
	}
}
