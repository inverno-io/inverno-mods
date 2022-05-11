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
package io.inverno.mod.security.jose.internal.jwa;

import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwk.JWK;

/**
 * <p>
 * Base JWA key manager implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type
 * @param <B> the JWA algorithm type
 */
public abstract class AbstractJWAKeyManager<A extends JWK, B extends JWAAlgorithm<A>> extends AbstractJWA implements JWAKeyManager {

	/**
	 * The key.
	 */
	protected final A jwk;
	
	/**
	 * The algorithm
	 */
	protected B algorithm;

	/**
	 * <p>
	 * Creates a JWA key manager.
	 * </p>
	 * 
	 * @param jwk       a JWK
	 * @param algorithm a JWA algorithm
	 * 
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AbstractJWAKeyManager(A jwk, B algorithm) throws JWAProcessingException {
		if(!algorithm.isKeyManagement()) {
			throw new JWAProcessingException("Not a key management algorithm: " + algorithm.getAlgorithm());
		}
		this.jwk = jwk;
		this.algorithm = algorithm;
	}
	
	/**
	 * <p>
	 * Creates a JWA key manager.
	 * </p>
	 *
	 * @param jwk a JWK
	 */
	protected AbstractJWAKeyManager(A jwk) {
		this.jwk = jwk;
	}
}
