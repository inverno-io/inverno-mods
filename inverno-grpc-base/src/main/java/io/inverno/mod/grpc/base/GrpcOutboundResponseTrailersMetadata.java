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

/**
 * <p>
 * Represents mutable outbound gRPC response metadata.
 * </p>
 * 
 * <p>
 * This extends the {@link GrpcOutboundMetadata} to expose response specific information like accepted message encodings or message encoding.
 * </p>
 * 
 * <p>
 * An outbound response is sent by a server in a server exchange, trailers metadata are sent after the response message publisher completes to terminates the gRPC exchange.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcOutboundResponseTrailersMetadata extends GrpcInboundResponseTrailersMetadata, GrpcOutboundMetadata<GrpcOutboundResponseTrailersMetadata> {

	/**
	 * <p>
	 * Sets the gRPC status code.
	 * </p>
	 * 
	 * @param statusCode a gRPC status code
	 * 
	 * @return the response trailers metadata
	 */
	GrpcOutboundResponseTrailersMetadata status(int statusCode);
	
	/**
	 * <p>
	 * Sets the gRPC status.
	 * </p>
	 * 
	 * @param status a gRPC status
	 * 
	 * @return the response trailers metadata
	 */
	GrpcOutboundResponseTrailersMetadata status(GrpcStatus status);
	
	/**
	 * <p>
	 * Sets the gRPC status message.
	 * </p>
	 * 
	 * @param message a message
	 * 
	 * @return the response trailers metadata
	 */
	GrpcOutboundResponseTrailersMetadata statusMessage(String message);
}
