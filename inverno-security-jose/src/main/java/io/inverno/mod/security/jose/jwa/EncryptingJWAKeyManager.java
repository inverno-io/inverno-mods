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
import io.inverno.mod.security.jose.jwk.JWK;
import java.security.SecureRandom;
import java.util.Map;

/**
 * <p>
 * An encrypting Key Management algorithm used to encrypt a generated CEK used to encrypt a JWE payload.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface EncryptingJWAKeyManager extends JWAKeyManager {
	
	/**
	 * <p>
	 * Encrypts the CEK using a default {@link SecureRandom}.
	 * </p>
	 *
	 * @param cek        the Content encryption Key to encrypt.
	 * @param parameters the JOSE header custom parameters that might be required by the algorithm to encrypt the CEK
	 *
	 * @return an encrypted CEK
	 *
	 * @throws JWAKeyManagerException if there was an error encrypting the CEK
	 */
	default EncryptedCEK encryptCEK(JWK cek, Map<String, Object> parameters) throws JWAKeyManagerException {
		return this.encryptCEK(cek, parameters, JOSEUtils.DEFAULT_SECURE_RANDOM);
	}
	
	/**
	 * <p>
	 * Encrypts the CEK using the specified {@link SecureRandom}.
	 * </p>
	 *
	 * @param cek          the Content encryption Key to encrypt.
	 * @param parameters   the JOSE header custom parameters that might be required by the algorithm to encrypt the CEK
	 * @param secureRandom a secure random
	 *
	 * @return an encrypted CEK
	 *
	 * @throws JWAKeyManagerException if there was an error encrypting the CEK
	 */
	EncryptedCEK encryptCEK(JWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException;
	
	/**
	 * <p>
	 * Decrypts the specified encrypted key and returned the decrypted CEK.
	 * </p>
	 *
	 * @param encrypted_key an encrypted key
	 * @param enc           the content encryption algorithm
	 * @param parameters    the JOSE header custom parameters that might be required by the algorithm to decrypt the CEK
	 *
	 * @return a decrypted CEK
	 *
	 * @throws JWAKeyManagerException if there was an error decrypting the CEK
	 */
	JWK decryptCEK(byte[] encrypted_key, String enc, Map<String, Object> parameters) throws JWAKeyManagerException;
	
	/**
	 * <p>
	 * An encrypted CEK composed of the encrypted key and a map of specific parameters resulting from the CEK encryption and required by the recipient to decrypt the key. 
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface EncryptedCEK {
		
		/**
		 * <p>
		 * Returns the encrypted CEK.
		 * </p>
		 * 
		 * @return the encrypted key
		 */
		byte[] getEncryptedKey();
		
		/**
		 * <p>
		 * Returns the specific parameters resulting from the CEK encryption and required by a recipient to derive the key.
		 * </p>
		 * 
		 * @return a map of parameters or the empty map
		 */
		Map<String, Object> getMoreHeaderParameters();
	}
}
