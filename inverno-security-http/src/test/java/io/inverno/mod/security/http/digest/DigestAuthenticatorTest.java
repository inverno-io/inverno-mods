/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package io.inverno.mod.security.http.digest;

import io.inverno.mod.security.Authentication;
import io.inverno.mod.security.UserCredentials;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class DigestAuthenticatorTest {
	
	@Test
	public void testAuthenticate() {
		String username = "Mufasa";
		String password = "Circle of Life";
		
		DigestUserAuthenticator authenticator = new DigestUserAuthenticator(ign -> Mono.just(new UserCredentials("user", "password")), "secret");
		
		/*
		 * username="user", 
		 * realm="inverno", 
		 * nonce="MTIzNzI2MTk1MzM2Mzg6YjBiZGQwYjQ5YjUyNjRiYTcyODU0YmExYTI1NjkyOTNhNWQ4ZWE0MTViZWZmMjhiOGE5MjVlN2U4YjRmMThhMw==", 
		 * uri="/digest/hello", 
		 * response="d00aee3b043113da65957dbb023d78c5", 
		 * qop=auth,
		 * nc=00000001,
		 * cnonce="e6990262408a5a1d"
		 */
		
		// TODO MD5
		
//		public DigestCredentials(String method, String uri, String realm, String username, boolean userhash, String algorithm, String qop, String nc, String cnonce, String nonce, String response) throws MalformedCredentialsException {
		
		/*DigestCredentials credentials = new DigestCredentials("GET", "/digest/hello", "inverno", "user", false, "SHA-256", "auth", "00000001", "e6990262408a5a1d", "MTIzNzI2MTk1MzM2Mzg6YjBiZGQwYjQ5YjUyNjRiYTcyODU0YmExYTI1NjkyOTNhNWQ4ZWE0MTViZWZmMjhiOGE5MjVlN2U4YjRmMThhMw==", "d00aee3b043113da65957dbb023d78c5");
		
		Authentication authentication = authenticator.authenticate(credentials).block();
	
		Assertions.assertTrue(authentication.isAuthenticated());*/
	}
}
