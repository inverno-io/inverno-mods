/*
 * Copyright 2025 Jeremy KUHN
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
package io.inverno.mod.security.http.session;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.http.login.LogoutSuccessHandler;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.http.context.SessionContext;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A logout success handler that invalidates the session.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the authentication type
 * @param <B> the identity type
 * @param <C> the access controller type
 * @param <D> the session data type
 * @param <E> the session type
 * @param <F> the session security exchange context type
 * @param <G> the exchange type
 */
public class SessionLogoutSuccessHandler<A extends Authentication, B extends Identity, C extends AccessController, D, E extends Session<D>, F extends SessionContext<D, E> & SecurityContext<B, C>, G extends Exchange<F>> implements LogoutSuccessHandler<A, B, C, F, G> {

	@Override
	public Mono<Void> handleLogoutSuccess(G exchange, A authentication) {
		return exchange.context().getSession().flatMap(Session::invalidate);
	}
}
