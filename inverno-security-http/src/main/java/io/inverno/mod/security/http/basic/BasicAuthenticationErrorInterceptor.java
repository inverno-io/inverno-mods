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
 * @author jkuhn
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
