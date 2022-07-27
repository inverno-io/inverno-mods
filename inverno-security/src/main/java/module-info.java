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
import io.inverno.mod.security.SecurityManager;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;

/**
 * <p>
 * The Inverno framework security module provides general support to secure access to protected service or resources in an application.
 * </p>
 * 
 * <p>
 * An Inverno application divides the security process into three parts:
 * </p>
 * 
 * <dl>
 * <dt><b>Authentication</b></dt>
 * <dd>which is the process of authenticating a request or the entity behind it by validating credentials.</dd>
 * <dt><b>Identification</b></dt>
 * <dd>which is the process of identifying the authenticated entity.</dd>
 * <dt><b>Access control</b></dt>
 * <dd>which is the process of controling the access to protected services or resources for the authenticated entity.</dd>
 * </dl>
 * 
 * <p>
 * The entity represents the originator of an access to the application, it can be external or internal. An application can secure the access to protected services or resources by authenticating the 
 * entity which must provide valid credentials. An {@link AccessController} can then be obtained from the resulting {@link Authentication} to fine-grained control the access to services and 
 * resources. The {@link Identity} of the authenticated entity might also be resolved from the authentication and used whenever strong identification is required.
 * </p>
 *
 * <p>
 * It is important to understand that {@link Authentication}, {@link Identity} and {@link AccessController} are decorrelated even though they are all related to the authenticated entity. We can have 
 * an authentication without identification (e.g. OAuth2) and/or without access controller. 
 * </p>
 * 
 * <p>
 * The {@link SecurityContext} is the central security component in an application, it is composed of above components and it should be created and used whenever there is a need to protect services 
 * or resources. It allows to verify whether an authenticated entity is accessing a protected service or resource and control whether the access should be granted based on its roles, permissions or 
 * any other access control method.
 * </p>
 * 
 * <p>
 * A {@link SecurityManager} can be used to authenticate credentials and create a security context for an entity, it is built from an {@link Authenticator}, an {@link IdentityResolver} and an
 * {@link AccessControllerResolver}.
 * </p>
 * 
 * <p>
 * The following is a complete example of how to authenticate an entity and obtain a security context to secure an application:
 * </p>
 * 
 * <pre>{@code
 *     SecurityManager<LoginCredentials, PersonIdentity, RoleBasedAccessController> securityManager = SecurityManager.of(
 *         // The authenticator used to authenticate login credentials
 *         new UserAuthenticator<>(
 *             // The in-memory user repository initialized with one user (password is encoded before it is stored in the repository)
 *             InMemoryUserRepository
 *                 .of(List.of(
 *                     User.of("jsmith")
 *                         .identity(new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io"))
 *                         .password(new RawPassword("password"))
 *                         .groups("readers")
 *                         .build()
 *                     ))
 *                     .build(),
 *               // The login matcher used to match the credentials to authenticate with the stored credentials
 *               new LoginCredentialsMatcher<>()
 *         ),
 *         // The identity resolver used to resolve the identity from the authentication
 *         new UserIdentityResolver<>(),
 *         // The access controller resolver used to resolve the access controller from the authentication
 *         new GroupsRoleBasedAccessControllerResolver()
 *     );
 *     
 *     // Authenticating credentials and create the security context
 *     SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = securityManager.authenticate(LoginCredentials.of("jsmith", new RawPassword("password"))).block();
 *     
 *     // Secure the application
 *     if(securityContext.isAuthenticated()) {
 *         // Do something usefull with identity...
 *         securityContext.getIdentity().ifPresent(identity -> System.out.println("Hello " + identity.getFirstName() + " " + identity.getLastName()));
 *     
 *         // Access control
 *         if(securityContext.getAccessController().orElseThrow().hasRole("readers").block()) {
 *             // Authenticated user has 'readers' role...
 *         }
 *         else {
 *             // unauthorized access...
 *             throw new AccessControlException("Unauthorized access");
 *         }
 *     }    }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
module io.inverno.mod.security {
	requires static io.inverno.mod.configuration;
	requires static io.inverno.mod.redis;
	
	requires com.fasterxml.jackson.databind;
	requires org.apache.commons.lang3;
	requires org.apache.logging.log4j;
	requires static org.bouncycastle.provider;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	
	exports io.inverno.mod.security.internal.authentication to com.fasterxml.jackson.databind;
	exports io.inverno.mod.security.internal.authentication.user to com.fasterxml.jackson.databind;
	
	exports io.inverno.mod.security;
	exports io.inverno.mod.security.accesscontrol;
	exports io.inverno.mod.security.authentication;
	exports io.inverno.mod.security.authentication.password;
	exports io.inverno.mod.security.authentication.user;
	exports io.inverno.mod.security.context;
	exports io.inverno.mod.security.identity;
}
