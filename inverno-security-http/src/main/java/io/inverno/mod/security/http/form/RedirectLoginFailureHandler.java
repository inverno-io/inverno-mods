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

import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.http.login.LoginActionHandler;
import io.inverno.mod.security.http.login.LoginFailureHandler;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A login failure handler implementation that redirects the client (302) after a failed login authentication.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see LoginActionHandler
 * 
 * @param <A> the context type
 * @param <B> the exchange type
 */
public class RedirectLoginFailureHandler<A extends ExchangeContext, B extends Exchange<A>> implements LoginFailureHandler<A, B> {

	/**
	 * The default login failure URI: {@code /login}.
	 */
	public static final String DEFAULT_LOGIN_FAILURE_URI = "/login";

	/**
	 * The URI where the client is redirected after a failed login authentication.
	 */
	private final String loginFailureUri;
	
	/**
	 * <p>
	 * Creates a redirect login failure handler which redirects the client to the default login failure URI.
	 * </p>
	 */
	public RedirectLoginFailureHandler() {
		this(DEFAULT_LOGIN_FAILURE_URI);
	}
	
	/**
	 * <p>
	 * Creates a redirect login failure handler which redirects the client to the specified login failure URI.
	 * </p>
	 * 
	 * @param loginFailureUri the URI where to redirect the client after a failed login authentication
	 */
	public RedirectLoginFailureHandler(String loginFailureUri) {
		this.loginFailureUri = loginFailureUri;
	}

	/**
	 * <p>
	 * Returns the URI where the client is redirected after a failed login authentication.
	 * </p>
	 * 
	 * @return the login failure URI
	 */
	public String getLoginFailureUri() {
		return loginFailureUri;
	}
	
	@Override
	public Mono<Void> handleLoginFailure(B exchange, io.inverno.mod.security.SecurityException error) {
		return exchange.request().body().get().urlEncoded().collectMap()
			.flatMap(parameterMap -> {
				URIBuilder failureUriBuilder = URIs.uri(this.loginFailureUri);
				
				failureUriBuilder.queryParameter(FormLoginPageHandler.PARAMETER_ERROR, error.getClass().getSimpleName() + ": " + error.getMessage());
				
				Parameter redirectUri = parameterMap.get(FormLoginPageHandler.PARAMETER_REDIRECT_URI);
				if(redirectUri != null) {
					failureUriBuilder.queryParameter(FormLoginPageHandler.PARAMETER_REDIRECT_URI, redirectUri.getValue());
				}				
				
				exchange.response()
					.headers(headers -> headers
						.status(Status.FOUND)
						.set(Headers.NAME_LOCATION, failureUriBuilder.buildString())
					)
					.body().empty();
				
				return Mono.empty();
			});
	}
}
