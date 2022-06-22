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
package io.inverno.mod.security.context;

import io.inverno.mod.security.accesscontrol.AccessControlException;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.GroupAwareAuthentication;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthentication;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.authentication.user.UserRepository;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class SecurityContextTest {

	@Test
	public void test() {
		// Use an in-memory user repository initialized with one user (password is encoded before it is stored in the repository)
		UserRepository<PersonIdentity, User<PersonIdentity>> userRepository = InMemoryUserRepository
			.of(List.of(
				User.of("jsmith")
					.identity(new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io"))
					.password(new RawPassword("password"))
					.groups("readers")
					.build()
			))
			.build();
		
		// The authenticator used to authenticate login credentials, application users are stored in a user repository, a login matcher is used to match the provided credentials with the stored credentials
		UserAuthenticator<LoginCredentials, PersonIdentity, User<PersonIdentity>> authenticator = new UserAuthenticator<>(userRepository, new LoginCredentialsMatcher<>());

		// The identity resolver used to resolve the identity from the authentication
		IdentityResolver<UserAuthentication<PersonIdentity>, PersonIdentity> identityResolver = new UserIdentityResolver<>();
		
		// The access controller resolver used to resolve the access controller from the authentication
		AccessControllerResolver<GroupAwareAuthentication, RoleBasedAccessController> accessControllerResolver = new GroupsRoleBasedAccessControllerResolver();
		
		// Resolve the security context
		SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = authenticator
			.authenticate(LoginCredentials.of("jsmith", new RawPassword("password")))
			.flatMap(authentication -> Mono.zip(identityResolver.resolveIdentity(authentication), accessControllerResolver.resolveAccessController(authentication))
					.map(tuple -> SecurityContext.of(authentication, tuple.getT1(), tuple.getT2()))
			)
			.block();

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
	}
}
