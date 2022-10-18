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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.security.http.HttpAuthenticationErrorInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;


/**
 * <p>
 * An HTTP authentication error interceptor that implements <a href="https://datatracker.ietf.org/doc/html/rfc7617">RFC 7617 The 'Basic' HTTP Authentication Scheme</a>.
 * </p>
 * 
 * <p>
 * As per RFC 7617, a basic challenge with the {@code realm} parameter is sent to the requester to initiate basic HTTP authentication.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the context type
 * @param <B> the error exchange type
 */
public class BasicAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends HttpAuthenticationErrorInterceptor<A, B> {

	/**
	 * The realm parameter.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7235#section-2.2">RFC 7235 Section 2.2</a>
	 * </p>
	 */
	private static final String PARAMETER_REALM = "realm";

	/**
	 * The www-authenticate challenge format.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7235#section-2.2">RFC 7235 Section 2</a>
	 * </p>
	 */
	private static final String FORMAT_WWW_AUTHENTICATE = Headers.Authorization.AUTH_SCHEME_BASIC + " " + PARAMETER_REALM + "=\"%s\"";

	/**
	 * The realm.
	 */
	private final String realm;

	/**
	 * <p>
	 * Creates a basic authentication error interceptor.
	 * </p>
	 * 
	 * @param realm the realm
	 */
	public BasicAuthenticationErrorInterceptor(String realm) {
		if(StringUtils.isBlank(realm)) {
			throw new IllegalArgumentException("realm is null or empty");
		}
		this.realm = realm;
	}

	/***
	 * <p>
	 * Returns the realm.
	 * </p>
	 * 
	 * @return the realm
	 */
	public String getRealm() {
		return realm;
	}
	
	@Override
	protected String createChallenge(io.inverno.mod.security.SecurityException cause) {
		return String.format(FORMAT_WWW_AUTHENTICATE, StringEscapeUtils.escapeJava(this.realm));
	}
}
