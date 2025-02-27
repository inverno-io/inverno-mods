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
package io.inverno.mod.session.http;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.CookieParameter;
import io.inverno.mod.http.server.Exchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session identifier extractor that extracts the identifier from a request cookie.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 */
public class CookieSessionIdExtractor<A extends ExchangeContext, B extends Exchange<A>> implements SessionIdExtractor<A, B> {

	/**
	 * The default session cookie name.
	 *
	 * <p>
	 * This constant is also used in {@link CookieSessionInjector}.
	 * </p>
	 */
	public static final String DEFAULT_COOKIE_NAME = "SESSION-ID";

	/**
	 * The session cookie name.
	 */
	private final String sessionCookie;

	/**
	 * <p>
	 * Creates a cookie session identifier extractor with the default session cookie name.
	 * </p>
	 */
	public CookieSessionIdExtractor() {
		this(DEFAULT_COOKIE_NAME);
	}

	/**
	 * <p>
	 * Creates a cookie session identifier extractor with the specified session cookie name.
	 * </p>
	 *
	 * @param sessionCookie the session cookie name
	 */
	public CookieSessionIdExtractor(String sessionCookie) {
		this.sessionCookie = sessionCookie;
	}

	/**
	 * <p>
	 * Returns the session cookie name.
	 * </p>
	 *
	 * @return the session cookie name
	 */
	public String getSessionCookie() {
		return sessionCookie;
	}

	@Override
	public Mono<String> extract(B exchange) {
		return Mono.fromSupplier(() -> exchange.request().headers().cookies().get(this.sessionCookie).map(CookieParameter::asString).orElse(null));
	}
}
