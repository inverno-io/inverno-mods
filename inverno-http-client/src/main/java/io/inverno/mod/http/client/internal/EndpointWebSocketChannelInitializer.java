/*
 * Copyright 2023 Jeremy KUHN
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

import io.inverno.mod.http.client.HttpClientConfiguration;
import io.netty.channel.socket.SocketChannel;
import java.net.InetSocketAddress;

/**
 * <p>
 * The endpoint WebSocket channel initializer used to initialize WebSocket connection.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class EndpointWebSocketChannelInitializer extends EndpointChannelInitializer {

	/**
	 * <p>
	 * Creates the endpoint WebSocket channel initializer.
	 * </p>
	 * 
	 * @param sslContextProvider the SSL context provider
	 * @param channelConfigurer  the endpoint channel configurer
	 * @param serverAddress      the address of the endpoint
	 * @param configuration      an HTTP client configuration
	 */
	public EndpointWebSocketChannelInitializer(SslContextProvider sslContextProvider, EndpointChannelConfigurer channelConfigurer, InetSocketAddress serverAddress, HttpClientConfiguration configuration) {
		super(sslContextProvider, channelConfigurer, serverAddress, configuration);
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		this.channelConfigurer.configureWebSocketV2(ch.pipeline(), this.configuration, this.sslContext, this.serverAddress);
	}
}
