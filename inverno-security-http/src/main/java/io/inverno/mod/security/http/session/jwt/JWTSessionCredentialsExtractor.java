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
package io.inverno.mod.security.http.session.jwt;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import io.inverno.mod.security.http.session.SessionCredentials;
import io.inverno.mod.session.http.context.jwt.JWTSessionContext;
import io.inverno.mod.session.jwt.JWTSession;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A credentials extractor that extracts session credentials from a JWT session.
 * </p>
 *
 * <p>
 * This basically looks for the {@link Authentication} stored in the stateless session data embedded in the JWT session id and returns a {@link SessionCredentials} containing the authentication.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the authentication type
 * @param <B> the session data type
 * @param <C> the JWT session context type
 * @param <D> the exchange type
 */
public class JWTSessionCredentialsExtractor<A extends Authentication, B, C extends JWTSessionContext<B, A>, D extends Exchange<C>> implements CredentialsExtractor<SessionCredentials<A>, C, D> {

	@Override
	public Mono<SessionCredentials<A>> extract(D exchange) throws MalformedCredentialsException {
		if(exchange.context().isSessionPresent()) {
			return exchange.context().getSession().mapNotNull(JWTSession::getStatelessData).map(SessionCredentials::new);
		}
		return Mono.empty();
	}
}
