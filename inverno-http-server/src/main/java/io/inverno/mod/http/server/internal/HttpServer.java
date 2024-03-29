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
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.http.server.HttpServerConfiguration;

/**
 * <p>
 * The HTTP1.x and HTTP/2 server.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean(visibility = Visibility.PRIVATE)
public class HttpServer {

	private Logger logger = LogManager.getLogger(this.getClass());
	
	private NetService netService;
	
	private HttpServerConfiguration configuration;
	
	private ChannelInitializer<SocketChannel> channelInitializer;

	private ChannelFuture serverChannelFuture;
	
	/**
	 * <p>
	 * Creates a HTTP server.
	 * </p>
	 * 
	 * @param configuration      the HTTP server configuration
	 * @param netService         the Net service
	 * @param channelInitializer the channel initializer
	 */
	public HttpServer(HttpServerConfiguration configuration, NetService netService, ChannelInitializer<SocketChannel> channelInitializer) {
		this.configuration = configuration;
		this.netService = netService;
		this.channelInitializer = channelInitializer;
	}
	
	/**
	 * <p>
	 * Starts the HTTP server.
	 * </p>
	 * 
	 * @throws CertificateException
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 */
	@Init
	public void start() throws CertificateException, InterruptedException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		InetSocketAddress serverAddress = new InetSocketAddress(this.configuration.server_host(), this.configuration.server_port());
		
		ServerBootstrap serverBootstrap;
		if(this.configuration.server_event_loop_group_size() != null) {
			serverBootstrap = this.netService.createServer(serverAddress, this.configuration.server_event_loop_group_size());
		}
		else {
			serverBootstrap = this.netService.createServer(serverAddress);
		}

		this.serverChannelFuture = serverBootstrap
			.childHandler(this.channelInitializer)
			.bind(serverAddress).sync();
		
		String scheme = this.configuration.tls_enabled() ? "https://" : "http://";
		if (this.serverChannelFuture.isSuccess()) {
			this.logger.info("HTTP Server ({}) listening on {}", this.netService.getTransportType().toString().toLowerCase(), scheme + serverAddress.getHostString() + ":" + serverAddress.getPort());
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
