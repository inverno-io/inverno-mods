package io.winterframework.mod.web.internal.server;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;

@Bean(visibility = Visibility.PRIVATE)
@Sharable
public class WebChannelInitializer extends ChannelInitializer<SocketChannel> {

	private SslContext sslContext;
	
	private ApplicationProtocolNegotiationHandler protocolNegociationHandler;
	
	public WebChannelInitializer(SslContext sslContext, ApplicationProtocolNegotiationHandler protocolNegociationHandler) {
		this.sslContext = sslContext;
		this.protocolNegociationHandler = protocolNegociationHandler;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(this.sslContext.newHandler(ch.alloc()));
        ch.pipeline().addLast(this.protocolNegociationHandler);
	}
}
