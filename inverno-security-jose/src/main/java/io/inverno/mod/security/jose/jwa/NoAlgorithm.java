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

import io.inverno.mod.security.jose.jwk.JWK;

/**
 * <p>
 * NoOp algorithms as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a>.
 * </p>
 * 
 * <p>
 * Signature algorithms:
 * </p>
 * 
 * <ul>
 * <li>none</li>
 * </ul>
 * 
 * <p>
 * Key Management algorithms:
 * </p>
 *
 * <ul>
 * <li>dir</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum NoAlgorithm implements JWAAlgorithm<JWK> {
	
	/**
	 * None signature algorithm for unsecured JWS as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.6">RFC7518 Section 3.6</a>
	 */
	NONE("none", true, false, false),
	
	/**
	 * Direct encryption with a shared symmetric key as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.5">RFC7518 Section 4.5</a>
	 */
	DIR("dir", false, true, false);
	
	/**
	 * The JWA registered algorithm name.
	 */
	private final String alg;
	
	/**
	 * Signature algorithm flag.
	 */
	private final boolean signature;
	/**
	 * Key management algorithm flag.
	 */
	private final boolean keyManagement;
	/**
	 * Encryption algorithm flag.
	 */
	private final boolean encryption;

	/**
	 * <p>
	 * Creates a NoOp algorithm.
	 * </p>
	 *
	 * @param alg           the JWA registered algorithm name
	 * @param signature     signature algorithm flag
	 * @param keyManagement key management algorithm flag
	 * @param encryption    encryption algorithm flag
	 */
	NoAlgorithm(String alg, boolean signature, boolean keyManagement, boolean encryption) {
		this.alg = alg;
		this.signature = signature;
		this.keyManagement = keyManagement;
		this.encryption = encryption;
	}

	@Override
	public String getAlgorithm() {
		return alg;
	}
	
	@Override
	public boolean isSignature() {
		return signature;
	}

	@Override
	public boolean isKeyManagement() {
		return keyManagement;
	}

	@Override
	public boolean isEncryption() {
		return encryption;
	}

	@Override
	public JWASigner createSigner(JWK jwk) throws JWAProcessingException {
		throw new JWAProcessingException("Algorithm does not support signature operations");
	}

	@Override
	public JWAKeyManager createKeyManager(JWK jwk) throws JWAProcessingException {
		throw new JWAProcessingException("Algorithm does not support key management operations");
	}

	@Override
	public JWACipher createCipher(JWK jwk) throws JWAProcessingException {
		throw new JWAProcessingException("Algorithm does not support encryption operations");
	}
	
	/**
	 * <p>
	 * Returns the NoOp algorithm corresponding to the specified JWA registered algorithm name.
	 * </p>
	 * 
	 * @param alg a JWA registered algorithm name
	 * 
	 * @return a NoOp algorithm
	 * 
	 * @throws IllegalArgumentException if the specified algorithm is not a NoOp algorithm.
	 */
	public static NoAlgorithm fromAlgorithm(String alg) throws IllegalArgumentException {
		switch(alg) {
			case "none": 
				return NONE;
			case "dir": 
				return DIR;
			default: 
				throw new IllegalArgumentException("Unknown algorithm " + alg);
		}
	}
}
