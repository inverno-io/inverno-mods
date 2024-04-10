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

import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.OutboundSetCookies;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.server.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;

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
	
	protected final InternalResponseHeaders responseHeaders; 
	protected GenericResponseBody responseBody;	
	protected OutboundHeaders<?> responseTrailers;

	/**
	 * <p>
	 * Creates a response with the specified channel handler context, header service and response headers.
	 * </p>
	 *
	 * @param context         the channel handler context
	 * @param headerService   the header service
	 * @param responseHeaders the response headers
	 */
	public AbstractResponse(ChannelHandlerContext context, HeaderService headerService, InternalResponseHeaders responseHeaders) {
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
	
	@Override
	public boolean isHeadersWritten() {
		return this.responseHeaders.isWritten();
	}

	@Override
	public InternalResponseHeaders headers() {
		return this.responseHeaders;
	}
	
	@Override
	public AbstractResponse headers(Consumer<OutboundResponseHeaders> headersConfigurer) {
		if(this.responseHeaders.isWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		if(headersConfigurer != null) {
			headersConfigurer.accept(this.responseHeaders);
		}
		return this;
	}
	
	@Override
	@Deprecated
	public AbstractResponse cookies(Consumer<OutboundSetCookies> cookiesConfigurer) {
		this.responseHeaders.cookies(cookiesConfigurer);
		return this;
	}
	
	@Override
	public GenericResponseBody body() {
		return this.responseBody;
	}

	@Override
	public InboundHeaders trailers() {
		if(this.responseTrailers == null) {
			this.responseTrailers = this.createTrailers();
		}
		return this.responseTrailers;
	}

	@Override
	public Response trailers(Consumer<OutboundHeaders<?>> trailersConfigurer) {
		if(trailersConfigurer != null) {
			trailersConfigurer.accept((OutboundHeaders<?>)this.trailers());
		}
		return this;
	}
	
	protected abstract OutboundHeaders<?> createTrailers();
}
