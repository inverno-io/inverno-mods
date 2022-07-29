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
package io.inverno.mod.security.http.digest;

import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.CredentialsMatcher;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.password.Password;
import io.inverno.mod.security.authentication.password.RawPassword;

/**
 * <p>
 * A credentials matcher used to verify digest credentials as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616">RFC 7616</a>.
 * </p>
 * 
 * <p>
 * HTTP Digest authentication basically requires a raw password in the login credentials in order to compute {@code A1} as defined by
 * <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4.2">RFC 7616 Section 3.4.2</a> and compute the expected digest response. This implementation accepts login credentials with raw
 * password as well as login credentials with {@link DigestPassword} which allows to store login credentials with encoded passwords (still limited to digest encoding). Using any other type of password
 * in the login credentials will result in an authentication failure.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of login credentials
 */
public class DigestCredentialsMatcher<A extends LoginCredentials> implements CredentialsMatcher<DigestCredentials, A> {

	/**
	 * The secret.
	 */
	private final String secret;

	/**
	 * <p>
	 * Creates a digest credentials matcher with the specified secret.
	 * </p>
	 * 
	 * <p>
	 * The secret must be the same as the one specified used to generate the {@code www-authenticate} header previously sent to the client.
	 * </p>
	 * 
	 * @param secret the secret
	 */
	public DigestCredentialsMatcher(String secret) {
		this.secret = secret;
	}
	
	@Override
	public boolean matches(DigestCredentials credentials, A otherCredentials) throws AuthenticationException {
		// Following System.nanotime() recommendation: 
		// To compare elapsed time against a timeout, use 
		// {@code if (System.nanoTime() - startTime >= timeoutNanos) ...} instead of 
		// {@code if (System.nanoTime() >= startTime + timeoutNanos) ...} 
		// because of the possibility of numerical overflow.
		if(System.nanoTime() - credentials.getNonceExpire() > 0){
			throw new ExpiredNonceException("Nonce has expired");
		}
		
		String serverNonceHash = DigestUtils.kd(credentials.getDigest(), this.secret, Long.toString(credentials.getNonceExpire()));
		if(!serverNonceHash.equals(credentials.getNonceHash())) {
			throw new AuthenticationException("Invalid nonce hash");
		}
		
		Password<?, ?> resolvedPassword = otherCredentials.getPassword();
		String hash_a1;
		if(resolvedPassword instanceof RawPassword) {
			hash_a1 = DigestUtils.h(credentials.getDigest(), credentials.getUsername() + ":" + credentials.getRealm() + ":" + resolvedPassword.getValue());
		}
		else if(resolvedPassword instanceof DigestPassword) {
			DigestPassword resolvedDigestPassword = (DigestPassword)resolvedPassword;
			if(!resolvedDigestPassword.getEncoder().getAlgorithm().equals(credentials.getDigest().getAlgorithm())) {
				throw new AuthenticationException("Algorithm does not match credentials algorithm");
			}
			if(resolvedDigestPassword.getEncoder().getRealm().equals(credentials.getRealm())) {
				throw new AuthenticationException("Realm does not match credentials realm");
			}
			hash_a1 = resolvedPassword.getValue();
		}
		else {
			throw new AuthenticationException("Resolved credentials are not compatible with digest authentication scheme");
		}
		
		String hash_a2 = DigestUtils.h(credentials.getDigest(), credentials.getMethod() + ":" + credentials.getUri());
		
		final String serverDigest;
		if(null == credentials.getQop()) {
			// https://datatracker.ietf.org/doc/html/rfc2617#section-3.2.2
			// request-digest = <"> < KD ( H(A1), unq(nonce-value) ":" H(A2) ) >
			serverDigest = DigestUtils.kd(credentials.getDigest(), hash_a1, credentials.getNonce() + ":" + hash_a2);
		}
		else switch (credentials.getQop()) {
			case "auth": {
				// https://datatracker.ietf.org/doc/html/rfc7616#section-3.4.1
				// response = <"> < KD ( H(A1), unq(nonce) ":" nc ":" unq(cnonce) ":" unq(qop) ":" H(A2) ) <">
				// A1 = unq(username) ":" unq(realm) ":" passwd
				// A2 = Method ":" request-uri
				// KD(secret, data) = H(concat(secret, ":", data))

				serverDigest = DigestUtils.kd(credentials.getDigest(), hash_a1, credentials.getNonce() + ":" + credentials.getNc() + ":" + credentials.getCnonce() + ":" + credentials.getQop() + ":" + hash_a2);
				break;
			}
			default:
				throw new AuthenticationException("Unsupported qop: " + credentials.getQop());
		}
		return serverDigest.equals(credentials.getResponse());
	}
}
