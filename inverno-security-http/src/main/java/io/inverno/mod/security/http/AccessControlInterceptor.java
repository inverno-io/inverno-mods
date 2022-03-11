package io.inverno.mod.security.http;

import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import reactor.core.publisher.Mono;

public class AccessControlInterceptor<A extends Exchange<InterceptingSecurityContext>> implements ExchangeInterceptor<InterceptingSecurityContext, A> {

	@Override
	public Mono<? extends A> intercept(A exchange) {
		boolean auth = exchange.context().isAuthenticated();
		if(!exchange.context().isAuthenticated()) {
			throw new UnauthorizedException();
		}
		return Mono.just(exchange);
	}
}
