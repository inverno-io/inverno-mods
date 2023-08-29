/*
 * Copyright 2020 Jeremy KUHN
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

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.internal.AbstractRequest;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.channel.ChannelHandlerContext;

/**
 * <p>
 * HTTP/2 {@link Request} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class Http2Request extends AbstractRequest {

	private URIBuilder pathBuilder;
	
	private Method method;
	private String scheme;
	private String authority;
	private String path;
	
	/**
	 * <p>
	 * Creates a HTTP/2 server request.
	 * </p>
	 * 
	 * @param context
	 * @param requestHeaders
	 * @param parameterConverter
	 * @param urlEncodedBodyDecoder
	 * @param multipartBodyDecoder
	 */
	public Http2Request(ChannelHandlerContext context, InboundRequestHeaders requestHeaders, ObjectConverter<String> parameterConverter, MultipartDecoder<Parameter> urlEncodedBodyDecoder, MultipartDecoder<Part> multipartBodyDecoder) {
		super(context, requestHeaders, parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder);
	}
	
	@Override
	protected URIBuilder getPrimaryPathBuilder() {
		if(this.pathBuilder == null) {
			this.pathBuilder = this.requestHeaders.get(Headers.NAME_PSEUDO_PATH)
				.map(path -> URIs.uri(path, false, URIs.Option.NORMALIZED))
				.orElseThrow(() -> new IllegalStateException("Request has no :path"));
		}
		return this.pathBuilder;
	}
	
	@Override
	public Method getMethod() {
		if(this.method == null) {
			this.method = this.requestHeaders.get(Headers.NAME_PSEUDO_METHOD)
				.map(methodString -> {
					try {
						return Method.valueOf(methodString);
					}
					catch(IllegalArgumentException e) {
						return Method.UNKNOWN;
					}
				})
				.orElse(null);
		}
		return method;
	}
	
	@Override
	public String getScheme() {
		if(this.scheme == null) {
			this.scheme = this.requestHeaders.get(Headers.NAME_PSEUDO_SCHEME).orElse(null);
		}
		return this.scheme;
	}
	
	@Override
	public String getAuthority() {
		if(this.authority == null) {
			this.authority = this.requestHeaders.get(Headers.NAME_PSEUDO_AUTHORITY).orElse(null);
		}
		return this.authority;
	}
	
	@Override
	public String getPath() {
		if(this.path == null) {
			this.path = this.requestHeaders.get(Headers.NAME_PSEUDO_PATH).orElse(null);
		}
		return this.path;
	}
}
