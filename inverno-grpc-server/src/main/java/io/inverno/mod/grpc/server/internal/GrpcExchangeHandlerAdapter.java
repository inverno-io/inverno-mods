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
package io.inverno.mod.grpc.server.internal;

import com.google.protobuf.Message;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcExchangeHandler;
import io.inverno.mod.grpc.server.GrpcRequest;
import io.inverno.mod.grpc.server.GrpcResponse;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import java.util.function.BiConsumer;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A {@link GrpcExchangeHandler} to {@link ExchangeHandler} adapter.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
public class GrpcExchangeHandlerAdapter<A extends ExchangeContext, B extends Exchange<A>, C extends Message, D extends Message, E extends GrpcRequest<C>, F extends GrpcResponse<D>, G extends GrpcExchange<A, C, D, E, F>> implements ExchangeHandler<A, B> {

	/**
	 * The gRPC exchange handler.
	 */
	private final GrpcExchangeHandler<A, C, D, E, F, G> grpcExchangeHandler;
	/**
	 * The gRPC exchange factory.
	 */
	private final Function<B, G> exchangeFactory;
	/**
	 * The gRPC error handler.
	 */
	private final BiConsumer<B, Throwable> errorHandler;

	/**
	 * <p>
	 * Creates a gRPC exchange handler adapter.
	 * </p>
	 * 
	 * @param grpcExchangeHandler the gRPC exchange handler to adapt
	 * @param exchangeFactory     the gRPC exchange factory
	 * @param errorHandler        the gRPC error handler
	 */
	public GrpcExchangeHandlerAdapter(GrpcExchangeHandler<A, C, D, E, F, G> grpcExchangeHandler, Function<B, G> exchangeFactory, BiConsumer<B, Throwable> errorHandler) {
		this.grpcExchangeHandler = grpcExchangeHandler;
		this.exchangeFactory = exchangeFactory;
		this.errorHandler = errorHandler;
	}
	
	/**
	 * <p>
	 * Converts the HTTP server exchange in a gRPC server exchange and delegates to the gRPC exchange handler.
	 * </p>
	 * 
	 * <p>
	 * The returned mono doesn't fail as any error is handled by the gRPC error handler.
	 * </p>
	 */
	@Override
	public Mono<Void> defer(B exchange) throws HttpException {
		return this.grpcExchangeHandler.defer(this.exchangeFactory.apply(exchange))
			.doOnError(t -> this.errorHandler.accept(exchange, t))
			.onErrorResume(ign -> Mono.empty());
	}

	/**
	 * <p>
	 * Converts the HTTP server exchange in a gRPC server exchange and delegates to the gRPC exchange handler.
	 * </p>
	 * 
	 * <p>
	 * This method doesn't fail as any error is handled by the gRPC error handler.
	 * </p>
	 */
	@Override
	public void handle(B exchange) throws HttpException {
		try {
			this.grpcExchangeHandler.handle(this.exchangeFactory.apply(exchange));
		}
		catch(Throwable t) {
			this.errorHandler.accept(exchange, t);
		}
	}
}
