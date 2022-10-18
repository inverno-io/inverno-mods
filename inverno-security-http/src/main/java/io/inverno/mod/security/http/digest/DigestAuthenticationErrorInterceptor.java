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
package io.inverno.mod.security.http.digest;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.security.http.HttpAuthenticationErrorInterceptor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * An HTTP authentication error interceptor that implements <a href="https://datatracker.ietf.org/doc/html/rfc7616">RFC 7616 HTTP Digest Access Authentication</a>.
 * </p>
 * 
 * <p>
 * As per RFC 7616, a digest challenge with {@code realm}, {@code domain}, {@code nonce}, {@code opaque}, {@code stale}, {@code algorithm}, {@code qop}, {@code charset} (optional) and {@code userhash}
 * (optional) parameters is sent to the requester to initiate digest HTTP authentication.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the context type
 * @param <B> the error exchange type
 */
public class DigestAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends HttpAuthenticationErrorInterceptor<A, B> {
	
	/**
	 * MD5 algorithm (default).
	 * 
	 * <p>
	 * Only MD5 is supported in "modern" web browsers.
	 * </p>
	 */
	public static final String ALGORITHM_MD5 = "MD5";
	
	/**
	 * SHA-256 algorithm.
	 * 
	 * <p>
	 * Firefox 93 is the first browser supporting SHA-256.
	 * </p>
	 */
	public static final String ALGORITHM_SHA_256 = "SHA-256";
	
	/**
	 * SHA-512-256 algorithm.
	 */
	public static final String ALGORITHM_SHA_512_256 = "SHA-512-256";
	
	/**
	 * The default nonce validity period in seconds.
	 */
	public static final long DEFAULT_NONCE_VALIDITY_SECONDS = 300;
	
	/**
	 * The realm parameter.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>
	 * </p>
	 */
	private static final String PARAMETER_REALM = "realm";
	
	/**
	 * The qop parameter.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>
	 * </p>
	 */
	private static final String PARAMETER_QOP = "qop";
	
	/**
	 * The nonce parameter.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>
	 * </p>
	 */
	private static final String PARAMETER_NONCE = "nonce";
	
	/**
	 * The algorithm parameter.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>
	 * </p>
	 */
	private static final String PARAMETER_ALGORITHM = "algorithm";
	
	/**
	 * The stale parameter.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>
	 * </p>
	 */
	private static final String PARAMETER_STALE = "stale";
	
	/**
	 * The {@code auth} qop parameter value.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>
	 * </p>
	 */
	private static final String VALUE_QOP_AUTH = "auth";
	
	/**
	 * The {@code auth-int} qop parameter value.
	 * 
	 * <p>
	 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.3">RFC 7616 Section 3.3</a>
	 * </p>
	 */
	private static final String VALUE_QOP_AUTH_INT = "auth-int";
	
	/**
	 * The {@code www-authenticate} format.
	 */
	private static final String FORMAT_WWW_AUTHENTICATE = Headers.Authorization.AUTH_SCHEME_DIGEST + " " + PARAMETER_REALM + "=\"%s\"," + PARAMETER_QOP + "=\"%s\"," + PARAMETER_NONCE + "=\"%s\"," + PARAMETER_ALGORITHM + "=%s";
	
	/**
	 * The {@code www-authenticate} format for stale response (i.e. expired nonce).
	 */
	private static final String FORMAT_WWW_AUTHENTICATE_STALE = FORMAT_WWW_AUTHENTICATE + "," + PARAMETER_STALE + "=%s";
	
	/**
	 * The message digest.
	 */
	private final MessageDigest digest;
	
	/**
	 * The realm.
	 */
	private final String realm;
	
	/**
	 * The secret.
	 */
	private final String secret;
	
	/**
	 * The nonce validity period.
	 */
	private long nonceValiditySeconds = DEFAULT_NONCE_VALIDITY_SECONDS;

	/**
	 * <p>
	 * Creates a digest authentication error interceptor with the specified realm and secret.
	 * </p>
	 * 
	 * @param realm  a realm
	 * @param secret a secret
	 */
	public DigestAuthenticationErrorInterceptor(String realm, String secret) {
		this(realm, secret, ALGORITHM_MD5);
	}
	
	/**
	 * <p>
	 * Creates a digest authentication error interceptor with the specified realm, secret and algorithm.
	 * </p>
	 *
	 * @param realm     a realm
	 * @param secret    a secret
	 * @param algorithm an algorithm
	 *
	 * @throws IllegalArgumentException if specified parameters are incorrect
	 */
	public DigestAuthenticationErrorInterceptor(String realm, String secret, String algorithm) throws IllegalArgumentException {
		if(StringUtils.isBlank(realm)) {
			throw new IllegalArgumentException("realm is null or empty");
		}
		if(StringUtils.isBlank(secret)) {
			throw new IllegalArgumentException("secret is null or empty");
		}
		try {
			this.digest = MessageDigest.getInstance(algorithm);
		} 
		catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Invalid nonce algorithm: " + algorithm, e);
		}
		this.realm = realm;
		this.secret = secret;
	}

	/**
	 * <p>
	 * Returns the realm.
	 * </p>
	 * 
	 * @return the realm
	 */
	public String getRealm() {
		return realm;
	}

	/**
	 * <p>
	 * Returns the algorithm.
	 * </p>
	 * 
	 * @return the algorithm
	 */
	public String getAlgorithm() {
		return this.digest.getAlgorithm();
	}
	
	/**
	 * <p>
	 * Sets the nonce validity period.
	 * </p>
	 * 
	 * @param nonceValiditySeconds the nonce validity period in seconds
	 */
	public void setNonceValiditySeconds(long nonceValiditySeconds) {
		this.nonceValiditySeconds = nonceValiditySeconds;
	}

	/**
	 * <p>
	 * Returns the nonce validity period in seconds.
	 * </p>
	 * 
	 * @return the nonce validity period in seconds
	 */
	public long getNonceValiditySeconds() {
		return nonceValiditySeconds;
	}
	
	@Override
	protected String createChallenge(io.inverno.mod.security.SecurityException cause) {
		long nonceExpire = System.nanoTime() + this.nonceValiditySeconds * 1000000000;
		String nonce = nonceExpire + ":" +  DigestUtils.kd(this.digest, this.secret, Long.toString(nonceExpire));
		String nonceB64 = Base64.getEncoder().encodeToString(nonce.getBytes());
		
		if(cause instanceof ExpiredNonceException) {
			return String.format(FORMAT_WWW_AUTHENTICATE_STALE, StringEscapeUtils.escapeJava(this.realm), VALUE_QOP_AUTH, nonceB64, this.digest.getAlgorithm(), true);
		}
		else {
			return String.format(FORMAT_WWW_AUTHENTICATE, StringEscapeUtils.escapeJava(this.realm), VALUE_QOP_AUTH, nonceB64, this.digest.getAlgorithm(), StringEscapeUtils.escapeJava(this.realm));
		}
	}
}
