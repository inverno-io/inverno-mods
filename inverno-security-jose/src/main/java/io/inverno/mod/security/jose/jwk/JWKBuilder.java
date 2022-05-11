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
 * A JWK builder is used to build JSON Web Keys.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type built by the builder
 * @param <B> the JWK builder type
 */
public interface JWKBuilder<A extends JWK, B extends JWKBuilder<A, B>> {

	/**
	 * <p>
	 * Specifies the public key use.
	 * </p>
	 * 
	 * @param use the public key use
	 * 
	 * @return this builder
	 */
	B publicKeyUse(String use);
	
	/**
	 * <p>
	 * Specifies the set of key operations for which the key is intended to be used.
	 * </p>
	 * 
	 * @param key_ops a list of key operations
	 * 
	 * @return this builder
	 */
	B keyOperations(String... key_ops);

	/**
	 * <p>
	 * Specifies the algorithm intended for use with the key.
	 * </p>
	 * 
	 * @param alg a JWA algorithm
	 * 
	 * @return this builder
	 */
	B algorithm(String alg);

	/**
	 * <p>
	 * Specified the key id that uniquely identifies the key.
	 * </p>
	 * 
	 * @param kid a unique key id
	 * 
	 * @return this builder
	 */
	B keyId(String kid);

	/**
	 * <p>
	 * Returns a single publisher that builds the key.
	 * </p>
	 *
	 * @return a single key publisher
	 *
	 * @throws JWKBuildException      if there was an error building the key
	 * @throws JWKResolveException    if there was an error resolving the key
	 * @throws JWKProcessingException if there was an error processing the key
	 */
	Mono<A> build() throws JWKBuildException, JWKResolveException, JWKProcessingException;
}
