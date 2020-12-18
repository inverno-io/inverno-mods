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
package io.winterframework.mod.web.internal.server;

import java.net.SocketAddress;
import java.util.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestCookies;
import io.winterframework.mod.web.RequestHeaders;
import io.winterframework.mod.web.RequestParameters;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * @author jkuhn
 *
 */
public abstract class AbstractRequest implements Request<RequestBody> {

	private final boolean releaseData;
	protected final ChannelHandlerContext context;
	protected final RequestHeaders requestHeaders;
	protected final RequestBodyDecoder<Parameter> urlEncodedBodyDecoder;
	protected final RequestBodyDecoder<Part> multipartBodyDecoder;
	
	protected GenericRequestParameters requestParameters;
	protected GenericRequestCookies requestCookies;
	
	private GenericRequestBody requestBody;
	private Sinks.Many<ByteBuf> data;
	
	public AbstractRequest(ChannelHandlerContext context, RequestHeaders requestHeaders, RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, RequestBodyDecoder<Part> multipartBodyDecoder, boolean releaseData) {
		this.context = context;
		this.requestHeaders = requestHeaders;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		this.releaseData = releaseData;
	}

	@Override
	public RequestHeaders headers() {
		return this.requestHeaders;
	}

	@Override
	public RequestParameters parameters() {
		if(this.requestParameters == null) {
			this.requestParameters = new GenericRequestParameters(this.requestHeaders.getPath());
		}
		return this.requestParameters;
	}

	@Override
	public RequestCookies cookies() {
		if(this.requestCookies == null) {
			this.requestCookies = new GenericRequestCookies(this.requestHeaders);
		}
		return this.requestCookies;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.context.channel().remoteAddress();
	}
	
	@Override
	public Optional<RequestBody> body() {
		Method method = this.headers().getMethod();
		if(method == Method.POST || method == Method.PUT || method == Method.PATCH) {
			if(this.requestBody == null) {
				// TODO deal with backpressure using a custom queue: if the queue reach a given threshold we should suspend the read on the channel: this.context.channel().config().setAutoRead(false)
				// and resume when this flux is actually consumed (doOnRequest? this might impact performance but here )
				this.data = Sinks.many().unicast().onBackpressureBuffer();
				
				Flux<ByteBuf> requestBodyData = this.data.asFlux();
				if(releaseData) {
					requestBodyData = requestBodyData.flatMap(chunk -> {
						return Flux.just(chunk).doFinally(sgn -> {
							chunk.release();
						});
					});
				}
				
				this.requestBody = new GenericRequestBody(
					this.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE),
					urlEncodedBodyDecoder, 
					multipartBodyDecoder, 
					requestBodyData
				);
			}
			return Optional.of(this.requestBody);
		}
		else {
			return Optional.empty();
		}
	}

	public Optional<Sinks.Many<ByteBuf>> data() {
		return Optional.ofNullable(this.data);
	}
}
