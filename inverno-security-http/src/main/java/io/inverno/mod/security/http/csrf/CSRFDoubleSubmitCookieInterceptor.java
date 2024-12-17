/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.security.http.csrf;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.CookieParameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.security.MessageDigest;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A security interceptor that protects against Cross-site request forgery attack using the double submit cookie technique.
 * </p>
 * 
 * <p>
 * This implementation uses the stateless double submit cookie technique as recommended by
 * <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#double-submit-cookie">Cross-Site Request Forgery Prevention Cheat Sheet</a>.
 * </p>
 * 
 * <p>
 * For any {@code POST}, {@code PUT}, {@code PATCH} and {@code DELETE}, it successively tries to compare the value of a previously generated token specified in an cookie to the value supplied as a
 * header, as a query parameter or as a form parameter (assuming the request is a {@code application/x-www-form-urlencoded} request). If the two token values are equals, the request is authorized
 * otherwise a {@code FORBIDDEN(403)} error is returned.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the context type
 * @param <B> the exchange type
 */
public class CSRFDoubleSubmitCookieInterceptor<A extends ExchangeContext, B extends Exchange<A>> implements ExchangeInterceptor<A, B> {

	/**
	 * The default cookie name: {@code XSRF-TOKEN}.
	 */
	public static final String DEFAULT_COOKIE_NAME = "XSRF-TOKEN";
	
	/**
	 * The default header name: {@code X-CSRF-TOKEN}.
	 */
	public static final String DEFAULT_HEADER_NAME = "X-CSRF-TOKEN";
	
	/**
	 * The default parameter name: {@code _csrf_token}.
	 */
	public static final String DEFAULT_PARAMETER_NAME = "_csrf_token";
	
	/**
	 * The cookie name.
	 */
	protected final String cookieName;
	
	/**
	 * The header name.
	 */
	protected final String headerName;
	
	/**
	 * The parameter name.
	 */
	protected final String parameterName;
	
	/**
	 * The max age of the CSRF token cookie.
	 */
	protected final Integer maxAge;
	
	/**
	 * The domain of the CSRF token cookie.
	 */
	protected final String domain;
	
	/**
	 * The path of the CSRF token cookie.
	 */
	protected final String path;
	
	/**
	 * Flag indicating whether the token cookie should be secured.
	 */
	protected final boolean secure;
	
	/**
	 * Flag indicating whether the token cookie should be HTTP only.
	 */
	protected final boolean httpOnly;

	/**
	 * <p>
	 * Creates a CSRF double submit cookie interceptor.
	 * </p>
	 *
	 * @param cookieName    the cookie name
	 * @param headerName    the header name
	 * @param parameterName the parameter name
	 * @param maxAge        the cookie max age
	 * @param domain        the cookie domain
	 * @param path          the cookie path
	 * @param secure        the cookie secure flag
	 * @param httpOnly      the cookie HTTP only flag
	 */
	protected CSRFDoubleSubmitCookieInterceptor(String cookieName, String headerName, String parameterName, Integer maxAge, String domain, String path, Boolean secure, Boolean httpOnly) {
		this.cookieName = StringUtils.isNotBlank(cookieName) ? cookieName : DEFAULT_COOKIE_NAME;
		this.headerName = StringUtils.isNotBlank(headerName) ? headerName : DEFAULT_HEADER_NAME;
		this.parameterName = StringUtils.isNotBlank(parameterName) ? parameterName : DEFAULT_PARAMETER_NAME;
		this.maxAge = maxAge;
		this.domain = domain;
		this.path = path;
		this.secure = secure != null ? secure : true;
		this.httpOnly = httpOnly != null ? httpOnly : true;
	}

	/**
	 * <p>
	 * Returns the CSRF token cookie name.
	 * </p>
	 * 
	 * @return the cookie name
	 */
	public String getCookieName() {
		return cookieName;
	}

	/**
	 * <p>
	 * Returns the CSRF token header name.
	 * </p>
	 * 
	 * @return the header name
	 */
	public String getHeaderName() {
		return headerName;
	}

	/**
	 * <p>
	 * Returns the CSRF token parameter name.
	 * </p>
	 * 
	 * @return the parameter name
	 */
	public String getParameterName() {
		return parameterName;
	}
	
