package io.winterframework.mod.web.internal.server.http2;

import java.util.Optional;
import java.util.function.Supplier;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Settings;
import io.winterframework.core.annotation.Lazy;
import io.winterframework.mod.web.WebConfiguration;

// TODO we have an issue here: apparently the builder is referenced somehow in the resulting handler => the prototypewrapperbean weak hashmap is never cleaned which is more than problematic...
// This is too bad...
//@Bean(strategy = Strategy.PROTOTYPE, visibility = Visibility.PRIVATE)
//@Wrapper
public class Http2ChannelHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2ChannelHandler, Http2ChannelHandlerBuilder> implements Supplier<Http2ChannelHandler> {

	private Supplier<Http2ServerStreamBuilder> http2ServerStreamBuilderSupplier;
	
	public Http2ChannelHandlerBuilder(WebConfiguration configuration, @Lazy Supplier<Http2ServerStreamBuilder> http2ServerStreamBuilderSupplier) {
		//this.frameLogger(new Http2FrameLogger(LogLevel.INFO, Http2ConnectionAndFrameHandler.class));
		this.http2ServerStreamBuilderSupplier = http2ServerStreamBuilderSupplier;
		
		Http2Settings initialSettings = this.initialSettings();
		
		Optional.ofNullable(configuration.http2_header_table_size()).ifPresent(initialSettings::headerTableSize);
		Optional.ofNullable(configuration.http2_push_enabled()).ifPresent(initialSettings::pushEnabled);
		Optional.ofNullable(configuration.http2_max_concurrent_streams()).ifPresent(initialSettings::maxConcurrentStreams);
		Optional.ofNullable(configuration.http2_initial_window_size()).ifPresent(initialSettings::initialWindowSize);
		Optional.ofNullable(configuration.http2_max_frame_size()).ifPresent(initialSettings::maxFrameSize);
		Optional.ofNullable(configuration.http2_max_header_list_size()).ifPresent(initialSettings::maxHeaderListSize);
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
