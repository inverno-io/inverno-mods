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
package io.inverno.mod.grpc.base;

import io.netty.buffer.ByteBuf;

/**
 * <p>
 * A gRPC message compressor is used to compress or uncompress gRPC messages.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcMessageCompressor {

	/**
	 * <p>
	 * Returns the message encoding identifying the compressor.
	 * </p>
	 * 
	 * <p>
	 * The message encoding is specified in gRPC request and response and indicates how message must be compressed or uncompressed to a client or a server.
	 * </p>
	 * 
	 * @return a message encoding
	 */
	String getMessageEncoding();
	
	/**
	 * <p>
	 * Compresses the specified data.
	 * </p>
	 * 
	 * @param data the data to compress
	 * 
	 * @return compressed data
	 * 
	 * @throws GrpcException if there was an error compressing data
	 */
	ByteBuf compress(ByteBuf data) throws GrpcException;
	
	/**
	 * <p>
	 * Uncompresses the specified data.
	 * </p>
	 * 
	 * @param data the data to uncompress
	 * 
	 * @return uncompressed data
	 * 
	 * @throws GrpcException if ther was an error uncompressing data
	 */
	ByteBuf uncompress(ByteBuf data) throws GrpcException;
}
