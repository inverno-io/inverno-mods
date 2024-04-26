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
package io.inverno.mod.http.server.internal.http2;

import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.http.server.internal.AbstractResponseBody;

/**
 * <p>
 * Http/2 {@link ResponseBody} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http2ResponseBody extends AbstractResponseBody<Http2ResponseHeaders, Http2ResponseBody> {

	/**
	 * <p>
	 * Creates an Http/2 response body.
	 * </p>
	 * 
	 * @param headers the response headers
	 */
	public Http2ResponseBody(Http2ResponseHeaders headers) {
		super(headers);
	}
}
