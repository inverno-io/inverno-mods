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
package io.inverno.mod.security.authentication.user;

import io.inverno.mod.security.authentication.AuthenticationException;
import io.inverno.mod.security.authentication.CredentialsResolver;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.PrincipalAuthenticator;
import io.inverno.mod.security.authentication.password.PasswordException;
import io.inverno.mod.security.authentication.password.PasswordPolicyException;
import io.inverno.mod.security.identity.Identity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A repository used to store, access and manage users.
 * </p>
 * 
 * <p>
 * It is used in a {@link UserAuthenticator} to resolve users during the authentication process. Since it also implements {@link CredentialsResolver}, it can also be used in the more generic
 * {@link PrincipalAuthenticator}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 * @param <B> the user type
 */
public interface UserRepository<A extends Identity, B extends User<A>> extends CredentialsResolver<B> {
	
	/**
	 * <p>
	 * Creates a user.
	 * </p>
	 * 
	 * @param user the user to create
	 * 
	 * @return a mono emitting the created user
	 * 
	 * @throws UserRepositoryException if there was an error creating the user
	 */
	Mono<B> createUser(B user) throws UserRepositoryException;
	
	/**
	 * <p>
	 * Returns the user identified by the specified username.
	 * </p>
	 * 
	 * @param username a username
	 * 
	 * @return a mono emittin the user or an empty mono if no user exists with the specified name
	 * 
	 * @throws UserRepositoryException if there was an error fetchin the user
	 */
	Mono<B> getUser(String username) throws UserRepositoryException;
	
	/**
	 * <p>
	 * Lists the users in the repository.
	 * </p>
	 * 
	 * @return a publisher of users
	 * 
	 * @throws UserRepositoryException if there was an error fetching users
	 */
	Flux<B> listUsers() throws UserRepositoryException;
	
	/**
	 * <p>
	 * Updates the specified user.
	 * </p>
	 * 
	 * <p>
	 * Note that this method does not update password nor groups and can not be used to lock a user, adhoc methods 
	 * {@link #changePassword(io.inverno.mod.security.authentication.LoginCredentials, java.lang.String) }, {@link #addUserToGroups(java.lang.String, java.lang.String...) }, 
	 * {@link #lockUser(java.lang.String) } must be used instead.
	 * </p>
	 * 
	 * @param user the user to update
	 * 
	 * @return a mono emitting the updated user
	 * 
	 * @throws UserRepositoryException if there was an error updating the user
	 */
	Mono<B> updateUser(B user) throws UserRepositoryException;
	
	/**
	 * <p>
	 * Changes the password of the user identified by the specified credentials.
	 * </p>
	 *
	 * <p>
	 * Implementors must make sure the provided credentials are valid before actually updating the password. Whether a full authentiation is performed or a simple password match is implementation
	 * specific.
	 * </p>
	 *
	 * @param credentials the current login credentials of the user for which password must be changed
	 * @param rawPassword the new raw password value
	 *
	 * @return a mono emitting the updated user
	 *
	 * @throws AuthenticationException if there was an error authenticating the credentials
	 * @throws PasswordPolicyException if the new password is not compliant with the password policy
	 * @throws PasswordException       if there was an error processing the new password
	 * @throws UserRepositoryException if there was an error updating the user
	 */
	Mono<B> changePassword(LoginCredentials credentials, String rawPassword) throws AuthenticationException, PasswordPolicyException, PasswordException, UserRepositoryException;
	
	/**
	 * <p>
	 * Locks the user identified by the specified username.
	 * </p>
	 * 
	 * @param username the name of the user to lock
	 * 
	 * @return a mono emitting the updated user
	 * 
	 * @throws UserRepositoryException if there was an error updating the user
	 */
	Mono<B> lockUser(String username) throws UserRepositoryException;
	
	/**
	 * <p>
	 * Unlocks the user identified by the specified username.
	 * </p>
	 * 
	 * @param username the name of the user to unlock
	 * 
	 * @return a mono emitting the updated user
	 * 
	 * @throws UserRepositoryException if there was an error updating the user
	 */
	Mono<B> unlockUser(String username) throws UserRepositoryException;
	
	/**
	 * <p>
	 * Adds the user identified by the specified username to the specified groups.
	 * </p>
	 * 
	 * @param username a username
	 * @param groups a list of groups
	 * 
	 * @return a mono emitting the updated user
	 * 
	 * @throws UserRepositoryException if there was an error updating the user
	 */
	Mono<B> addUserToGroups(String username, String... groups) throws UserRepositoryException;
	
	/**
	 * <p>
	 * Removes the user identified by the specified username from the specified groups.
	 * </p>
	 * 
	 * @param username a username
	 * @param groups a list of groups
	 * 
	 * @return a mono emitting the updated user
	 * 
	 * @throws UserRepositoryException if there was an error updating the user
	 */
	Mono<B> removeUserFromGroups(String username, String... groups) throws UserRepositoryException;
	
	/**
	 * <p>
	 * Deletes the he user identified by the specified username from the repository.
	 * </p>
	 * 
	 * @param username a username
	 * 
	 * @return a mono emitting the deleted user or an empty mono if no user exists with the specified name
	 * 
	 * @throws UserRepositoryException if there was an error deleting the user
	 */
	Mono<B> deleteUser(String username) throws UserRepositoryException;
}
