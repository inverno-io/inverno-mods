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

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcMessageCompressor;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * <p>
 * A gRPC message reader used to transform raw data publishers into gRPC message publishers.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @param <A> the message type
 */
public class GrpcMessageReader<A extends Message> implements Function<Publisher<ByteBuf>, Publisher<A>> {

	/**
	 * The default message instance.
	 */
	private final A defaultMessageInstance;
	/**
	 * The extension registry.
	 */
	private final ExtensionRegistry extensionRegistry;
	/**
	 * The gRPC message compressor.
	 */
	private final GrpcMessageCompressor messageCompressor;
	/**
	 * The ByteBuf allocator.
	 */
	private final ByteBufAllocator allocator;
	
	/**
	 * The current buffer containing length prefix data.
	 */
	private ByteBuf lengthPrefixBuffer;
	/**
	 * The current length prefix.
	 */
	private LengthPrefix lengthPrefix;
	/**
	 * The current accumulated size.
	 */
	private long accumulatedSize;
	/**
	 * The buffer retained from previous message.
	 */
	private ByteBuf retainedBuffer;
	
	/**
	 * <p>
	 * Creates a gRPC message reader for reading plain data.
	 * </p>
	 * 
	 * @param defaultMessageInstance the default message instance
	 * @param extensionRegistry      the extension registry
	 * @param netService             the net service
	 */
	public GrpcMessageReader(A defaultMessageInstance, ExtensionRegistry extensionRegistry, NetService netService) {
		this(defaultMessageInstance, extensionRegistry, netService, null);
	}

	/**
	 * <p>
	 * Creates a gRPC message reader for reading compressed data.
	 * </p>
	 * 
	 * @param defaultMessageInstance the default message instance
	 * @param extensionRegistry      the extension registry
	 * @param netService             the net service
	 * @param messageCompressor      the gRPC message compressor
	 */
	public GrpcMessageReader(A defaultMessageInstance, ExtensionRegistry extensionRegistry, NetService netService, GrpcMessageCompressor messageCompressor) {
		this.defaultMessageInstance = defaultMessageInstance;
		this.extensionRegistry = extensionRegistry != null ? extensionRegistry : ExtensionRegistry.getEmptyRegistry();
		this.messageCompressor = messageCompressor != null ? messageCompressor : IdentityGrpcMessageCompressor.INSTANCE;
		this.allocator = netService.getByteBufAllocator();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Publisher<A> apply(Publisher<ByteBuf> data) {
		return Flux.from(data)
			.bufferUntil(chunk -> {
				if(this.lengthPrefix == null) {
					if(this.lengthPrefixBuffer == null) {
						this.lengthPrefixBuffer = this.allocator.buffer(5, 5);
					}
					if(this.lengthPrefixBuffer.writableBytes() > 0) {
						if(this.retainedBuffer != null) {
							this.retainedBuffer.readBytes(this.lengthPrefixBuffer, Math.min(this.lengthPrefixBuffer.writableBytes(), this.retainedBuffer.readableBytes()));
							if(this.lengthPrefixBuffer.writableBytes() > 0) {
								this.retainedBuffer.release();
								this.retainedBuffer = null;
								chunk.readBytes(this.lengthPrefixBuffer, Math.min(this.lengthPrefixBuffer.writableBytes(), chunk.readableBytes()));
							}
							else {
								this.accumulatedSize = this.retainedBuffer.readableBytes();
							}
						}
						else {
							chunk.readBytes(this.lengthPrefixBuffer, Math.min(this.lengthPrefixBuffer.writableBytes(), chunk.readableBytes()));
						}
					}
					if(this.lengthPrefixBuffer.writableBytes() > 0) {
						chunk.release();
						return false;
					}
					this.lengthPrefix = new LengthPrefix(this.lengthPrefixBuffer);
					this.lengthPrefixBuffer = null;
				}

				this.accumulatedSize += chunk.readableBytes();

				return this.accumulatedSize >= this.lengthPrefix.messageLength;
			})
			.flatMapIterable(accBuffers -> {
				ByteBuf buffer;
				if(this.retainedBuffer != null) {
					CompositeByteBuf compBuffer = this.allocator.compositeBuffer(accBuffers.size() + 1);
					compBuffer.addComponent(true, this.retainedBuffer);
					accBuffers.forEach(chunk -> compBuffer.addComponent(true, chunk));
					buffer = compBuffer;
				}
				else {
					int index = 0;

					// remove and release first buffers when they are unreadable
					//noinspection StatementWithEmptyBody
					for(;index < accBuffers.size() && !accBuffers.get(index).isReadable();index++) {}

					if(index < accBuffers.size() - 1) {
						CompositeByteBuf compBuffer = this.allocator.compositeBuffer(accBuffers.size() - index);
						for(;index < accBuffers.size();index++) {
							compBuffer.addComponent(true, accBuffers.get(index));
						}
						buffer = compBuffer;
					}
					else if(index == accBuffers.size() - 1) {
						buffer = accBuffers.get(index);
					}
					else {
						// nothing is readable: this can happen we received an empty buffer and the publisher completes
						this.lengthPrefix = null;
						return List.of();
					}
				}
				
				try {
					List<A> messages = new LinkedList<>();
					do {
						ByteBuf messageBuffer = buffer.readRetainedSlice(this.lengthPrefix.messageLength);
						try {
							if(this.lengthPrefix.compressedFlag) {
								messageBuffer = this.messageCompressor.uncompress(messageBuffer);
							}
							messages.add((A)this.defaultMessageInstance.getParserForType().parseFrom(messageBuffer.nioBuffer(), this.extensionRegistry));

							if(buffer.readableBytes() < 5) {
								this.lengthPrefix = null;
								break;
							}
							else {
								this.lengthPrefix = new LengthPrefix(buffer.readRetainedSlice(5));
							}
						}
						catch(InvalidProtocolBufferException e) {
							throw new GrpcException(GrpcStatus.INTERNAL, "Invalid protobuf byte sequence", e);
						}
						finally {
							messageBuffer.release();
						}
					} while(buffer.readableBytes() >= this.lengthPrefix.messageLength);

					if(buffer.readableBytes() > 0) {
						this.retainedBuffer = buffer.retain();
					}

					return messages;
				}
				finally {
					buffer.release();
				}
			});
	}
	
	/**
	 * <p>
	 * Represents the length prefix (5 bytes) sent before gRPC message to indicate the message length and whether the message is compressed as defined by
	 * <a href="https://datatracker.ietf.org/doc/html/draft-kumar-rtgwg-grpc-protocol-00">gRPC protocol</a>
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	private static class LengthPrefix {
		
		/**
		 * The compressed flag.
		 */
		final boolean compressedFlag;
	
		/**
		 * The message length.
		 */
		final private int messageLength;
		
		/**
		 * <p>
		 * Creates a length prefix by reading data from the specified buffer.
		 * </p>
		 * 
		 * @param lengthPrefixBuffer a buffer container the length prefix
		 */
		public LengthPrefix(ByteBuf lengthPrefixBuffer) {
			try {
				this.compressedFlag = lengthPrefixBuffer.readByte() == 1;
				this.messageLength = lengthPrefixBuffer.readInt();
			}
			finally {
				lengthPrefixBuffer.release();
			}
		}
	}
}
