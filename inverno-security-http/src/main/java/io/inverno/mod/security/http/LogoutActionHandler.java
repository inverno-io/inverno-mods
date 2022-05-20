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

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationReleaser;
import io.inverno.mod.security.http.context.SecurityContext;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class LogoutActionHandler<A extends Authentication, B extends SecurityContext, C extends Exchange<B>> implements ExchangeHandler<B, C> {

	private final AuthenticationReleaser<A> authenticationReleasor;
	
	private final LogoutSuccessHandler<A, B, C> logoutSuccessHandler;
	
	public LogoutActionHandler(AuthenticationReleaser<A> authenticationReleasor) {
		this(authenticationReleasor, null);
	}
	
	public LogoutActionHandler(AuthenticationReleaser<A> authenticationReleasor, LogoutSuccessHandler<A, B, C> logoutSuccessHandler) {
		this.authenticationReleasor = authenticationReleasor;
		this.logoutSuccessHandler = logoutSuccessHandler;
	}

	public AuthenticationReleaser<A> getAuthenticationReleasor() {
		return authenticationReleasor;
	}

	public LogoutSuccessHandler<A, B, C> getLogoutSuccessHandler() {
		return logoutSuccessHandler;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> defer(C exchange) {
		Authentication authentication = exchange.context().getAuthentication();
		/* 
		 * We can have ClassCastException here but at some point we can't make everything generic, this risk should be limited assuming everything is 
		 * properly configured. Since everything related to configuration is generic, it should be ok and if not, the error will be raised at runtime 
		 * instead of compile time.
		 */
		Mono<Void> result = this.authenticationReleasor.release((A)authentication);
		if(this.logoutSuccessHandler != null) {
			result = result.then(this.logoutSuccessHandler.handleLogoutSuccess(exchange, (A)authentication));
		}
		return result;
	}
	
	@Override
	public void handle(C exchange) throws HttpException {
		throw new UnsupportedOperationException();
	}
}
