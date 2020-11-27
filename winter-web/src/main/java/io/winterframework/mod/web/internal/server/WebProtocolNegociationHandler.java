package io.winterframework.mod.web.internal.server;

import java.util.function.Supplier;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.core.annotation.Lazy;
import io.winterframework.mod.web.internal.server.http1x.Http1xChannelHandler;
import io.winterframework.mod.web.internal.server.http2.Http2ChannelHandler;

@Bean(visibility = Visibility.PRIVATE)
@Sharable
public class WebProtocolNegociationHandler extends ApplicationProtocolNegotiationHandler {

	private Supplier<Http1xChannelHandler> http11ChannelHandlerSupplier;
	
	private Supplier<Http2ChannelHandler> http2ChannelHandlerSupplier;
	
	public WebProtocolNegociationHandler(@Lazy Supplier<Http1xChannelHandler> http11ChannelHandlerSupplier, @Lazy Supplier<Http2ChannelHandler> http2ChannelHandlerSupplier) {
		super(ApplicationProtocolNames.HTTP_1_1);
		
		this.http11ChannelHandlerSupplier = http11ChannelHandlerSupplier;
		this.http2ChannelHandlerSupplier = http2ChannelHandlerSupplier;
	}

	@Override
	protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
		if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            ctx.pipeline().addLast(this.http2ChannelHandlerSupplier.get());
        }
		else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            ctx.pipeline().addLast(new HttpServerCodec(), this.http11ChannelHandlerSupplier.get());
        }
		else {
			throw new IllegalStateException("unknown protocol: " + protocol);
		}
	}
}
