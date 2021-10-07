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

import java.util.function.Function;

import reactor.core.publisher.Mono;

/**
 * <p>
 * Represents a failing server exchange.
 * </p>
 * 
 * <p>
 * The HTTP server creates a failing exchange when an exception is thrown during
 * the normal processing of a server {@link Exchange}. They are handled in an
 * {@link ErrorExchangeHandler} that formats the actual response returned to the
 * client.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Exchange
 * 
 * @param <A> the error type
 */
public interface ErrorExchange<A extends Throwable> extends Exchange<ExchangeContext> {

	/**
	 * <p>
	 * Returns the error at the origin of the exchange.
	 * </p>
	 * 
	 * @return a throwable of type A
	 */
	A getError();

	/**
	 * <p>
	 * Returns an error exchange consisting of the result of applying the given
	 * function to the error of the exchange.
	 * </p>
	 * 
	 * @param <T>         the error type of the new exchange
	 * @param errorMapper an error mapper
	 * 
	 * @return a new error exchange
	 */
	default <T extends Throwable> ErrorExchange<T> mapError(Function<? super A, ? extends T> errorMapper) {
		ErrorExchange<A> thisExchange = this;
		return new ErrorExchange<T>() {

			@Override
			public Request request() {
				return thisExchange.request();
			}

			@Override
			public Response response() {
				return thisExchange.response();
			}
			
			@Override
			public ExchangeContext context() {
				return thisExchange.context();
			}
			
			@Override
			public ErrorExchange<T> finalizer(Mono<Void> finalizer) {
				thisExchange.finalizer(finalizer);
				return this;
			}

			@Override
			public T getError() {
				return errorMapper.apply(thisExchange.getError());
			}
			
		};
	}
}
