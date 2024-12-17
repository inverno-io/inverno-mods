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
package io.inverno.mod.security.jose;

import io.inverno.mod.configuration.Configuration;
import java.net.URI;
import java.util.Set;

/**
 * <p>
 * JOSE module configuration.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Configuration( name = "configuration" )
public interface JOSEConfiguration {
	
	/**
	 * <p>
	 * The path to the key store.
	 * </p>
	 * 
	 * @return the key store URI
	 */
	URI key_store();
	
	/**
	 * <p>
	 * The type of key store.
	 * </p>
	 * 
	 * <p>
	 * Defaults to "JKS".
	 * </p>
	 * 
	 * @return the key store type
	 */
	default String key_store_type() {
		return "JKS";
	}

	/**
	 * <p>
	 * The password of the key store.
	 * </p>
	 * 
	 * @return a password
	 */
	String key_store_password();
	
	/**
	 * <p>
	 * Enable/disable JWK URL (i.e. {@code jku}) resolution.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true to resolve JWK URL, false otherwise
	 */
	default boolean resolve_jku() {
		return false;
	}
	
	/**
	 * <p>
	 * The list of trusted JWK URLs for which keys should be trusted.
	 * </p>
	 * 
	 * <p>
	 * Defaults to the empty list.
	 * </p>
	 * 
	 * @return the set of trusted JWK URLs
	 */
	default Set<URI> trusted_jku() {
		return Set.of();
	}
	
	/**
	 * <p>
	 * Enable/disable X.509 certificate URL (i.e. {@code x5u}) resolution.
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true to resolve X.509 certificate URL, false otherwise
	 */
	default boolean resolve_x5u() {
		return false;
	}
	
	/**
	 * <p>
	 * Enable/disable X.509 certificates chain validation (x5c or x5u).
	 * </p>
	 * 
	 * <p>
	 * Defaults to false.
	 * </p>
	 * 
	 * @return true to validate X.509 certificates chain, false otherwise
	 */
	default boolean validate_certificate() {
		return false;
	}
}
