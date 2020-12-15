package io.winterframework.mod.web.internal.server;

import java.util.function.Supplier;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.internal.server.http1x.Http1xChannelHandler;
import io.winterframework.mod.web.internal.server.http1x.Http1xRequestDecoder;
import io.winterframework.mod.web.internal.server.http1x.Http1xResponseEncoder;
import io.winterframework.mod.web.internal.server.http2.Http2ChannelHandler;

@Bean(visibility = Visibility.PRIVATE)
@Sharable
public class WebProtocolNegociationHandler extends ApplicationProtocolNegotiationHandler {

	private Supplier<Http1xChannelHandler> http1xChannelHandlerFactory;
	private Supplier<Http2ChannelHandler> http2ChannelHandlerFactory;
	
	public WebProtocolNegociationHandler(
			Supplier<Http1xChannelHandler> http1xChannelHandlerFactory,
			Supplier<Http2ChannelHandler> http2ChannelHandlerFactory) {
		super(ApplicationProtocolNames.HTTP_1_1);
		
		this.http1xChannelHandlerFactory = http1xChannelHandlerFactory;
		this.http2ChannelHandlerFactory = http2ChannelHandlerFactory;
	}

	@Override
	protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
		if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            ctx.pipeline().addLast(this.http2ChannelHandlerFactory.get());
        }
		else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
			ctx.pipeline().addLast(new Http1xRequestDecoder());
			ctx.pipeline().addLast(new Http1xResponseEncoder());
			ctx.pipeline().addLast(this.http1xChannelHandlerFactory.get());
        }
		else {
			throw new IllegalStateException("Unsupported protocol: " + protocol);
		}
	}
}
