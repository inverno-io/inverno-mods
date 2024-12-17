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

import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.grpc.base.GrpcHeaders;
import io.inverno.mod.grpc.base.GrpcMessageCompressor;
import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Identity {@link GrpcMessageCompressor} implementation.
 * </p>
 * 
 * <p>
 * This is basically a Noop compressor.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public final class IdentityGrpcMessageCompressor implements GrpcMessageCompressor {

	/**
	 * The identity message compressor singleton.
	 */
	public static final IdentityGrpcMessageCompressor INSTANCE = new IdentityGrpcMessageCompressor();
	
	private IdentityGrpcMessageCompressor() {}
	
	@Override
	public ByteBuf compress(ByteBuf data) throws GrpcException {
		return data;
	}

	@Override
	public ByteBuf uncompress(ByteBuf data) throws GrpcException {
		return data;
	}

	@Override
	public String getMessageEncoding() {
		return GrpcHeaders.VALUE_IDENTITY;
	}
}