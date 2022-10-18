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
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.security.http.AuthenticationErrorInterceptor;
import io.inverno.mod.security.http.login.LoginActionHandler;
import io.inverno.mod.security.http.login.LogoutActionHandler;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An authentication error interceptor that redirects (302) the client to a login page.
 * </p>
 * 
 * <p>
 * This interceptor is usually used in conjunction with a {@link LoginActionHandler} and a {@link LogoutActionHandler}. The login action is targeted by the login page in order to authenticate the
 * login credentials provided by the user and the logout action allows to free resources and invalidate any temporary credentials resulting from the login process and communicated to the authenticated
 * user.
 * </p>
 *
 * <p>
 * It is important to understand the difference between login and authentication: whereas authentication is involved in the login process to authenticate login credentials with the aim of signing in a
 * user in an application, login is usually not involved during authentication which consists in validating credentials with the aim of granting access to a protected resource.
 * </p>
 * 
 * <p>
 * More specifically, a successful login usually results in temporary credentials (e.g. a token) being created and communicated to the authenticated user which can reuse them to access protected
 * services or resources in further requests. The login process is then performed once whereas the authentication process is performed on all requests.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the context type
 * @param <B> the error echange type
 */
public class FormAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends AuthenticationErrorInterceptor<A, B> {

	/**
	 * The default login page URI: {@code /login}.
	 */
	public static final String DEFAULT_LOGIN_PAGE_URI = "/login";
	
	/**
	 * The login page URI where to redirect the client.
	 */
	private final String loginUri;
	
	/**
	 * <p>
	 * Creates a form authentication error interceptor that redirects the client to the default login page URI.
	 * </p>
	 */
	public FormAuthenticationErrorInterceptor() {
		this(DEFAULT_LOGIN_PAGE_URI);
	}

	/**
	 * <p>
	 * Creates a form authentication error interceptor that redirects the client to the specified login page URI.
	 * </p>
	 * 
	 * @param loginUri the login page URI
	 */
	public FormAuthenticationErrorInterceptor(String loginUri) {
		super(true);
		this.loginUri = loginUri;
	}

	/**
	 * <p>
	 * Returns the login page URI.
	 * </p>
	 * 
	 * @return the login page URI
	 */
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
