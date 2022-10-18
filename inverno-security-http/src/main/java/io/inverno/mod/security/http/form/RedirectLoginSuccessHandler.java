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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.login.LoginActionHandler;
import io.inverno.mod.security.http.login.LoginSuccessHandler;
import java.util.Optional;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A login success handler implementation that redirects the client (302) after a successful login authentication.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see LoginActionHandler
 * 
 * @param <A> the authentication type
 * @param <B> the context type
 * @param <C> the exchange type
 */
public class RedirectLoginSuccessHandler<A extends Authentication, B extends ExchangeContext, C extends Exchange<B>> implements LoginSuccessHandler<A, B, C> {

	/**
	 * The default login success URI: {@code /}.
	 */
	public static final String DEFAULT_LOGIN_SUCCESS_URI = "/";
	
	/**
	 * The URI where the client is redirected after a successful login authentication.
	 */
	private final String loginSuccessUri;

	/**
	 * <p>
	 * Creates a redirect login success handler which redirects the client to the default login success URI.
	 * </p>
	 */
	public RedirectLoginSuccessHandler() {
		this(DEFAULT_LOGIN_SUCCESS_URI);
	}
	
	/**
	 * <p>
	 * Creates a redirect login success handler which redirects the client to the specified login success URI.
	 * </p>
	 * 
	 * @param loginSuccessUri the URI where to redirect the client after a successful login authentication
	 */
	public RedirectLoginSuccessHandler(String loginSuccessUri) {
		this.loginSuccessUri = loginSuccessUri;
	}

	/**
	 * <p>
	 * Returns the URI where the client is redirected after a successful login authentication.
	 * </p>
	 * 
	 * @return the login success URI
	 */
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
