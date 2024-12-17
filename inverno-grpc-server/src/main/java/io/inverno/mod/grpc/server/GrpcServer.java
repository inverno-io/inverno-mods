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
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ServerController;

/**
 * <p>
 * A gRPC server is used to adapt unary, client streaming, server streaming or bidirectional streaming server {@link GrpcExchangeHandler} into HTTP server {@link ExchangeHandler}.
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
 * The {@code GrpcServer} defines four methods for adapting above RPC methods into an {@link ExchangeHandler} that can then be used as HTTP server handler in a {@link ServerController} or as a Web
 * route handler. The resulting exchange handler basically converts the HTTP server exchange into a gRPC exchange corresponding to above RPC methods before delegating the processing to the wrapped
 * gRPC exchange handler.
 * </p>
 * 
 * <p>
 * The handler provided by {@link #errorHandler() } can also be used to handle global errors in an HTTP server or a Web server by setting the gRPC error status and message corresponding to the error
 * into the response.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public interface GrpcServer {
	
	/**
	 * <p>
	 * Adapts the specified unary gRPC exchange handler in an HTTP exchange handler.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the server exchange type
	 * @param <C>                     the request type
	 * @param <D>                     the response type
	 * @param <E>                     the gRPC unary exchange type
	 * @param defaultRequestInstance  the default instance of the request
	 * @param defaultResponseInstance the default instance of the response
	 * @param grpcExchangeHandler     the gRPC exchange handler
	 *
	 * @return an HTTP exchange handler
	 */
	<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.Unary<A, C, D>> ExchangeHandler<A, B> unary(C defaultRequestInstance, D defaultResponseInstance, GrpcExchangeHandler<A, C, D, GrpcRequest.Unary<C>, GrpcResponse.Unary<D>, E> grpcExchangeHandler);
	
	/**
	 * <p>
	 * Adapts the specified client streaming gRPC exchange handler in an HTTP exchange handler.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the server exchange type
	 * @param <C>                     the request type
	 * @param <D>                     the response type
	 * @param <E>                     the gRPC unary exchange type
	 * @param defaultRequestInstance  the default instance of the request
	 * @param defaultResponseInstance the default instance of the response
	 * @param grpcExchangeHandler     the gRPC exchange handler
	 *
	 * @return an HTTP exchange handler
	 */
	<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.ClientStreaming<A, C, D>> ExchangeHandler<A, B> clientStreaming(C defaultRequestInstance, D defaultResponseInstance, GrpcExchangeHandler<A, C, D, GrpcRequest.Streaming<C>, GrpcResponse.Unary<D>, E> grpcExchangeHandler);
	
	/**
	 * <p>
	 * Adapts the specified server streaming gRPC exchange handler in an HTTP exchange handler.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the server exchange type
	 * @param <C>                     the request type
	 * @param <D>                     the response type
	 * @param <E>                     the gRPC unary exchange type
	 * @param defaultRequestInstance  the default instance of the request
	 * @param defaultResponseInstance the default instance of the response
	 * @param grpcExchangeHandler     the gRPC exchange handler
	 *
	 * @return an HTTP exchange handler
	 */
	<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.ServerStreaming<A, C, D>> ExchangeHandler<A, B> serverStreaming(C defaultRequestInstance, D defaultResponseInstance, GrpcExchangeHandler<A, C, D, GrpcRequest.Unary<C>, GrpcResponse.Streaming<D>, E> grpcExchangeHandler);
	
	/**
	 * <p>
	 * Adapts the specified bidirectional streaming gRPC exchange handler in an HTTP exchange handler.
	 * </p>
	 *
	 * @param <A>                     the exchange context type
	 * @param <B>                     the server exchange type
	 * @param <C>                     the request type
	 * @param <D>                     the response type
	 * @param <E>                     the gRPC unary exchange type
	 * @param defaultRequestInstance  the default instance of the request
	 * @param defaultResponseInstance the default instance of the response
	 * @param grpcExchangeHandler     the gRPC exchange handler
	 *
	 * @return an HTTP exchange handler
	 */
	<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcExchange.BidirectionalStreaming<A, C, D>> ExchangeHandler<A, B> bidirectionalStreaming(C defaultRequestInstance, D defaultResponseInstance, GrpcExchangeHandler<A, C, D, GrpcRequest.Streaming<C>, GrpcResponse.Streaming<D>, E> grpcExchangeHandler);
	
	/**
	 * <p>
	 * A global error handler that sets gRPC error status and message.
	 * </p>
	 *
	 * <p>
	 * This handler is only useful for handling errors that happened outside the normal processing of the gRPC exchange, this is especially the case of internal errors or errors raised in interceptors
	 * (e.g. authentication errors, permissions errors...)
	 * </p>
	 *
	 * @param <A> the exchange context type
	 * @param <B> the error exchange type
	 *
	 * @return an error exchange handler
	 */
	<A extends ExchangeContext, B extends ErrorExchange<A>> ExchangeHandler<A, B> errorHandler();
}
