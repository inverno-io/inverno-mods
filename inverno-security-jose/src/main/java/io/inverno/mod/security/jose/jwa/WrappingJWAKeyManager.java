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
 * A wrapping Key Management algorithm used to wrap a generated CEK used to encrypt a JWE payload.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface WrappingJWAKeyManager extends JWAKeyManager {
	
	/**
	 * <p>
	 * Wraps the CEK using a default {@link SecureRandom}.
	 * </p>
	 *
	 * @param cek        the Content encryption Key to encrypt.
	 * @param parameters the JOSE header custom parameters that might be required by the algorithm to wrap the CEK
	 *
	 * @return a wrapped CEK
	 *
	 * @throws JWAKeyManagerException if there was an error wrapping the CEK
	 */
	default WrappedCEK wrapCEK(JWK cek, Map<String, Object> parameters) throws JWAKeyManagerException {
		return this.wrapCEK(cek, parameters, JOSEUtils.DEFAULT_SECURE_RANDOM);
	}
	
	/**
	 * <p>
	 * Wraps the CEK using the specified {@link SecureRandom}.
	 * </p>
	 *
	 * @param cek        the Content encryption Key to encrypt.
	 * @param parameters the JOSE header custom parameters that might be required by the algorithm to wrap the CEK
	 * @param secureRandom a secure random
	 *
	 * @return a wrapped CEK
	 *
	 * @throws JWAKeyManagerException if there was an error wrapping the CEK
	 */
	WrappedCEK wrapCEK(JWK cek, Map<String, Object> parameters, SecureRandom secureRandom) throws JWAKeyManagerException;
	
	/**
	 * <p>
	 * Unwraps the specified encrypted key and returned the unwrapped CEK.
	 * </p>
	 *
	 * @param encrypted_key a wrapped key
	 * @param enc           the content encryption algorithm
	 * @param parameters    the JOSE header custom parameters that might be required by the algorithm to unwrap the CEK
	 *
	 * @return an unwrapped CEK
	 *
	 * @throws JWAKeyManagerException if there was an error unwrapping the CEK
	 */
	JWK unwrapCEK(byte[] encrypted_key, String enc, Map<String, Object> parameters) throws JWAKeyManagerException;
	
	/**
	 * <p>
	 * A wrapped CEK composed of the wrapped key and a map of specific parameters resulting from the CEK wrapping and required by the recipient to unwrap the key. 
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	interface WrappedCEK {
		
		/**
		 * <p>
		 * Returns the wrapped CEK.
		 * </p>
		 * 
		 * @return the wrapped key
		 */
		byte[] getWrappedKey();
		
		/**
		 * <p>
		 * Returns the specific parameters resulting from the CEK wrapping and required by a recipient to unwrap the key.
		 * </p>
		 * 
		 * @return a map of parameters or the empty map
		 */
		Map<String, Object> getMoreHeaderParameters();
	}
}
