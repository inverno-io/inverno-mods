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
package io.inverno.mod.http.server.internal.http1x;

import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslHandler;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.RequestHeaders;
import io.inverno.mod.http.server.internal.AbstractRequest;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import reactor.core.publisher.Sinks.Many;

/**
 * <p>
 * HTTP1.x {@link Request} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class Http1xRequest extends AbstractRequest {

	private final HttpRequest httpRequest;
	
	private URIBuilder pathBuilder;
	
	private Method method;
	private String scheme;
	private String authority;
	
	/**
	 * <p>
	 * Creates a HTTP1.x server request.
	 * </p>
	 * 
	 * @param context               the channel handler context
	 * @param httpRequest           the underlying HTTP request
	 * @param requestHeaders        the HTTP1.x request headers
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body
	 *                              decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 */
	public Http1xRequest(ChannelHandlerContext context, HttpRequest httpRequest, RequestHeaders requestHeaders, ObjectConverter<String> parameterConverter, MultipartDecoder<Parameter> urlEncodedBodyDecoder, MultipartDecoder<Part> multipartBodyDecoder) {
		super(context, requestHeaders, parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder);
		this.httpRequest = httpRequest;
	}
	
	@Override
	protected URIBuilder getPrimaryPathBuilder() {
		if(this.pathBuilder == null) {
			this.pathBuilder = URIs.uri(this.httpRequest.uri(), false, URIs.Option.NORMALIZED);
		}
		return this.pathBuilder;
	}
	
	@Override
	public Optional<Many<ByteBuf>> data() {
		// In order to support pipelining we must always create the data sink even if
		// it might not be consumed by the exchange handler
		// This comes from the fact that the exchange is only started after the previous
		// exchange has completed, which means we can receive data before we actually
		// invoke the exchange handler which is supposed to create the body
		this.body();
		return super.data();
	}
	
	@Override
	public Method getMethod() {
		if(this.method == null) {
			try {
				this.method = Method.valueOf(this.httpRequest.method().name());
			}
			catch (IllegalArgumentException e) {
				this.method = Method.UNKNOWN;
			}
		}
		return method;
	}
	
	@Override
	public String getScheme() {
		if(this.scheme == null) {
			this.scheme = this.context.pipeline().get(SslHandler.class) != null ? "https" : "http";
		}
		return this.scheme;
	}
	
	@Override
	public String getAuthority() {
		if(this.authority == null) {
			this.authority = this.requestHeaders.get((CharSequence)Headers.NAME_HOST).orElse(null);
		}
		return this.authority;
	}
	
	@Override
	public String getPath() {
		return this.httpRequest.uri();
	}
}
