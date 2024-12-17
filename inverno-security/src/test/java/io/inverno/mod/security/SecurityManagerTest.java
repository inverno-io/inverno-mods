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
package io.inverno.mod.security;

import io.inverno.mod.security.accesscontrol.AccessControlException;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class SecurityManagerTest {

	@Test
	public void test() {
		SecurityManager<LoginCredentials, PersonIdentity, RoleBasedAccessController> securityManager = SecurityManager.of(
			new UserAuthenticator<>(
				InMemoryUserRepository
					.of(List.of(
						User.of("jsmith")
							.identity(new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io"))
							.password(new RawPassword("password"))
							.groups("readers")
							.build()
					))
					.build(),
				new LoginCredentialsMatcher<>()
			),
			new UserIdentityResolver<>(),
			new GroupsRoleBasedAccessControllerResolver()
		);
		
		SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = securityManager.authenticate(LoginCredentials.of("jsmith", new RawPassword("password"))).block();
		
		Assertions.assertNotNull(securityContext);
		Assertions.assertTrue(securityContext.getIdentity().isPresent());
		Assertions.assertTrue(securityContext.getAccessController().isPresent());
		Assertions.assertTrue(securityContext.isAuthenticated());
		
		if(securityContext.isAuthenticated()) {
			// Do something usefull with identity...
			securityContext.getIdentity().ifPresent(identity -> {
				Assertions.assertEquals("John", identity.getFirstName());
				Assertions.assertEquals("Smith", identity.getLastName());
				Assertions.assertEquals("jsmith@inverno.io", identity.getEmail());
			});
			
			// Access control
			if(securityContext.getAccessController().orElseThrow().hasRole("readers").block()) {
				// Authenticated user has 'readers' role...
			}
			else {
				// unauthorized access...
				Assertions.fail("User should have readers role");
				throw new AccessControlException("Unauthorized access");
			}
		}
		
		securityContext = securityManager.authenticate(LoginCredentials.of("invalid", new RawPassword("credentials"))).block();
		
		Assertions.assertFalse(securityContext.isAuthenticated());
		Assertions.assertFalse(securityContext.getIdentity().isPresent());
		Assertions.assertFalse(securityContext.getAccessController().isPresent());
		
		securityContext = securityManager.authenticate(LoginCredentials.of("jsmith", new RawPassword("invalid"))).block();
		
		Assertions.assertFalse(securityContext.isAuthenticated());
		Assertions.assertFalse(securityContext.getIdentity().isPresent());
		Assertions.assertFalse(securityContext.getAccessController().isPresent());
		
		Assertions.assertTrue(securityContext.getAuthentication().getCause().isPresent());
		Assertions.assertInstanceOf(InvalidCredentialsException.class, securityContext.getAuthentication().getCause().get());
		Assertions.assertEquals("Invalid credentials", securityContext.getAuthentication().getCause().get().getMessage());
		
		securityContext = securityManager.authenticate(null).block();
		Assertions.assertFalse(securityContext.isAuthenticated());
		Assertions.assertFalse(securityContext.getIdentity().isPresent());
		Assertions.assertFalse(securityContext.getAccessController().isPresent());
		
		Assertions.assertFalse(securityContext.getAuthentication().getCause().isPresent());
		Assertions.assertEquals(Authentication.anonymous(), securityContext.getAuthentication());
	}
}
