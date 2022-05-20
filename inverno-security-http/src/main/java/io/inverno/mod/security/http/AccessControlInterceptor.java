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

import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Simple Access control interceptor that verifies that the exchange is authenticated.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
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
