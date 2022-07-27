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
package io.inverno.mod.security.authentication;

import io.inverno.mod.security.SecurityException;
import io.inverno.mod.security.authentication.password.MessageDigestPassword;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthentication;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.identity.Identity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class AuthenticatorTest {
	
	@Test
	public void testComposition() {
		Authenticator<LoginCredentials, Authentication> authenticator1 = credentials -> Mono.fromSupplier(() -> {
			if(credentials.getUsername().equals("user1")) {
				if(credentials.getPassword().matches("password")) {
					return Authentication.granted();
				}
				// Claim the credentials and terminate the chain
				return Authentication.denied();
			}
			// Delegate to next authenticator in the chain
			return null;
		});
		
		authenticator1.map(authentication -> {
			final String token = UUID.randomUUID().toString();
			return new TokenAuthentication() {
				@Override
				public String getToken() {
					return token;
				}

				@Override
				public boolean isAuthenticated() {
					return authentication.isAuthenticated();
				}

				@Override
				public Optional<SecurityException> getCause() {
					return authentication.getCause();
				}
			};
		});
		
		
		Authenticator<LoginCredentials, Authentication> authenticator2 = credentials -> Mono.fromSupplier(() -> {
			if (credentials.getUsername().equals("user2") && credentials.getPassword().matches("password")) {
				return Authentication.granted();
			}
			return Authentication.denied();
		});
		
		Authenticator<LoginCredentials, Authentication> compositeAuthenticator = authenticator1.or(authenticator2);

		// A granted authentication is returned by authenticator1
		Assertions.assertTrue(compositeAuthenticator.authenticate(LoginCredentials.of("user1", new RawPassword("password"))).block().isAuthenticated());
		
		// A denied authentication is returned by authenticator2 which claimed the credentials
		Assertions.assertFalse(compositeAuthenticator.authenticate(LoginCredentials.of("user1", new RawPassword("invalid"))).block().isAuthenticated());
		
		// A granted authentication is returned by authenticator2
		Assertions.assertTrue(compositeAuthenticator.authenticate(LoginCredentials.of("user2", new RawPassword("password"))).block().isAuthenticated());
		
		// A denied authentication is returned by authenticator2 which is terminal
		Assertions.assertFalse(compositeAuthenticator.authenticate(LoginCredentials.of("user2", new RawPassword("invalid"))).block().isAuthenticated());
		
		// A denied authentication is returned by authenticator2 which is terminal
		Assertions.assertFalse(compositeAuthenticator.authenticate(LoginCredentials.of("unknown", new RawPassword("password"))).block().isAuthenticated());
		
		UserAuthenticator<LoginCredentials, Identity, User<Identity>> userAuthenticator = new UserAuthenticator<>(
			InMemoryUserRepository
				.of(List.of(
					User.of("jsmith")
						.password(new RawPassword("password"))
						.build(),
					User.of("adoe")
						.password(new RawPassword("password"))
						.build()
				))
				.build(),
			new LoginCredentialsMatcher<>()
		);
		userAuthenticator.setTerminal(false);
		
		Authenticator<LoginCredentials, UserAuthentication<Identity>> simpleAuthenticator = credentials -> Mono.fromSupplier(() -> {
			if(credentials.getUsername().equals("user")) {
				if(credentials.getPassword() instanceof RawPassword) {
					if(credentials.getPassword().getValue().equals("password")) {
						return UserAuthentication.of("user");
					}
					else {
						throw new AuthenticationException("Invalid credentials");
					}
				}
				else {
					return null;
				}
			}
			return null;
		});
		
		Authenticator<LoginCredentials, UserAuthentication<Identity>> composedAuthenticator = userAuthenticator.or(simpleAuthenticator);
		
		UserAuthentication<Identity> authentication = composedAuthenticator.authenticate(LoginCredentials.of("jsmith", new RawPassword("password"))).block();
		Assertions.assertTrue(authentication.isAuthenticated());
		Assertions.assertEquals("jsmith", authentication.getUsername());
		
		authentication = composedAuthenticator.authenticate(LoginCredentials.of("jsmith", new RawPassword("invalid"))).block();
		Assertions.assertNull(authentication);
		
		authentication = composedAuthenticator.authenticate(LoginCredentials.of("user", new RawPassword("password"))).block();
		Assertions.assertTrue(authentication.isAuthenticated());
		Assertions.assertEquals("user", authentication.getUsername());
		
		authentication = composedAuthenticator.authenticate(LoginCredentials.of("user", new MessageDigestPassword.Encoder().encode("password"))).block();
		Assertions.assertNull(authentication);
		
		Assertions.assertEquals("Invalid credentials", Assertions.assertThrows(AuthenticationException.class, () -> composedAuthenticator.authenticate(LoginCredentials.of("user", new RawPassword("invalid"))).block()).getMessage());
		
		authentication = composedAuthenticator.authenticate(LoginCredentials.of("unknown", new RawPassword("password"))).block();
		Assertions.assertNull(authentication);
	}
}
