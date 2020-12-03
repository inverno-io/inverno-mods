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
package io.winterframework.mod.web.internal.server.http2;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.AsciiString;
import io.winterframework.mod.web.WebConfiguration;

/**
 * @author jkuhn
 *
 */
public class H2cUpgradeHandler extends ByteToMessageDecoder {

	private static final ByteBuf CONNECTION_PREFACE = Unpooled .unreleasableBuffer(Http2CodecUtil.connectionPrefaceBuf());

	private final HttpServerCodec httpServerCodec;
	private final HttpServerUpgradeHandler httpServerUpgradeHandler;

	private Supplier<Http2ChannelHandler> http2ChannelHandlerSupplier;
	
	private static class H2CUpgradeCodecFactory implements HttpServerUpgradeHandler.UpgradeCodecFactory {
		
		private WebConfiguration configuration;
		
		public H2CUpgradeCodecFactory(WebConfiguration configuration) {
			this.configuration = configuration;
		}
		
		@Override
		public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
			if(AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
				Http2ConnectionHandlerBuilder builder = new Http2ConnectionHandlerBuilder();
				
				Http2Settings initialSettings = builder.initialSettings();
				Optional.ofNullable(this.configuration.http2_header_table_size()).ifPresent(initialSettings::headerTableSize);
				Optional.ofNullable(this.configuration.http2_push_enabled()).ifPresent(initialSettings::pushEnabled);
				Optional.ofNullable(this.configuration.http2_max_concurrent_streams()).ifPresent(initialSettings::maxConcurrentStreams);
				Optional.ofNullable(this.configuration.http2_initial_window_size()).ifPresent(initialSettings::initialWindowSize);
				Optional.ofNullable(this.configuration.http2_max_frame_size()).ifPresent(initialSettings::maxFrameSize);
				Optional.ofNullable(this.configuration.http2_max_header_list_size()).ifPresent(initialSettings::maxHeaderListSize);
				
				builder.frameListener(new Http2FrameAdapter());
				
				return new Http2ServerUpgradeCodec(builder.build());
            } 
			return null;
		}
	}

	public H2cUpgradeHandler(WebConfiguration configuration, Supplier<Http2ChannelHandler> http2ChannelHandlerSupplier) {
		this.http2ChannelHandlerSupplier = Objects.requireNonNull(http2ChannelHandlerSupplier);

		this.httpServerCodec = new HttpServerCodec();
		this.httpServerUpgradeHandler = new HttpServerUpgradeHandler(this.httpServerCodec, new H2CUpgradeCodecFactory(configuration));
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		ctx.pipeline()
			.addAfter(ctx.name(), null, this.httpServerUpgradeHandler)
			.addAfter(ctx.name(), null, this.httpServerCodec);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		int prefaceLength = CONNECTION_PREFACE.readableBytes();
		int bytesRead = Math.min(in.readableBytes(), prefaceLength);
		if(!ByteBufUtil.equals(CONNECTION_PREFACE, CONNECTION_PREFACE.readerIndex(), in, in.readerIndex(), bytesRead)) {
			ctx.pipeline().remove(this);
		} 
		else if(bytesRead == prefaceLength) {
			ctx.pipeline()
				.remove(this.httpServerCodec)
				.remove(this.httpServerUpgradeHandler);
			
			ctx.pipeline().addAfter(ctx.name(), null, this.http2ChannelHandlerSupplier.get());
            ctx.pipeline().remove(this);
			ctx.fireUserEventTriggered(PriorKnowledgeUpgradeEvent.INSTANCE);
		}
	}

	/**
	 * User event that is fired to notify about HTTP/2 protocol is started.
	 */
	public static final class PriorKnowledgeUpgradeEvent {
		private static final PriorKnowledgeUpgradeEvent INSTANCE = new PriorKnowledgeUpgradeEvent();

		private PriorKnowledgeUpgradeEvent() {}
	}
}
