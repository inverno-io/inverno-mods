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

/**
 * <p>
 * Represents immutable inbound gRPC response trailers metadata.
 * </p>
 * 
 * <p>
 * This extends the {@link GrpcInboundMetadata} to expose response trailers specific information like the gRPC status and message.
 * </p>
 * 
 * <p>
 * An inbound response is received by a client in a client exchange, the trailers metadata are received last and terminates the gRPC exchange.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcInboundResponseTrailersMetadata extends GrpcInboundMetadata {

	/**
	 * <p>
	 * Returns the gRPC status.
	 * </p>
	 * 
	 * @return the response status
	 * 
	 * @throws IllegalArgumentException if the status code specified in the metadata is not a known gRPC status
	 */
	GrpcStatus getStatus();
	
	/**
	 * <p>
	 * Returns the gRPC status code.
	 * </p>
	 * 
	 * @return the gRPC status code
	 */
	int getStatusCode();
	
	/**
	 * <p>
	 * Returns the gRPC status message.
	 * </p>
	 * 
	 * @return an optional returning the gRPC status message or an empty optional if none was specified
	 */
	Optional<String> getStatusMessage();
}
