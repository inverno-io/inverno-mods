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

import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.security.http.HttpAuthenticationErrorInterceptor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class DigestAuthenticationErrorInterceptor<A extends ExchangeContext, B extends ErrorExchange<A>> extends HttpAuthenticationErrorInterceptor<A, B> {
	
	/**
	 * Only MD5 is supported in "modern" web browsers
	 * FF93 is the first supporting SHA-256
	 */
	public static final String ALGORITHM_MD5 = "MD5";
	public static final String ALGORITHM_SHA_256 = "SHA-256";
	public static final String ALGORITHM_SHA_512_256 = "SHA-512-256";
	
	private static final String PARAMETER_REALM = "realm";
	private static final String PARAMETER_QOP = "qop";
	private static final String PARAMETER_NONCE = "nonce";
	private static final String PARAMETER_ALGORITHM = "algorithm";
	private static final String PARAMETER_STALE = "stale";
	
	private static final String VALUE_QOP_AUTH = "auth";
	
	private static final String FORMAT_WWW_AUTHENTICATE = Headers.Authorization.AUTH_SCHEME_DIGEST + " " + PARAMETER_REALM + "=\"%s\"," + PARAMETER_QOP + "=\"%s\"," + PARAMETER_NONCE + "=\"%s\"," + PARAMETER_ALGORITHM + "=%s";
	private static final String FORMAT_WWW_AUTHENTICATE_STALE = FORMAT_WWW_AUTHENTICATE + "," + PARAMETER_STALE + "=%s";
	
	public static final long DEFAULT_NONCE_VALIDITY_SECONDS = 300;
	
	private final MessageDigest nonceDigest;
	
	private final String realm;
	
	private final String secret;
	
	private long nonceValiditySeconds = DEFAULT_NONCE_VALIDITY_SECONDS;

	public DigestAuthenticationErrorInterceptor(String realm, String secret) {
		this(realm, secret, ALGORITHM_MD5);
	}
	
	public DigestAuthenticationErrorInterceptor(String realm, String secret, String algorithm) {
		if(StringUtils.isBlank(realm)) {
			throw new IllegalArgumentException("realm is null or empty");
		}
		if(StringUtils.isBlank(secret)) {
			throw new IllegalArgumentException("secret is null or empty");
		}
		try {
			this.nonceDigest = MessageDigest.getInstance(algorithm);
		} 
		catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Invalid nonce algorithm: " + algorithm, e);
		}
		this.realm = realm;
		this.secret = secret;
	}
	
	public void setNonceValiditySeconds(long nonceValiditySeconds) {
		this.nonceValiditySeconds = nonceValiditySeconds;
	}

	public long getNonceValiditySeconds() {
		return nonceValiditySeconds;
	}
	
	@Override
	protected String createChallenge(io.inverno.mod.security.SecurityException cause) {
		long nonceExpire = System.nanoTime() + this.nonceValiditySeconds * 1000000000;
		String nonce = nonceExpire + ":" +  kd(this.nonceDigest, this.secret, Long.toString(nonceExpire));
		String nonceB64 = Base64.getEncoder().encodeToString(nonce.getBytes());
		
		if(cause instanceof ExpiredNonceException) {
			return String.format(FORMAT_WWW_AUTHENTICATE_STALE, StringEscapeUtils.escapeJava(this.realm), VALUE_QOP_AUTH, nonceB64, this.nonceDigest.getAlgorithm(), true);
		}
		else {
			return String.format(FORMAT_WWW_AUTHENTICATE, StringEscapeUtils.escapeJava(this.realm), VALUE_QOP_AUTH, nonceB64, this.nonceDigest.getAlgorithm(), StringEscapeUtils.escapeJava(this.realm));
		}
	}
	
	private String kd(MessageDigest digest, String secret, String data) {
		return Hex.encodeHexString(digest.digest((secret + ":" + data).getBytes()));
	}
}
