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

import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.http.AuthenticationErrorInterceptor;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class FormAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends AuthenticationErrorInterceptor<A, B> {

	public static final String DEFAULT_LOGIN_PAGE_URI = "/login";
	
	private final String loginUri;
	
	public FormAuthenticationErrorInterceptor() {
		this(DEFAULT_LOGIN_PAGE_URI);
	}

	public FormAuthenticationErrorInterceptor(String loginUri) {
		super(true);
		this.loginUri = loginUri;
	}

	public String getLoginUri() {
		return loginUri;
	}

	@Override
	public Mono<? extends B> intercept(B exchange) {
		if(exchange.request().getMethod().equals(Method.GET)) {
			return super.intercept(exchange);
		}
		return Mono.just(exchange);
	}

	@Override
	protected void interceptUnauthorized(B exchange) throws HttpException {
		exchange.response()
			.headers(headers -> headers
				.status(Status.FOUND)
				.set(Headers.NAME_LOCATION, URIs.uri(this.loginUri).queryParameter(FormLoginPageHandler.PARAMETER_REDIRECT_URI, exchange.request().getPath()).buildString())
			);
	}
}
