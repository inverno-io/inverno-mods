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
import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * <p>
 * HTTP/2 {@link Request} implementation
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http2Request extends AbstractRequest {

	/**
	 * <p>
	 * Creates an HTTP/2 request.
	 * </p>
	 *
	 * @param context            the channel context
	 * @param tls                true if connection is secured, false otherwise
	 * @param parameterConverter the parameter converter
	 * @param headerService      the header service
	 * @param endpointRequest    the original endpoint request
	 */
	public Http2Request(ChannelHandlerContext context, boolean tls, ObjectConverter<String> parameterConverter, HeaderService headerService, EndpointRequest endpointRequest) {
		super(context, tls, parameterConverter, endpointRequest, new Http2RequestHeaders(headerService, parameterConverter, endpointRequest), endpointRequest.getBody() != null ? new Http2RequestBody(endpointRequest.getBody()) : null);
	}

	@Override
	public Http2RequestHeaders headers() {
		return (Http2RequestHeaders)this.requestHeaders;
	}
}
