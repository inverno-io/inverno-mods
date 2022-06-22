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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.GroupAwareAuthentication;
import io.inverno.mod.security.authentication.PrincipalAuthentication;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import io.inverno.mod.security.internal.authentication.user.GenericUserAuthentication;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * An authentication resulting from the authentication of a user uniquely identified by a username in a {@link UserAuthenticator}.
 * </p>
 * 
 * <p>
 * It extends the {@link PrincipalAuthentication} by exposing the identity of the authenticated user (if any) and the groups it belongs to. This implementation is then allows to create a complete
 * {@link SecurityContext} with an {@link Authentication}, an {@link Identity} and an {@link AccessController} (i.e. {@link RoleBasedAccessController}).
 * </p>
 * 
 * <p>
 * For example, an application could then build its security context using a {@link UserAuthenticator}, a {@link UserIdentityResolver} and a {@link GroupsRoleBasedAccessControllerResolver}. Note that
 * the API is flexible and allow other combinations as well.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the identity type
 */
@JsonDeserialize( as = GenericUserAuthentication.class )
public interface UserAuthentication<A extends Identity> extends PrincipalAuthentication, GroupAwareAuthentication {
	
	/**
	 * <p>
	 * Returns the user's identity.
	 * </p>
	 * 
	 * @return the user identity or null if none was resolved during the authentication process.
	 */
	@JsonProperty( "identity" )
	A getIdentity();
	
	/**
	 * <p>
	 * Creates a user authentication with the specified name and groups.
	 * </p>
	 * 
	 * <p>
	 * This is a conveninence method that should be used with care and only used after a successful authentication to generate the resulting authentication.
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param username a username
	 * @param groups an array of groups
	 * 
	 * @return a new user authentication
	 */
	static <A extends Identity> UserAuthentication<A> of(String username, String... groups) {
		return new GenericUserAuthentication<>(username, null, Arrays.stream(groups).filter(Objects::nonNull).collect(Collectors.toSet()), true);
	}
	
	/**
	 * <p>
	 * Creates a user authentication with the specified name and groups.
	 * </p>
	 * 
	 * <p>
	 * This is a conveninence method that should be used with care and only used after a successful authentication to generate the resulting authentication.
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param username a username
	 * @param groups a collection of groups
	 * 
	 * @return a new user authentication
	 */
	static <A extends Identity> UserAuthentication<A> of(String username, Collection<String> groups) {
		return new GenericUserAuthentication<>(username, null, groups.stream().filter(Objects::nonNull).collect(Collectors.toSet()), true);
	}
	
	/**
	 * <p>
	 * Creates a user authentication with the specified name, identity and groups.
	 * </p>
	 * 
	 * <p>
	 * This is a conveninence method that should be used with care and only used after a successful authentication to generate the resulting authentication.
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param username a username
	 * @param identity the identity of the user
	 * @param groups an array of groups
	 * 
	 * @return a new user authentication
	 */
	static <A extends Identity> UserAuthentication<A> of(String username, A identity, String... groups) {
		return new GenericUserAuthentication<>(username, identity, Arrays.stream(groups).filter(Objects::nonNull).collect(Collectors.toSet()), true);
	}
	
	/**
	 * <p>
	 * Creates a user authentication with the specified name, identity and groups.
	 * </p>
	 * 
	 * <p>
	 * This is a conveninence method that should be used with care and only used after a successful authentication to generate the resulting authentication.
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param username a username
	 * @param identity the identity of the user
	 * @param groups a collection of groups
	 * 
	 * @return a new user authentication
	 */
	static <A extends Identity> UserAuthentication<A> of(String username, A identity, Collection<String> groups) {
		return new GenericUserAuthentication<>(username, identity, groups.stream().filter(Objects::nonNull).collect(Collectors.toSet()), true);
	}
	
	/**
	 * <p>
	 * Creates a user authentication from the specified user.
	 * </p>
	 * 
	 * <p>
	 * This is a conveninence method that should be used with care. In order to respect the {@link Authentication} contract it is important to make sure that the specified user has been
	 * previously authenticated by an {@link Authenticator}.
	 * </p>
	 *
	 * <p>
	 * The resulting authentication is authenticated if the specified user is not locked.
	 * </p>
	 * 
	 * @param <A> the identity type
	 * @param user an authenticated user
	 * 
	 * @return a new user authentication
	 */
	@SuppressWarnings("unchecked")
	static <A extends Identity> UserAuthentication<A> of(User<A> user) {
		return new GenericUserAuthentication<>(user.getUsername(), user.getIdentity(), user.getGroups(), !user.isLocked());
	}
}
