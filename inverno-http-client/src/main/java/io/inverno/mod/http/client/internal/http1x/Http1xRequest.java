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
import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.netty.channel.ChannelHandlerContext;

/**
 * <p>
 * HTTP/1.x {@link Request} implementation
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xRequest extends AbstractRequest {

	/**
	 * <p>
	 * Creates an HTTP/1.x request.
	 * </p>
	 *
	 * @param context            the channel context
	 * @param tls                true if connection is secured, false otherwise
	 * @param supportsFileRegion true if the connection supports file region, false otherwise
	 * @param parameterConverter the parameter converter
	 * @param endpointRequest    the original endpoint request
	 */
	public Http1xRequest(ChannelHandlerContext context, boolean tls, boolean supportsFileRegion, ObjectConverter<String> parameterConverter, EndpointRequest endpointRequest) {
		super(context, tls, parameterConverter, endpointRequest, new Http1xRequestHeaders(endpointRequest.getHeaders()), endpointRequest.getBody() != null ? new Http1xRequestBody(endpointRequest.getBody(), supportsFileRegion) : null);
	}

	@Override
	public Http1xRequestHeaders headers() {
		return (Http1xRequestHeaders)this.requestHeaders;
	}
	
	@Override
	public Http1xRequestBody body() {
		return (Http1xRequestBody)this.requestBody;
	}
}
