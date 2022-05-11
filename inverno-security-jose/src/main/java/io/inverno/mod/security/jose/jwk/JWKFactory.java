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

import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWK factory is used to build, read or generate a particular type of key.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type built by the builder
 * @param <B> the JWK builder type
 * @param <C> the JWK generator type
 */
public interface JWKFactory<A extends JWK, B extends JWKBuilder<A, ?>, C extends JWKGenerator<A, ?>> {

	/**
	 * <p>
	 * Returns a new JWK builder for the key type supported by the factory.
	 * </p>
	 * 
	 * @return a new JWK builder
	 */
	B builder();
	
	/**
	 * <p>
	 * Returns a new JWK builder for the key type supported by the factory.
	 * </p>
	 * 
	 * @return a new JWK generator
	 */
	C generator();
	
	/**
	 * <p>
	 * Determines whether the factory supports the specified key type.
	 * </p>
	 * 
	 * @param kty a JWA key type
	 * 
	 * @return true if the factory supports the key type, false otherwise
	 */
	boolean supports(String kty);
	
	/**
	 * <p>
	 * Determines whether the factory supports the specified algorithm.
	 * </p>
	 * 
	 * @param alg a JWA algorithm
	 * 
	 * @return true if the factory supports the algorithm, false otherwise
	 */
	boolean supportsAlgorithm(String alg);
	
	/**
	 * <p>
	 * Reads the specified JWK or JWK set serialized as JSON.
	 * </p>
	 *
	 * @param jwk a JSON serialized JWK or JWK set
	 *
	 * @return a publisher of keys
	 *
	 * @throws JWKReadException       if there was an error reading the input or a particular key
	 * @throws JWKBuildException      if there was an error building a key
	 * @throws JWKResolveException    if there was an error resolving a key
	 * @throws JWKProcessingException if there was an error processing a key
	 */
	Mono<A> read(String jwk) throws JWKReadException, JWKBuildException, JWKResolveException, JWKProcessingException;
	
	/**
	 * <p>
	 * Read the specified JWK or JWK set represented in the specified map.
	 * </p>
	 * 
	 * @param jwk a map representing a JWK or a JWK set
	 * 
	 * @return a publisher of keys
	 *
	 * @throws JWKReadException       if there was an error reading the input or a particular key
	 * @throws JWKBuildException      if there was an error building a key
	 * @throws JWKResolveException    if there was an error resolving a key
	 * @throws JWKProcessingException if there was an error processing a key
	 */
	Mono<A> read(Map<String, Object> jwk) throws JWKReadException, JWKBuildException, JWKResolveException, JWKProcessingException;
	
	/**
	 * <p>
	 * Generates a new key using the specified parameters.
	 * </p>
	 *
	 * <p>
	 * This is a convenience method, you should prefer using {@link #generator()} which is more robust.
	 * </p>
	 *
	 * @param alg        a JWA algorithm
	 * @param parameters a map of key parameters
	 *
	 * @return a single publisher of key
	 *
	 * @throws JWKGenerateException   if there was an error generating the key
	 * @throws JWKProcessingException if there was a processing error
	 */
	Mono<A> generate(String alg, Map<String, Object> parameters) throws JWKGenerateException, JWKProcessingException;
}
