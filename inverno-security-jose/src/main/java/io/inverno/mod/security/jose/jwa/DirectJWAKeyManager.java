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

import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.util.Map;

/**
 * <p>
 * A direct Key Management algorithm that derives the Content Encryption Key which is used directly to encrypt a JWE payload.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface DirectJWAKeyManager extends JWAKeyManager {
	
	/**
	 * <p>
	 * Derives a Content Encryption Key.
	 * </p>
	 *
	 * @param enc        the encryption algorithm
	 * @param parameters the JOSE header custom parameters that might be required by the algorithm to derive the CEK
	 *
	 * @return A direct CEK
	 *
	 * @throws JWAKeyManagerException if there was an error deriving the CEK
	 */
	DirectCEK deriveCEK(String enc, Map<String, Object> parameters) throws JWAKeyManagerException;
	
	/**
	 * <p>
	 * A direct CEK composed of a derived encryption key and a map of specific parameters resulting from the key derivation and required by a recipient to derive the key.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface DirectCEK {
		
		/**
		 * <p>
		 * Returns a derived encryption key.
		 * </p>
		 * 
		 * @return an encryption key
		 */
		OCTJWK getEncryptionKey();
		
		/**
		 * <p>
		 * Returns the specific parameters resulting from the key derivation and required by a recipient to derive the key.
		 * </p>
		 * 
		 * @return a map of parameters or the empty map
		 */
		Map<String, Object> getMoreHeaderParameters();
	}
}
