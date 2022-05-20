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
package io.inverno.mod.security.http.form;

import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.LoginSuccessHandler;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class RedirectLoginSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> implements LoginSuccessHandler<A, B, C> {

	public static final String DEFAULT_LOGIN_SUCCESS_URI = "/";
	
	private final String loginSuccessUri;

	public RedirectLoginSuccessHandler() {
		this(DEFAULT_LOGIN_SUCCESS_URI);
	}
	
	public RedirectLoginSuccessHandler(String loginSuccessUri) {
		this.loginSuccessUri = loginSuccessUri;
	}

	public String getLoginSuccessUri() {
		return loginSuccessUri;
	}

	@Override
	public Mono<Void> handleLoginSuccess(C exchange, A authentication) {
		return exchange.request().body().get().urlEncoded().collectMap()
			.doOnNext(parameterMap -> exchange.response()
				.headers(headers -> headers
					.status(Status.FOUND)
					.set(Headers.NAME_LOCATION, Optional.ofNullable(parameterMap.get(FormLoginPageHandler.PARAMETER_REDIRECT_URI)).map(Parameter::asString).orElse(this.loginSuccessUri))
				)
				.body().empty()
			)
			.then();
	}
}
