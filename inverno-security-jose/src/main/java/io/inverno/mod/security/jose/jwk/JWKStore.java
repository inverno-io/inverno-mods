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
package io.inverno.mod.security.jose.jwk;

import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWK store is used to store frequently used keys and make them available to {@link JWKFactory} and {@link JWKBuilder} so keys can be automatically resolved when building or reading JOSE objects.
 * </p>
 * 
 * <p>
 * Keys are stored and resolved based on the key id, the X.509 SHA1 thumbprint, the X.509 SHA256 thumbprint or the JWK thumbprint in that order.
 * </p>
 * 
 * <p>
 * It is recommended to only store trusted keys inside a JWK store to prevent them from being evicted when resolving a JOSE object key.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWKStore {

	/**
	 * <p>
	 * Returns the key stored for the specified key id.
	 * </p>
	 * 
	 * @param <T> the expected type of the key
	 * @param kid a key id
	 * 
	 * @return a single key publisher or an empty publisher
	 * 
	 * @throws JWKStoreException if there was an error accessing the store
	 */
	<T extends JWK> Mono<T> getByKeyId(String kid) throws JWKStoreException;
	
	/**
	 * <p>
	 * Returns the key stored for the specified X.509 SHA1 thumbprint
	 * </p>
	 * 
	 * @param <T> the expected type of the key
	 * @param x5t an X.509 SHA1 thumbprint
	 * 
	 * @return a single key publisher or an empty publisher
	 * 
	 * @throws JWKStoreException if there was an error accessing the store
	 */
	<T extends JWK> Mono<T> getBy509CertificateSHA1Thumbprint(String x5t) throws JWKStoreException;
	
	/**
	 * <p>
	 * Returns the key stored for the specified X.509 SHA256 thumbprint
	 * </p>
	 * 
	 * @param <T> the expected type of the key
	 * @param x5t_S256 an X.509 SHA256 thumbprint
	 * 
	 * @return a single key publisher or an empty publisher
	 * 
	 * @throws JWKStoreException if there was an error accessing the store
	 */
	<T extends JWK> Mono<T> getByX509CertificateSHA256Thumbprint(String x5t_S256) throws JWKStoreException;
	
	/**
	 * <p>
	 * Returns the key stored for the specified JWK thumbprint.
	 * </p>
	 * 
	 * @param <T> the expected type of the key
	 * @param jwkThumbprint a JWK thumbprint
	 * 
	 * @return a single key publisher or an empty publisher
	 * 
	 * @throws JWKStoreException if there was an error accessing the store
	 */
	<T extends JWK> Mono<T> getByJWKThumbprint(String jwkThumbprint) throws JWKStoreException;
	
	/**
	 * <p>
	 * Stores the specified key into the store.
	 * </p>
	 * 
	 * <p>
	 * This method should store the key for all available identifiers: key id, X.509 SHA1 thumbprint, X.509 SHA256 thumbprint and JWK thumbprint.
	 * </p>
	 * 
	 * @param jwk the key to store
	 * 
	 * @return a single empty publisher that completes once the key has been stored
	 * 
	 * @throws JWKStoreException if there was an error accessing the store
	 */
	Mono<Void> set(JWK jwk) throws JWKStoreException;
	
	/**
	 * <p>
	 * Removes the specified key from the store.
	 * </p>
	 * 
	 * <p>
	 * This method should remove the key associated to all available identifiers: key id, X.509 SHA1 thumbprint, X.509 SHA256 thumbprint and JWK thumbprint.
	 * </p>
	 * 
	 * @param jwk the key to remove
	 * 
	 * @return a single empty publisher that completes once the key has been removed
	 * 
	 * @throws JWKStoreException if there was an error accessing the store
	 */
	Mono<Void> remove(JWK jwk) throws JWKStoreException;
}
