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

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An exhange handler that serves a whitelabel login HTML page.
 * </p>
 *
 * <p>
 * The resulting HTML page contains a login form with {@code username} and {@code password} parameters that can be used by a user to submit login credentials and enter an application.
 * </p>
 * 
 * <p>
 * Providing a custom login page is recommended.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
public class FormLoginPageHandler<A extends ExchangeContext, B extends Exchange<A>> implements ExchangeHandler<A, B> {

	/**
	 * The default login action URI: {@code /login}.
	 */
	public static final String DEFAULT_LOGIN_ACTION_URI = "/login";
	
	/**
	 * The redirect URI query parameter name.
	 * 
	 * <p>
	 * This constant is also used in {@link FormAuthenticationErrorInterceptor}, {@link RedirectLoginSuccessHandler} and {@link RedirectLoginFailureHandler}.
	 * </p>
	 */
	public static final String PARAMETER_REDIRECT_URI = "redirect_uri";
	
	/**
	 * The error query parameter name.
	 * 
	 * <p>
	 * This constant is also used in {@link FormAuthenticationErrorInterceptor}, {@link RedirectLoginSuccessHandler} and {@link RedirectLoginFailureHandler}.
	 * </p>
	 */
	public static final String PARAMETER_ERROR = "error";
	
	/**
	 * The login view template.
	 */
	private static final LoginView.Renderer<CompletableFuture<String>> LOGIN_PAGE_RENDERER = LoginView.string();

	/**
	 * The login action URI.
	 */
	private final String loginActionUri;
	
	/**
	 * <p>
	 * Creates a form login page handler targeting the default login action URI.
	 * </p>
	 */
	public FormLoginPageHandler() {
		this(DEFAULT_LOGIN_ACTION_URI);
	}
	
	/**
	 * <p>
	 * Creates a form login page handler targeting the specified login action URI.
	 * </p>
	 * 
	 * @param loginActionUri the login action URI targeted by the login form
	 */
	public FormLoginPageHandler(String loginActionUri) {
		this.loginActionUri = loginActionUri;
	}

	/**
	 * <p>
	 * Returns the login action URI targeted by the login form.
	 * </p>
	 * 
	 * @return a login action URI
	 */
	public String getLoginActionUri() {
		return loginActionUri;
	}

	@Override
	public void handle(B exchange) throws HttpException {
		String redirect_uri = exchange.request().queryParameters().get(PARAMETER_REDIRECT_URI).map(Parameter::asString).orElse(null);
		String error = exchange.request().queryParameters().get(PARAMETER_ERROR).map(Parameter::asString).orElse(null);
		exchange.response()
			.headers(headers -> headers.contentType(MediaTypes.TEXT_HTML))
			.body().string().stream(Mono.fromFuture(LOGIN_PAGE_RENDERER.render(this.loginActionUri, redirect_uri, error)));
	}
}
