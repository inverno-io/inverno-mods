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

import io.inverno.mod.security.jose.internal.jwa.PBES2KeyManager;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWK;
import java.util.function.BiFunction;

/**
 * <p>
 * Password-based encryption key management algorithms as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a>.
 * </p>
 * 
 * <p>
 * Key Management algorithms:
 * </p>
 * 
 * <ul>
 * <li>PBES2-HS256+A128KW</li>
 * <li>PBES2-HS384+A192KW</li>
 * <li>PBES2-HS512+A256KW</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum PBES2Algorithm implements JWAAlgorithm<PBES2JWK> {
	
	/**
	 * PBES2-HS256+A128KW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.8">RFC7518 Section 4.8</a>
	 */
	PBES2_HS256_A128KW("PBES2-HS256+A128KW", null, PBES2Algorithm::createPBES2KeyManager, null, "PBKDF2WithHmacSHA256", "AESWrap_128", 16),
	/**
	 * PBES2-HS384+A192KW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.8">RFC7518 Section 4.8</a>
	 */
	PBES2_HS384_A192KW("PBES2-HS384+A192KW", null, PBES2Algorithm::createPBES2KeyManager, null, "PBKDF2WithHmacSHA384", "AESWrap_192", 24),
	/**
	 * PBES2-HS512+A256KW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.8">RFC7518 Section 4.8</a>
	 */
	PBES2_HS512_A256KW("PBES2-HS512+A256KW", null, PBES2Algorithm::createPBES2KeyManager, null, "PBKDF2WithHmacSHA512", "AESWrap_256", 32);
	
	/**
	 * The minimum salt length.
	 */
	public static final int MINIMUM_SALT_LENGTH = 8;
	/**
	 * The default salt length.
	 */
	public static final int DEFAULT_SALT_LENGTH = 16;

	/**
	 * The minimum iteration count.
	 */
	public static final int MINIMUM_ITERATION_COUNT = 1000;
	/**
	 * The default iteration count.
	 */
	public static final int DEFAULT_ITERATION_COUNT = MINIMUM_ITERATION_COUNT;
	
	/**
	 * The JWA registered algorithm name.
	 */
	private final String alg;
	/**
	 * The JWA signer factory.
	 */
	private final BiFunction<PBES2JWK, PBES2Algorithm, JWASigner> signerFactory;
	/**
	 * The JWA key manager factory.
	 */
	private final BiFunction<PBES2JWK, PBES2Algorithm, JWAKeyManager> keyManagerFactory;
	/**
	 * The JWA cipher factory.
	 */
	private final BiFunction<PBES2JWK, PBES2Algorithm, JWACipher> cipherFactory;
	/**
	 * The corresponding JCA algorithm.
	 */
	private final String jcaAlg;
	/**
	 * The corresponding JCA encryption algorithm.
	 */
	private final String jcaEncryptionAlg;
	/**
	 * The encryption key length in bytes.
	 */
	private final int encKeyLength;
	
	/**
	 * <p>
	 * Creates a password-based encryption key management algorithm.
	 * </p>
	 *
	 * @param alg               the JWA registered algorithm name
	 * @param signerFactory     the JWA signer factory
	 * @param keyManagerFactory the JWA key manager factory
	 * @param cipherFactory     the JWA cipher factory
	 * @param jcaAlg            the JCA algorithm
	 * @param jcaEncryptionAlg  the JCA encryption algorithm
	 * @param keyLength         the encryption key length in bytes
	 */
	private PBES2Algorithm(String alg, BiFunction<PBES2JWK, PBES2Algorithm, JWASigner> signerFactory, BiFunction<PBES2JWK, PBES2Algorithm, JWAKeyManager> keyManagerFactory, BiFunction<PBES2JWK, PBES2Algorithm, JWACipher> cipherFactory, String jcaAlg, String jcaEncryptionAlg, int encKeyLength) {
		this.alg = alg;
		this.signerFactory = signerFactory;
		this.keyManagerFactory = keyManagerFactory;
		this.cipherFactory = cipherFactory;
		this.jcaAlg = jcaAlg;
		this.jcaEncryptionAlg = jcaEncryptionAlg;
		this.encKeyLength = encKeyLength;
	}

	@Override
	public String getAlgorithm() {
		return alg;
	}
	
	@Override
	public boolean isSignature() {
		return this.signerFactory != null;
	}

	@Override
	public boolean isKeyManagement() {
		return this.keyManagerFactory != null;
	}

	@Override
	public boolean isEncryption() {
		return this.cipherFactory != null;
	}

	@Override
	public JWASigner createSigner(PBES2JWK jwk) throws JWAProcessingException {
		if(this.signerFactory == null) {
			throw new JWAProcessingException("Not a signature algorithm: " + this.alg);
		}
		return this.signerFactory.apply(jwk, this);
	}

	@Override
	public JWAKeyManager createKeyManager(PBES2JWK jwk) throws JWAProcessingException {
		if(this.keyManagerFactory == null) {
			throw new JWAProcessingException("Not a key management algorithm: " + this.alg);
		}
		return this.keyManagerFactory.apply(jwk, this);
	}

	@Override
	public JWACipher createCipher(PBES2JWK jwk) throws JWAProcessingException {
		if(this.cipherFactory == null) {
			throw new JWAProcessingException("Not an encryption algorithm: " + this.alg);
		}
		return this.cipherFactory.apply(jwk, this);
	}
	
	/**
	 * <p>
	 * Return the JCA algorithm corresponding to the JWA algorithm.
	 * </p>
	 * 
	 * @return a JCA algorithm name
	 */
	public String getJcaAlgorithm() {
		return this.jcaAlg;
	}
	
	/**
	 * <p>
	 * Return the JCA encryption algorithm.
	 * </p>
	 * 
	 * @return a JCA algorithm name
	 */
	public String getJcaEncryptionAlgorithm() {
		return this.jcaEncryptionAlg;
	}
	
	/**
	 * <p>
	 * Returns the encryption key length in bytes.
	 * </p>
	 * 
	 * @return the encryption key length in bytes
	 */
	public int getEncryptionKeyLength() {
		return this.encKeyLength;
	}
	
	/**
	 * <p>
	 * Returns the password-based encryption key management algorithm corresponding to the specified JWA registered algorithm name.
	 * </p>
	 * 
	 * @param alg a JWA registered algorithm name
	 * 
	 * @return a password-based encryption key management algorithm
	 * 
	 * @throws IllegalArgumentException if the specified algorithm is not a PBES2 algorithm.
	 */
	public static PBES2Algorithm fromAlgorithm(String alg) {
		switch(alg) {
			case "PBES2-HS256+A128KW": 
				return PBES2_HS256_A128KW;
			case "PBES2-HS384+A192KW": 
				return PBES2_HS384_A192KW;
			case "PBES2-HS512+A256KW": 
				return PBES2_HS512_A256KW;
			default: 
				throw new IllegalArgumentException("Unknown password-based algorithm " + alg);
		}
	}
	
	/**
	 * <p>
	 * Creates a password-based encryption key manager.
	 * </p>
	 *
	 * @param jwk       a password-based encryption key
	 * @param algorithm a password-based encryption key management algorithm
	 *
	 * @return a password-based encryption key manager
	 *
	 * @throws JWAProcessingException if there was an error creating the key manager
	 */
	private static PBES2KeyManager createPBES2KeyManager(PBES2JWK jwk, PBES2Algorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerPBES2KeyManager(jwk);
	}
	
	/**
	 * <p>
	 * An inner password-based encryption key manager.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerPBES2KeyManager extends PBES2KeyManager {
		
		/**
		 * <p>
		 * Creates an inner password-based encryption key manager.
		 * </p>
		 * 
		 * @param jwk a password-based encryption key
		 * 
		 * @throws JWAProcessingException if there was an error creating the key manager
		 */
		InnerPBES2KeyManager(PBES2JWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = PBES2Algorithm.this;
			this.init();
		}
	}
}