	/**
	 * <p>
	 * Creates a CSRF double submit cookie interceptor builder.
	 * </p>
	 * 
	 * @return a CSRF double submit cookie interceptor builder
	 */
	public static CSRFDoubleSubmitCookieInterceptor.Builder builder() {
		return new CSRFDoubleSubmitCookieInterceptor.Builder();
	}
	
	@Override
	public Mono<? extends B> intercept(B exchange) {
		// TODO check same origin
		
		// curl -iv -X POST -H 'cookie: XSRF-TOKEN=abc' -H 'X-CSRF-TOKEN:abc' http://127.0.0.1:8080/csrf
		// curl -iv -X POST -H 'cookie: XSRF-TOKEN=abc' http://127.0.0.1:8080/csrf?_csrf_token=abc
		// curl -iv -X POST -H 'cookie: XSRF-TOKEN=abc' -H 'content-type: application/x-www-form-urlencoded' -d '_csrf_token=abc' http://127.0.0.1:8080/csrf
		switch(exchange.request().getMethod()) {
			case POST:
			case PUT:
			case PATCH:
			case DELETE: {
				// "Double Submit Cookie" check
				byte[] csrfCookie = exchange.request().headers().cookies().get(this.cookieName)
					.map(CookieParameter::asString)
					.orElseThrow(() -> new ForbiddenException("Missing CSRF token cookie"))
					.getBytes(Charsets.UTF_8);
				
				byte[] csrfParameter = exchange.request().headers().get(this.headerName)
					.or(() -> exchange.request().queryParameters().get(this.parameterName).map(Parameter::asString))
					.map(value -> value.getBytes(Charsets.UTF_8)).orElse(null);
				if(csrfParameter != null) {
					if(!MessageDigest.isEqual(csrfParameter, csrfCookie)) {
						throw new ForbiddenException("CSRF token header does not match CSRF token cookie");
					}
					this.addCSRFTokenCookie(exchange);
					return Mono.just(exchange);
				}
				else if(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED.equalsIgnoreCase(exchange.request().headers().getContentType())) {
					/*
					 * We can't extract the CSRF token from parts in a multipart/form-data request as this is meant for streaming possibly big data and as a result consuming the body here would mean
					 * caching the data for later processing which is clearly inconsistent.
					 * Note that this behavior is the same as JEE HttpServletRequest which differentiates parts in multipart/form-data request from parameters in an application/x-www-form-urlencoded
					 * request. 
					 * application/x-www-form-urlencoded can be safely consumed here as the publisher is cached in the request body and can be subscribed multiple times.
					 */
					return exchange.request().body().orElseThrow(() -> new ForbiddenException("Missing CSRF token header/parameter"))
						.urlEncoded()
						.collectMap()
						.mapNotNull(parameters -> {
							Parameter csrfFormParameter = parameters.get(this.parameterName);
							return csrfFormParameter != null ? csrfFormParameter.asString() : null;
						})
						.map(csrfFormParameter -> {
							if(!MessageDigest.isEqual(csrfFormParameter.getBytes(Charsets.UTF_8), csrfCookie)) {
								throw new ForbiddenException("CSRF token header does not match CSRF token cookie");
							}
							this.addCSRFTokenCookie(exchange);
							return exchange;
						})
						.switchIfEmpty(Mono.error(() -> new ForbiddenException("Missing CSRF token header/parameter")));
				}
				else {
					throw new ForbiddenException("Missing CSRF token header/parameter");
				}
			}
			default: {
				this.addCSRFTokenCookie(exchange);
				return Mono.just(exchange); 
			}
		}
	}
	
	/**
	 * <p>
	 * Adds the CSRF token cookie to the response.
	 * </p>
	 * 
	 * @param exchange the exchange
	 */
	private void addCSRFTokenCookie(B exchange) {
		exchange.response().headers(headers -> headers.cookies(cookies -> cookies.addCookie(cookie -> {
			cookie.name(this.cookieName)
				.value(this.generateToken())
				.sameSite(Headers.SetCookie.SameSitePolicy.STRICT);

			if(this.maxAge != null) {
				cookie.maxAge(this.maxAge);
			}
			if(this.domain != null) {
				cookie.domain(domain);
			}
			if(this.path != null) {
				cookie.path(this.path);
			}
			if(this.secure) {
				cookie.secure(this.secure);
			}
			if(this.httpOnly) {
				cookie.httpOnly(this.httpOnly);
			}
		})));
	}
	
