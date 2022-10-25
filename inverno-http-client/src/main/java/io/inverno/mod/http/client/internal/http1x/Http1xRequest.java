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
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.netty.channel.ChannelHandlerContext;
import java.util.Optional;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
class Http1xRequest extends AbstractRequest {

	public Http1xRequest(
			ChannelHandlerContext context, 
			boolean tls,
			ObjectConverter<String> parameterConverter, 
			HttpVersion protocol, 
			Method method, 
			String authority,
			String path, 
			Http1xRequestHeaders requestHeaders, 
			Http1xRequestBody requestBody) {
		super(context, tls, parameterConverter, protocol, method, authority, path, requestHeaders, requestBody);
	}

	@Override
	public Http1xRequestHeaders headers() {
		return (Http1xRequestHeaders)this.requestHeaders;
	}

	@Override
	public Optional<Http1xRequestBody> body() {
		return this.requestBody.map(body -> (Http1xRequestBody)body);
	}
}
