package io.winterframework.mod.web.internal.server;

import java.util.function.Supplier;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.core.annotation.Lazy;
import io.winterframework.mod.web.WebConfiguration;
import io.winterframework.mod.web.internal.server.http1x.Http1xChannelHandler;
import io.winterframework.mod.web.internal.server.http2.H2cUpgradeHandler;
import io.winterframework.mod.web.internal.server.http2.Http2ChannelHandler;

@Bean(visibility = Visibility.PRIVATE)
@Sharable
public class WebChannelInitializer extends ChannelInitializer<SocketChannel> {

	private WebConfiguration configuration;
	
	private Supplier<Http1xChannelHandler> http11ChannelHandlerSupplier;
	private Supplier<Http2ChannelHandler> http2ChannelHandlerSupplier;
	private Supplier<ApplicationProtocolNegotiationHandler> protocolNegociationHandlerSupplier;
	private SslContext sslContext;
	
	public WebChannelInitializer(
		WebConfiguration configuration, 
		@Lazy Supplier<Http1xChannelHandler> http11ChannelHandlerSupplier, 
		@Lazy Supplier<Http2ChannelHandler> http2ChannelHandlerSupplier,
		@Lazy Supplier<SslContext> sslContextSupplier, 
		@Lazy Supplier<ApplicationProtocolNegotiationHandler> protocolNegociationHandlerSupplier) {
		this.configuration = configuration;
		this.http11ChannelHandlerSupplier = http11ChannelHandlerSupplier;
		this.http2ChannelHandlerSupplier = http2ChannelHandlerSupplier;
		this.protocolNegociationHandlerSupplier = protocolNegociationHandlerSupplier;
		
		if(this.configuration.ssl_enabled()) {
			this.sslContext = sslContextSupplier.get();			
		}
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if(this.configuration.ssl_enabled()) {
			ch.pipeline().addLast(this.sslContext.newHandler(ch.alloc()));
	        ch.pipeline().addLast(this.protocolNegociationHandlerSupplier.get());
		}
		else {
			if(this.configuration.h2c_enabled()) {
				ch.pipeline().addLast(new H2cUpgradeHandler(this.http2ChannelHandlerSupplier), this.http11ChannelHandlerSupplier.get());
			}
			else {
				ch.pipeline().addLast(new HttpServerCodec(), this.http11ChannelHandlerSupplier.get());
			}
			
		}
	}
}
