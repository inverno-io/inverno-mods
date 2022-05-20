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

import io.inverno.mod.security.authentication.Credentials;
import io.inverno.mod.security.http.MalformedCredentialsException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class DigestCredentials implements Credentials {
	
	private final String method;
	
	private final String uri;
	
	private final String realm;
	
	private final String username;
	
	private final boolean userhash;
	
	private final MessageDigest digest;
	
	private final String qop;
	
	private final String nc;
	
	private final String cnonce;
	
	private final String nonce;
	
	private final long nonceExpire;
	
	private final String nonceHash;
	
	private final String response;
	
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

	public MessageDigest getDigest() {
		return digest;
	}

	public String getResponse() {
		return response;
	}

	public String getUsername() {
		return username;
	}

	public String getRealm() {
		return realm;
	}

	public boolean isUserhash() {
		return userhash;
	}

	public String getMethod() {
		return method;
	}
	
	public String getUri() {
		return uri;
	}

	public String getQop() {
		return qop;
	}

	public String getCnonce() {
		return cnonce;
	}

	public String getNc() {
		return nc;
	}

	public String getNonce() {
		return nonce;
	}

	public long getNonceExpire() {
		return nonceExpire;
	}

	public String getNonceHash() {
		return nonceHash;
	}
}

