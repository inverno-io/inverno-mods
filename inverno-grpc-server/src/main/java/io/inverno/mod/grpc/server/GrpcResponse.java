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
import io.inverno.mod.grpc.base.GrpcBaseResponse;
import io.inverno.mod.grpc.base.GrpcOutboundResponseMetadata;
import java.util.function.Consumer;
import io.inverno.mod.grpc.base.GrpcOutboundResponseTrailersMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a server gRPC response in a server exchange.
 * </p>
 * 
 * <p>
 * Depending on the kind of exchange considered a gRPC response can be {@link GrpcResponse.Unary} when a single message is sent to the client or {@link GrpcResponse.Streaming} when a stream of
 * messages is sent to the client.
 * </p>
 * 
 * <p>
 * Once the response has been sent to the client it is no longer possible to modify it resulting in {@link IllegalStateException} on such operations.
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
	 * Configures the gRPC response metadata to send in the response.
	 * </p>
	 * 
	 * @param metadataConfigurer an outbound response metadata configurer
	 * 
	 * @return the response
	 * 
	 * @throws IllegalStateException if the response has already been sent to the client
	 */
	GrpcResponse<A> metadata(Consumer<GrpcOutboundResponseMetadata> metadataConfigurer) throws IllegalStateException;
	
	/**
	 * <p>
	 * Configures the gRPC response trailers metadata to send in the response.
	 * </p>
	 * 
	 * @param metadataConfigurer an outbound response trailers metadata configurer
	 * 
	 * @return the response
	 * 
	 * @throws IllegalStateException if the response has already been sent to the endpoint
	 */
	GrpcResponse<A> trailersMetadata(Consumer<GrpcOutboundResponseTrailersMetadata> metadataConfigurer) throws IllegalStateException;
	
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

		@Override
		public GrpcResponse.Unary<A> metadata(Consumer<GrpcOutboundResponseMetadata> metadataConfigurer) throws IllegalStateException;

		@Override
		public GrpcResponse.Unary<A> trailersMetadata(Consumer<GrpcOutboundResponseTrailersMetadata> metadataConfigurer) throws IllegalStateException;
		
		/**
		 * <p>
		 * Sets the response message.
		 * </p>
		 * 
		 * @param value a response message
		 */
		default void value(A value) {
			this.value(Mono.just(value));
		}
		
		/**
		 * <p>
		 * Sets the response message.
		 * </p>
		 * 
		 * @param value a response message mono
		 */
		void value(Mono<A> value);
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
		
		@Override
		public GrpcResponse.Streaming<A> metadata(Consumer<GrpcOutboundResponseMetadata> metadataConfigurer) throws IllegalStateException;

		@Override
		public GrpcResponse.Streaming<A> trailersMetadata(Consumer<GrpcOutboundResponseTrailersMetadata> metadataConfigurer) throws IllegalStateException;

		/**
		 * <p>
		 * Sets the response message publisher.
		 * </p>
		 * 
		 * @param <T>   the response message type
		 * @param value the message publisher
		 * 
		 * @throws IllegalStateException if messages were already sent to the client
		 */
		<T extends A> void stream(Publisher<T> value) throws IllegalStateException;
	}
}
