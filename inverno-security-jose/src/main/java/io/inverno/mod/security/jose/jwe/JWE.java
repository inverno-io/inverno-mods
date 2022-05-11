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
package io.inverno.mod.security.jose.jwe;

import io.inverno.mod.security.jose.JOSEObject;

/**
 * <p>
 * A JSON Web Encryption object as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516">RFC7516</a>.
 * </p>
 * 
 * <p>
 * This represents a single JWE object serialized using the compact representation as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-7.1">RFC7516 Section 7.1</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public interface JWE<A> extends JOSEObject<A, JWEHeader> {

	/**
	 * <p>
	 * Returns the encrypted key encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded encrypted key with no padding
	 */
	String getEncryptedKey();
	
	/**
	 * <p>
	 * Returns the initialization vector encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded initialization vector with no padding
	 */
	String getInitializationVector();
	
	/**
	 * <p>
	 * Returns the cipher text encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded cipher text with no padding
	 */
	String getCipherText();
	
	/**
	 * <p>
	 * Returns the authentication tag encoded as Base64URL.
	 * </p>
	 * 
	 * @return the Base64URL encoded authentication tag with no padding
	 */
	String getAuthenticationTag();
	
	@Override
	int hashCode();
	
	@Override
	boolean equals(Object obj);
}
