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
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.client.internal.GenericRequestHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * HTTP/1.x {@link OutboundRequestHeaders} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xRequestHeaders extends GenericRequestHeaders {

	/**
	 * <p>
	 * Creates blank HTTP/1.x request headers.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public Http1xRequestHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(headerService, parameterConverter);
	}

	/**
	 * <p>
	 * Creates HTTP/1.x request headers populated with specified header entries.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param entries            a list of HTTP header entries
	 */
	public Http1xRequestHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter, List<Map.Entry<String, String>> entries) {
		super(headerService, parameterConverter, entries);
	}
	
	/**
	 * <p>
	 * Returns the underlying Netty's {@link HttpHeaders}.
	 * </p>
	 * 
	 * @return underlying HttpHeaders
	 */
	public LinkedHttpHeaders toHttp1xHeaders() {
		return this.underlyingHeaders;
	}
}
