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
package io.inverno.mod.security.jose.jwa;

import io.inverno.mod.security.jose.jwk.JWK;

/**
 * <p>
 * Base JWA algorithm as specified by <a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of key
 */
public interface JWAAlgorithm<A extends JWK> {

	/**
	 * <p>
	 * Returns the JWA registered name of the algorithm.
	 * </p>
	 * 
	 * @return a JWA registered name
	 */
	String getAlgorithm();
	
	/**
	 * <p>
	 * Determines whether the algorithm is a signature algorithm.
	 * </p>
	 * 
	 * @return true if the algorithm is a signature algorithm, false otherwise
	 */
	boolean isSignature();
	
	/**
	 * <p>
	 * Determines whether the algorithm is a key management algorithm.
	 * </p>
	 * 
	 * @return true if the algorithm is a key management algorithm, false otherwise
	 */
	boolean isKeyManagement();
	
	/**
	 * <p>
	 * Determines whether the algorithm is an encryption algorithm.
	 * </p>
	 * 
	 * @return true if the algorithm is an encryption algorithm, false otherwise
	 */
	boolean isEncryption();
	
	/**
	 * <p>
	 * Creates a signer from the algorithm.
	 * </p>
	 * 
	 * @param jwk the key to use for signing
	 * 
	 * @return a JWA signer
	 * 
	 * @throws JWAProcessingException if the algorithm is not a signature algorithm or if there was an error creating the signer
	 */
	JWASigner createSigner(A jwk) throws JWAProcessingException;
	
	/**
	 * <p>
	 * Creates a key manager from the algorithm.
	 * </p>
	 * 
	 * @param jwk the key to use for signing
	 * 
	 * @return a JWA key manager
	 * 
	 * @throws JWAProcessingException if the algorithm is not a key management algorithm or if there was an error creating the key manager
	 */
	JWAKeyManager createKeyManager(A jwk) throws JWAProcessingException;
	
	/**
	 * <p>
	 * Creates a cipher from the algorithm.
	 * </p>
	 * 
	 * @param jwk the key to use for signing
	 * 
	 * @return a JWA cipher
	 * 
	 * @throws JWAProcessingException if the algorithm is not an encryption algorithm or if there was an error creating the cipher
	 */
	JWACipher createCipher(A jwk) throws JWAProcessingException;
}
