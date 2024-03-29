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
import io.inverno.mod.http.client.internal.http1x.Http1xWebSocketConnection;
import io.inverno.mod.http.client.internal.http2.Http2Connection;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateClientExtensionHandshaker;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * The client endpoint channel configurer used to configure the channel pipeline when establishing a HTTP/1.X, HTTP/2 or WebSocket connection to an endpoint.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @version 1.6
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class EndpointChannelConfigurer {
	
	private final CompressionOptionsProvider compressionOptionsProvider;
	private final HttpConnectionFactory<Http1xConnection> http1xConnectionFactory;
	private final WebSocketConnectionFactory<Http1xWebSocketConnection> http1xWebSocketConnectionFactory;
	private final HttpConnectionFactory<Http2Connection> http2ConnectionFactory;
	
	private final ByteBufAllocator allocator;
	private final ByteBufAllocator directAllocator;

	/**
	 * <p>
	 * Creates an Endpoint channel configurer.
	 * </p>
	 *
	 * @param netService                       the net service
	 * @param compressionOptionsProvider       the compression options provider
	 * @param http1xConnectionFactory          the HTTP/1.x conection factory
	 * @param http1xWebSocketConnectionFactory the HTTP/1.x connection factory
	 * @param http2ConnectionFactory           the HTTP/2 connection factory
	 */
	public EndpointChannelConfigurer(
			NetService netService, 
			CompressionOptionsProvider compressionOptionsProvider,
			HttpConnectionFactory<Http1xConnection> http1xConnectionFactory,
			WebSocketConnectionFactory<Http1xWebSocketConnection> http1xWebSocketConnectionFactory,
			HttpConnectionFactory<Http2Connection> http2ConnectionFactory) {
		this.compressionOptionsProvider = compressionOptionsProvider;
		this.http1xConnectionFactory = http1xConnectionFactory;
		this.http1xWebSocketConnectionFactory = http1xWebSocketConnectionFactory;
		this.http2ConnectionFactory = http2ConnectionFactory;
		
		this.allocator = netService.getByteBufAllocator();
		this.directAllocator = netService.getDirectByteBufAllocator();
	}
	
	/**
	 * <p>
	 * Configures the specified channel pipeline.
	 * </p>
	 *
	 * @param pipeline      the pipeline to configure
	 * @param configuration an HTTP client configuration
	 * @param sslContext    the SSL context
	 * @param serverAddress the address of the endpoint
	 */
	void configure(ChannelPipeline pipeline, HttpClientConfiguration configuration, SslContext sslContext, InetSocketAddress serverAddress) {
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
	
	/**
	 * <p>
	 * Configures the specified channel pipeline for a WebSocket communication.
	 * </p>
	 * 
	 * @param pipeline      the pipeline to configure
	 * @param configuration an HTTP client configuration
	 * @param sslContext    the SSL context
	 * @param serverAddress the address of the endpoint
	 * 
	 * @return a WebSocket connection
	 */
	Http1xWebSocketConnection configureWebSocket(ChannelPipeline pipeline, HttpClientConfiguration configuration, SslContext sslContext, InetSocketAddress serverAddress) {
		if(sslContext != null) {
			if(configuration.tls_send_sni()) {
				pipeline.addLast("sslHandler", sslContext.newHandler(this.allocator, serverAddress.getHostName(), serverAddress.getPort()));
			}
			else {
				pipeline.addLast("sslHandler", sslContext.newHandler(this.allocator));
			}
		}
		
		pipeline.addLast("http1xDecoder", new HttpResponseDecoder());
		pipeline.addLast("http1xEncoder", new Http1xRequestEncoder(this.directAllocator));
		pipeline.addLast("http1xAggregator", new HttpObjectAggregator(8192));
		
		List<WebSocketClientExtensionHandshaker> extensionHandshakers = new LinkedList<>();
		if(configuration.ws_frame_compression_enabled()) {
			extensionHandshakers.add(new DeflateFrameClientExtensionHandshaker(configuration.ws_frame_compression_level(), false));
			extensionHandshakers.add(new DeflateFrameClientExtensionHandshaker(configuration.ws_frame_compression_level(), true));
		}
		if(configuration.ws_message_compression_enabled()) {
			extensionHandshakers.add(new PerMessageDeflateClientExtensionHandshaker(
				configuration.ws_message_compression_level(),
					configuration.ws_message_allow_client_window_size(),
					configuration.ws_message_requested_server_window_size(),
					configuration.ws_message_allow_client_no_context(),
					configuration.ws_message_requested_server_no_context()
			));
		}
		if(!extensionHandshakers.isEmpty()) {
			pipeline.addLast(new WebSocketClientExtensionHandler(extensionHandshakers.toArray(WebSocketClientExtensionHandshaker[]::new)));
		}
		
		Http1xWebSocketConnection connection = this.http1xWebSocketConnectionFactory.create(configuration, HttpVersion.HTTP_1_1);
		pipeline.addLast("connection", connection);
		
		return connection;
	}
	
	/**
	 * <p>
	 * Configures the specified channel pipeline for HTTP/1.x communication.
	 * </p>
	 * 
	 * @param pipeline      the pipeline to configure
	 * @param httpVersion   the HTTP version (HTTP/1.0 or HTTP/1/1)
	 * @param configuration an HTTP client configuration
	 * 
	 * @return an HTTP/1.x connection
	 */
	Http1xConnection configureHttp1x(ChannelPipeline pipeline, HttpVersion httpVersion, HttpClientConfiguration configuration) {
		// add HTTP decoder, in case of upgrade we must make sure clean stuff if needed
		pipeline.addLast("http1xDecoder", new HttpResponseDecoder());
		pipeline.addLast("http1xEncoder", new Http1xRequestEncoder(this.directAllocator));
		if(configuration.decompression_enabled()) {
			pipeline.addLast("http1xDecompressor", new HttpContentDecompressor(false));
		}
		if(configuration.compression_enabled()) {
			pipeline.addLast("http1xCompressor", new HttpContentCompressor(configuration.compression_contentSizeThreshold(), this.compressionOptionsProvider.get(configuration)));
		}
		Http1xConnection connection = this.http1xConnectionFactory.create(configuration, httpVersion, this);
		pipeline.addLast("connection", connection);
		
		return connection;
	}
	
	/**
	 * <p>
	 * Configures the specified channel pipeline for HTTP/2 communication.
	 * </p>
	 * 
	 * @param pipeline      the pipeline to configure
	 * @param configuration an HTTP client configuration
	 * 
	 * @return an HTTP/2 connection
	 */
	Http2Connection configureHttp2(ChannelPipeline pipeline, HttpClientConfiguration configuration) {
		Http2Connection connection = this.http2ConnectionFactory.create(configuration, HttpVersion.HTTP_2_0, this);
		pipeline.addLast("connection", connection);
		
		return connection;
	}
	
	/**
	 * <p>
	 * Configures the specified pipeline to start an H2C upgrade negotiation.
	 * <p>
	 * 
	 * @param pipeline          the pipeline to configure
	 * @param configuration     an HTTP client configuration
	 * @param upgradingExchange the HTTP/1.x upgrading exchange
	 *
	 * @return the future HTTP/2 connection that will be used in case negotiation succeeds
	 */
	public Http2Connection startHttp2Upgrade(ChannelPipeline pipeline, HttpClientConfiguration configuration, Http1xUpgradingExchange upgradingExchange) {
		Http2Connection connection = this.http2ConnectionFactory.create(configuration, HttpVersion.HTTP_2_0, this);
		upgradingExchange.init(connection);
		return connection;
	}
	
	/**
	 * <p>
	 * Configures the specified pipeline to completes the H2C upgrade after a successful negotiation to establish an HTTP/2 communication.
	 * </p>
	 * 
	 * @param pipeline          the pipeline to configure
	 * @param configuration     an HTTP client configuration
	 * @param upgradingExchange the HTTP/1.x upgrading exchange
	 * @param messageBuffer     the current message buffer
	 * 
	 * @throws HttpClientUpgradeException if there was an error during the upgrade
	 */
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
			
			// 5. Propagate buffered messages to the HTTP/2 connection handler
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
