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

import io.inverno.mod.security.authentication.PrincipalCredentials;
import io.inverno.mod.security.http.MalformedCredentialsException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * <p>
 * HTTP Digest specific credentials as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class DigestCredentials implements PrincipalCredentials {
	
	/**
	 * The HTTP method.
	 */
	private final String method;
	
	/**
	 * The URI.
	 */
	private final String uri;
	
	/**
	 * The realm.
	 */
	private final String realm;
	
	/**
	 * The username.
	 */
	private final String username;
	
	/**
	 * The user hash.
	 */
	private final boolean userhash;
	
	/**
	 * The message digest.
	 */
	private final MessageDigest digest;
	
	/**
	 * The quality of protection.
	 */
	private final String qop;
	
	/**
	 * The nonce count.
	 */
	private final String nc;
	
	/**
	 * The client nonce.
	 */
	private final String cnonce;
	
	/**
	 * The server nonce.
	 */
	private final String nonce;
	
	/**
	 * The server nonce expire time stamp.
	 */
	private final long nonceExpire;
	
	/**
	 * The server nonce hash.
	 */
	private final String nonceHash;
	
	/**
	 * The digest response.
	 */
	private final String response;
	
	/**
	 * <p>
	 * Creates digest credentials.
	 * </p>
	 * 
	 * @param method the HTTP method
	 * @param uri the URI
	 * @param realm the realm
	 * @param username the username
	 * @param userhash the user hash
	 * @param algorithm the algorithm
	 * @param qop the quality of protection
	 * @param nc the nonce count
	 * @param cnonce the client nonce
	 * @param nonce the server nonce
	 * @param response the digest response
	 * 
	 * @throws MalformedCredentialsException if specified parameters and therefore credentials are incorrect
	 */
	public DigestCredentials(String method, String uri, String realm, String username, boolean userhash, String algorithm, String qop, String nc, String cnonce, String nonce, String response) throws MalformedCredentialsException {
		if(response == null || username == null || realm == null || nonce == null || uri == null) {
			throw new MalformedCredentialsException("Missing required digest parameters");
		}
		
		try {
			if(algorithm == null) {
				this.digest = MessageDigest.getInstance("MD5");
			}
			else switch (algorithm) {
				case "SHA-256":
					this.digest = MessageDigest.getInstance("SHA-256");
					break;
				case "SHA-512-256":
					this.digest = MessageDigest.getInstance("SHA-512-256");
					break;
				case "MD5":
					this.digest = MessageDigest.getInstance("MD5");
					break;
				default:
					throw new MalformedCredentialsException("Unsupported digest algorithm: " + algorithm);
			}
		} 
		catch (NoSuchAlgorithmException e) {
			throw new MalformedCredentialsException("Unsupported digest algorithm: " + algorithm, e);
		}
		
		if(qop != null) {
			if(!"auth".equals(qop)) {
				throw new MalformedCredentialsException("Unsupported qop: " + qop);
			}
			if(cnonce == null || nc == null) {
				throw new MalformedCredentialsException("Missing required digest parameters");
			}
		}

		// nonce = time-stamp ":" H(time-stamp ":" private-key)
		final String decodedNonce;
		try {
			decodedNonce = new String(Base64.getDecoder().decode(nonce.getBytes()));
		}
		catch(IllegalArgumentException e) {
			throw new MalformedCredentialsException("Digest nonce parameter is not encoded in Base64: " + nonce);
		}
		
		final String[] splitNonce = decodedNonce.split(":");
		if(splitNonce.length != 2) {
			throw new MalformedCredentialsException("Invalid digest nonce parameter");
		}
		
		try {
			this.nonceExpire = Long.parseLong(splitNonce[0]);
		}
		catch(NumberFormatException e) {
			throw new MalformedCredentialsException("Digest nonce expire parameter is not a number: " + splitNonce[0]);
		}
		this.nonceHash = splitNonce[1];
		
		this.method = method;
		this.uri = uri;
		this.realm = realm;
		this.username = username;
		this.userhash = userhash;
		this.qop = qop;
		this.nc = nc;
		this.cnonce = cnonce;
		this.nonce = nonce;
		this.response = response;
	}

	/**
	 * <p>
	 * Returns the message digest.
	 * </p>
	 * 
	 * @return the message digest
	 */
	public MessageDigest getDigest() {
		return digest;
	}

	/**
	 * <p>
	 * Returns the digest response
	 * </p>
	 * 
	 * @return the digest response
	 */
	public String getResponse() {
		return response;
	}

	@Override
	public String getUsername() {
		return username;
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
	 * Returns the user hash.
	 * </p>
	 * 
	 * @return the user hash
	 */
	public boolean isUserhash() {
		return userhash;
	}

	/**
	 * <p>
	 * Returns the HTTP method.
	 * </p>
	 * 
	 * @return the HTTP method
	 */
	public String getMethod() {
		return method;
	}
	
	/**
	 * <p>
	 * Returns the URI.
	 * </p>
	 * 
	 * @return the URI
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * <p>
	 * Returns the quality of protection.
	 * </p>
	 * 
	 * @return the qop
	 */
	public String getQop() {
		return qop;
	}

	/**
	 * <p>
	 * Returns the client nonce.
	 * </p>
	 * 
	 * @return the client nonce
	 */
	public String getCnonce() {
		return cnonce;
	}

	/**
	 * <p>
	 * Returns the nonce count.
	 * </p>
	 * 
	 * @return the nonce count
	 */
	public String getNc() {
		return nc;
	}

	/**
	 * <p>
	 * Returns the server nonce.
	 * </p>
	 * 
	 * @return the server nonce
	 */
	public String getNonce() {
		return nonce;
	}

	/**
	 * <p>
	 * Returns the server nonce expire timestamp.
	 * </p>
	 * 
	 * @return the server nonce expire timestamp
	 */
	public long getNonceExpire() {
		return nonceExpire;
	}

	/**
	 * <p>
	 * Returns the server nonce hash.
	 * </p>
	 * 
	 * @return the server nonce hash
	 */
	public String getNonceHash() {
		return nonceHash;
	}
}

