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
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public abstract class AuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> implements ExchangeInterceptor<A, B> {

	protected final boolean terminal;

	protected AuthenticationErrorInterceptor() {
		this(false);
	}
	
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
	
	protected abstract void interceptUnauthorized(B exchange) throws HttpException;
}
