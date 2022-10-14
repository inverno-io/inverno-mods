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

package io.inverno.mod.http.client.internal.http2;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.internal.AbstractResponse;
import io.netty.handler.codec.http2.Http2Headers;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
class Http2Response extends AbstractResponse {

	public Http2Response(Http2Headers headers, HeaderService headerService, ObjectConverter<String> parameterConverter) {
		super(new Http2ResponseHeaders(headers, headerService, parameterConverter), parameterConverter);
	}
	
	public void setResponseTrailers(Http2ResponseTrailers responseTrailers) {
		this.responseTrailers = responseTrailers;
	}
}
