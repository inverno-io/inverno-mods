/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.http.server.internal.http2;

import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.http.HttpContentCompressor;

/**
 * <p>
 * Used to determine the target content encoding of a response based on the {@code accept-encoding} header of a request.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.2
 */
class Http2ContentEncodingResolver extends HttpContentCompressor {

	public Http2ContentEncodingResolver(CompressionOptions... compressionOptions) {
		super(compressionOptions);
	}
	
	/**
	 * <p>
	 * Resolves the response content encoding.
	 * </p>
	 * 
	 * @param acceptEncoding the accept encoding header of a request
	 * 
	 * @return a content encoding or null
	 * @throws NullPointerException if acceptEncoding is null
	 */
	public String resolve(String acceptEncoding) throws NullPointerException {
		return this.determineEncoding(acceptEncoding);
	}
}
