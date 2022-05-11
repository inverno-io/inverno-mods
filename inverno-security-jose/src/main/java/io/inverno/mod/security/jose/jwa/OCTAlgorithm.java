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

import io.inverno.mod.security.jose.internal.jwa.AESCBCCipher;
import io.inverno.mod.security.jose.internal.jwa.AESGCMCipher;
import io.inverno.mod.security.jose.internal.jwa.AESGCMKWKeyManager;
import io.inverno.mod.security.jose.internal.jwa.AESKWKeyManager;
import io.inverno.mod.security.jose.internal.jwa.HMACSigner;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.util.function.BiFunction;

/**
 * <p>
 * Octect symmetric algorithms as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a>.
 * </p>
 * 
 * <p>
 * Signature algorithms:
 * </p>
 * 
 * <ul>
 * <li>HS256</li>
 * <li>HS384</li>
 * <li>HS512</li>
 * </ul>
 * 
 * <p>
 * Key Management algorithms:
 * </p>
 * 
 * <ul>
 * <li>A128KW</li>
 * <li>A256KW</li>
 * <li>A512KW</li>
 * <li>A128GCMKW</li>
 * <li>A192GCMKW</li>
 * <li>A256GCMKW</li>
 * </ul>
 * 
 * <p>
 * Encryption algorithms:
 * </p>
 * 
 * <ul>
 * <li>A128GCM</li>
 * <li>A192GCM</li>
 * <li>A256GCM</li>
 * <li>A128CBC-HS256</li>
 * <li>A192CBC-HS384</li>
 * <li>A256CBC-HS512</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum OCTAlgorithm implements JWAAlgorithm<OCTJWK> {
	
	/**
	 * HS256 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.2">RFC7518 Section 3.2</a>
	 */
	HS256("HS256", OCTAlgorithm::createHMACSigner, null, null, "HmacSHA256", 32),
	/**
	 * HS384 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.2">RFC7518 Section 3.2</a>
	 */
	HS384("HS384", OCTAlgorithm::createHMACSigner, null, null, "HmacSHA384", 48),
	/**
	 * HS512 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.2">RFC7518 Section 3.2</a>
	 */
	HS512("HS512", OCTAlgorithm::createHMACSigner, null, null, "HmacSHA512", 64),
	
	/**
	 * A128KW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.4">RFC7518 Section 4.4</a>
	 */
	A128KW("A128KW", null, OCTAlgorithm::createAESKWKeyManager, null, "AESWrap_128", 16),
	/**
	 * A192KW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.4">RFC7518 Section 4.4</a>
	 */
	A192KW("A192KW", null, OCTAlgorithm::createAESKWKeyManager, null, "AESWrap_192", 24),
	/**
	 * A256KW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.4">RFC7518 Section 4.4</a>
	 */
	A256KW("A256KW", null, OCTAlgorithm::createAESKWKeyManager, null, "AESWrap_256", 32),
	
	/**
	 * A128GCMKW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.7">RFC7518 Section 4.7</a>
	 */
	A128GCMKW("A128GCMKW", null, OCTAlgorithm::createAESGCMKWKeyManager, null, "AES/GCM/NoPadding", 16, 12, 16),
	/**
	 * A192GCMKW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.7">RFC7518 Section 4.7</a>
	 */
	A192GCMKW("A192GCMKW", null, OCTAlgorithm::createAESGCMKWKeyManager, null, "AES/GCM/NoPadding", 24, 12, 16),
	/**
	 * A256GCMKW key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.7">RFC7518 Section 4.7</a>
	 */
	A256GCMKW("A256GCMKW", null, OCTAlgorithm::createAESGCMKWKeyManager, null, "AES/GCM/NoPadding", 32, 12, 16),
	
	/**
	 * A128CBC-HS256 encryption algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-5.2.6">RFC7518 Section 5.2.6</a>
	 */
	A128CBC_HS256("A128CBC-HS256", null, null, OCTAlgorithm::createAESCBCCipher, "AES/CBC/PKCS5Padding", 16, 16, 16, "HmacSHA256", 16),
	/**
	 * A192CBC-HS384 encryption algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-5.2.6">RFC7518 Section 5.2.6</a>
	 */
	A192CBC_HS384("A192CBC-HS384", null, null, OCTAlgorithm::createAESCBCCipher, "AES/CBC/PKCS5Padding", 24, 16, 16, "HmacSHA384", 24),
	/**
	 * A256CBC-HS512 encryption algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-5.2.6">RFC7518 Section 5.2.6</a>
	 */
	A256CBC_HS512("A256CBC-HS512", null, null, OCTAlgorithm::createAESCBCCipher, "AES/CBC/PKCS5Padding", 32, 16, 16, "HmacSHA512", 32),
	
	/**
	 * A128GCM encryption algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-5.3">RFC7518 Section 5.3</a>
	 */
	A128GCM("A128GCM", null, null, OCTAlgorithm::createAESGCMCipher, "AES/GCM/NoPadding", 16, 12, 16),
	/**
	 * A192GCM encryption algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-5.3">RFC7518 Section 5.3</a>
	 */
	A192GCM("A192GCM", null, null, OCTAlgorithm::createAESGCMCipher, "AES/GCM/NoPadding", 24, 12, 16),
	/**
	 * A256GCM encryption algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-5.3">RFC7518 Section 5.3</a>
	 */
	A256GCM("A256GCM", null, null, OCTAlgorithm::createAESGCMCipher, "AES/GCM/NoPadding", 32, 12, 16);
	
	/**
	 * The JWA registered algorithm name.
	 */
	private final String alg;
	/**
	 * The JWA signer factory.
	 */
	private final BiFunction<OCTJWK, OCTAlgorithm, JWASigner> signerFactory;
	/**
	 * The JWA key manager factory.
	 */
	private final BiFunction<OCTJWK, OCTAlgorithm, JWAKeyManager> keyManagerFactory;
	/**
	 * The JWA cipher factory.
	 */
	private final BiFunction<OCTJWK, OCTAlgorithm, JWACipher> cipherFactory;
	/**
	 * The corresponding JCA algorithm.
	 */
	private final String jcaAlg;
	/**
	 * The encryption key length in bytes.
	 */
	private final int encKeyLength;
	/**
	 * The initialization vector length in bytes when applicable.
	 */
	private final Integer ivLength;
	/**
	 * The authentication tag length in bytes when applicable.
	 */
	private final Integer tagLength;
	/**
	 * The JCA Mac algorithm when applicable.
	 */
	private final String jcaMacAlg;
	/**
	 * The Mac key length in bytes when applicable.
	 */
	private final Integer macKeyLength;
	
	/**
	 * <p>
	 * Creates an Octet symmetic algorithm.
	 * </p>
	 *
	 * @param alg               the JWA registered algorithm name
	 * @param signerFactory     the JWA signer factory
	 * @param keyManagerFactory the JWA key manager factory
	 * @param cipherFactory     the JWA cipher factory
	 * @param jcaAlg            the JCA algorithm
	 * @param encKeyLength      the encryption key length in bytes
	 */
	private OCTAlgorithm(String alg, BiFunction<OCTJWK, OCTAlgorithm, JWASigner> signerFactory, BiFunction<OCTJWK, OCTAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<OCTJWK, OCTAlgorithm, JWACipher> cipherFactory, String jcaAlg, int encKeyLength) {
		this(alg, signerFactory, keyManagerFactory, cipherFactory, jcaAlg, encKeyLength, null, null, null, null);
	}
	
	/**
	 * <p>
	 * Creates an Octet symmetric algorithm.
	 * </p>
	 *
	 * @param alg               the JWA registered algorithm name
	 * @param signerFactory     the JWA signer factory
	 * @param keyManagerFactory the JWA key manager factory
	 * @param cipherFactory     the JWA cipher factory
	 * @param jcaAlg            the JCA algorithm
	 * @param encKeyLength      the encryption key length in bytes
	 * @param ivLength          the initialization vector length in bytes
	 * @param tagLength         the authentication tag length in bytes
	 */
	private OCTAlgorithm(String alg, BiFunction<OCTJWK, OCTAlgorithm, JWASigner> signerFactory, BiFunction<OCTJWK, OCTAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<OCTJWK, OCTAlgorithm, JWACipher> cipherFactory, String jcaAlg, int encKeyLength, Integer ivLength, Integer tagLength) {
		this(alg, signerFactory, keyManagerFactory, cipherFactory, jcaAlg, encKeyLength, ivLength, tagLength, null, null);
	}
	
	/**
	 * <p>
	 * Creates an Octet symmetric algorithm.
	 * </p>
	 *
	 * @param alg               the JWA registered algorithm name
	 * @param signerFactory     the JWA signer factory
	 * @param keyManagerFactory the JWA key manager factory
	 * @param cipherFactory     the JWA cipher factory
	 * @param jcaAlg            the JCA algorithm
	 * @param encKeyLength      the encryption key length in bytes
	 * @param ivLength          the initialization vector length in bytes
	 * @param tagLength         the authentication tag length in bytes
	 * @param jcaMacAlg         the JCA Mac algorithm
	 * @param macKeyLength      the Mac key length in bytes
	 */
	private OCTAlgorithm(String alg, BiFunction<OCTJWK, OCTAlgorithm, JWASigner> signerFactory, BiFunction<OCTJWK, OCTAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<OCTJWK, OCTAlgorithm, JWACipher> cipherFactory, String jcaAlg, int encKeyLength, Integer ivLength, Integer tagLength, String jcaMacAlg, Integer macKeyLength) {
		this.alg = alg;
		this.signerFactory = signerFactory;
		this.keyManagerFactory = keyManagerFactory;
		this.cipherFactory = cipherFactory;
		this.jcaAlg = jcaAlg;
		this.encKeyLength = encKeyLength;
		this.ivLength = ivLength;
		this.tagLength = tagLength;
		this.jcaMacAlg = jcaMacAlg;
		this.macKeyLength = macKeyLength;
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
	public JWASigner createSigner(OCTJWK jwk) throws JWAProcessingException {
		if(this.signerFactory == null) {
			throw new JWAProcessingException("Not a signature algorithm: " + this.alg);
		}
		return this.signerFactory.apply(jwk, this);
	}
	
	@Override
	public JWAKeyManager createKeyManager(OCTJWK jwk) throws JWAProcessingException {
		if(this.keyManagerFactory == null) {
			throw new JWAProcessingException("Not a key management algorithm: " + this.alg);
		}
		return this.keyManagerFactory.apply(jwk, this);
	}
	
	@Override
	public JWACipher createCipher(OCTJWK jwk) throws JWAProcessingException {
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
		return jcaAlg;
	}

	/**
	 * <p>
	 * Returns the encryption key length in bytes.
	 * </p>
	 * 
	 * @return the encryption key length in bytes
	 */
	public int getEncryptionKeyLength() {
		return encKeyLength;
	}

	/**
	 * <p>
	 * Returns the initialization vector length in bytes.
	 * </p>
	 * 
	 * <p>
	 * Note that this only applies to encryption algorithms.
	 * </p>
	 * 
	 * @return the initialization vector key length in bytes or null
	 */
	public Integer getInitializationVectorLength() {
		return ivLength;
	}
	
	/**
	 * <p>
	 * Returns the authentication tag length in bytes.
	 * </p>
	 * 
	 * <p>
	 * Note that this only applies to encryption algorithms.
	 * </p>
	 * 
	 * @return the authentication tag key length in bytes or null
	 */
	public Integer getAuthenticationTagLength() {
		return tagLength;
	}
	
	/**
	 * <p>
	 * Return the JCA Mac algorithm.
	 * </p>
	 * 
	 * <p>
	 * Note that this only applies to algorithms where a MAC is computed.
	 * </p>
	 * 
	 * @return a JCA Mac algorithm or null
	 */
	public String getMacAlgorithm() {
		return jcaMacAlg;
	}
	
	/**
	 * <p>
	 * Return the Mac key length in bytes.
	 * </p>
	 * 
	 * <p>
	 * Note that this only applies to algorithms where a MAC is computed.
	 * </p>
	 * 
	 * @return a Mac key length in bytes
	 */
	public Integer getMacKeyLength() {
		return macKeyLength;
	}
	
	/**
	 * <p>
	 * Returns the Octet symmetric algorithm corresponding to the specified JWA registered algorithm name.
	 * </p>
	 * 
	 * @param alg a JWA registered algorithm name
	 * 
	 * @return an octet symmetric algorithm
	 * 
	 * @throws IllegalArgumentException if the specified algorithm is not an OCT algorithm.
	 */
	public static OCTAlgorithm fromAlgorithm(String alg) throws IllegalArgumentException {
		switch(alg) {
			case "HS256": 
				return HS256;
			case "HS384": 
				return HS384;
			case "HS512": 
				return HS512;
			case "A128KW": 
				return A128KW;
			case "A192KW": 
				return A192KW;
			case "A256KW": 
				return A256KW;
			case "A128GCMKW": 
				return A128GCMKW;
			case "A192GCMKW": 
				return A192GCMKW;
			case "A256GCMKW": 
				return A256GCMKW;
			case "A128CBC-HS256": 
				return A128CBC_HS256;
			case "A192CBC-HS384": 
				return A192CBC_HS384;
			case "A256CBC-HS512": 
				return A256CBC_HS512;
			case "A128GCM": 
				return A128GCM;
			case "A192GCM":	
				return A192GCM;
			case "A256GCM":	
				return A256GCM;
			default :
				throw new IllegalArgumentException("Unknown OCT algorithm " + alg);
		}
	}
	
	/**
	 * <p>
	 * Creates an HMAC signer.
	 * </p>
	 *
	 * @param jwk       a symmetric key
	 * @param algorithm an Octet symmetric algorithm
	 *
	 * @return an HMAC signer
	 *
	 * @throws JWAProcessingException if there was an error creating the signer
	 */
	private static HMACSigner createHMACSigner(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerHMACSigner(jwk);
	}
	
	/**
	 * <p>
	 * An inner HMAC signer.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerHMACSigner extends HMACSigner {

		/**
		 * <p>
		 * Creates an inner HMAC signer.
		 * </p>
		 * 
		 * @param jwk an Octet symmetric key
		 * 
		 * @throws JWAProcessingException if there was an error creating the signer
		 */
		InnerHMACSigner(OCTJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = OCTAlgorithm.this;
			this.init();
		}
	}
	
	/**
	 * <p>
	 * Creates an AES_CBC cipher.
	 * </p>
	 *
	 * @param jwk       a symmetric key
	 * @param algorithm an Octet symmetric algorithm
	 *
	 * @return an HMAC signer
	 *
	 * @throws JWAProcessingException if there was an error creating the cipher
	 */
	private static AESCBCCipher createAESCBCCipher(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerAESCBCCipher(jwk);
	}

	/**
	 * <p>
	 * An inner AES_CBC cipher.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */	
	private class InnerAESCBCCipher extends AESCBCCipher {

		/**
		 * <p>
		 * Creates an inner AES_CBC cipher.
		 * </p>
		 * 
		 * @param jwk an Octet symmetric key
		 * 
		 * @throws JWAProcessingException if there was an error creating the signer
		 */
		InnerAESCBCCipher(OCTJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = OCTAlgorithm.this;
			this.init();
		}
	}
	
	/**
	 * <p>
	 * Creates an AES_GCM cipher.
	 * </p>
	 *
	 * @param jwk       a symmetric key
	 * @param algorithm an Octet symmetric algorithm
	 *
	 * @return an HMAC signer
	 *
	 * @throws JWAProcessingException if there was an error creating the cipher
	 */
	private static AESGCMCipher createAESGCMCipher(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerAESGCMCipher(jwk);
	}
	
	/**
	 * <p>
	 * An inner AES_GCM cipher.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerAESGCMCipher extends AESGCMCipher {

		/**
		 * <p>
		 * Creates an inner AES_GCM cipher.
		 * </p>
		 * 
		 * @param jwk an Octet symmetric key
		 * 
		 * @throws JWAProcessingException if there was an error creating the cipher
		 */
		InnerAESGCMCipher(OCTJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = OCTAlgorithm.this;
			this.init();
		}
	}
	
	/**
	 * <p>
	 * Creates an AES Key Wrap key manager.
	 * </p>
	 *
	 * @param jwk       a symmetric key
	 * @param algorithm an Octet symmetric algorithm
	 *
	 * @return an HMAC signer
	 *
	 * @throws JWAProcessingException if there was an error creating the key manager
	 */
	private static AESKWKeyManager createAESKWKeyManager(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerAESKWKeyManager(jwk);
	}
	
	/**
	 * <p>
	 * An inner AES Key Wrap key manager.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerAESKWKeyManager extends AESKWKeyManager {

		/**
		 * <p>
		 * Creates an inner AES Key Wrap cipher.
		 * </p>
		 * 
		 * @param jwk an Octet symmetric key
		 * 
		 * @throws JWAProcessingException if there was an error creating the key manager
		 */
		InnerAESKWKeyManager(OCTJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = OCTAlgorithm.this;
			this.init();
		}
	}
	
	/**
	 * <p>
	 * Creates an AES_GCM Key Wrap key manager.
	 * </p>
	 *
	 * @param jwk       a symmetric key
	 * @param algorithm an Octet symmetric algorithm
	 *
	 * @return an HMAC signer
	 *
	 * @throws JWAProcessingException if there was an error creating the key manager
	 */
	private static AESGCMKWKeyManager createAESGCMKWKeyManager(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerAESGCMKWKeyManager(jwk);
	}
	
	/**
	 * <p>
	 * An inner AES_GCM Key Wrap key manager.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerAESGCMKWKeyManager extends AESGCMKWKeyManager {

		/**
		 * <p>
		 * Creates an inner AES_GCM Key Wrap cipher.
		 * </p>
		 * 
		 * @param jwk an Octet symmetric key
		 * 
		 * @throws JWAProcessingException if there was an error creating the key manager
		 */
		InnerAESGCMKWKeyManager(OCTJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = OCTAlgorithm.this;
			this.init();
		}
	}
}
