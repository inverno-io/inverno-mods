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

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class FormLoginPageHandler<A extends ExchangeContext, B extends Exchange<A>> implements ExchangeHandler<A, B> {

	private static final LoginView.Renderer<CompletableFuture<String>> LOGIN_PAGE_RENDERER = LoginView.string();
	
	public static final String DEFAULT_LOGIN_ACTION_URI = "/login";
	
	public static final String PARAMETER_REDIRECT_URI = "redirect_uri";
	
	public static final String PARAMETER_ERROR = "error";

	private final String loginActionUri;
	
	public FormLoginPageHandler() {
		this(DEFAULT_LOGIN_ACTION_URI);
	}
	
	public FormLoginPageHandler(String loginActionUri) {
		this.loginActionUri = loginActionUri;
	}

	public String getLoginActionUri() {
		return loginActionUri;
	}

	@Override
	public void handle(B exchange) throws HttpException {
		String redirect_uri = exchange.request().queryParameters().get(PARAMETER_REDIRECT_URI).map(Parameter::asString).orElse(null);
		String error = exchange.request().queryParameters().get(PARAMETER_ERROR).map(Parameter::asString).orElse(null);
		exchange.response().body().string().stream(Mono.fromFuture(LOGIN_PAGE_RENDERER.render(this.loginActionUri, redirect_uri, error)));
	}
}
