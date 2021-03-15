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
package io.winterframework.mod.http.server.internal;

import java.net.SocketAddress;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.base.net.URIBuilder;
import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.Part;
import io.winterframework.mod.http.server.QueryParameters;
import io.winterframework.mod.http.server.Request;
import io.winterframework.mod.http.server.RequestBody;
import io.winterframework.mod.http.server.RequestCookies;
import io.winterframework.mod.http.server.RequestHeaders;
import io.winterframework.mod.http.server.internal.multipart.MultipartDecoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base {@link Request} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractRequest implements Request {

	protected final ChannelHandlerContext context;
	protected final RequestHeaders requestHeaders;
	protected final ObjectConverter<String> parameterConverter;
	protected final MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	protected final MultipartDecoder<Part> multipartBodyDecoder;
	
	protected GenericQueryParameters queryParameters;
	protected GenericRequestCookies requestCookies;
	
	private Optional<RequestBody> requestBody;
	private Sinks.Many<ByteBuf> data;
	
	private String pathAbsolute;
	private String queryString;
	
	/**
	 * <p>
	 * Creates a request with the specified channel handler context, request
	 * headers, parameter value converter, URL encoded body decoder and multipart
	 * body decoder.
	 * </p>
	 * 
	 * @param context               the channel handler context
	 * @param requestHeaders        the underlying request headers
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 */
	public AbstractRequest(ChannelHandlerContext context, RequestHeaders requestHeaders, ObjectConverter<String> parameterConverter, MultipartDecoder<Parameter> urlEncodedBodyDecoder, MultipartDecoder<Part> multipartBodyDecoder) {
		this.context = context;
		this.requestHeaders = requestHeaders;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
	}

	/**
	 * <p>
	 * Returns the path builder created from the request path.
	 * </p>
	 * 
	 * @return the path builder
	 */
	protected abstract URIBuilder getPathBuilder();
	
	@Override
	public RequestHeaders headers() {
		return this.requestHeaders;
	}

	@Override
	public QueryParameters queryParameters() {
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.getPathBuilder().getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}

	@Override
	public RequestCookies cookies() {
		if(this.requestCookies == null) {
			this.requestCookies = new GenericRequestCookies(this.requestHeaders, this.parameterConverter);
		}
		return this.requestCookies;
	}
	
	@Override
	public String getPathAbsolute() {
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.getPathBuilder().buildRawPath();
		}
		return this.pathAbsolute;
	}
	
	@Override
	public String getQuery() {
		if(this.queryString == null) {
			this.queryString = this.getPathBuilder().buildRawQuery();
		}
		return this.queryString;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.context.channel().remoteAddress();
	}
	
	@Override
	public Optional<RequestBody> body() {
		if(this.requestBody == null) {
			Method method = this.getMethod();
			if(method == Method.POST || method == Method.PUT || method == Method.PATCH) {
				if(this.requestBody == null) {
					// TODO deal with backpressure using a custom queue: if the queue reach a given threshold we should suspend the read on the channel: this.context.channel().config().setAutoRead(false)
					// and resume when this flux is actually consumed (doOnRequest? this might impact performance)
					this.data = Sinks.many().unicast().onBackpressureBuffer();
					Flux<ByteBuf> requestBodyData = this.data.asFlux()
						.doOnDiscard(ByteBuf.class, ByteBuf::release);
					
					this.requestBody = Optional.of(new GenericRequestBody(
						this.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE),
						this.urlEncodedBodyDecoder, 
						this.multipartBodyDecoder, 
						requestBodyData
					));
				}
			}
			else {
				this.requestBody = Optional.empty();
			}
		}
		return this.requestBody;
	}

	/**
	 * <p>
	 * Returns the request payload data sink.
	 * </p>
	 * 
	 * @return an optional returning the payload data sink or an empty optional if
	 *         the request has no body
	 */
	public Optional<Sinks.Many<ByteBuf>> data() {
		return Optional.ofNullable(this.data);
	}
	
	/**
	 * <p>
	 * Drains and release the request data flux.
	 * </p>
	 */
	public void dispose() {
		if(this.data != null) {
			// Try to drain and release buffered data 
			this.data.asFlux().subscribe(
				chunk -> chunk.release(), 
				ex -> {
					// TODO Should be ignored but can be logged as debug or trace log
				}
			);
		}
	}
}
