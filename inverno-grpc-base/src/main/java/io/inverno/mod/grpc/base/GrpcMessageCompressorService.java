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

import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * A message compressor service allows to resolve gRPC message compressors from message encodings.
 * </p>
 * 
 * <p>
 * This service shall concentrate all {@link GrpcMessageCompressor} instances injected in the module. A client or a server relies on this service to determine which compressor must be used to compress
 * or uncompress messages based on {@link GrpcHeaders#NAME_GRPC_ACCEPT_MESSAGE_ENCODING} or {@link GrpcHeaders#NAME_GRPC_MESSAGE_ENCODING} headers.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcMessageCompressorService {

	/**
	 * <p>
	 * Returns the first available gRPC message compressor matching the specified list of message encodings.
	 * </p>
	 * 
	 * @param messageEncodings a list of message encodings by order of preference
	 * 
	 * @return an optional returning the message compressor or an empty optional if no compressor was found or the specified list of encodings was null or empty
	 */
	Optional<GrpcMessageCompressor> getMessageCompressor(String... messageEncodings);
	
	/**
	 * <p>
	 * Returns the list of supported message encodings.
	 * </p>
	 * 
	 * @return a list of message encodings
	 */
	Set<String> getMessageEncodings();
}
