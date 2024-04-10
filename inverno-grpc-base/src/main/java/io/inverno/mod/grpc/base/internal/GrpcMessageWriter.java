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

import com.google.protobuf.Message;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcMessageCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * <p>
 * A gRPC message writer used to transform gRPC message publishers into raw data publishers.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @param <A> the message type
 */
public class GrpcMessageWriter<A extends Message> implements Function<Publisher<A>, Publisher<ByteBuf>> {

	/**
	 * The gRPC message compressor.
	 */
	private final GrpcMessageCompressor messageCompressor;
	/**
	 * The ByteBuf allocator.
	 */
	private final ByteBufAllocator allocator;

	/**
	 * <p>
	 * Creates a gRPC message reader for writing plain data.
	 * </p>
	 * 
	 * @param netService the net service
	 */
	public GrpcMessageWriter(NetService netService) {
		this(netService, null);
	}
	
	/**
	 * <p>
	 * Creates a gRPC message reader for writing compressed data.
	 * </p>
	 * 
	 * @param netService        the net service
	 * @param messageCompressor the gRPC message compressor
	 */
	public GrpcMessageWriter(NetService netService, GrpcMessageCompressor messageCompressor) {
		this.messageCompressor = messageCompressor != null ? messageCompressor : IdentityGrpcMessageCompressor.INSTANCE;
		this.allocator = netService.getByteBufAllocator();
	}
	
	@Override
	public Publisher<ByteBuf> apply(Publisher<A> data) {
		return Flux.from(data).map(message -> {
			ByteBuf lengthPrefixBuffer = this.allocator.buffer(5, 5);
			boolean compressed = !this.messageCompressor.getMessageEncoding().equals(GrpcHeaders.VALUE_IDENTITY);
			
			// compressed-flag
			lengthPrefixBuffer.writeByte(compressed ? 1 : 0);

			byte[] payload = message.toByteArray();
			ByteBuf payloadBuffer = Unpooled.wrappedBuffer(payload);
			if(compressed) {
				payloadBuffer = this.messageCompressor.compress(payloadBuffer);
			}
			
			// message-length
			lengthPrefixBuffer.writeInt(payloadBuffer.readableBytes());
			
			CompositeByteBuf buffer = this.allocator.compositeBuffer(2);
			buffer.addComponent(true, lengthPrefixBuffer);
			buffer.addComponent(true, payloadBuffer);
			
			return buffer;
		});
	}
}
