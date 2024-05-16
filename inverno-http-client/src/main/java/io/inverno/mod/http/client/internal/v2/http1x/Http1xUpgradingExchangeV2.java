/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal.v2.http1x;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.HttpConnectionExchange;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.inverno.mod.http.client.internal.HttpConnectionResponse;
import io.inverno.mod.http.client.internal.v2.http2.Http2ConnectionV2;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.CharObjectMap;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Http/1.x {@link Exchange} implementation supporting H2C upgrade.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http1xUpgradingExchangeV2<A extends ExchangeContext> extends Http1xExchangeV2<A> {

	private final Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> upgradedSink;
	private Http2ConnectionV2 upgradedConnection;
	
	/**
	 * <p>
	 * Creates an HTTP/1.x upgrading exchange.
	 * </p>
	 *
	 * @param configuration      the HTTP client configurartion
	 * @param sink               the exchange sink
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param context            the exchange context
	 * @param connection         the Http/1.x connection
	 * @param endpointRequest    the endpoint request
	 */
	public Http1xUpgradingExchangeV2(
			HttpClientConfiguration configuration, 
			Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> sink,
			HeaderService headerService, 
			ObjectConverter parameterConverter, 
			A context, 
			Http1xConnectionV2 connection,
			EndpointRequest endpointRequest) {
		super(configuration, null, headerService, parameterConverter, context, connection, endpointRequest);
		this.upgradedSink = sink;
	}
	
	/**
	 * <p>
	 * Encodes the HTTP/2 settings to be sent as HTTP/1.x header within the upgrade request.
	 * </p>
	 * 
	 * @param settings the HTTP/2 settings
	 * 
	 * @return the encoded settings
	 */
	private CharSequence encodeSettingsHeaderValue(Http2Settings settings) {
        ByteBuf buf = null;
		ByteBuf encodedBuf = null;
		try {
			// Serialize the payload of the SETTINGS frame.
			int payloadLength = Http2CodecUtil.SETTING_ENTRY_LENGTH * settings.size();
			buf = this.connection.alloc().buffer(payloadLength);
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
	
	/**
	 * <p>
	 * Initializes the H2C upgrade process.
	 * </p>
	 * 
	 * @param upgradedConnection the HTTP/2 upgraded connection
	 */
	public void init(Http2ConnectionV2 upgradedConnection) {
		this.upgradedConnection = upgradedConnection;
		this.request().headers().set(Headers.NAME_UPGRADE, Headers.VALUE_UPGRADE_H2C);
		this.request().headers().set(Headers.NAME_HTTP2_SETTINGS, this.encodeSettingsHeaderValue(this.upgradedConnection.decoder().localSettings()));
		this.request().headers().set(Headers.NAME_CONNECTION, Headers.NAME_UPGRADE + "," + Headers.NAME_HTTP2_SETTINGS);
	}

	@Override
	void dispose(Throwable cause) {
		this.upgradedSink.tryEmitError(cause != null ? cause : new HttpClientException("Exchange was disposed"));
		super.dispose(cause);
	}
	
	/**
	 * <p>
	 * Returns the ugraded exchange sink.
	 * </p>
	 * 
	 * @return the upgraded exchange sink
	 */
	public Sinks.One<HttpConnectionExchange<A, ? extends HttpConnectionRequest, ? extends HttpConnectionResponse>> getUpgradedSink() {
		return this.upgradedSink;
	}

	/**
	 * <p>
	 * Returns the HTTP/2 upgraded connection.
	 * </p>
	 * 
	 * @return the HTTP/2 uprgaded connection
	 */
	public Http2ConnectionV2 getUpgradedConnection() {
		return upgradedConnection;
	}
}
