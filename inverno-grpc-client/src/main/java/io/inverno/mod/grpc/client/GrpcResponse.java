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
package io.inverno.mod.grpc.client;

import com.google.protobuf.Message;
import io.inverno.mod.grpc.base.GrpcBaseResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a client gRPC response in a client exchange.
 * </p>
 * 
 * <p>
 * Depending on the kind of exchange considered a gRPC response can be {@link GrpcResponse.Unary} when a single message is received from the endpoint or {@link GrpcResponse.Streaming} when a stream of
 * messages is received from the endpoint.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @param <A> the response message type
 */
public interface GrpcResponse<A extends Message> extends GrpcBaseResponse {

	/**
	 * <p>
	 * Represents a unary (single message) response.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> The response message type
	 */
	interface Unary<A extends Message> extends GrpcResponse<A> {

		/**
		 * <p>
		 * Returns the response message.
		 * </p>
		 * 
		 * @return the mono emitting the response message
		 */
		Mono<A> value();
	}
	
	/**
	 * <p>
	 * Represents a streaming (stream of messages) response.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> The response message type
	 */
	interface Streaming<A extends Message> extends GrpcResponse<A> {

		/**
		 * <p>
		 * Returns the response message publisher.
		 * </p>
		 * 
		 * @return the response message publisher
		 */
		Publisher<A> stream();
	}
}
