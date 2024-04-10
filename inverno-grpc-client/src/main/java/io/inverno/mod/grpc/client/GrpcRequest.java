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
import io.inverno.mod.grpc.base.GrpcBaseRequest;
import io.inverno.mod.grpc.base.GrpcOutboundRequestMetadata;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a client gRPC request in a client exchange.
 * </p>
 * 
 * <p>
 * Depending on the kind of exchange considered a gRPC request can be {@link GrpcRequest.Unary} when a single message is sent to the endpoint or {@link GrpcRequest.Streaming} when a stream of messages
 * is sent to the endpoint.
 * </p>
 * 
 * <p>
 * Once the request has been sent to the endpoint it is no longer possible to modify it resulting in {@link IllegalStateException} on such operations.
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
	 * Configures the gRPC request metadata to send in the request.
	 * </p>
	 * 
	 * @param metadataConfigurer an outbound request metadata configurer
	 * 
	 * @return the request
	 * 
	 * @throws IllegalStateException if the request has already been sent to the endpoint
	 */
	GrpcRequest<A> metadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) throws IllegalStateException;
	
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

		@Override
		public GrpcRequest.Unary<A> metadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) throws IllegalStateException;

		/**
		 * <p>
		 * Sets the request message.
		 * </p>
		 * 
		 * @param value a request message
		 */
		default void value(A value) {
			this.value(Mono.just(value));
		}
		
		/**
		 * <p>
		 * Sets the request message.
		 * </p>
		 * 
		 * @param value a request message mono
		 */
		void value(Mono<A> value);
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

		@Override
		public GrpcRequest.Streaming<A> metadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) throws IllegalStateException;
		
		/**
		 * <p>
		 * Sets the request message publisher.
		 * </p>
		 * 
		 * @param <T>   the request message type
		 * @param value the message publisher
		 * 
		 * @throws IllegalStateException if messages were already sent to the endpoint
		 */
		<T extends A> void stream(Publisher<T> value) throws IllegalStateException;
	}
}
