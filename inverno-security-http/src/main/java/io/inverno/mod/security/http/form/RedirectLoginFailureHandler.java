/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.form;

import io.inverno.mod.security.http.LoginFailureHandler;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class RedirectLoginFailureHandler<A extends ExchangeContext, B extends Exchange<A>> implements LoginFailureHandler<A, B> {

	public static final String DEFAULT_FAILURE_URI = "/login";

	private final String failureUri;
	
	public RedirectLoginFailureHandler() {
		this(DEFAULT_FAILURE_URI);
	}
	
	public RedirectLoginFailureHandler(String failureUri) {
		this.failureUri = failureUri;
	}

	public String getFailureUri() {
		return failureUri;
	}
	
	@Override
	public Mono<Void> handleLoginFailure(B exchange, io.inverno.mod.security.SecurityException error) {
		return exchange.request().body().get().urlEncoded().collectMap()
			.flatMap(parameterMap -> {
				URIBuilder failureUriBuilder = URIs.uri(this.failureUri);
				
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
