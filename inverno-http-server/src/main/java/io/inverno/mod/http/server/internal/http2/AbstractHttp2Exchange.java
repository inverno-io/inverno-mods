/*
 * Copyright 2024 Jeremy Kuhn
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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.AbstractExchange;
import io.netty.handler.codec.http2.Http2Headers;

/**
 * <p>
 * Base Http/2 {@link Exchange} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
abstract class AbstractHttp2Exchange extends AbstractExchange<Http2Request, Http2Response, Http2ErrorExchange> {
	
	protected final Http2ConnectionStream connectionStream;
	
	/**
	 * <p>
	 * Creates an Http/2 exchange.
	 * </p>
	 * 
	 * @param configuration the server configuration
	 * @param controller    the server controller
	 * @param connection    the Http/2 connection
	 * @param headers       the originating Http headers
	 */
	public AbstractHttp2Exchange(
			HttpServerConfiguration configuration, 
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller, 
			Http2ConnectionStream connectionStream, 
			Http2Headers headers
		) {
		super(configuration, controller, Method.HEAD.name().equals(headers.get(Headers.NAME_PSEUDO_METHOD)));
		this.connectionStream = connectionStream;
	}
	
	/**
	 * <p>
	 * Creates an Http/2 exchange from the specified parent exchange.
	 * </p>
	 * 
	 * <p>
	 * This is used by {@link Http2ErrorExchange}.
	 * </p>
	 * 
	 * @param parentExchange the parent exchange
	 */
	protected AbstractHttp2Exchange(AbstractHttp2Exchange parentExchange) {
		super(parentExchange);
		this.connectionStream = parentExchange.connectionStream;
	}
	
	@Override
	public HttpVersion getProtocol() {
		return HttpVersion.HTTP_2_0;
	}
	
	@Override
	public final void reset(long code) {
		if(this.connectionStream.executor().inEventLoop()) {
			if(!this.connectionStream.isReset()) {
				// Exchange is eventually disposed when the stream is closed
				this.connectionStream.resetStream(code);
			}
		}
		else {
			this.connectionStream.executor().execute(() -> this.reset(code));
		}
	}
}
