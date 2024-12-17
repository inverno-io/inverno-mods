/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.server.internal;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;

/**
 * <p>
 * HTTP Channel initializer.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Visibility.PRIVATE)
@Sharable
public class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final HttpServerChannelConfigurer channelConfigurer;
	
	private final HttpConnectionGroup connectionGroup;
	
	/**
	 * <p>
	 * Creates an HTTP channel initializer.
	 * </p>
	 * 
	 * @param channelConfigurer the channel configurer
	 * @param connectionGroup   the connection group to track active connection
	 */
	public HttpServerChannelInitializer(HttpServerChannelConfigurer channelConfigurer, HttpConnectionGroup connectionGroup) {
		this.channelConfigurer = channelConfigurer;
		this.connectionGroup = connectionGroup;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if(!this.connectionGroup.isShuttingDown() && !this.connectionGroup.isClosed()) {
			this.channelConfigurer.configure(ch.pipeline());
			this.connectionGroup.register(ch);
		}
		else {
			// When the server is shutting down we must reject new connections
			ch.close();
		}
	}
}
