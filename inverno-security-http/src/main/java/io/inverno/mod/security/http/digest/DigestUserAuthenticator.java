/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.digest;

import io.inverno.mod.security.Authentication;
import io.inverno.mod.security.AuthenticationException;
import io.inverno.mod.security.Authenticator;
import io.inverno.mod.security.CredentialsResolver;
import io.inverno.mod.security.InvalidCredentialsException;
import io.inverno.mod.security.UserCredentials;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
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
