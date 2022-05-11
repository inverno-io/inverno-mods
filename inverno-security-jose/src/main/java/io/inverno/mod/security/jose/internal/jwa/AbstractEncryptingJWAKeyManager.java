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

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.jwa.EncryptingJWAKeyManager;
import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAKeyManagerException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.security.SecureRandom;
import java.util.Map;

/**
 * <p>
 * Base encrypting JWA key manager implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type
 * @param <B> the JWA algorithm type
 */
public abstract class AbstractEncryptingJWAKeyManager<A extends JWK, B extends JWAAlgorithm<A>> extends AbstractJWAKeyManager<A, B> implements EncryptingJWAKeyManager {

	/**
	 * <p>
	 * Creates an encrypting JWA key manager.
	 * </p>
	 *
	 * @param jwk       a JWK
	 * @param algorithm a JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AbstractEncryptingJWAKeyManager(A jwk, B algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
	}

	/**
	 * <p>
	 * Creates an encrypting JWA key manager.
	 * </p>
	 *
	 * @param jwk a JWK
	 */
	protected AbstractEncryptingJWAKeyManager(A jwk) {
		super(jwk);
	}
	
	@Override
	public EncryptingJWAKeyManager.EncryptedCEK encryptCEK(JWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException {
		if(this.jwk.getKeyOperations() != null && !(this.jwk.getKeyOperations().contains(JWK. KEY_OP_ENCRYPT))) {
			throw new JWAKeyManagerException("JWK does not support encrypt key operations");
		}
		if(!(cek instanceof OCTJWK)) {
			throw new JWAKeyManagerException("Unsupported CEK type: " + cek.getClass());
		}
		return this.doEncryptCEK((OCTJWK)cek, parameters, secureRandom != null ? secureRandom : JOSEUtils.DEFAULT_SECURE_RANDOM);
	}
	
	/**
	 * <p>
	 * Encrypts the Content Encryption Key.
	 * </p>
	 *
	 * @param cek          the content encryption key
	 * @param parameters   the parameters map
	 * @param secureRandom a secure random
	 *
	 * @return an encrypted CEK
	 *
	 * @throws JWAKeyManagerException if there was an error encrypting the CEK
	 */
	protected abstract EncryptingJWAKeyManager.EncryptedCEK doEncryptCEK(OCTJWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException;

	@Override
	public JWK decryptCEK(byte[] encrypted_key, String enc, Map<String, Object> parameters) throws JWAKeyManagerException {
		if(this.jwk.getKeyOperations() != null && !(this.jwk.getKeyOperations().contains(JWK. KEY_OP_DECRYPT))) {
			throw new JWAKeyManagerException("JWK does not support decrypt key operations");
		}
		OCTAlgorithm octEnc;
		try {
			octEnc = OCTAlgorithm.fromAlgorithm(enc);
		}
		catch(IllegalArgumentException e) {
			throw new JWAKeyManagerException("Unsupported encryption algorithm: " + enc);
		}
		
		return this.doDecryptCEK(encrypted_key, octEnc, parameters);
	}
	
	/**
	 * <p>
	 * Decrypts the Content Encryption Key.
	 * </p>
	 *
	 * @param encrypted_key the encrypted key
	 * @param octEnc        the target encryption algorithm
	 * @param parameters    the parameters map
	 *
	 * @return the decrypted CEK
	 *
	 * @throws JWAKeyManagerException if there was an error decrypting the CEK
	 */
	protected abstract OCTJWK doDecryptCEK(byte[] encrypted_key, OCTAlgorithm octEnc, Map<String, Object> parameters) throws JWAKeyManagerException;
}
