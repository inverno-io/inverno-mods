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

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface LoginFailureHandler<A extends ExchangeContext, B extends Exchange<A>> {

	Mono<Void> handleLoginFailure(B exchange, io.inverno.mod.security.SecurityException error);
	
	default LoginFailureHandler<A, B> andThen(LoginFailureHandler<? super A, ? super B> after) {
		return (exchange, authentication) -> {
			return this.handleLoginFailure(exchange, authentication).then(after.handleLoginFailure(exchange, authentication));
		};
	}
	
	default LoginFailureHandler<A, B> compose(LoginFailureHandler<? super A, ? super B> before) {
		return (exchange, authentication) -> {
			return before.handleLoginFailure(exchange, authentication).then(this.handleLoginFailure(exchange, authentication));
		};
	}
}
