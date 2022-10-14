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
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Stream;
import reactor.core.publisher.MonoSink;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Http2UpgradedExchange extends AbstractHttp2Exchange {

	private final Http2Stream upgradingStream;
	
	public Http2UpgradedExchange(ChannelHandlerContext context, MonoSink<Exchange<ExchangeContext>> exchangeSink, ExchangeContext exchangeContext, AbstractRequest request, Http2ConnectionEncoder encoder, Http2Stream upgradingStream) {
		super(context, exchangeSink, exchangeContext, request);
		this.upgradingStream = upgradingStream;
	}

	@Override
	public Http2Stream getStream() {
		return this.upgradingStream;
	}

	@Override
	protected void doStart() throws HttpClientException {
		// Does nothing since the request has already been sent
		this.handler.exchangeStart(this);
	}
}
