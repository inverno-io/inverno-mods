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

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.grpc.base.GrpcBaseConfiguration;
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcMessageCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.netty.handler.codec.compression.SnappyOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;

/**
 * <p>
 * Snappy {@link GrpcMessageCompressor} bean.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class SnappyGrpcMessageCompressor extends AbstractEmbeddedChannelGrpcMessageCompressor {

	/**
	 * The snappy compressor options.
	 */
	private final SnappyOptions snappyOptions;
	
	/**
	 * <p>
	 * Creates a snappy gRPC message compressor.
	 * </p>
	 * 
	 * @param configuration the gRPC base module configuration
	 * @param netService    the net service
	 */
	public SnappyGrpcMessageCompressor(GrpcBaseConfiguration configuration, NetService netService) {
		super(GrpcHeaders.VALUE_SNAPPY, netService);
		this.snappyOptions = StandardCompressionOptions.snappy();
	}
	
	@Override
	protected MessageToByteEncoder<ByteBuf> createEncoder() {
		return new SnappyFrameEncoder();
	}

	@Override
	protected ByteToMessageDecoder createDecoder() {
		return new SnappyFrameDecoder();
	}
}
