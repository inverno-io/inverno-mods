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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;

/**
 * <p>
 * An authentication error interceptor that implements <a href="https://datatracker.ietf.org/doc/html/rfc7235">RFC 7235 HTTP authentication</a>.
 * </p>
 * 
 * <p>
 * As per RFC 7235, this interceptor sends a challenge to the requester in the {@code www-authenticate} HTTP header on an {@code UNAUTHORIZED(401)} error.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the context type
 * @param <B> the error exchange type
 */
public abstract class HttpAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends AuthenticationErrorInterceptor<A, B> {

	@Override
	protected void interceptUnauthorized(B exchange) throws HttpException {
		final String challenge;
		if(exchange.getError().getCause() != null && exchange.getError().getCause() instanceof io.inverno.mod.security.SecurityException) {
			challenge = this.createChallenge((io.inverno.mod.security.SecurityException)exchange.getError().getCause());
		}
		else {
			challenge = this.createChallenge(null);
		}
		
		exchange.response()
			.headers(headers -> headers
				.status(Status.UNAUTHORIZED)
				.add(Headers.NAME_WWW_AUTHENTICATE, challenge)
			)
			.body().empty();
	}
	
	/**
	 * <p>
	 * Creates the challenge to send to the requester.
	 * </p>
	 * 
	 * @param cause the cause of the error (can be null)
	 * 
	 * @return a challenge
	 */
	protected abstract String createChallenge(io.inverno.mod.security.SecurityException cause);
	
}
