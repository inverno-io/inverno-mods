/*
 * Copyright 2024 Jeremy KUHN
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
import io.inverno.mod.grpc.base.GrpcOutboundRequestMetadata;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Exchange;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A gRPC client is used to transform client HTTP {@link Exchange} into unary, client streaming, server streaming or bidirectional streaming client {@link GrpcExchange}.
 * </p>
 * 
 * <p>
 * gRPC runs on top of HTTP/2 protocol, it defines four kinds of service method as defined by <a href="https://grpc.io/docs/what-is-grpc/core-concepts/">gRPC core concepts</a>:
 * </p>
 * 
 * <ul>
 * <li><b>Unary RPCs</b>: for request/response exchanges.</li>
 * <li><b>Client streaming RPCs</b>: for stream/response exchanges.</li>
 * <li><b>Server streaming RPCs</b>: for request/stream exchanges.</li>
 * <li><b>Bidirectional streaming RPCs</b>: for stream/stream exchanges.</li>
 * </ul>
 * 
 * <p>
 * The {@code GrpcClient} defines four methods for converting a base HTTP/2 client {@link Exchange} into specific client {@link GrpcExchange} corresponding to above RPC methods.
 * </p>
 * 
 * <p>
 * The {@link GrpcClient.Stub} interface is an helper class used for generating gRPC client stub from Protocol buffer definitions.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcClient {

	/**
	 * <p>
	 * Converts an client HTTP exchange in a unary client gRPC exchange.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the client exchange type
	 * @param <C>                     the request type
	 * @param <D>                     the response type
	 * @param <E>                     the gRPC unary exchange type
	 * @param exchange                the client exchange
	 * @param serviceName             the gRPC service name
	 * @param methodName              the gRPC service method name
	 * @param defaultRequestInstance  the default request instance
	 * @param defaultResponseInstance the default response instance
	 *
	 * @return a unary client gRPC exchange
	 */
	<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.Unary<A, C, D>> E unary(B exchange, GrpcServiceName serviceName, String methodName, C defaultRequestInstance, D defaultResponseInstance);
	
	/**
	 * <p>
	 * Converts an client HTTP exchange in a client streaming client gRPC exchange.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the client exchange type
	 * @param <C>                     the request type
	 * @param <D>                     the response type
	 * @param <E>                     the gRPC unary exchange type
	 * @param exchange                the client exchange
	 * @param serviceName             the gRPC service name
	 * @param methodName              the gRPC service method name
	 * @param defaultRequestInstance  the default request instance
	 * @param defaultResponseInstance the default response instance
	 *
	 * @return a client streaming client gRPC exchange
	 */
	<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.ClientStreaming<A, C, D>> E clientStreaming(B exchange, GrpcServiceName serviceName, String methodName, C defaultRequestInstance, D defaultResponseInstance);
	
	/**
	 * <p>
	 * Converts an client HTTP exchange in a server streaming client gRPC exchange.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the client exchange type
	 * @param <C>                     the request type
	 * @param <D>                     the response type
	 * @param <E>                     the gRPC unary exchange type
	 * @param exchange                the client exchange
	 * @param serviceName             the gRPC service name
	 * @param methodName              the gRPC service method name
	 * @param defaultRequestInstance  the default request instance
	 * @param defaultResponseInstance the default response instance
	 *
	 * @return a server streaming client gRPC exchange
	 */
	<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.ServerStreaming<A, C, D>> E serverStreaming(B exchange, GrpcServiceName serviceName, String methodName, C defaultRequestInstance, D defaultResponseInstance);
	
	/**
	 * <p>
	 * Converts an client HTTP exchange in a bidirectional streaming client gRPC exchange.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the client exchange type
	 * @param <C>                     the request type
	 * @param <D>                     the response type
	 * @param <E>                     the gRPC unary exchange type
	 * @param exchange                the client exchange
	 * @param serviceName             the gRPC service name
	 * @param methodName              the gRPC service method name
	 * @param defaultRequestInstance  the default request instance
	 * @param defaultResponseInstance the default response instance
	 *
	 * @return a bidirectional streaming client gRPC exchange
	 */
	<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.BidirectionalStreaming<A, C, D>> E bidirectionalStreaming(B exchange, GrpcServiceName serviceName, String methodName, C defaultRequestInstance, D defaultResponseInstance);
	
	/**
	 * <p>
	 * A base gRPC client stub definition.
	 * </p>
	 * 
	 * <p>
	 * This is intended to be used when generating gRPC client stub from Protocol buffer definitions.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> the exchange context type
	 * @param <B> the stub type
	 */
	interface Stub<A extends ExchangeContext, B extends GrpcClient.Stub<A, B>> {

		/**
		 * <p>
		 * Returns a new stub configured with the specified metadata.
		 * </p>
		 * 
		 * @param metadataConfigurer a gRPC request metadata configurer
		 * 
		 * @return a configured stub
		 */
		B withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer);
	}

	/**
	 * <p>
	 * A closeable gRPC client stub definition.
	 * </p>
	 *
	 * <p>
	 * This is intended to be used when generating gRPC client stub from Protocol buffer definitions.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 * @param <B> the stub type
	 */
	interface CloseableStub<A extends ExchangeContext, B extends GrpcClient.Stub<A, B>> extends Stub<A, B>, AutoCloseable {

		/**
		 * <p>
		 * Subscribes to {@link #shutdownGracefully() } in order to eventually close the underlying endpoint.
		 * </p>
		 *
		 * <p>
		 * This method should return right away before the endpoint is actually closed, you should prefer {@link #shutdown()} or {@link #shutdownGracefully()} to control when the endpoint is actually
		 * closed.
		 * </p>
		 */
		@Override
		default void close() {
			this.shutdownGracefully().subscribe();
		}

		/**
		 * <p>
		 * Shutdowns the underlying endpoint right away.
		 * </p>
		 *
		 * @return a mono which completes once the underlying endpoint is shutdown
		 */
		Mono<Void> shutdown();

		/**
		 * <p>
		 * Gracefully shutdown the underlying endpoint.
		 * </p>
		 *
		 * @return a mono which completes once the underlying endpoint is shutdown
		 */
		Mono<Void> shutdownGracefully();
	}
}
