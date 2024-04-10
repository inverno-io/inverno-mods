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
package io.inverno.mod.grpc.server;

import com.google.protobuf.Message;
import io.inverno.mod.grpc.base.GrpcBaseRequest;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a server gRPC request in a server exchange.
 * </p>
 * 
 * <p>
 * Depending on the kind of exchange considered a gRPC request can be {@link GrpcRequest.Unary} when a single message is received from the client or {@link GrpcRequest.Streaming} when a stream of
 * messages is received from the client.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @param <A> the request message type
 */
public interface GrpcRequest<A extends Message> extends GrpcBaseRequest {
	
	/**
	 * <p>
	 * Represents a unary (single message) request.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> The request message type
	 */
	interface Unary<A extends Message> extends GrpcRequest<A> {

		/**
		 * <p>
		 * Returns the request message.
		 * </p>
		 * 
		 * @return the mono emitting the request message
		 */
		Mono<A> value();
	}
	
	/**
	 * <p>
	 * Represents a streaming (stream of messages) request.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> The request message type
	 */
	interface Streaming<A extends Message> extends GrpcRequest<A> {

		/**
		 * <p>
		 * Returns the request message publisher.
		 * </p>
		 * 
		 * @return the request message publisher
		 */
		Publisher<A> stream();
	}
}
