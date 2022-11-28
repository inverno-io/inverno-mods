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
import io.inverno.mod.http.client.internal.AbstractExchange;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoSink;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public abstract class AbstractHttp2Exchange extends AbstractExchange<AbstractRequest, Http2Response, AbstractHttp2Exchange> {

	ScheduledFuture<AbstractHttp2Exchange> timeoutFuture;
	long lastModified;
	
	public AbstractHttp2Exchange(ChannelHandlerContext context, MonoSink<Exchange<ExchangeContext>> exchangeSink, ExchangeContext exchangeContext, AbstractRequest request, Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer) {
		super(context, exchangeSink, exchangeContext, request, responseBodyTransformer);
	}

	public abstract Http2Stream getStream();
}
