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
package io.inverno.mod.grpc.base.internal;

import io.inverno.mod.base.net.NetService;
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcMessageCompressor;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * <p>
 * A {@link GrpcMessageCompressor} implementation based on Netty's {@link EmbeddedChannel}. 
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public abstract class AbstractEmbeddedChannelGrpcMessageCompressor implements GrpcMessageCompressor {

	/**
	 * The message encoding identifying the compressor
	 */
	private final String messageEncoding;
	
	/**
	 * The ByteBuf allocator.
	 */
	private final ByteBufAllocator allocator;

	/**
	 * <p>
	 * Creates an embedded channel gRPC message compressor. 
	 * </p>
	 * 
	 * @param messageEncoding a message encoding
	 * @param netService      the net service
	 */
	public AbstractEmbeddedChannelGrpcMessageCompressor(String messageEncoding, NetService netService) {
		this.messageEncoding = messageEncoding;
		this.allocator = netService.getByteBufAllocator();
	}
	
	@Override
	public ByteBuf compress(ByteBuf data) throws GrpcException {
		CompositeByteBuf buffer = this.allocator.compositeBuffer();
		EmbeddedChannel channel = new EmbeddedChannel(this.createEncoder());
		channel.config().setAllocator(this.allocator);
		try {
			channel.writeOutbound(data);
			channel.finish();

			ByteBuf current;
			while( (current = channel.readOutbound()) != null) {
				buffer.addComponent(true, current);
			}
			return buffer;
		}
		finally {
			channel.close();
		}
	}

	@Override
	public ByteBuf uncompress(ByteBuf data) throws GrpcException {
		EmbeddedChannel channel = new EmbeddedChannel(this.createDecoder());
		channel.config().setAllocator(this.allocator);
		try {
			ChannelFuture future = channel.writeOneInbound(data);
			channel.finish();
			
			if (future.isSuccess()) {
				CompositeByteBuf buffer = null;
				while(true) {
					ByteBuf current = channel.readInbound();
					if(current == null) {
						break;
					}
					if(buffer == null) {
						buffer = this.allocator.compositeBuffer();
					}
					buffer.addComponent(true, current);
				}
				
				if(buffer == null) {
					throw new GrpcException(GrpcStatus.INTERNAL, "Unable to uncompress " + this.messageEncoding + " input");
				}
				return buffer;
			}
			else {
				throw new GrpcException(GrpcStatus.INTERNAL, future.cause());
			}
		}
		finally {
			channel.close();
		}
	}

	@Override
	public String getMessageEncoding() {
		return this.messageEncoding;
	}

	/**
	 * <p>
	 * Creates a data encoder.
	 * </p>
	 * 
	 * @return an encoder
	 */
	protected abstract MessageToByteEncoder<ByteBuf> createEncoder();
	
	/**
	 * <p>
	 * Creates a data decoder.
	 * </p>
	 * 
	 * @return a decoder
	 */
	protected abstract ByteToMessageDecoder createDecoder();
}
