package io.inverno.mod.security.http;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ErrorExchangeHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class BasicSecurityErrorHandler<A extends Throwable, B extends ErrorExchange<A>> implements ErrorExchangeHandler<A, B> {

	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7617#section-2">RFC 7617 Section 2</a>
	 */
	private static final String VALUE_BASIC = "basic";
	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7235#section-2.2">RFC 7235 Section 2.2</a>
	 */
	private static final String VALUE_REALM = "realm";

	/**
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7235#section-2.2">RFC 7235 Section 2.2</a>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7617#section-2">RFC 7617 Section 2</a>
	 */
	private static final String FORMAT_WWW_AUTHENTICATE = VALUE_BASIC + " " + VALUE_REALM + "=\"%s\"";

	private final String realm;

	public BasicSecurityErrorHandler(String realm) {
		if(StringUtils.isBlank(realm)) {
			throw new IllegalArgumentException("realm is null or empty");
		}
		this.realm = realm;
	}

	@Override
	public void handle(B exchange) throws HttpException {
		if(exchange.getError() instanceof HttpException && ((HttpException)exchange.getError()).getStatusCode() == Status.UNAUTHORIZED.getCode()) {
			exchange.response()
				.headers(headers -> headers
					.status(Status.UNAUTHORIZED)
					.add(Headers.NAME_WWW_AUTHENTICATE, String.format(FORMAT_WWW_AUTHENTICATE, StringEscapeUtils.escapeJava(this.realm)))
				)
				.body().empty();
		}
		else {
			throw new IllegalStateException("A security exception handler is handling an unexpected error: " + exchange.getError().getClass().getCanonicalName());
		}
	}
}
