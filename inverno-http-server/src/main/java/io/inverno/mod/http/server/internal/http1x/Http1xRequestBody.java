/*
 * Copyright 2024 Jeremy Kuhn
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

import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.http.server.internal.AbstractRequestBody;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;

/**
 * <p>
 * Http/1.x {@link RequestBody} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http1xRequestBody extends AbstractRequestBody<Http1xRequestHeaders> {

	/**
	 * <p>
	 * Creates an Http/1.x request body.
	 * </p>
	 * 
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param headers               the request headers
	 */
	public Http1xRequestBody(MultipartDecoder<Parameter> urlEncodedBodyDecoder, MultipartDecoder<Part> multipartBodyDecoder, Http1xRequestHeaders headers) {
		super(urlEncodedBodyDecoder, multipartBodyDecoder, headers);
	}
}
