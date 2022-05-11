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

import io.inverno.mod.security.jose.internal.JOSEUtils;
import java.security.SecureRandom;

/**
 * <p>
 * A JWA cipher is used to encrypt and decrypt content.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWACipher extends JWA {
	
	/**
	 * <p>
	 * Encrypts the specified data using a default {@link SecureRandom}.
	 * </p>
	 * 
	 * @param data the data to encrypt
	 * @param aad additional authentication data
	 * 
	 * @return encrypted data
	 * 
	 * @throws JWACipherException if there was an error encrypting the data
	 */
	default EncryptedData encrypt(byte[] data, byte[] aad) throws JWACipherException {
		return this.encrypt(data, aad, JOSEUtils.DEFAULT_SECURE_RANDOM);
	}
	
	/**
	 * <p>
	 * Encrypts the specified data using the specified {@link SecureRandom}.
	 * </p>
	 * 
	 * @param data the data to encrypt
	 * @param aad additional authentication data
	 * @param secureRandom a secure random
	 * 
	 * @return encrypted data
	 * 
	 * @throws JWACipherException if there was an error encrypting the data
	 */
	EncryptedData encrypt(byte[] data, byte[] aad, SecureRandom secureRandom) throws JWACipherException;
	
	/**
	 * <p>
	 * Decrypts the specified cypher text.
	 * </p>
	 * 
	 * @param cipherText the cipher text to decrypt
	 * @param aad the additional authentication data
	 * @param iv the initilization vector
	 * @param tag the authentication tag
	 * 
	 * @return decrypted data
	 * 
	 * @throws JWACipherException if there was an error decrypting the data
	 */
	byte[] decrypt(byte[] cipherText, byte[] aad, byte[] iv, byte[] tag) throws JWACipherException;
	
	/**
	 * <p>
	 * An encrypted data composed of the initialization vector, the authentication tag and the cipher text.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface EncryptedData {
		
		/**
		 * <p>
		 * Returns the initialization vector used to encrypt the data.
		 * </p>
		 * 
		 * @return the initialization vector
		 */
		byte[] getInitializationVector();
		
		/**
		 * <p>
		 * The authentication tag resulting from the encryption.
		 * </p>
		 * 
		 * @return the authentication tag
		 */
		byte[] getAuthenticationTag();
		
		/**
		 * <p>
		 * Returns the cipher text.
		 * </p>
		 * 
		 * @return the cipher text
		 */
		byte[] getCipherText();
	}
}
