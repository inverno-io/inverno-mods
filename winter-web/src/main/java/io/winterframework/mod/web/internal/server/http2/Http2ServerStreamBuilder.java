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
package io.winterframework.mod.web.internal.server.http2;

import java.util.Objects;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Strategy;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.server.AbstractHttpServerExchangeBuilder;
import io.winterframework.mod.web.internal.server.GenericRequestParameters;
import io.winterframework.mod.web.internal.server.GenericResponse;
import io.winterframework.mod.web.internal.server.GetRequest;
import io.winterframework.mod.web.internal.server.PostRequest;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
@Bean(strategy = Strategy.PROTOTYPE, visibility = Visibility.PRIVATE)
public class Http2ServerStreamBuilder extends AbstractHttpServerExchangeBuilder<Http2ServerStream<?>> {

	private Http2Stream stream;
	private Http2Headers headers;
	private Http2ConnectionEncoder encoder;
	
	public Http2ServerStreamBuilder(HeaderService headerService, RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, RequestBodyDecoder<Part> multipartBodyDecoder) {
		super(headerService, urlEncodedBodyDecoder, multipartBodyDecoder);
	}

	public Http2ServerStreamBuilder stream(Http2Stream stream) {
		this.stream = stream;
		return this;
	}
	
	public Http2ServerStreamBuilder headers(Http2Headers headers) {
		this.headers = headers;
		return this;
	}
	
	public Http2ServerStreamBuilder encoder(Http2ConnectionEncoder encoder) {
		this.encoder = encoder;
		return this;
	}
	
	public Mono<Http2ServerStream<?>> build(ChannelHandlerContext context) {
		Objects.requireNonNull(this.stream, "Missing stream");
		Objects.requireNonNull(this.headers, "Missing request headers");
		Objects.requireNonNull(this.headers, "Missing Http2 encoder");
		
		return Mono.fromSupplier(() -> {
			Http2RequestHeaders requestHeaders = new Http2RequestHeaders(this.headerService, headers);
			GenericRequestParameters requestParameters = new GenericRequestParameters(requestHeaders.getPath());
			// TODO Cookie decoder
			// TODO path parameter decoder
			
			Method method = requestHeaders.getMethod();
			
			// It is maybe better to find the handler here
			if(method == Method.POST || method == Method.PUT || method == Method.PATCH) {
				PostRequest request = new PostRequest(context.channel().remoteAddress(), requestHeaders, requestParameters, this.urlEncodedBodyDecoder, this.multipartBodyDecoder, false);
				GenericResponse response = new GenericResponse(this.headerService);
				
				Http2ServerStream<RequestBody> postServerStream = new Http2ServerStream<>(request, response, this.stream, context, this.encoder);
				postServerStream.setHandler(this.findHandler(request, context));
				
				return postServerStream;
			}
			else {
				GetRequest request = new GetRequest(context.channel().remoteAddress(), requestHeaders, requestParameters);
				GenericResponse response = new GenericResponse(this.headerService);
				
				Http2ServerStream<Void> getServerStream = new Http2ServerStream<>(request, response, this.stream, context, this.encoder);
				getServerStream.setHandler(this.findHandler(request));
				
				return getServerStream;
			}
		});
	}
}

