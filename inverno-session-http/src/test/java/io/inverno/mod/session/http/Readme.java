/*
 * Copyright 2025 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.inverno.mod.session.http;

import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.session.BasicSessionStore;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.SessionStore;
import io.inverno.mod.session.http.context.BasicSessionContext;
import io.inverno.mod.session.http.context.SessionContext;
import io.inverno.mod.session.http.context.jwt.JWTSessionContext;
import io.inverno.mod.session.jwt.JWTSession;
import io.inverno.mod.session.jwt.JWTSessionStore;
import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {

	public void sessionInterceptor() {
		SessionIdExtractor<SessionContext.Intercepted<SessionData, Session<SessionData>>, Exchange<SessionContext.Intercepted<SessionData, Session<SessionData>>>> sessionIdExtractor = null;
		SessionStore<SessionData, Session<SessionData>> sessionStore = null;
		SessionInjector<SessionData, Session<SessionData>, SessionContext.Intercepted<SessionData, Session<SessionData>>, Exchange<SessionContext.Intercepted<SessionData, Session<SessionData>>>> sessionInjector = null;

		SessionInterceptor<SessionData, Session<SessionData>, SessionContext.Intercepted<SessionData, Session<SessionData>>, Exchange<SessionContext.Intercepted<SessionData, Session<SessionData>>>> sessionInterceptor = SessionInterceptor.of(sessionIdExtractor, sessionStore, sessionInjector);
	}

	public void basicSessionInterceptor() {
		SessionIdExtractor<BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> sessionIdExtractor = null;
		BasicSessionStore<SessionData> sessionStore = null;
		SessionInjector<SessionData, Session<SessionData>, BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> sessionInjector = null;

		SessionInterceptor<SessionData, Session<SessionData>, BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> sessionInterceptor = SessionInterceptor.of(sessionIdExtractor, sessionStore, sessionInjector);
	}

	public void jwtSessionInterceptor() {
		SessionIdExtractor<JWTSessionContext.Intercepted<SessionData, StatelessSessionData>, Exchange<JWTSessionContext.Intercepted<SessionData, StatelessSessionData>>> sessionIdExtractor = null;
		JWTSessionStore<SessionData, StatelessSessionData> sessionStore = null;
		SessionInjector<SessionData, JWTSession<SessionData, StatelessSessionData>, JWTSessionContext.Intercepted<SessionData, StatelessSessionData>, Exchange<JWTSessionContext.Intercepted<SessionData, StatelessSessionData>>> sessionInjector = null;

		SessionInterceptor<SessionData, JWTSession<SessionData, StatelessSessionData>, JWTSessionContext.Intercepted<SessionData, StatelessSessionData>, Exchange<JWTSessionContext.Intercepted<SessionData, StatelessSessionData>>> sessionInterceptor = SessionInterceptor.of(sessionIdExtractor, sessionStore, sessionInjector);

		List<String> toto = new ArrayList<String>();
	}

	public void sessionQueryParameterExtractor() {
		SessionIdExtractor<BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> queryParameterSessionIdExtractor = exchange -> Mono.justOrEmpty(
			exchange.request().queryParameters().get("session-id")
				.map(Parameter::asString)
				.orElse(null)
		);

		SessionIdExtractor<BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> composedSessionIdExtractor = queryParameterSessionIdExtractor.or(new CookieSessionIdExtractor<>());
	}

	public void sessionHeaderInjector() {
		SessionInjector<SessionData, Session<SessionData>, BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> headerSessionInjector = (exchange, session) -> Mono.fromRunnable(() ->
			exchange.response().headers(headers -> headers.set("session-id", session.getId()))
		);

		SessionInjector<SessionData, Session<SessionData>, BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> composedSessionInjector = headerSessionInjector.compose(new CookieSessionInjector<>());
	}

	public static class SessionData {

	}

	public static class StatelessSessionData {

	}
}
