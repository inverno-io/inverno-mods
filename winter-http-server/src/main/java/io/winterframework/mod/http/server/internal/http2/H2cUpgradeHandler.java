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
package io.winterframework.mod.http.server.internal.http2;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.ReferenceCountUtil;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.internal.HttpChannelConfigurer;
import io.winterframework.mod.http.server.internal.netty.FlatFullHttpResponse;
import io.winterframework.mod.http.server.internal.netty.LinkedHttpHeaders;

/**
 * <p>
 * HTTP/2 over cleartext upgrade handler.
 * </p>
 * 
 * <p>
 * Implements HTTP/2 over cleartext upgrade protocol as defined by
 * <a href="https://tools.ietf.org/html/rfc7540#section-3.2">RFC 7540 Section
 * 3.2</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class H2cUpgradeHandler extends HttpObjectAggregator {

	private final HttpChannelConfigurer configurer;
	
	private boolean upgrading; 
	
	/**
	 * <p>
	 * Creates a H2C upgrade handler handling upgrade in empty requests.
	 * </p>
	 * 
	 * @param configurer the HTTP channel configurer
	 */
	public H2cUpgradeHandler(HttpChannelConfigurer configurer) {
		this(configurer, 0);
	}
	
	/**
	 * <p>
	 * Creates a H2C upgrade handler handling upgrades in any requests.
	 * </p>
	 * 
	 * @param configurer the HTTP channel configurer
	 */
	public H2cUpgradeHandler(HttpChannelConfigurer configurer, int maxContentLength) {
		super(maxContentLength);
		this.configurer = configurer;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
		// Determine if we're already handling an upgrade request or just starting a new
		// one.
		this.upgrading |= msg instanceof HttpRequest && ((HttpRequest) msg).headers().contains(Headers.NAME_UPGRADE, Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, true);
		if(!this.upgrading) {
			// Not handling an upgrade request, just pass it to the next handler.
			ReferenceCountUtil.retain(msg);
			out.add(msg);
			return;
		}

		FullHttpRequest request;
		if (msg instanceof FullHttpRequest) {
			request = (FullHttpRequest) msg;
			ReferenceCountUtil.retain(msg);
			out.add(msg);
		} 
		else {
			// Call the base class to handle the aggregation of the full request.
			super.decode(ctx, msg, out);
			if (out.isEmpty()) {
				// The full request hasn't been created yet, still awaiting more data.
				return;
			}
			// Finished aggregating the full request, get it from the output list.
			this.upgrading = false;
			request = (FullHttpRequest) out.get(0);
		}
	
		ChannelPipeline pipeline = ctx.pipeline();
		HttpHeaders requestHeaders = request.headers();
		String connection = requestHeaders.get(Headers.NAME_CONNECTION);
		if(connection != null && connection.length() > 0) {
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
				this.sendBadRequest(ctx);
			}
			
			// Connection: upgrade, http2-settings
			List<String> http2SettingsHeader = requestHeaders.getAll(Headers.NAME_HTTP2_SETTINGS);
			if(http2SettingsHeader.isEmpty() || http2SettingsHeader.size() > 1) {
				// request MUST include exactly one HTTP2-Settings (Section 3.2.1) header field.
				this.sendBadRequest(ctx);
			}
			// parse the settings
			try {
				Http2Settings requestHttp2Settings = this.decodeSettingsHeader(http2SettingsHeader.get(0));
				
				ChannelFuture sendAcceptUpgradeComplete = this.sendAcceptUpgrade(ctx);
				
				Http2ChannelHandler http2ChannelHandler = this.configurer.upgradeToHttp2(pipeline);
				http2ChannelHandler.onHttpServerUpgrade(requestHttp2Settings);
				http2ChannelHandler.onSettingsRead(ctx, requestHttp2Settings);
				
				out.clear();
				
				// Convert to Http2 Headers and propagate
				DefaultHttp2Headers headers = new DefaultHttp2Headers();
				headers.method(request.method().name());
				headers.path(request.uri());
				headers.authority(request.headers().get("host"));
				headers.scheme("http");
				request.headers().remove("http2-settings");
				request.headers().remove("host");
				request.headers().forEach(header -> headers.set(header.getKey().toLowerCase(), header.getValue()));
				
				boolean emptyRequest = request.content().readableBytes() == 0;
				
				Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, emptyRequest);
				http2ChannelHandler.onHeadersRead(ctx, 1, headersFrame.headers(), headersFrame.padding(), headersFrame.isEndStream());
				if(!emptyRequest) {
					Http2DataFrame dataFrame = new DefaultHttp2DataFrame(request.content(), true, 0);
					http2ChannelHandler.onDataRead(ctx, 1, dataFrame.content(), dataFrame.padding(), dataFrame.isEndStream());
					ctx.fireChannelRead(new DefaultHttp2DataFrame(request.content(), true, 0));
				}
				sendAcceptUpgradeComplete.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
			} 
			catch (IOException e) {
				this.sendBadRequest(ctx);
			}
		}
	}
	
	private ChannelFuture sendBadRequest(ChannelHandlerContext ctx) {
		HttpHeaders responseHeaders = new LinkedHttpHeaders();
		responseHeaders.add(Headers.NAME_CONNECTION, Headers.VALUE_CLOSE);
		FullHttpResponse response = new FlatFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, responseHeaders, Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE);
		return ctx.writeAndFlush(response);
	}
	
	private ChannelFuture sendAcceptUpgrade(ChannelHandlerContext ctx) {
		HttpHeaders responseHeaders = new LinkedHttpHeaders();
		responseHeaders.add(Headers.NAME_CONNECTION, Headers.NAME_UPGRADE);
		responseHeaders.add(Headers.NAME_UPGRADE, Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME);
		FullHttpResponse response = new FlatFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS, responseHeaders, Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE);
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
