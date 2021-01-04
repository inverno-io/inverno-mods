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
package io.winterframework.mod.web.internal.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.core.annotation.Destroy;
import io.winterframework.core.annotation.Init;
import io.winterframework.mod.commons.net.NetService;
import io.winterframework.mod.web.WebConfiguration;

/**
 * 
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class WebServer {

	private Logger logger = LogManager.getLogger(this.getClass());
	
	private NetService netService;
	
	private WebConfiguration configuration;
	
	private ChannelInitializer<SocketChannel> channelInitializer;

	private ChannelFuture serverChannelFuture;
	
	public WebServer(WebConfiguration configuration, NetService netService, ChannelInitializer<SocketChannel> channelInitializer) {
		this.configuration = configuration;
		this.netService = netService;
		this.channelInitializer = channelInitializer;
	}
	
	@Init
	public void start() throws CertificateException, InterruptedException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		InetSocketAddress serverAddress = new InetSocketAddress(this.configuration.server_host(), this.configuration.server_port());
		
		ServerBootstrap serverBootstrap = this.netService.createServer(serverAddress, 8)
			.childHandler(this.channelInitializer);

		this.serverChannelFuture = serverBootstrap.bind(serverAddress).sync();
		String scheme = this.configuration.ssl_enabled() ? "https://" : "http://";
		if (this.serverChannelFuture.isSuccess()) {
			this.logger.info(() -> "Web Server listening on " + scheme + serverAddress.getHostString() + ":" + serverAddress.getPort());
		}
		else {
			throw new RuntimeException("Can't start Web server on " + scheme + serverAddress.getHostString() + ":" + serverAddress.getPort(), this.serverChannelFuture.cause());
		}
	}

	@Destroy
	public void stop() throws InterruptedException {
		this.serverChannelFuture.channel().close();
	}
}
