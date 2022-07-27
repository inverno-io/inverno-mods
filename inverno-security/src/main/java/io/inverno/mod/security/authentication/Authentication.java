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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.security.internal.authentication.GenericAuthentication;
import java.util.Optional;

/**
 * <p>
 * An authentication represents a proof that the credentials of an entity have been authenticated.
 * </p>
 * 
 * <p>
 * An authenticated entity must present an authentication to the application in order certify that it has been authenticated and claim access to protected services or resources. The identity of the
 * entity and what services and resources are accessible directly depends on the authentication which is intimately related and results from the authentication of the entity credentials by an
 * {@link Authenticator}.
 * </p>
 * 
 * <p>
 * The {@link Identity} of the authenticated entity can be resolved from the authentication using an {@link IdentityResolver}. Note that the entity identity might not always be available, the
 * authentication certifies that an authentication took place and that protected access might be granted but this is unrelated with identification.
 * </p>
 * 
 * <p>
 * The {@link AccessController} used to control the access to protected services and resources for the authenticated entity can be resolved from the authentication using an
 * {@link AccessControllerResolver}. There is no guarantee that an access controller can be resolved for an authenticated entity based on the authentication, this bascially depends on the application
 * and more specifically on the chosen access control strategy.
 * </p>
 * 
 * <p>
 * The authentication is one of the components that make up the {@link SecurityContext} along with {@link AccessController} and {@link Identity}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see SecurityContext
 * @see AccessController
 * @see Identity
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize( as = GenericAuthentication.class )
@JsonIgnoreProperties( value = { "anonymous" }, allowGetters = true )
public interface Authentication {

	/**
	 * <p>
	 * Returns an anonymous authentication.
	 * </p>
	 * 
	 * <p>
	 * It is used to indicate that services or resources are accessed by an anonymous entity which has not been authenticated. This allows an application to differentiate between authenticated and
	 * non-authenticated access.
	 * </p>
	 * 
	 * @return an anonymous authentication
	 */
	static Authentication anonymous() {
		return GenericAuthentication.ANONYMOUS;
	}
	
	/**
	 * <p>
	 * Returns a generic granted authentication.
	 * </p>
	 * 
	 * <p>
	 * An granted authentication simply indicates that an authentication took place and was successful.
	 * </p>
	 * 
	 * <p>
	 * Note that this is a convenience method, in a properly secured application such authentication should not give access to an identity nor an access controller and all access control other than
	 * checking whether an entity has been authenticated should be avoided.
	 * </p>
	 * 
	 * @return an granted authentication
	 */
	static Authentication granted() {
		return GenericAuthentication.GRANTED;
	}

	/**
	 * <p>
	 * Returns a generic denied authentication which indicates a failed authentication.
	 * </p>
	 * 
	 * @return a generic denied authentication
	 */
	static Authentication denied() {
		return GenericAuthentication.DENIED;
	}
	
	/**
	 * <p>
	 * Returns a generic denied authentication which indicates a failed authentication with the specified cause.
	 * </p>
	 * 
	 * @param cause a cause 
	 * 
	 * @return a generic denied authentication
	 */
	static Authentication denied(io.inverno.mod.security.SecurityException cause) {
		return new GenericAuthentication(cause);
	}

	
	/**
	 * <p>
	 * Determine whether the authentication is authenticated.
	 * </p>
	 * 
	 * <p>
	 * A non-authenticated authentication might indicates that no authentication took place (i.e. anonymous access) or that the authentication failed. In case of a failed authentication, the cause is
	 * exposed by {@link #getCause() }.
	 * </p>
	 * 
	 * @return true if the the authentication is authenticated, false otherwise
	 */
	@JsonProperty( "authenticated" )
	boolean isAuthenticated();
	
	/**
	 * <p>
	 * Determine whether the authentication is anonymous.
	 * </p>
	 * 
	 * <p>
	 * An anonymous authentication indicates an anonymous access with no authentication. This is basically equivalent to {@code !this.isAuthenticated() && !this.getCause().isPresent()}.
	 * </p>
	 * 
	 * @return true if the authentication is anonymous, false otherwise
	 */
	@JsonProperty( "anonymous" )
	default boolean isAnonymous() {
		return !this.isAuthenticated() && !this.getCause().isPresent();
	}

	/**
	 * <p>
	 * Returns the cause of a failed authentication.
	 * </p>
	 * 
	 * <p>
	 * A non-authenticated authentication with no cause indicates that no authentication took place (i.e. anonymous access).
	 * </p>
	 * 
	 * <p>
	 * A non-authenticated authentication with a cause indicates a failed authentication.
	 * </p>
	 * 
	 * @return an optional returning the cause of the failed authentication, or an empty optional if there was no authentication or if the authentication was successful
	 */
	@JsonIgnore
	Optional<io.inverno.mod.security.SecurityException> getCause();
}
