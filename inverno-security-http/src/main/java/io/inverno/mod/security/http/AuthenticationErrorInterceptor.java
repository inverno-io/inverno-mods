/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.security.http;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base implemenation for authentication error interceptors.
 * </p>
 * 
 * <p>
 * An authentication error interceptor intercepts {@code UNAUTHORIZED(401)} errors in order to provide authentication instructions to the requester in the error response. Such interceptor is typically
 * used to initiate the authentication process with the requester.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the context type
 * @param <B> the error echange type
 */
public abstract class AuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> implements ExchangeInterceptor<A, B> {

	/**
	 * Flag indicating whether the interceptor should claim the exchange (i.e. terminates the exchange by returning an empty response).
	 */
	protected final boolean terminal;

	/**
	 * <p>
	 * Creates a non-terminating authentication error interceptor.
	 * </p>
	 */
	protected AuthenticationErrorInterceptor() {
		this(false);
	}
	
	/**
	 * <p>
	 * Creates an authentication error interceptor.
	 * </p>
	 * 
	 * <p>
	 * If terminal, the interceptor claims the exchange and return an empty response resulting in no further processing.
	 * </p>
	 * 
	 * @param terminal true to terminate the exchange, false otherwise
	 */
	protected AuthenticationErrorInterceptor(boolean terminal) {
		this.terminal = terminal;
	}
	
	@Override
	public Mono<? extends B> intercept(B exchange) {
		if(this.terminal) {
			return Mono.fromRunnable(() -> {
				Throwable error = exchange.getError();
				if(error instanceof HttpException && ((HttpException)error).getStatusCode() == Status.UNAUTHORIZED.getCode()) {
					this.interceptUnauthorized(exchange);
					exchange.response().body().empty();
				}
			});
		}
		else {
			return Mono.fromSupplier(() -> {
				Throwable error = exchange.getError();
				if(error instanceof HttpException && ((HttpException)error).getStatusCode() == Status.UNAUTHORIZED.getCode()) {
					this.interceptUnauthorized(exchange);
				}
				return exchange;
			});
		}
	}
	
	/**
	 * <p>
	 * Intercepts an unauthorized exchange.
	 * </p>
	 * 
	 * @param exchange the unauthorized exchange to intercept
	 * 
	 * @throws HttpException if there was an error intercepting the exchange
	 */
	protected abstract void interceptUnauthorized(B exchange) throws HttpException;
}
