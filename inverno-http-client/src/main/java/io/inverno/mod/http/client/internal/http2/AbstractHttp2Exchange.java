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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.internal.AbstractExchange;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.ScheduledFuture;
import reactor.core.publisher.MonoSink;

/**
 * <p>
 * Base HTTP/2 {@link Exchange} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public abstract class AbstractHttp2Exchange extends AbstractExchange<AbstractRequest, Http2Response, AbstractHttp2Exchange> {

	ScheduledFuture<AbstractHttp2Exchange> timeoutFuture;
	long lastModified;
	
	/**
	 * <p>
	 * Creates an HTTP/2 exchane.
	 * </p>
	 *
	 * @param context                 the channel context
	 * @param exchangeSink            the exchange sink
	 * @param exchangeContext         the exchange context
	 * @param request                 the HTTP request
	 */
	public AbstractHttp2Exchange(
			ChannelHandlerContext context, 
			MonoSink<HttpConnectionExchange<ExchangeContext, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> exchangeSink, 
			ExchangeContext exchangeContext, 
			AbstractRequest request) {
		super(context, exchangeSink, exchangeContext, HttpVersion.HTTP_2_0, request);
	}
	
	/**
	 * <p>
	 * Returns the channel context.
	 * </p>
	 * 
	 * @return the channel context
	 */
	public ChannelHandlerContext getChannelContext() {
		return this.context;
	}

	/**
	 * <p>
	 * Returns the HTTP/2 stream bound to the exchange.
	 * </p>
	 * 
	 * @return the HTTP/2 stream
	 */
	public abstract Http2Stream getStream();
}
