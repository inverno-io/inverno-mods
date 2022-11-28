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

package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.internal.http2.Http2Connection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.CharObjectMap;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoSink;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Http1xUpgradingExchange extends Http1xExchange {

	private final MonoSink<Exchange<ExchangeContext>> upgradedExchangeSink;
	private Http2Connection upgradedConnection;
	
	public Http1xUpgradingExchange(ChannelHandlerContext context, MonoSink<Exchange<ExchangeContext>> exchangeSink, ExchangeContext exchangeContext, Http1xRequest request, Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer, Http1xConnectionEncoder encoder) {
		super(context, null, exchangeContext, request, responseBodyTransformer, encoder);
		this.upgradedExchangeSink = exchangeSink;
	}
	
	public void init(Http2Connection upgradedConnection) {
		this.upgradedConnection = upgradedConnection;
		this.request.headers(headers -> {
			headers.set(Headers.NAME_UPGRADE, Headers.VALUE_UPGRADE_H2C);
			headers.set(Headers.NAME_HTTP2_SETTINGS, this.encodeSettingsHeaderValue(this.upgradedConnection.decoder().localSettings()));
			headers.set(Headers.NAME_CONNECTION, Headers.NAME_UPGRADE + "," + Headers.NAME_HTTP2_SETTINGS);
		});
	}

	@Override
	public void dispose(Throwable error) {
		this.upgradedExchangeSink.error(error);
		super.dispose(error);
	}
	
	public Function<Publisher<ByteBuf>, Publisher<ByteBuf>> getResponseBodyTransformer() {
		return this.responseBodyTransformer;
	}

	public MonoSink<Exchange<ExchangeContext>> getUpgradedExchangeSink() {
		return this.upgradedExchangeSink;
	}

	public Http2Connection getUpgradedConnection() {
		return upgradedConnection;
	}

	public long getLastModified() {
		return lastModified;
	}
	
	private CharSequence encodeSettingsHeaderValue(Http2Settings settings) {
        ByteBuf buf = null;
		ByteBuf encodedBuf = null;
		try {
			// Serialize the payload of the SETTINGS frame.
			int payloadLength = Http2CodecUtil.SETTING_ENTRY_LENGTH * settings.size();
			buf = this.context.alloc().buffer(payloadLength);
			for (CharObjectMap.PrimitiveEntry<Long> entry : settings.entries()) {
				buf.writeChar(entry.key());
				buf.writeInt(entry.value().intValue());
			}

			// Base64 encode the payload and then convert to a string for the header.
			encodedBuf = Base64.encode(buf, io.netty.handler.codec.base64.Base64Dialect.URL_SAFE);
			return encodedBuf.toString(Charsets.UTF_8);
		} 
		finally {
			ReferenceCountUtil.release(buf);
			ReferenceCountUtil.release(encodedBuf);
		}
    }
}
