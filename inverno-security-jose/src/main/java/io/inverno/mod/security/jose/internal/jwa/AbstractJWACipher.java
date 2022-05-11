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
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWACipherException;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.security.SecureRandom;

/**
 * <p>
 * Base JWA cipher implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public abstract class AbstractJWACipher extends AbstractJWA implements JWACipher {

	/**
	 * The key.
	 */
	protected final OCTJWK jwk;
	
	/**
	 * The algorithm.
	 */
	protected OCTAlgorithm algorithm;

	/**
	 * <p>
	 * Creates a JWA cipher.
	 * </p>
	 *
	 * @param jwk       a JWK
	 * @param algorithm a JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AbstractJWACipher(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		if(!algorithm.isEncryption()) {
			throw new JWAProcessingException("Not an encryption algorithm: " + algorithm.getAlgorithm());
		}
		this.jwk = jwk;
		this.algorithm = algorithm;
	}
	
	/**
	 * <p>
	 * Creates a JWA cipher.
	 * </p>
	 * 
	 * @param jwk a JWK
	 */
	protected AbstractJWACipher(OCTJWK jwk) {
		this.jwk = jwk;
	}

	@Override
	public EncryptedData encrypt(byte[] data, byte[] aad, SecureRandom secureRandom) throws JWACipherException {
		if(this.jwk.getKeyOperations() != null && !this.jwk.getKeyOperations().contains(JWK.KEY_OP_ENCRYPT)) {
			throw new JWACipherException("JWK does not support encrypt operation");
		}
		return this.doEncrypt(data, aad, secureRandom != null ? secureRandom : JOSEUtils.DEFAULT_SECURE_RANDOM);
	}
	
	/**
	 * <p>
	 * Encrypts the specified data.
	 * </p>
	 *
	 * @param data         the data to encrypt
	 * @param aad          the additional authentication data
	 * @param secureRandom a secure random
	 *
	 * @return encrypted data
	 *
	 * @throws JWACipherException if there was an error encrypting the data
	 */
	protected abstract EncryptedData doEncrypt(byte[] data, byte[] aad, SecureRandom secureRandom) throws JWACipherException;
	
	@Override
	public byte[] decrypt(byte[] data, byte[] cipherText, byte[] iv, byte[] tag) throws JWACipherException {
		if(this.jwk.getKeyOperations() != null && !this.jwk.getKeyOperations().contains(JWK.KEY_OP_DECRYPT)) {
			throw new JWACipherException("JWK does not support decrypt operation");
		}
		return this.doDecrypt(data, cipherText, iv, tag);
	}
	
	/**
	 * <p>
	 * Decrypts the specified cipher text.
	 * </p>
	 *
	 * @param cipherText the cipher text to decrypt
	 * @param aad        the additional authentication data
	 * @param iv         the initialization vector
	 * @param tag        the authentication tag
	 *
	 * @return the decrypted data
	 *
	 * @throws JWACipherException if there was an error decrypting the data
	 */
	protected abstract byte[] doDecrypt(byte[] cipherText, byte[] aad, byte[] iv, byte[] tag) throws JWACipherException;
}
