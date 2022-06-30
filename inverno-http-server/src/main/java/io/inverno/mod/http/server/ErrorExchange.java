/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.server;

import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import java.util.Optional;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a failing server exchange.
 * </p>
 *
 * <p>
 * The HTTP server creates a failing exchange when an exception is thrown during the normal processing of a server {@link Exchange}. They are handled
 * in an {@link ExchangeHandler} that formats the actual response returned to the client.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Exchange
 *
 * @param <A> the type of the exchange context
 */
public interface ErrorExchange<A extends ExchangeContext> extends Exchange<A> {

	/**
	 * <p>
	 * Returns the error at the origin of the exchange.
	 * </p>
	 * 
	 * @return a throwable of type A
	 */
	Throwable getError();

	/**
	 * <p>
	 * Returns an empty optional since an error exchange does not support WebSocket upgrade.
	 * </p>
	 * 
	 * @return an empty optional
	 */
	@Override
	default Optional<? extends WebSocket<A, ? extends WebSocketExchange<A>>> webSocket(String... subProtocols) {
		return Optional.empty();
	}
	
	/**
	 * <p>
	 * Returns an error exchange consisting of the result of applying the given function to the error of the exchange.
	 * </p>
	 *
	 * @param errorMapper an error mapper
	 *
	 * @return a new error exchange
	 */
	default ErrorExchange<A> mapError(Function<? super Throwable, ? extends Throwable> errorMapper) {
		ErrorExchange<A> thisExchange = this;
		return new ErrorExchange<A>() {

			@Override
			public Request request() {
				return thisExchange.request();
			}

			@Override
			public Response response() {
				return thisExchange.response();
			}
			
			@Override
			public A context() {
				return thisExchange.context();
			}
			
			@Override
			public ErrorExchange<A> finalizer(Mono<Void> finalizer) {
				thisExchange.finalizer(finalizer);
				return this;
			}

			@Override
			public Throwable getError() {
				return errorMapper.apply(thisExchange.getError());
			}
			
		};
	}
}
