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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.net.SocketAddress;
import java.util.Optional;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base {@link Request} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractRequest implements Request {

	protected final ChannelHandlerContext context;
	protected final InboundRequestHeaders requestHeaders;
	protected final ObjectConverter<String> parameterConverter;
	protected final MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	protected final MultipartDecoder<Part> multipartBodyDecoder;
	
	private GenericRequestBody requestBody;
	
	private String pathAbsolute;
	private String queryString;
	protected GenericQueryParameters queryParameters;
	
	/**
	 * <p>
	 * Creates a request with the specified channel handler context, request headers, parameter value converter, URL encoded body decoder and multipart body decoder.
	 * </p>
	 * 
	 * @param context               the channel handler context
	 * @param requestHeaders        the underlying request headers
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 */
	public AbstractRequest(
			ChannelHandlerContext context, 
			InboundRequestHeaders requestHeaders, 
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder) {
		this.context = context;
		this.requestHeaders = requestHeaders;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
	}

	/**
	 * <p>
	 * Returns or creates the primary path builder created from the request path.
	 * </p>
	 * 
	 * <p>
	 * The primary path builder is used to extract absolute path, query parameters and query string and to create the path builder returned by {@link #getPathBuilder() }.
	 * </p>
	 * 
	 * @return the path builder
	 */
	protected abstract URIBuilder getPrimaryPathBuilder();
	
	@Override
	public SocketAddress getLocalAddress() {
		return this.context.channel().localAddress();
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		return this.context.channel().remoteAddress();
	}
	
	@Override
	public String getPathAbsolute() {
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.getPrimaryPathBuilder().buildRawPath();
		}
		return this.pathAbsolute;
	}
	
	@Override
	public URIBuilder getPathBuilder() {
		return this.getPrimaryPathBuilder().clone();
	}
	
	@Override
	public String getQuery() {
		if(this.queryString == null) {
			this.queryString = this.getPrimaryPathBuilder().buildRawQuery();
		}
		return this.queryString;
	}
	
	@Override
	public QueryParameters queryParameters() {
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.getPrimaryPathBuilder().getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}
	
	@Override
	public InboundRequestHeaders headers() {
		return this.requestHeaders;
	}
	
	@Override
	@Deprecated
	public InboundCookies cookies() {
		return this.requestHeaders.cookies();
	}

	@Override
	public Optional<RequestBody> body() {
		if(this.requestBody == null) {
			Method method = this.getMethod();
			switch(method) {
				case POST:
				case PUT:
				case PATCH:
				case DELETE: {
					if(this.requestBody == null) {
						this.requestBody = new GenericRequestBody(
							this.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE),
							this.urlEncodedBodyDecoder, 
							this.multipartBodyDecoder
						);
					}
					break;
				}
				default: {
					this.requestBody = null;
					break;
				}
			}
		}
		return Optional.ofNullable(this.requestBody);
	}

	/**
	 * <p>
	 * Returns the request payload data sink.
	 * </p>
	 *
	 * @return an optional returning the payload data sink or an empty optional if the request has no body
	 */
	public Optional<Sinks.Many<ByteBuf>> data() {
		return Optional.ofNullable(this.requestBody).map(body -> body.dataSink);
	}
	
	/**
	 * <p>
	 * Drains and release the request data flux.
	 * </p>
	 */
	public void dispose() {
		if(this.requestBody != null) {
			this.requestBody.dispose();
		}
	}
}
