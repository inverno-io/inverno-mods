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

package io.inverno.mod.http.client.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientUpgradeException;
import io.inverno.mod.http.client.internal.http1x.Http1xConnection;
import io.inverno.mod.http.client.internal.http1x.Http1xRequestEncoder;
import io.inverno.mod.http.client.internal.http1x.Http1xUpgradingExchange;
import io.inverno.mod.http.client.internal.http2.Http2Connection;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.Set;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class EndpointChannelConfigurer {
	
	private final CompressionOptionsProvider compressionOptionsProvider;
	private final HttpConnectionFactory<Http1xConnection> http1xConnectionFactory;
	private final HttpConnectionFactory<Http2Connection> http2ConnectionFactory;
	
	private final ByteBufAllocator allocator;
	private final ByteBufAllocator directAllocator;

	public EndpointChannelConfigurer(
			NetService netService, 
			CompressionOptionsProvider compressionOptionsProvider,
			HttpConnectionFactory<Http1xConnection> http1xConnectionFactory,
			HttpConnectionFactory<Http2Connection> http2ConnectionFactory) {
		this.compressionOptionsProvider = compressionOptionsProvider;
		this.http1xConnectionFactory = http1xConnectionFactory;
		this.http2ConnectionFactory = http2ConnectionFactory;
		
		this.allocator = netService.getByteBufAllocator();
		this.directAllocator = netService.getDirectByteBufAllocator();
	}
	
	public void configure(ChannelPipeline pipeline, HttpClientConfiguration configuration, SslContext sslContext, InetSocketAddress serverAddress) {
		// TODO connection timeout
		Set<HttpVersion> httpVersions = configuration.http_protocol_versions();
		if(httpVersions == null || httpVersions.isEmpty()) {
			httpVersions = HttpClientConfiguration.DEFAULT_HTTP_PROTOCOL_VERSIONS;
		}
		
		if(sslContext != null) {
			if(configuration.tls_send_sni()) {
				pipeline.addLast("sslHandler", sslContext.newHandler(this.allocator, serverAddress.getHostName(), serverAddress.getPort()));
			}
			else {
				pipeline.addLast("sslHandler", sslContext.newHandler(this.allocator));
			}

			if(!sslContext.applicationProtocolNegotiator().protocols().isEmpty()) {
				// alpn
				pipeline.addLast("protocolNegotiationHandler", new HttpProtocolNegotiationHandler(this, configuration));
			}
			else if(httpVersions.contains(HttpVersion.HTTP_1_1)) {
				this.configureHttp1x(pipeline, HttpVersion.HTTP_1_1, configuration);
			}
			else {
				this.configureHttp1x(pipeline, HttpVersion.HTTP_1_0, configuration);
			}
		}
		else {
			if(httpVersions.contains(HttpVersion.HTTP_2_0)) {
				if(httpVersions.size() == 1) {
					this.configureHttp2(pipeline, configuration);
				}
				else {
					// H2C
					this.configureHttp1x(pipeline, HttpVersion.HTTP_2_0, configuration);
				}
			}
			else if(httpVersions.contains(HttpVersion.HTTP_1_1)) {
				// HTTP/1.x
				this.configureHttp1x(pipeline, HttpVersion.HTTP_1_1, configuration);
			}
			else {
				this.configureHttp1x(pipeline, HttpVersion.HTTP_1_0, configuration);
			}
		}
	}
	
	// Must return the connection which is also the channel handler
	public Http1xConnection configureHttp1x(ChannelPipeline pipeline, HttpVersion httpVersion, HttpClientConfiguration configuration) {
		// add HTTP decoder, in case of upgrade we must make sure clean stuff if needed
		pipeline.addLast("http1xDecoder", new HttpResponseDecoder());
		pipeline.addLast("http1xEncoder", new Http1xRequestEncoder(this.directAllocator));
		if(configuration.decompression_enabled()) {
			pipeline.addLast("http1xDecompressor", new HttpContentDecompressor(false));
		}
		if(configuration.compression_enabled()) {
			pipeline.addLast("http1xCompressor", new HttpContentCompressor(configuration.compression_contentSizeThreshold(), this.compressionOptionsProvider.get(configuration)));
		}
		Http1xConnection connection = this.http1xConnectionFactory.get(configuration, httpVersion, this);
		pipeline.addLast("connection", connection);
		
		return connection;
	}
	
	public Http2Connection configureHttp2(ChannelPipeline pipeline, HttpClientConfiguration configuration) {
		Http2Connection connection = this.http2ConnectionFactory.get(configuration, HttpVersion.HTTP_2_0, this);
		pipeline.addLast("connection", connection);
		
		return connection;
	}
	
	public Http2Connection startHttp2Upgrade(ChannelPipeline pipeline, HttpClientConfiguration configuration, Http1xUpgradingExchange upgradingExchange) {
		Http2Connection connection = this.http2ConnectionFactory.get(configuration, HttpVersion.HTTP_2_0, this);
		upgradingExchange.init(connection);
		return connection;
	}
	
	public void completeHttp2Upgrade(ChannelPipeline pipeline, HttpClientConfiguration configuration, Http1xUpgradingExchange upgradingExchange, Deque<Object> messageBuffer) throws HttpClientUpgradeException {
		try {
			// 1. Remove the HTTP/1.x encoder
			pipeline.remove("http1xEncoder");
			if(configuration.compression_enabled()) {
				pipeline.remove("http1xCompressor");
			}
			// 2. Remove the HTTP/1.x upgrading connection
			pipeline.remove("connection");
			
			// 3. Add the HTTP/2 connection handler
			// This sends the preface (which is why we removed the HTTP/1.x encoder before)
			Http2Connection connection = upgradingExchange.getUpgradedConnection();
			pipeline.addLast("connection", connection);
			
			// 4. Upgrade
			// This reserves stream 1 and sets the initial upgraded exchange in the connection
			connection.onHttpClientUpgrade(upgradingExchange);
			
			// 5. Popagate buffered messages to the HTTP/2 connection handler
			if(messageBuffer != null) {
				Object current;
				while( (current = messageBuffer.poll()) != null) {
					pipeline.fireChannelRead(current);
				}
			}
			
			// 6. Remove the HTTP/1.x decoder
			// This propagates any buffered messages
			pipeline.remove("http1xDecoder");
			if(configuration.decompression_enabled()) {
				pipeline.remove("http1xDecompressor");
			}
		}
		catch(Http2Exception e) {
			// We must make sure this never happens
			throw new IllegalStateException(e);
		}
	}
}
