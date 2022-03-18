/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
 * @author jkuhn
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
