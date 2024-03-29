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
 * a JWK generator is used to generate JSON Web Keys.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type generated by the builder
 * @param <B> the JWK generator type
 */
public interface JWKGenerator<A extends JWK, B extends JWKGenerator<A,B>> {

	/**
	 * <p>
	 * Specifies the public key use.
	 * </p>
	 * 
	 * @param use the public key use
	 * 
	 * @return this generator
	 */
	B publicKeyUse(String use);
	
	/**
	 * <p>
	 * Specifies the set of key operations for which the key is intended to be used.
	 * </p>
	 * 
	 * @param key_ops a list of key operations
	 * 
	 * @return this generator
	 */
	B keyOperations(String... key_ops);

	/**
	 * <p>
	 * Specifies the key id that uniquely identifies the key.
	 * </p>
	 * 
	 * @param kid a unique key id
	 * 
	 * @return this generator
	 */
	B keyId(String kid);
	
	/**
	 * <p>
	 * Specifies the algorithm intended for use with the key.
	 * </p>
	 * 
	 * @param alg a JWA algorithm
	 * 
	 * @return this generator
	 */
	B algorithm(String alg);
	
	/**
	 * <p>
	 * Returns a single publisher that generates a new key.
	 * </p>
	 *
	 * @return a single key publisher
	 *
	 * @throws JWKGenerateException   if there was an error generating the key
	 * @throws JWKProcessingException if there was a processing error
	 */
	Mono<A> generate() throws JWKGenerateException, JWKProcessingException;
}
