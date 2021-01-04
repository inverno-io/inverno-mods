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
