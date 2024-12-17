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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeHandler;

/**
 * <p>
 * Generic gRPC error exchange handler.
 * </p>
 * 
 * <p>
 * This handlers simply sets the gRPC status and message corresponding to the error in the HTTP response trailers. It is mostly used to handle errors that were raised outside the normal gRPC exchange 
 * processing such as internal errors or errors raised in interceptors, other errors should normally be handled within the gRPC exchange itself.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @see GenericGrpcServer#handleError(io.inverno.mod.http.server.Exchange, java.lang.Throwable) 
 * 
 * @param <A> the exchange context type
 * @param <B> the error exchange type
 */
public class GenericGrpcErrorExchangeHandler<A extends ExchangeContext, B extends ErrorExchange<A>> implements ExchangeHandler<A, B> {

	/**
	 * The gRPC server.
	 */
	private final GenericGrpcServer grpcServer;

	/**
	 * <p>
	 * Creates a generic gRPC error exchange handler.
	 * </p>
	 * 
	 * @param grpcServer the gRPC server
	 */
	public GenericGrpcErrorExchangeHandler(GenericGrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	/**
	 * <p>
	 * Delegates to {@link GenericGrpcServer#handleError(io.inverno.mod.http.server.Exchange, java.lang.Throwable) }
	 * </p>
	 */
	@Override
	public void handle(B exchange) throws HttpException {
		this.grpcServer.handleError(exchange, exchange.getError());
	}
}
