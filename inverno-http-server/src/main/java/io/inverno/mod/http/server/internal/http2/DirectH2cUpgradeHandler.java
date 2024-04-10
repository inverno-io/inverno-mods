/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.server.internal.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import java.util.List;
import java.util.function.Supplier;

/**
 * <p>
 * Handle direct HTTP/2 connection over clear text.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.7
 */
public class DirectH2cUpgradeHandler extends ByteToMessageDecoder {
    
	private static final ByteBuf CONNECTION_PREFACE = Unpooled.unreleasableBuffer(Http2CodecUtil.connectionPrefaceBuf()).asReadOnly();

	private final HttpServerUpgradeHandler.SourceCodec sourceCodec;
	
	private final Supplier<Http2Connection> http2ConnectionFactory;
	
	/**
	 * <p>
	 * Creates a direct H2C handler.
	 * </p>
	 * 
	 * @param sourceCodec            the source codec
	 * @param http2ConnectionFactory the HTTP/2 connection factory
	 */
	public DirectH2cUpgradeHandler(HttpServerUpgradeHandler.SourceCodec sourceCodec, Supplier<Http2Connection> http2ConnectionFactory) {
		this.sourceCodec = sourceCodec;
		this.http2ConnectionFactory = http2ConnectionFactory;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		int prefaceLength = CONNECTION_PREFACE.readableBytes();
		int bytesRead = Math.min(in.readableBytes(), prefaceLength);

		if(!ByteBufUtil.equals(CONNECTION_PREFACE, CONNECTION_PREFACE.readerIndex(),
				in, in.readerIndex(), bytesRead)) {
			ctx.pipeline().remove(this);
		} 
		else if(bytesRead == prefaceLength) {
			// Full h2 preface match, removed source codec, using http2 codec to handle
			// following network traffic
			this.sourceCodec.upgradeFrom(ctx);

			ctx.pipeline().addAfter(ctx.name(), "connection", this.http2ConnectionFactory.get());
			ctx.pipeline().remove(this);
		}
	}
}
