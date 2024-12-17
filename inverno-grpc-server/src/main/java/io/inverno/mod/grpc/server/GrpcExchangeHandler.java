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
import io.inverno.mod.grpc.base.GrpcException;
import io.inverno.mod.http.base.ExchangeContext;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A gRPC exchange handler is used to handle gRPC server exchanges.
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
 * @param <F> the gRPC exchange type
 */
@FunctionalInterface
public interface GrpcExchangeHandler<A extends ExchangeContext, B extends Message, C extends Message, D extends GrpcRequest<B>, E extends GrpcResponse<C>, F extends GrpcExchange<A, B, C, D, E>> {

	/**
	 * <p>
	 * Returns a Mono that defers the processing of the exchange.
	 * </p>
	 * 
	 * <p>
	 * By default, returns a Mono that defers the execution of {@link #handle(io.inverno.mod.grpc.server.GrpcExchange) }.
	 * </p>
	 * 
	 * @param exchange the gRPC exchange to process
	 * 
	 * @return an empty mono that completes when the exchange has been processed
	 * 
	 * @throws GrpcException if an error occurs during the processing of the exchange
	 */
	default Mono<Void> defer(F exchange) throws GrpcException {
		return Mono.fromRunnable(() -> this.handle(exchange));
	}
	
	/**
	 * <p>
	 * Processes the specified server exchange.
	 * </p>
	 *
	 * <p>
	 * This method is more convenient than {@link #defer(io.inverno.mod.grpc.server.GrpcExchange) } when the handling logic does not need to be reactive.
	 * </p>
	 * 
	 * @param exchange the gRPC exchange to process
	 * 
	 * @throws GrpcException if an error occurs during the processing of the exchange
	 */
	void handle(F exchange) throws GrpcException;
}
