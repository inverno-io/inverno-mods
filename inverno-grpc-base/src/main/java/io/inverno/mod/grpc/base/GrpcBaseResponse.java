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
 * Base gRPC response for representing client or server responses.
 * </p>
 * 
 * <p>
 * It exposes gRPC response content as defined by <a href="https://datatracker.ietf.org/doc/html/draft-kumar-rtgwg-grpc-protocol-00">gRPC protocol</a>.
 * </p>
 * 
 * <p>
 * Considering a client exchange, where the response is received by the client from the server, implementation shall provide methods to access gRPC response content. Considering a server exchange,
 * where the response is provided and sent from the server to the client, implementation shall provide methods to set gRPC response content.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcBaseResponse {

	/**
	 * <p>
	 * Returns gRPC response metadata.
	 * </p>
	 * 
	 * @return the response metadata
	 */
	GrpcInboundResponseMetadata metadata();
	
	/**
	 * <p>
	 * Returns gRPC response trailers metadata.
	 * </p>
	 * 
	 * <p>
	 * In a gRPC exchange the final gRPC status shall be provided in the trailers.
	 * </p>
	 * 
	 * @return the response trailers metadata
	 */
	GrpcInboundResponseTrailersMetadata trailersMetadata();
}
