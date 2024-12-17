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
import io.inverno.mod.grpc.base.GrpcBaseExchange;
import io.inverno.mod.grpc.base.GrpcStatus;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;

/**
 * <p>
 * Represents a gRPC server exchange between a client and a server.
 * </p>
 * 
 * <p>
 * A server gRPC exchange is created in a specific {@link ExchangeHandler} provided by the {@link GrpcServer} which basically converts an HTTP server exchange into one of
 * {@link GrpcExchange.Unary}, {@link GrpcExchange.ClientStreaming}, {@link GrpcExchange.ServerStreaming} or {@link GrpcExchange.BidirectionalStreaming} exchange which reflect the four kinds of
 * service method defined by <a href="https://grpc.io/docs/what-is-grpc/core-concepts/">gRPC core concepts</a>. The exchange handler then delegates the processing of the exchange to the
 * {@link GrpcExchangeHandler} which was initialy provided to the {@link GrpcServer}.
 * </p>
 * 
 * <p>
 * The server exchange terminates when the response message publisher terminates, gRPC metadata are then sent to the client with final {@link GrpcStatus}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @param <A> The exchange context type 
 * @param <B> The request message type
 * @param <C> The response message type
 * @param <D> the request type
 * @param <E> the response type
 */
public interface GrpcExchange<A extends ExchangeContext, B extends Message, C extends Message, D extends GrpcRequest<B>, E extends GrpcResponse<C>> extends GrpcBaseExchange<A, D, E> {

	/**
	 * <p>
	 * Represents a unary (request/response) server gRPC exchange.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> The exchange context type 
	 * @param <B> The request message type
	 * @param <C> The response message type
	 */
	interface Unary<A extends ExchangeContext, B extends Message, C extends Message> extends GrpcExchange<A, B, C, GrpcRequest.Unary<B>, GrpcResponse.Unary<C>> {
		
	}
	
	/**
	 * <p>
	 * Represents a client streaming (stream/response) server gRPC exchange.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> The exchange context type 
	 * @param <B> The request message type
	 * @param <C> The response message type
	 */
	interface ClientStreaming<A extends ExchangeContext, B extends Message, C extends Message> extends GrpcExchange<A, B, C, GrpcRequest.Streaming<B>, GrpcResponse.Unary<C>> {
		
	}
	
	/**
	 * <p>
	 * Represents a server streaming (request/stream) server gRPC exchange.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> The exchange context type 
	 * @param <B> The request message type
	 * @param <C> The response message type
	 */
	interface ServerStreaming<A extends ExchangeContext, B extends Message, C extends Message> extends GrpcExchange<A, B, C, GrpcRequest.Unary<B>, GrpcResponse.Streaming<C>> {
		
	}
	
	/**
	 * <p>
	 * Represents a bidirectional streaming (stream/stream) server gRPC exchange.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 * 
	 * @param <A> The exchange context type 
	 * @param <B> The request message type
	 * @param <C> The response message type
	 */
	interface BidirectionalStreaming<A extends ExchangeContext, B extends Message, C extends Message> extends GrpcExchange<A, B, C, GrpcRequest.Streaming<B>, GrpcResponse.Streaming<C>> {
		
	}
}
