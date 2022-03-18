package io.inverno.mod.security.http;

import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.security.Authentication;
import reactor.core.publisher.Mono;

public class AccessControlInterceptor<A extends Exchange<InterceptingSecurityContext>> implements ExchangeInterceptor<InterceptingSecurityContext, A> {

	@Override
	public Mono<? extends A> intercept(A exchange) {
		Authentication authentication = exchange.context().getAuthentication();
		if(!authentication.isAuthenticated()) {
			throw authentication.getCause()
				.map(UnauthorizedException::new)
				.orElse(new UnauthorizedException());
		}
		return Mono.just(exchange);
	}
}
