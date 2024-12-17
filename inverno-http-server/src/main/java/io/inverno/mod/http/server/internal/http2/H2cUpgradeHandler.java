/*
 * Copyright 2021 Jeremy KUHN
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

import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.netty.FlatFullHttpResponse;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.server.internal.HttpServerChannelConfigurer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Settings;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * <p>
 * HTTP/2 over cleartext upgrade handler.
 * </p>
 *
 * <p>
 * Implements HTTP/2 over cleartext upgrade protocol as defined by <a href="https://tools.ietf.org/html/rfc7540#section-3.2">RFC 7540 Section 3.2</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @deprecated Replaced by Netty's {@link HttpServerUpgradeHandler}
 * 
 * @see HttpServerChannelConfigurer
 */
@Deprecated
public class H2cUpgradeHandler extends ChannelInboundHandlerAdapter {

	private final HttpServerChannelConfigurer configurer;
	
	private Http2Connection http2Connection;
	
	private boolean upgrading;
	
	/**
	 * <p>
	 * Creates a H2C upgrade handler.
	 * </p>
	 * 
	 * @param configurer the HTTP channel configurer
	 */
	public H2cUpgradeHandler(HttpServerChannelConfigurer configurer) {
		this.configurer = configurer;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		this.upgrading |= msg instanceof HttpRequest && ((HttpRequest) msg).headers().contains(Headers.NAME_UPGRADE, Headers.VALUE_UPGRADE_H2C, true);
		if(!this.upgrading) {
			// Not an H2C upgrade request: forward to http1x connection
			ctx.fireChannelRead(msg);
			return;
		}
		
		if(msg instanceof HttpRequest) {
			HttpRequest request = (HttpRequest) msg;
			HttpHeaders requestHeaders = request.headers();

			// look for connection, settings...
			String connection = requestHeaders.get(Headers.NAME_CONNECTION);
			if(connection != null && !connection.isEmpty()) {
				int connectionIndex = 0;
				int length = connection.length();
				String currentHeader = null;
				int currentHeaderIndex = 0;
				boolean skip = false;
				int headersFound = 0;
				while(connectionIndex < length) {
					char nextChar = Character.toLowerCase(connection.charAt(connectionIndex++));
					if(nextChar == ',' || connectionIndex == length) {
						if(!skip) {
							headersFound++;
						}
						currentHeader = null;
						currentHeaderIndex = 0;
						skip = false;
					}
					else if(!skip && nextChar != ' ') {
						if(currentHeader == null) {
							if(nextChar == Headers.NAME_UPGRADE.charAt(currentHeaderIndex)) {
								currentHeader = Headers.NAME_UPGRADE;
							}
							else if(nextChar == Headers.NAME_HTTP2_SETTINGS.charAt(currentHeaderIndex)) {
								currentHeader = Headers.NAME_HTTP2_SETTINGS;
							}
							else {
								skip = true;
							}
							currentHeaderIndex++;
						}
						else {
							skip = nextChar != currentHeader.charAt(currentHeaderIndex++);
						}
					}
				}

				if(headersFound != 2) {
					// We must have: Connection: upgrade, http2-settings
					this.sendBadRequest(request.protocolVersion(), ctx);
				}

				// Connection: upgrade, http2-settings
				List<String> http2SettingsHeader = requestHeaders.getAll(Headers.NAME_HTTP2_SETTINGS);
				if(http2SettingsHeader.size() != 1) {
					// request MUST include exactly one HTTP2-Settings (Section 3.2.1) header field.
					this.sendBadRequest(request.protocolVersion(), ctx);
				}
				// parse the settings
				try {
					Http2Settings requestHttp2Settings = this.decodeSettingsHeader(http2SettingsHeader.getFirst());

					this.sendAcceptUpgrade(request.protocolVersion(), ctx);
					this.http2Connection = this.configurer.startHttp2Upgrade(ctx.pipeline());
					this.http2Connection.onHttpServerUpgrade(requestHttp2Settings);
					this.http2Connection.onSettingsRead(ctx, requestHttp2Settings);

					// Convert to Http2 Headers and propagate
					DefaultHttp2Headers headers = new DefaultHttp2Headers();
					headers.method(request.method().name());
					headers.path(request.uri());
					headers.authority(request.headers().get("host"));
					headers.scheme("http");
					request.headers().remove("http2-settings");
					request.headers().remove("host");
					request.headers().forEach(header -> headers.set(header.getKey().toLowerCase(), header.getValue()));

					this.http2Connection.onHeadersRead(ctx, 1, headers, 0, false);
				} 
				catch (IOException e) {
					this.sendBadRequest(request.protocolVersion(), ctx);
				}
			}
		}
		else if(this.http2Connection != null && msg instanceof HttpContent) {
			// transform content in http2 frame
			// just ignore content if connection is null since error should have been reported
			HttpContent content = (HttpContent)msg;
			boolean endStream = content instanceof LastHttpContent;
			
			try {
				this.http2Connection.onDataRead(ctx, 1, content.content(), 0, endStream);
				if(endStream) {
					this.configurer.completeHttp2Upgrade(ctx.pipeline());
				}
			}
			finally {
				// Http2Connection usually receives non-retained buffers which is not the case when the buffer comes from the http1xDecoder
				// For some reason we can't do this before otherwise it leads to a protocol error
				content.content().release();
			}
		}
	}
	
	private ChannelFuture sendBadRequest(HttpVersion version, ChannelHandlerContext ctx) {
		HttpHeaders responseHeaders = new LinkedHttpHeaders();
		responseHeaders.add(Headers.NAME_CONNECTION, Headers.VALUE_CLOSE);
		FullHttpResponse response = new FlatFullHttpResponse(version, HttpResponseStatus.BAD_REQUEST, responseHeaders, Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE);
		return ctx.writeAndFlush(response);
	}
	
	private ChannelFuture sendAcceptUpgrade(HttpVersion version, ChannelHandlerContext ctx) {
		HttpHeaders responseHeaders = new LinkedHttpHeaders();
		responseHeaders.add(Headers.NAME_CONNECTION, Headers.NAME_UPGRADE);
		responseHeaders.add(Headers.NAME_UPGRADE, Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME);
		FullHttpResponse response = new FlatFullHttpResponse(version, HttpResponseStatus.SWITCHING_PROTOCOLS, responseHeaders, Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE);
		return ctx.writeAndFlush(response);
	}
	
    private Http2Settings decodeSettingsHeader(CharSequence settingsHeader) throws IOException {
    	Http2Settings http2Settings = new Http2Settings();
    	byte[] settingsHeaderBytes = Base64.getUrlDecoder().decode(settingsHeader.toString());
    	try(DataInputStream settingsDataStream = new DataInputStream(new ByteArrayInputStream(settingsHeaderBytes))) {
    		int readCount = 0;
    		while(readCount < settingsHeaderBytes.length) {
    			int identifier = settingsDataStream.readUnsignedShort();
    			long value = Integer.toUnsignedLong(settingsDataStream.readInt());
    			http2Settings.put((char)identifier, (Long)value);
    			readCount += 6;
    		}
    		return http2Settings;
    	}
    }
}
