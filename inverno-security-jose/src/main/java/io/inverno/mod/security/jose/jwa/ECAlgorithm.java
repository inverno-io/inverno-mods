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

import io.inverno.mod.security.jose.internal.jwa.ECDH_ESKeyManager;
import io.inverno.mod.security.jose.internal.jwa.ECDH_ES_AESKWKeyManager;
import io.inverno.mod.security.jose.internal.jwa.ECSigner;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import java.util.function.BiFunction;

/**
 * <p>
 * Elliptic Curve algorithms as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a>.
 * </p>
 * 
 * <p>
 * Signature algorithms:
 * </p>
 * 
 * <ul>
 * <li>ES256</li>
 * <li>ES384</li>
 * <li>ES512</li>
 * <li>ES256K (deprecated)</li>
 * </ul>
 *
 * <p>These algorithms are bound to the following Elliptic curves respectively: {@link ECCurve#P_256}, {@link ECCurve#P_384}, {@link ECCurve#P_521} and {@link ECCurve#SECP256K1}.</p>
 * 
 * <p>
 * Key Management algorithms:
 * </p>
 * 
 * <ul>
 * <li>ECDH-ES</li>
 * <li>ECDH-ES+A128KW</li>
 * <li>ECDH-ES+A192KW</li>
 * <li>ECDH-ES+A256KW</li>
 * </ul>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum ECAlgorithm implements JWAAlgorithm<ECJWK> {
	
	/**
	 * ES256 ECDSA signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.4">RFC7518 Section 3.4</a>
	 */
	ES256("ES256", ECAlgorithm::createECSigner, null, null, "SHA256withECDSA", ECCurve.P_256),
	/**
	 * ES384 ECDSA signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.4">RFC7518 Section 3.4</a>
	 */
	ES384("ES384", ECAlgorithm::createECSigner, null, null, "SHA384withECDSA", ECCurve.P_384),
	/**
	 * ES512 ECDSA signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.4">RFC7518 Section 3.4</a>
	 */
	ES512("ES512", ECAlgorithm::createECSigner, null, null, "SHA512withECDSA", ECCurve.P_521),
	/**
	 * ES256K ECDSA signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8812#section-3.2">RFC8812 Section 3.2</a>
	 * 
	 * @deprecated secp256k1 elliptic curve has been disabled in the JDK (>=15), it can be activated by setting jdk.sunec.disableNative property to false ({@code -Djdk.sunec.disableNative=false})
	 */
	@Deprecated
	ES256K("ES256K", ECAlgorithm::createECSigner, null, null, "SHA256withECDSA", ECCurve.SECP256K1),
	
	/**
	 * ECDH-ES Elliptic Curve Diffie-Hellman key agreement algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.6">RFC7518 Section 4.6</a>
	 */
	ECDH_ES("ECDH-ES", null, ECAlgorithm::createECDH_ESKeyManager, null, null),
	/**
	 * ECDH-ES+A128KW Elliptic Curve Diffie-Hellman key agreement algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.6">RFC7518 Section 4.6</a>
	 */
	ECDH_ES_A128KW("ECDH-ES+A128KW", null, ECAlgorithm::createECDH_ES_AESKWKeyManager, null, "A128KW"),
	/**
	 * ECDH-ES+A192KW Elliptic Curve Diffie-Hellman key agreement algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.6">RFC7518 Section 4.6</a>
	 */
	ECDH_ES_A192KW("ECDH-ES+A192KW", null, ECAlgorithm::createECDH_ES_AESKWKeyManager, null, "A192KW"),
	/**
	 * ECDH-ES+A256KW Elliptic Curve Diffie-Hellman key agreement algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.6">RFC7518 Section 4.6</a>
	 */
	ECDH_ES_A256KW("ECDH-ES+A256KW", null, ECAlgorithm::createECDH_ES_AESKWKeyManager, null, "A256KW");
	
	/**
	 * The JWA registered algorithm name.
	 */
	private final String alg;
	/**
	 * The JWA signer factory.
	 */
	private final BiFunction<ECJWK, ECAlgorithm, JWASigner> signerFactory;
	/**
	 * The JWA key manager factory.
	 */
	private final BiFunction<ECJWK, ECAlgorithm, JWAKeyManager> keyManagerFactory;
	/**
	 * The JWA cipher factory.
	 */
	private final BiFunction<ECJWK, ECAlgorithm, JWACipher> cipherFactory;
	/**
	 * The corresponding JCA signing algorithm when applicable.
	 */
	private final String jcaAlg;
	/**
	 * The elliptic curve bound to the algorithm when applicable.
	 */
	private final ECCurve curve;
	/**
	 * The JCA key wrapping algorithm when applicable.
	 */
	private final String keyWrappingAlgorithm;

	/**
	 * <p>
	 * Creates an Elliptic curve algorithm.
	 * </p>
	 *
	 * @param alg                  the JWA registered algorithm name
	 * @param signerFactory        the JWA signer factory
	 * @param keyManagerFactory    the JWA key manager factory
	 * @param cipherFactory        the JWA cipher factory
	 * @param keyWrappingAlgorithm the key wrapping algorithm
	 */
	ECAlgorithm(String alg, BiFunction<ECJWK, ECAlgorithm, JWASigner> signerFactory, BiFunction<ECJWK, ECAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<ECJWK, ECAlgorithm, JWACipher> cipherFactory, String keyWrappingAlgorithm) {
		this(alg, signerFactory, keyManagerFactory, cipherFactory, null, null, keyWrappingAlgorithm);
	}
	
	/**
	 * <p>
	 * Creates an Elliptic curve algorithm.
	 * </p>
	 *
	 * @param alg               the JWA registered algorithm name
	 * @param signerFactory     the JWA signer factory
	 * @param keyManagerFactory the JWA key manager factory
	 * @param cipherFactory     the JWA cipher factory
	 * @param jcaAlg            the JCA signing algorithm
	 * @param curve             the Elliptic curve bound to the algorithm
	 */
	ECAlgorithm(String alg, BiFunction<ECJWK, ECAlgorithm, JWASigner> signerFactory, BiFunction<ECJWK, ECAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<ECJWK, ECAlgorithm, JWACipher> cipherFactory, String jcaAlg, ECCurve curve) {
		this(alg, signerFactory, keyManagerFactory, cipherFactory, jcaAlg, curve, null);
	}
	
	/**
	 * <p>
	 * Creates an Elliptic curve algorithm.
	 * </p>
	 *
	 * @param alg                  the JWA registered algorithm name
	 * @param signerFactory        the JWA signer factory
	 * @param keyManagerFactory    the JWA key manager factory
	 * @param cipherFactory        the JWA cipher factory
	 * @param jcaAlg               the JCA signing algorithm
	 * @param curve                the Elliptic curve bound to the algorithm
	 * @param keyWrappingAlgorithm the key wrapping algorithm
	 */
	ECAlgorithm(String alg, BiFunction<ECJWK, ECAlgorithm, JWASigner> signerFactory, BiFunction<ECJWK, ECAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<ECJWK, ECAlgorithm, JWACipher> cipherFactory, String jcaAlg, ECCurve curve, String keyWrappingAlgorithm) {
		this.alg = alg;
		this.signerFactory = signerFactory;
		this.keyManagerFactory = keyManagerFactory;
		this.cipherFactory = cipherFactory;
		this.jcaAlg = jcaAlg;
		this.curve = curve;
		this.keyWrappingAlgorithm = keyWrappingAlgorithm;
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
	public JWASigner createSigner(ECJWK jwk) throws JWAProcessingException {
		if(this.signerFactory == null) {
			throw new JWAProcessingException("Not a signature algorithm: " + this.alg);
		}
		return this.signerFactory.apply(jwk, this);
	}

	@Override
	public JWAKeyManager createKeyManager(ECJWK jwk) throws JWAProcessingException {
		if(this.keyManagerFactory == null) {
			throw new JWAProcessingException("Not a key management algorithm: " + this.alg);
		}
		return this.keyManagerFactory.apply(jwk, this);
	}

	@Override
	public JWACipher createCipher(ECJWK jwk) throws JWAProcessingException {
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
	 * <p>
	 * Note that this is only applicable for signing algorithms.
	 * </p>
	 * 
	 * @return a JCA algorithm name or null
	 */
	public String getJcaAlgorithm() {
		return this.jcaAlg;
	}

	/**
	 * <p>
	 * Returns the Elliptic curve bound to the algorithm.
	 * </p>
	 * 
	 * <p>
	 * Note that this does not apply to ECDH algorithms for which the curve is defined by an ephemeral key.
	 * </p>
	 * 
	 * @return an Elliptic curve or null
	 */
	public ECCurve getCurve() {
		return this.curve;
	}

	/**
	 * <p>
	 * Returns a JCA key wrapping algorithm to be used to wrap a derived key.
	 * </p>
	 * 
	 * <p>
	 * Note that this does not apply to signing algorithm.
	 * </p>
	 * 
	 * @return a JCA key wrapping algorithm or null
	 */
	public String getKeyWrappingAlgorithm() {
		return this.keyWrappingAlgorithm;
	}
	
	/**
	 * <p>
	 * Returns the Elliptic Curve algorithm corresponding to the specified JWA registered algorithm name.
	 * </p>
	 *
	 * @param alg a JWA registered algorithm name
	 *
	 * @return an Elliptic curve algorithm
	 *
	 * @throws IllegalArgumentException if the specified algorithm is not an EC algorithm
	 */
	public static ECAlgorithm fromAlgorithm(String alg) throws IllegalArgumentException {
		switch(alg) {
			case "ES256": 
				return ES256;
			case "ES384": 
				return ES384;
			case "ES512": 
				return ES512;
			case "ES256K": 
				return ES256K;
			case "ECDH-ES": 
				return ECDH_ES;
			case "ECDH-ES+A128KW": 
				return ECDH_ES_A128KW;
			case "ECDH-ES+A192KW": 
				return ECDH_ES_A192KW;
			case "ECDH-ES+A256KW": 
				return ECDH_ES_A256KW;
			default: 
				throw new IllegalArgumentException("Unknown EC algorithm " + alg);
		}
	}
	
	/**
	 * <p>
	 * Creates an Elliptic curve signer.
	 * </p>
	 *
	 * @param jwk       an Elliptic curve key
	 * @param algorithm an Elliptic curve algorithm
	 *
	 * @return an Elliptic curve signer
	 *
	 * @throws JWAProcessingException if there was an error creating the signer
	 */
	private static ECSigner createECSigner(ECJWK jwk, ECAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerECSigner(jwk);
	}
	
	/**
	 * <p>
	 * An inner Elliptic curve signer.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerECSigner extends ECSigner {
		
		/**
		 * <p>
		 * Creates an inner Elliptic curve signer.
		 * </p>
		 * 
		 * @param jwk an Elliptic curve key
		 * 
		 * @throws JWAProcessingException if there was an error creating the signer
		 */
		InnerECSigner(ECJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = ECAlgorithm.this;
			this.init();
		}
	}
	
	/**
	 * <p>
	 * Creates an Elliptic curve ECDH key manager.
	 * </p>
	 *
	 * @param jwk       an Elliptic curve key
	 * @param algorithm an Elliptic curve algorithm
	 *
	 * @return an Elliptic curve signer
	 *
	 * @throws JWAProcessingException if there was an error creating the key manager
	 */
	private static ECDH_ESKeyManager createECDH_ESKeyManager(ECJWK jwk, ECAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerECDH_ESKeyManager(jwk);
	}
	
	/**
	 * <p>
	 * An inner Elliptic curve ECDH key manager.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerECDH_ESKeyManager extends ECDH_ESKeyManager {
		
		/**
		 * <p>
		 * Creates an inner Elliptic curve ECDH key manager.
		 * </p>
		 * 
		 * @param jwk an Elliptic curve key
		 * @throws JWAProcessingException if there was an error creating the key manager
		 */
		InnerECDH_ESKeyManager(ECJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = ECAlgorithm.this;
			this.init();
		}
	}
	
	/**
	 * <p>
	 * Creates an Elliptic curve ECDH with key wrapping key manager.
	 * </p>
	 *
	 * @param jwk       an Elliptic curve key
	 * @param algorithm an Elliptic curve algorithm
	 *
	 * @return an Elliptic curve signer
	 *
	 * @throws JWAProcessingException if there was an error creating the key manager
	 */
	private static ECDH_ES_AESKWKeyManager createECDH_ES_AESKWKeyManager(ECJWK jwk, ECAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerECDH_ES_AESKWKeyManager(jwk);
	}
	
	/**
	 * <p>
	 * An inner Elliptic curve ECDH with key wrapping key manager.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerECDH_ES_AESKWKeyManager extends ECDH_ES_AESKWKeyManager {
		
		/**
		 * <p>
		 * Creates an inner Elliptic curve ECDH with key wrapping key manager.
		 * </p>
		 * 
		 * @param jwk an Elliptic curve key
		 * @throws JWAProcessingException if there was an error creating the key manager
		 */
		InnerECDH_ES_AESKWKeyManager(ECJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = ECAlgorithm.this;
			this.init();
		}
	}
}
