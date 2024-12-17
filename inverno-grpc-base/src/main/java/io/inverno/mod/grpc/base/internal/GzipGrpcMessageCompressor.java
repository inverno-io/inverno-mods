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
import io.netty.handler.codec.compression.GzipOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

/**
 * <p>
 * Gzip {@link GrpcMessageCompressor} bean.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class GzipGrpcMessageCompressor extends AbstractEmbeddedChannelGrpcMessageCompressor {

	/**
	 * The gzip compressor options.
	 */
	private final GzipOptions gzipOptions;
	
	/**
	 * <p>
	 * Creates a gzip gRPC message compressor.
	 * </p>
	 * 
	 * @param configuration the gRPC base module configuration
	 * @param netService    the net service
	 */
	public GzipGrpcMessageCompressor(GrpcBaseConfiguration configuration, NetService netService) {
		super(GrpcHeaders.VALUE_GZIP, netService);
		this.gzipOptions = StandardCompressionOptions.gzip(configuration.compression_gzip_compressionLevel(), configuration.compression_gzip_windowBits(), configuration.compression_gzip_memLevel());
	}
	
	@Override
	protected MessageToByteEncoder<ByteBuf> createEncoder() {
		return ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP, this.gzipOptions.compressionLevel(), this.gzipOptions.windowBits(), this.gzipOptions.memLevel());
	}

	@Override
	protected ByteToMessageDecoder createDecoder() {
		return ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP);
	}
}
