package io.winterframework.mod.web.internal.server.http2;

import java.util.function.Supplier;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import io.winterframework.core.annotation.Lazy;

// TODO we have an issue here: apparently the builder is referenced in the resulting handler => the prototypewrapperbean weak hashmap is never cleaned which is more than problematic...
// This is too bad...
//@Bean(strategy = Strategy.PROTOTYPE, visibility = Visibility.PRIVATE)
//@Wrapper
public class Http2ChannelHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2ChannelHandler, Http2ChannelHandlerBuilder> implements Supplier<Http2ChannelHandler> {

	private Supplier<Http2ServerStreamBuilder> http2ServerStreamBuilderSupplier;
	
	public Http2ChannelHandlerBuilder(@Lazy Supplier<Http2ServerStreamBuilder> http2ServerStreamBuilderSupplier) {
		//this.frameLogger(new Http2FrameLogger(LogLevel.INFO, Http2ConnectionAndFrameHandler.class));
		
		this.http2ServerStreamBuilderSupplier = http2ServerStreamBuilderSupplier;
	}
	
	@Override
	public Http2ChannelHandler get() {
		return this.build();
	}

	@Override
	protected Http2ChannelHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) throws Exception {
		Http2ChannelHandler handler = new Http2ChannelHandler(decoder, encoder, initialSettings, this.http2ServerStreamBuilderSupplier);
		this.frameListener(handler);
		return handler;
	}
	
	@Override
	public Http2ChannelHandler build() {
		return super.build();
	}
}
