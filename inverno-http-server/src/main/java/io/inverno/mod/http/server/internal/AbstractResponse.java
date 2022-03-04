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

import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.ResponseCookies;
import io.inverno.mod.http.server.ResponseHeaders;
import io.inverno.mod.http.server.ResponseTrailers;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.reactivestreams.Subscriber;

import java.util.function.Consumer;

/**
 * <p>
 * Base {@link Response} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractResponse implements Response {

	protected final ChannelHandlerContext context;
	
	protected final HeaderService headerService;
	
	protected final AbstractResponseHeaders responseHeaders; 
	protected GenericResponseBody responseBody;	
	
	protected ResponseTrailers responseTrailers;

	protected GenericResponseCookies responseCookies;
	
	/**
	 * <p>
	 * Creates a response with the specified channel handler context, header service
	 * and response headers.
	 * </p>
	 * 
	 * @param context         the channel handler context
	 * @param headerService   the header service
	 * @param responseHeaders the response headers
	 */
	public AbstractResponse(ChannelHandlerContext context, HeaderService headerService, AbstractResponseHeaders responseHeaders) {
		this.context = context;
		this.headerService = headerService;
		this.responseHeaders = responseHeaders;
	}
	
	/**
	 * <p>
	 * Returns true if the response payload is composed of a single chunk of data.
	 * <p>
	 * 
	 * @return true if the response payload is single, false otherwise
	 */
	public boolean isSingle() {
		return this.responseBody.isSingle();
	}

	/**
	 * <p>
	 * Subscribes to the response data publisher.
	 * </p>
	 *
	 * @param s the Subscriber that will consume signals from this Publisher
	 */
	public void dataSubscribe(Subscriber<ByteBuf> s) {
		this.responseBody.dataSubscribe(s);
	}

	/**
	 * <p>
	 * Returns the response cookies.
	 * </p>
	 * 
	 * @return the cookies
	 */
	public GenericResponseCookies getCookies() {
		return this.responseCookies;
	}
	
	@Override
	public boolean isHeadersWritten() {
		return this.responseHeaders.isWritten();
	}
	
	@Override
	public AbstractResponseHeaders headers() {
		return this.responseHeaders;
	}
	
	@Override
	public AbstractResponse headers(Consumer<ResponseHeaders> headersConfigurer) {
		if(this.isHeadersWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		headersConfigurer.accept(this.responseHeaders);
		return this;
	}

	@Override
	public ResponseTrailers trailers() {
		return this.responseTrailers;
	}
	
	@Override
	public AbstractResponse cookies(Consumer<ResponseCookies> cookiesConfigurer) {
		if(this.isHeadersWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		if(this.responseCookies == null) {
			this.responseCookies = new GenericResponseCookies(this.headerService, this.responseHeaders);
		}
		cookiesConfigurer.accept(this.responseCookies);
		return this;
	}

	@Override
	public GenericResponseBody body() {
		return this.responseBody;
	}
}
