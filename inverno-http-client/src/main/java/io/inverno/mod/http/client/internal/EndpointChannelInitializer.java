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

import io.inverno.mod.http.client.HttpClientConfiguration;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;

/**
 * <p>
 * The endpoint channel initializer used to initialize HTTP/1.x and HTTP/2 connection.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @version 1.6
 * 
 * @see EndpointChannelConfigurer
 */
@Sharable
public class EndpointChannelInitializer extends ChannelInitializer<SocketChannel> {

	protected final SslContextProvider sslContextProvider;
	protected final EndpointChannelConfigurer channelConfigurer;
	protected final InetSocketAddress serverAddress;
	protected final HttpClientConfiguration configuration;
	protected final SslContext sslContext;
	
	/**
	 * <p>
	 * Creates the endpoint channel initializer.
	 * </p>
	 * 
	 * @param sslContextProvider the SSL context provider
	 * @param channelConfigurer  the endpoint channel configurer
	 * @param serverAddress      the address of the endpoint
	 * @param configuration      an HTTP client configuration
	 */
	public EndpointChannelInitializer(SslContextProvider sslContextProvider, EndpointChannelConfigurer channelConfigurer, InetSocketAddress serverAddress, HttpClientConfiguration configuration) {
		this.sslContextProvider = sslContextProvider;
		this.channelConfigurer = channelConfigurer;
		this.serverAddress = serverAddress;
		this.configuration = configuration;
		this.sslContext = configuration.tls_enabled() ? this.sslContextProvider.create(configuration) : null;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		this.channelConfigurer.configure(ch.pipeline(), this.configuration, this.sslContext, this.serverAddress);
	}
}
