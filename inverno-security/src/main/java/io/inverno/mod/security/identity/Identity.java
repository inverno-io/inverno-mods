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
package io.inverno.mod.security.identity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.context.SecurityContext;

/**
 * <p>
 * An identity exposes the information that specifies the identity of an authenticated entity.
 * </p>
 * 
 * <p>
 * An entity can be authenticated using credentials that might include an identifier such as a username, a hostname, an IP or MAC address, a personal token... This identifier is usually used during
 * the authentication process to obtain an {@link Authentication}. However this authentication and especially any identifier involved is sometimes not enough to be able to fully identify the entity.
 * Indeed, an entity can be authenticated in multiple ways using multiple identifiers whereas only one single identifier uniquely identifies it.
 * </p>
 * 
 * <p>
 * An identity must have one unique immutable identifier and provides specific information about the user. For a person it can be a first name, a last name, an email address and any kind of personal
 * information. For a a system or a device it can be a serial number, an IP address, a screen resolution, an X.509 certificate...
 * </p>
 * 
 * <p>
 * The identity is one of the components that make up the {@link SecurityContext} along with {@link Authentication} and {@link AccessController}. An identity is usually resolved from an
 * {@link Authentication} using an {@link IdentityResolver}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@JsonInclude( JsonInclude.Include.NON_NULL )
@JsonTypeInfo( use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY )
public interface Identity {
	
	/**
	 * <p>
	 * Returns the unique identifier that identifies the authenticated entity.
	 * </p>
	 * 
	 * @return a unique identifier
	 */
	@JsonProperty("uid")
	String getUid();
}
