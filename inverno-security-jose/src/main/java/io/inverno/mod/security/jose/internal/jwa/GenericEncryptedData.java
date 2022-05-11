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

import io.inverno.mod.security.jose.jwa.JWACipher;

/**
 * <p>
 * Generic {@link JWACipher.EncryptedData} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericEncryptedData implements JWACipher.EncryptedData {

	private final byte[] iv;
	
	private final byte[] cipherText;
	
	private final byte[] authenticationTag;

	/**
	 * <p>
	 * Creates generic encrypted data.
	 * </p>
	 * 
	 * @param iv                the initialization vector
	 * @param cipherText        the cipher text
	 * @param authenticationTag the authentication tag
	 */
	public GenericEncryptedData(byte[] iv, byte[] cipherText, byte[] authenticationTag) {
		this.iv = iv;
		this.cipherText = cipherText;
		this.authenticationTag = authenticationTag;
	}

	@Override
	public byte[] getInitializationVector() {
		return this.iv;
	}
	
	@Override
	public byte[] getCipherText() {
		return this.cipherText;
	}

	@Override
	public byte[] getAuthenticationTag() {
		return this.authenticationTag;
	}
}
