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
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.session.Session;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A session injector that sets the session identifier in a response cookie.
 * </p>
 *
 * <p>
 * The secure flag is set on the session cookie when the server is configured with TLS (HTTPS). An application deployed behind an SSL offloader is likely to be configured without TLS and as a result
 * it is up to the SSL offloader to set the secure flag on the session cookie.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 */
public class CookieSessionInjector<A, B extends Session<A>, C extends ExchangeContext, D extends Exchange<C>> implements SessionInjector<A, B, C, D> {

	/**
	 * The default token cookie path: {@code /}.
	 */
	public static final String DEFAULT_PATH = "/";

	/**
	 * The default cookie same site policy.
	 */
	public static final Headers.SetCookie.SameSitePolicy DEFAULT_SAME_SITE_POLICY = Headers.SetCookie.SameSitePolicy.LAX;

	private final String path;
	private final String sessionCookie;

	private Headers.SetCookie.SameSitePolicy sameSitePolicy;

	/**
	 * <p>
	 * Creates a cookie session injector with default path and session cookie name.
	 * </p>
	 */
	public CookieSessionInjector() {
		this(DEFAULT_PATH, CookieSessionIdExtractor.DEFAULT_COOKIE_NAME);
	}

	/**
	 * <p>
	 * Creates a cookie session injector with specified path and default session cookie name.
	 * </p>
	 *
	 * @param path the session cookie path
	 */
	public CookieSessionInjector(String path) {
		this(path, CookieSessionIdExtractor.DEFAULT_COOKIE_NAME);
	}

	/**
	 * <p>
	 * Creates a cookie session injector with specified path and session cookie name.
	 * </p>
	 *
	 * @param path          the session cookie path
	 * @param sessionCookie the session cookie name
	 */
	public CookieSessionInjector(String path, String sessionCookie) {
		this.path = path;
		this.sessionCookie = sessionCookie;

		this.sameSitePolicy = DEFAULT_SAME_SITE_POLICY;
	}

	/**
	 * <p>
	 * Returns the session cookie path.
	 * </p>
	 *
	 * @return the session cookie path
	 */
	public String getPath() {
		return path;
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

	/**
	 * <p>
	 * Returns the cookie same site policy.
	 * </p>
	 *
	 * @return the same site policy
	 */
	public Headers.SetCookie.SameSitePolicy getSameSitePolicy() {
		return sameSitePolicy;
	}

	/**
	 * <p>
	 * Sets the cookie same site policy
	 * </p>
	 *
	 * @param sameSitePolicy a same site policy
	 */
	public void setSameSitePolicy(Headers.SetCookie.SameSitePolicy sameSitePolicy) {
		this.sameSitePolicy = sameSitePolicy;
	}

	@Override
	public Mono<Void> inject(D exchange, B session) {
		return Mono.fromRunnable(() -> exchange.response().headers(headers -> headers.cookies(cookies -> cookies
				.setCookie(cookie -> cookie
					// TODO should be secured when we are in https...
					// But we can also be behind an ssl offloader
					.path(this.path)
					.secure("https".equals(exchange.request().getScheme()))
					.sameSite(this.sameSitePolicy)
					.httpOnly(true)
					.name(this.sessionCookie)
					.value(session.getId())
				)
			)));
	}

	@Override
	public Mono<Void> remove(D exchange) {
		return Mono.fromRunnable(() -> {
			exchange.response().headers(headers -> headers.cookies(cookies -> cookies
				.setCookie(cookie -> cookie
					.path(this.path)
					.name(this.sessionCookie)
					.value("")
					.maxAge(0)
				)
			));
		});
	}
}
