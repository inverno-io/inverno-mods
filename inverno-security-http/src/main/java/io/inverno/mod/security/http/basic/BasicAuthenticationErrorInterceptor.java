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
package io.inverno.mod.security.http.basic;

import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.http.HttpAuthenticationErrorInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;


/**
 * https://datatracker.ietf.org/doc/html/rfc7617
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A>
 * @param <B> 
 */
public class BasicAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends HttpAuthenticationErrorInterceptor<A, B> {

	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7235#section-2.2">RFC 7235 Section 2.2</a>
	 */
	private static final String PARAMETER_REALM = "realm";

	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7235#section-2.2">RFC 7235 Section 2.2</a>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7617#section-2">RFC 7617 Section 2</a>
	 */
	private static final String FORMAT_WWW_AUTHENTICATE = Headers.Authorization.AUTH_SCHEME_BASIC + " " + PARAMETER_REALM + "=\"%s\"";

	private final String realm;

	public BasicAuthenticationErrorInterceptor(String realm) {
		if(StringUtils.isBlank(realm)) {
			throw new IllegalArgumentException("realm is null or empty");
		}
		this.realm = realm;
	}

	@Override
	protected String createChallenge(io.inverno.mod.security.SecurityException cause) {
		return String.format(FORMAT_WWW_AUTHENTICATE, StringEscapeUtils.escapeJava(this.realm));
	}
}
