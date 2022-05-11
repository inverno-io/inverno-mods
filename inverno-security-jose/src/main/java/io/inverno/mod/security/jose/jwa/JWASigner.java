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

/**
 * <p>
 * A JWA signer is used to sign data and or verify data signature.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWASigner extends JWA {

	/**
	 * <p>
	 * Signs the specified data and returns the signature.
	 * </p>
	 * 
	 * @param data the data to sign
	 * 
	 * @return a signature
	 * 
	 * @throws JWASignatureException if there was an error signing data
	 */
	byte[] sign(byte[] data) throws JWASignatureException;
	
	/**
	 * <p>
	 * Verifies the specified signature for the specified data.
	 * </p>
	 * 
	 * @param data the data
	 * @param signature the signature
	 * 
	 * @return true if the signature is valid, false otherwise
	 * 
	 * @throws JWASignatureException if there was an error verifying signature
	 */
	boolean verify(byte[] data, byte[] signature) throws JWASignatureException;
}