	/**
	 * <p>
	 * Generates the CSRF token.
	 * </p>
	 * 
	 * <p>
	 * It shall be collision free and hardly forgeable.
	 * </p>
	 * 
	 * @return a CSRF token
	 */
	protected String generateToken() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * <p>
	 * A CSRF double submit cookie interceptor builder.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Builder {
		
		/**
		 * The cookie name.
		 */
		protected String cookieName;
	
		/**
		 * The header name.
		 */
		protected String headerName;
		
		/**
		 * The parameter name.
		 */
		protected String parameterName;

		/**
		 * The CSRF token cookie max age parameter.
		 */
		protected Integer maxAge;

		/**
		 * The CSRF token cookie domain parameter.
		 */
		protected String domain;

		/**
		 * The CSRF token cookie path parameter.
		 */
		protected String path;

		/**
		 * The CSRF token cookie secure parameter.
		 */
		protected Boolean secure;

		/**
		 * The CSRF token cookie httpOnly parameter.
		 */
		protected Boolean httpOnly;
		
		/**
		 * <p>
		 * Creates a CSRF double submit cookie interceptor builder.
		 * </p>
		 */
		protected Builder() {}
		
		/**
		 * <p>
		 * Specifies the name of the CSRF token cookie.
		 * </p>
		 * 
		 * @param cookieName a cookie name
		 * 
		 * @return this builder
		 */
		public Builder cookieName(String cookieName) {
			this.cookieName = cookieName;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the name of the CSRF token header.
		 * </p>
		 * 
		 * @param headerName a header name
		 * 
		 * @return this builder
		 */
		public Builder headerName(String headerName) {
			this.headerName = headerName;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the name of the CSRF token parameter.
		 * </p>
		 * 
		 * @param parameterName a parameter name
		 * 
		 * @return this builder
		 */
		public Builder parameterName(String parameterName) {
			this.parameterName = parameterName;
			return this;
		}

		/**
		 * <p>
		 * Specifies the max age of the CSRF token cookie.
		 * </p>
		 * 
		 * @param maxAge a max age in seconds
		 * 
		 * @return this builder
		 */
		public Builder maxAge(int maxAge) {
			this.maxAge = maxAge >= 0 ? maxAge : null;
			return this;
		}

		/**
		 * <p>
		 * Specifies the domain of the CSRF token cookie.
		 * </p>
		 * 
		 * @param domain a domain
		 * 
		 * @return this builder
		 */
		public Builder domain(String domain) {
			this.domain = domain;
			return this;
		}

		/**
		 * <p>
		 * Specifies the path of the CSRF token cookie.
		 * </p>
		 * 
		 * @param path a path
		 * 
		 * @return this builder
		 */
		public Builder path(String path) {
			this.path = path;
			return this;
		}

		/**
		 * <p>
		 * Specifies the {@code secure} flag of the CSRF token cookie.
		 * </p>
		 * 
		 * <p>
		 * If not specified, this is enabled by default which is the recommended behaviour.
		 * </p>
		 * 
		 * @param secure true to create a secured cookie, false otherwise
		 * 
		 * @return this builder
		 */
		public Builder secure(boolean secure) {
			this.secure = secure;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the {@code httpOnly} flag of the CSRF token cookie.
		 * </p>
		 * 
		 * <p>
		 * If not specified, this is enabled by default which is the recommended behaviour.
		 * </p>
		 * 
		 * <p>
		 * You might need to set this to false when using JavaScript frameworks such as Angular, which requires to access the CSRF cookie token value in order to be able to send it in fetch calls in
		 * an HTTP header or a query parameter.
		 * </p>
		 * 
		 * @param httpOnly true to create an HTTP only cookie, false otherwise
		 * 
		 * @return this builder
		 */
		public Builder httpOnly(boolean httpOnly) {
			this.httpOnly = httpOnly;
			return this;
		}
		
		/**
		 * <p>
		 * Builds a CSRF double submit cookie interceptor.
		 * </p>
		 *
		 * @param <A> the context type
		 * @param <B> the exchange type
		 *
		 * @return a CSRF double submit cookie interceptor
		 */
		public <A extends ExchangeContext, B extends Exchange<A>> CSRFDoubleSubmitCookieInterceptor<A, B> build() {
			return new CSRFDoubleSubmitCookieInterceptor<>(this.cookieName, this.headerName, this.parameterName, this.maxAge, this.domain, this.path, this.secure, this.httpOnly);
		}
	}
}
