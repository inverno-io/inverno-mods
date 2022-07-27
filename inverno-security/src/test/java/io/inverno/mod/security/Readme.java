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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.redis.RedisClient;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.accesscontrol.ConfigurationSourcePermissionBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.PermissionBasedAccessController;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.CredentialsMatcher;
import io.inverno.mod.security.authentication.CredentialsResolver;
import io.inverno.mod.security.authentication.GroupAwareAuthentication;
import io.inverno.mod.security.authentication.InMemoryLoginCredentialsResolver;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.PrincipalAuthentication;
import io.inverno.mod.security.authentication.password.BCryptPassword;
import io.inverno.mod.security.authentication.password.Password;
import io.inverno.mod.security.authentication.password.PasswordPolicy;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.password.SimplePasswordPolicy;
import io.inverno.mod.security.authentication.user.RedisUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthentication;
import io.inverno.mod.security.authentication.user.UserRepository;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {
	
	private static final Logger LOGGER = LogManager.getLogger(Readme.class);
	
	public void doc() {
		
		
		
		CredentialsResolver<LoginCredentials> credentialsResolver = username -> Mono.fromSupplier(() -> {
			switch(username) {
				case "user1": return LoginCredentials.of("user1", new BCryptPassword.Encoder().encode("password1"));
				case "user2": return LoginCredentials.of("user2", new BCryptPassword.Encoder().encode("password2"));
				default: return null;
			}
		});
		
		InMemoryLoginCredentialsResolver inMemoryLoginCredentialsResolver = new InMemoryLoginCredentialsResolver(List.of(LoginCredentials.of("user1", new RawPassword("password"))));
		inMemoryLoginCredentialsResolver.put("user2", new RawPassword("password"));
		inMemoryLoginCredentialsResolver.remove("user1");
		
		UserRepository<PersonIdentity, User<PersonIdentity>> userRepository = null;
		
		// Create a user with Identity and groups
		userRepository.createUser(new User<>("jsmith", new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io"), new RawPassword("password"), "group1", "group2"));
		
		// Update user email
		userRepository.getUser("jsmith")
			.doOnNext(user -> user.getIdentity().setEmail("jsmith1@inverno.io"))
			.map(userRepository::updateUser)
			.block();
		
		// Password change requires current credentials
		userRepository.changePassword(LoginCredentials.of("jsmith", new RawPassword("password")), "newPassword");
		
		// Delete user
		userRepository.deleteUser("jsmith").block();
		
		PasswordPolicy<LoginCredentials, SimplePasswordPolicy.SimplePasswordStrength> passwordPolicy = new SimplePasswordPolicy<>(4, 8);
		
		// Throws a PasswordPolicyException since 'newPassword' is too long
//		SimplePasswordPolicy.SimplePasswordStrength passwordStrength = passwordPolicy.verify(LoginCredentials.of("jsmith", new RawPassword("password")), "newPassword");
		
		// Returns a passwordStrength
		SimplePasswordPolicy.SimplePasswordStrength passwordStrength = passwordPolicy.verify(LoginCredentials.of("jsmith", new RawPassword("password")), "newPassword");
		
		// WEAK, MEDIUM, STRONG...
		passwordStrength.getQualifier();
		
		// 10, 42, 100... The higher the better
		passwordStrength.getScore();
		
		
		RedisClient<String, String> redisClient = null;
		ObjectMapper mapper = null;
				
		UserRepository<PersonIdentity, User<PersonIdentity>> redisUserRepository = new RedisUserRepository<>(redisClient, mapper, new BCryptPassword.Encoder(8, 32), new SimplePasswordPolicy<>(10,20) );
		
		CredentialsMatcher<LoginCredentials, LoginCredentials> credentialsMatcher = (credentials, trustedCredentials) -> {
			return credentials.getPassword().matches(trustedCredentials.getPassword());
		};
		
		CredentialsMatcher<LoginCredentials, User<PersonIdentity>> credentialsMatcher2 = new LoginCredentialsMatcher();
		
		IdentityResolver<PrincipalAuthentication, PersonIdentity> identityResolver = authentication -> {
			// The authentication is a proof of authentication, we can assume valid credentials have been provided
			String authenticatedUsername = authentication.getUsername();
			
			// Retrieve user identity from a reactive data source using the authenticated username
			Mono<PersonIdentity> identity = null;
			
			return identity;
		};
		
		IdentityResolver<UserAuthentication<PersonIdentity>, PersonIdentity> identityResolver2 = new UserIdentityResolver<UserAuthentication<PersonIdentity>, PersonIdentity>();
		
		AccessControllerResolver<PrincipalAuthentication, RoleBasedAccessController> accessControllerResolver = authentication -> {
			// The authentication is a proof of authentication, we can assume valid credentials have been provided
			String authenticatedUsername = authentication.getUsername();
			
			// Retrieve the role of the authenticated entity from a reactive data source using the authenticated username
			Mono<Set<String>> roles = null;
			
			return roles.map(RoleBasedAccessController::of);
		};
		
		AccessControllerResolver<GroupAwareAuthentication, RoleBasedAccessController> accessControllerResolver2 = new GroupsRoleBasedAccessControllerResolver();
		
		UserAuthentication<PersonIdentity> auth = null;
		
		accessControllerResolver2.resolveAccessController(auth);
		
		// Configuration source defining permissions by user
		ConfigurationSource<?,?,?> configurationSource = null;
		
		ConfigurationSourcePermissionBasedAccessControllerResolver accessControllerResolver3 = new ConfigurationSourcePermissionBasedAccessControllerResolver(configurationSource);
	}
	
	public void securityContext() {
		
		Authentication authentication = Authentication.granted();
		PersonIdentity identity = new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io");
		RoleBasedAccessController accessController = RoleBasedAccessController.of("reader", "writer");
		
		SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = SecurityContext.of(authentication, identity, accessController);
		
		if(securityContext.getAuthentication().isAuthenticated()) {
			// Use access controller to secure services and resources
			// Use identity to get information about the authenticated entity
		}
		else if(securityContext.getAuthentication().isAnonymous()) {
			// Application is accessed anonymously
		}
		else {
			// Authentication failed
			LOGGER.error(securityContext.getAuthentication().getCause().get());
		}
		
		
		if(securityContext.isAuthenticated()) {
			// Use access controller to secure services and resources
			// Use identity to get information about the authenticated entity
		}
		else if(securityContext.isAnonymous()) {
			// Application is accessed anonymously
		}
		else {
			// Authentication failed
			LOGGER.error(securityContext.getAuthentication().getCause().get());
		}
		
		
		securityContext.getIdentity().ifPresentOrElse(
			identity2 -> {
				// Send an email to the authenticated user
				String email = identity2.getEmail();
			}, 
			() -> {
				LOGGER.warn("Unable to send email: missing identity");
			}
		);
		
		Mono<String> protectedReactiveService = securityContext.getAccessController()
			.map(accessController2 -> accessController2
					.hasRole("reader")
					.map(hasRole -> {
						if(!hasRole) {
							// throw new ForbiddenException();
							throw new RuntimeException();
						}
						// User is authorized: do something useful
						return "User is a reader";
					})
			)
			.orElseThrow(() -> {
//				return new InternalServerErrorException("Missing access controller");
				return new RuntimeException("Missing access controller");
			});
		
		securityContext.getAccessController()
			.ifPresent(accessController2 -> {
				// Returns true if the authenticated user has role 'reader'
				Mono<Boolean> canRead = accessController2.hasRole("reader");
				
				// Returns true if the authenticated user has any of the roles: 'writer', 'admin'
				Mono<Boolean> canWrite = accessController2.hasAnyRole("writer", "admin");
				
				// Returns true if the authenticated user has all of the roles: 'reader', 'writer'
				Mono<Boolean> canReadAndWrite = accessController2.hasAllRoles("reader", "writer");
			});
	}
	
	public void permissionBasedAccessController() {
		SecurityContext<PersonIdentity, PermissionBasedAccessController> securityContext = null;

		securityContext.getAccessController()
			.ifPresent(accessController -> {
				// Returns true if the authenticated user has permission read
				Mono<Boolean> canRead = accessController.hasPermission("read");
			
				// Returns true if the authenticated user has permission read on 'contract' documents
				Mono<Boolean> canReadContracts = accessController.hasPermission("read", PermissionBasedAccessController.Parameter.of("documentType", "contract"));
				
				// Returns true if the authenticated user has permission 'manage' or 'admin'
				Mono<Boolean> canManagePrinter = accessController.hasAnyPermission(Set.of("manage", "admin"));
				
				// Returns true if the authenticated user has permission can manage printer 'lp1200'
				Mono<Boolean> canManagePrinterLP1200 = accessController.hasAnyPermission(Set.of("manage", "admin"), PermissionBasedAccessController.Parameter.of("printer", "lp1200"));
				
				// Returns true if the authenticated user can book and modify 'AF' flights from 'Orly' airport
				Mono<Boolean> canBookAndModify = accessController.hasAllPermissions(Set.of("book", "modify"), PermissionBasedAccessController.Parameter.of("company", "AF"), PermissionBasedAccessController.Parameter.of("origin", "ORY"));
			});
		
	}
}
