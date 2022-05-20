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
import io.inverno.mod.security.authentication.Authentication;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@FunctionalInterface
public interface LoginSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> {

	Mono<Void> handleLoginSuccess(C exchange, A authentication);

	default LoginSuccessHandler<A, B, C> andThen(LoginSuccessHandler<? super A, ? super B, ?super C> after) {
		return (exchange, authentication) -> {
			return this.handleLoginSuccess(exchange, authentication).then(after.handleLoginSuccess(exchange, authentication));
		};
	}
	
	default LoginSuccessHandler<A, B, C> compose(LoginSuccessHandler<? super A, ? super B, ?super C> before) {
		return (exchange, authentication) -> {
			return before.handleLoginSuccess(exchange, authentication).then(this.handleLoginSuccess(exchange, authentication));
		};
	}
}
