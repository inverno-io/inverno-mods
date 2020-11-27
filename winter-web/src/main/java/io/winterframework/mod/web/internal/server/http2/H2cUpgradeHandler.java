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
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.util.AsciiString;

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
		
		@Override
		public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
			if(AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(new Http2ConnectionHandlerBuilder().build());
            } 
			return null;
		}
	}

	public H2cUpgradeHandler(Supplier<Http2ChannelHandler> http2ChannelHandlerSupplier) {
		this.http2ChannelHandlerSupplier = Objects.requireNonNull(http2ChannelHandlerSupplier);

		this.httpServerCodec = new HttpServerCodec();
		this.httpServerUpgradeHandler = new HttpServerUpgradeHandler(this.httpServerCodec, new H2CUpgradeCodecFactory());
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		ctx.pipeline().addAfter(ctx.name(), null, this.httpServerUpgradeHandler).addAfter(ctx.name(), null, this.httpServerCodec);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		int prefaceLength = CONNECTION_PREFACE.readableBytes();
		int bytesRead = Math.min(in.readableBytes(), prefaceLength);

		if(!ByteBufUtil.equals(CONNECTION_PREFACE, CONNECTION_PREFACE.readerIndex(), in, in.readerIndex(), bytesRead)) {
			ctx.pipeline().remove(this);
		} 
		else if(bytesRead == prefaceLength) {
			ctx.pipeline().remove(this.httpServerCodec).remove(this.httpServerUpgradeHandler);
			
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
