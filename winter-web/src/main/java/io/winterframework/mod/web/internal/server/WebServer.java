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
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.core.annotation.Destroy;
import io.winterframework.core.annotation.Init;
import io.winterframework.mod.web.WebConfiguration;

@Bean(visibility = Visibility.PRIVATE)
public class WebServer {

	private Logger logger = LogManager.getLogger(this.getClass());
	
	private WebConfiguration configuration;
	
	private ChannelInitializer<SocketChannel> channelInitializer;

	private EventLoopGroup acceptorGroup;
	
	private EventLoopGroup childGroup;
	
	private ChannelFuture serverChannelFuture;
	
	public WebServer(WebConfiguration configuration, ChannelInitializer<SocketChannel> channelInitializer) {
		this.configuration = configuration;
		this.channelInitializer = channelInitializer;
	}

	@Init
	public void start() throws CertificateException, InterruptedException, IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		if(Epoll.isAvailable()) {
			this.acceptorGroup = new EpollEventLoopGroup(1);
			this.childGroup = new EpollEventLoopGroup();
		}
		else {
			this.acceptorGroup = new NioEventLoopGroup(1);
			this.childGroup = new NioEventLoopGroup();
		}

		// Vertx does not rely on netty's pool, this reduces memory footprint, @see io.vertx.core.net.impl.PartialPooledByteBufAllocator
		ServerBootstrap serverBootstrap = new ServerBootstrap();
		serverBootstrap
			.option(ChannelOption.SO_BACKLOG, 1024)
			.childOption(ChannelOption.SO_KEEPALIVE, false)
			.childOption(ChannelOption.TCP_NODELAY, true)
			.group(this.acceptorGroup, this.childGroup)
			.childHandler(this.channelInitializer);
		
		if(Epoll.isAvailable()) {
			serverBootstrap.channel(EpollServerSocketChannel.class);
		}
		else {
			serverBootstrap.channel(NioServerSocketChannel.class);
		}

		InetSocketAddress serverAddress = new InetSocketAddress(this.configuration.server_host(), this.configuration.server_port());
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
	public void stop() {
		this.serverChannelFuture.channel().close();
		this.acceptorGroup.shutdownGracefully().syncUninterruptibly();
		this.childGroup.shutdownGracefully().syncUninterruptibly();
	}
}
