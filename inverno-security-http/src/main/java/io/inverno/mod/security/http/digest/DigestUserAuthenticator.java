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

import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.CredentialsResolver;
import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.UserCredentials;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class DigestUserAuthenticator implements Authenticator<DigestCredentials, Authentication> {

	private final CredentialsResolver<UserCredentials> credentialsResolver;
	
	private final String secret;
	
	// Requires a user database as we must hash the password
	// in case userhash is true we must be able to fetch the user from a hash: username = H( unq(username) ":" unq(realm) )
	// we need to create a UsernamePasswordRepository which returns users by username and by hash
	
	public DigestUserAuthenticator(CredentialsResolver<UserCredentials> credentialsResolver, String secret) {
		this.credentialsResolver = credentialsResolver;
		this.secret = secret;
	}

	@Override
	public Mono<Authentication> authenticate(DigestCredentials credentials) throws AuthenticationException {
		return this.credentialsResolver.resolveCredentials(credentials.getUsername())
			.map(resolvedCredentials -> {
				if(credentials.getNonceExpire() < System.nanoTime()){
					throw new ExpiredNonceException("Nonce has expired");
				}
				
				String serverNonceHash = kd(credentials.getDigest(), this.secret, Long.toString(credentials.getNonceExpire()));
				if(!serverNonceHash.equals(credentials.getNonceHash())) {
					throw new AuthenticationException("Invalid nonce hash");
				}
				
				// TODO password is in clear text here which is bad, an hashed password should be stored and resolved, 
				// ideally a salt should be used as well, but this might be complicated as the client only has the password
				String hash_a1 = h(credentials.getDigest(), credentials.getUsername() + ":" + credentials.getRealm() + ":" + resolvedCredentials.getPassword());
				String hash_a2 = h(credentials.getDigest(), credentials.getMethod() + ":" + credentials.getUri());
				
				final String serverDigest;
				if(null == credentials.getQop()) {
					// https://datatracker.ietf.org/doc/html/rfc2617#section-3.2.2
					// request-digest = <"> < KD ( H(A1), unq(nonce-value) ":" H(A2) ) >
					serverDigest = kd(credentials.getDigest(), hash_a1, credentials.getNonce() + ":" + hash_a2);
				}
				else switch (credentials.getQop()) {
					case "auth": {
						// https://datatracker.ietf.org/doc/html/rfc7616#section-3.4.1
						// response = <"> < KD ( H(A1), unq(nonce) ":" nc ":" unq(cnonce) ":" unq(qop) ":" H(A2) ) <">
						// A1 = unq(username) ":" unq(realm) ":" passwd
						// A2 = Method ":" request-uri
						// KD(secret, data) = H(concat(secret, ":", data))

						serverDigest = kd(credentials.getDigest(), hash_a1, credentials.getNonce() + ":" + credentials.getNc() + ":" + credentials.getCnonce() + ":" + credentials.getQop() + ":" + hash_a2);
						break;
					}
					default:
						throw new AuthenticationException("Unsupported qop: " + credentials.getQop());
				}
				
				if(serverDigest.equals(credentials.getResponse())) {
					return Authentication.granted();
				}
				else {
					throw new InvalidCredentialsException();
				}
			});
	}
	
	private String h(MessageDigest digest, String data) {
		return Hex.encodeHexString(digest.digest(data.getBytes()));
	}
	
	private String kd(MessageDigest digest, String secret, String data) {
		return Hex.encodeHexString(digest.digest((secret + ":" + data).getBytes()));
	}
}
