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

import io.inverno.mod.security.jose.jwa.DirectJWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAKeyManagerException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import java.util.Map;

/**
 * <p>
 * Base direct JWA key manager implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type
 * @param <B> the JWA algorithm type
 */
public abstract class AbstractDirectJWAKeyManager<A extends JWK, B extends JWAAlgorithm<A>> extends AbstractJWAKeyManager<A, B> implements DirectJWAKeyManager {

	/**
	 * <p>
	 * Creates a direct JWA key manager.
	 * </p>
	 *
	 * @param jwk       a JWK
	 * @param algorithm a JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AbstractDirectJWAKeyManager(A jwk, B algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
	}

	/**
	 * <p>
	 * Creates a direct JWA key manager.
	 * </p>
	 * 
	 * @param jwk a JWK
	 */
	public AbstractDirectJWAKeyManager(A jwk) {
		super(jwk);
	}

	@Override
	public DirectCEK deriveCEK(String enc, Map<String, Object> parameters) throws JWAKeyManagerException {
		if(this.jwk.getKeyOperations() != null && !(this.jwk.getKeyOperations().contains(JWK. KEY_OP_DERIVE_KEY))) {
			throw new JWAKeyManagerException("JWK does not support derive key operations");
		}
		OCTAlgorithm octEnc;
		try {
			octEnc = OCTAlgorithm.fromAlgorithm(enc);
		}
		catch(IllegalArgumentException e) {
			throw new JWAKeyManagerException("Unsupported encryption algorithm: " + enc);
		}
		
		return this.doDeriveCEK(octEnc, parameters);
	}

	/**
	 * <p>
	 * Derives the Content Encryption Key.
	 * </p>
	 *
	 * @param octEnc     the target encryption algorithm
	 * @param parameters the parameters map
	 *
	 * @return a direct CEK
	 *
	 * @throws JWAKeyManagerException if there was an error deriving the CEK
	 */
	protected abstract DirectCEK doDeriveCEK(OCTAlgorithm octEnc, Map<String, Object> parameters) throws JWAKeyManagerException;
}
