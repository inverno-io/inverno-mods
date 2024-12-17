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

import io.inverno.mod.security.jose.internal.jwa.OKP_ECDH_ESKeyManager;
import io.inverno.mod.security.jose.internal.jwa.OKP_ECDH_ES_AESKWKeyManager;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import java.util.function.BiFunction;

/**
 * <p>
 * OKP Elliptic curve key management algorithms as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.1">RFC8037 Section 3.1</a>.
 * </p>
 * 
 * <p>
 * Key management algorithms:
 * </p>
 * 
 * <ul>
 * <li>ECDH_ES-ES</li>
 * <li>ECDH-ES+A128KW</li>
 * <li>ECDH-ES+A192KW</li>
 * <li>ECDH-ES+A256KW</li>
 * </ul>
 * 
 * <p>These algorithms must be used with Octet Key Pair curves {@link OKPCurve#X25519} and {@link OKPCurve#X448}, they extends ECDH-ES algorithms with NIST Elliptic curves defined by {@link ECAlgorithm} and {@link ECCurve}.</p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum XECAlgorithm implements JWAAlgorithm<XECJWK> {
	
	/**
	 * ECDH-ES with X25519 or X448 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.2">RFC8037 Section 3.2</a>
	 */
	ECDH_ES("ECDH-ES", null, XECAlgorithm::createOKP_ECDH_ESKeyManager, null, null),
	/**
	 * ECDH-ES+A128KW with X25519 or X448 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.2">RFC8037 Section 3.2</a>
	 */
	ECDH_ES_A128KW("ECDH-ES+A128KW", null, XECAlgorithm::createOKP_ECDH_ES_AESKWKeyManager, null, "A128KW"),
	/**
	 * ECDH-ES+A192KW with X25519 or X448 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.2">RFC8037 Section 3.2</a>
	 */
	ECDH_ES_A192KW("ECDH-ES+A192KW", null, XECAlgorithm::createOKP_ECDH_ES_AESKWKeyManager, null, "A192KW"),
	/**
	 * ECDH-ES+A256KW with X25519 or X448 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.2">RFC8037 Section 3.2</a>
	 */
	ECDH_ES_A256KW("ECDH-ES+A256KW", null, XECAlgorithm::createOKP_ECDH_ES_AESKWKeyManager, null, "A256KW");
	
	/**
	 * The JWA registered algorithm name.
	 */
	private final String alg;
	/**
	 * The JWA signer factory.
	 */
	private final BiFunction<XECJWK, XECAlgorithm, JWASigner> signerFactory;
	/**
	 * The JWA key manager factory.
	 */
	private final BiFunction<XECJWK, XECAlgorithm, JWAKeyManager> keyManagerFactory;
	/**
	 * The JWA cipher factory.
	 */
	private final BiFunction<XECJWK, XECAlgorithm, JWACipher> cipherFactory;
	/**
	 * The JCA key wrapping algorithm.
	 */
	private final String keyWrappingAlgorithm;

	/**
	 * <p>
	 * Creates a OKP Elliptic curve key management algorithm.
	 * </p>
	 * 
	 * @param alg                  the JWA registered algorithm name
	 * @param signerFactory        the JWA signer factory
	 * @param keyManagerFactory    the JWA key manager factory
	 * @param cipherFactory        the JWA cipher factory
	 * @param keyWrappingAlgorithm the key wrapping algorithm
	 */
	XECAlgorithm(String alg, BiFunction<XECJWK, XECAlgorithm, JWASigner> signerFactory, BiFunction<XECJWK, XECAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<XECJWK, XECAlgorithm, JWACipher> cipherFactory, String keyWrappingAlgorithm) {
		this.alg = alg;
		this.signerFactory = signerFactory;
		this.keyManagerFactory = keyManagerFactory;
		this.cipherFactory = cipherFactory;
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
	public JWASigner createSigner(XECJWK jwk) throws JWAProcessingException {
		if(this.signerFactory == null) {
			throw new JWAProcessingException("Not a signature algorithm: " + this.alg);
		}
		return this.signerFactory.apply(jwk, this);
	}

	@Override
	public JWAKeyManager createKeyManager(XECJWK jwk) throws JWAProcessingException {
		if(this.keyManagerFactory == null) {
			throw new JWAProcessingException("Not a key management algorithm: " + this.alg);
		}
		return this.keyManagerFactory.apply(jwk, this);
	}

	@Override
	public JWACipher createCipher(XECJWK jwk) throws JWAProcessingException {
		if(this.cipherFactory == null) {
			throw new JWAProcessingException("Not an encryption algorithm: " + this.alg);
		}
		return this.cipherFactory.apply(jwk, this);
	}
	
	/**
	 * <p>
	 * Returns a JCA key wrapping algorithm to be used to wrap a derived key.
	 * </p>
	 * 
	 * @return a JCA key wrapping algorithm or null
	 */
	public String getKeyWrappingAlgorithm() {
		return this.keyWrappingAlgorithm;
	}
	
	/**
	 * <p>
	 * Returns the OKP Elliptic curve key management algorithm corresponding to the specified JWA registered algorithm name.
	 * </p>
	 *
	 * @param alg a JWA registered algorithm name
	 *
	 * @return an OKP Elliptic curve key management algorithm
	 *
	 * @throws IllegalArgumentException if the specified algorithm is not an OKP Elliptic curve key management algorithm
	 */
	public static XECAlgorithm fromAlgorithm(String alg) throws IllegalArgumentException {
		switch(alg) {
			case "ECDH-ES": {
					return ECDH_ES;
				}
			case "ECDH-ES+A128KW": {
					return ECDH_ES_A128KW;
				}
			case "ECDH-ES+A192KW": {
					return ECDH_ES_A192KW;
				}
			case "ECDH-ES+A256KW": {
					return ECDH_ES_A256KW;
				}
			default: 
				throw new IllegalArgumentException("Unknown OKP algorithm " + alg);
		}
	}
	
	/**
	 * <p>
	 * Creates an OKP ECDH-ES key manager.
	 * </p>
	 *
	 * @param jwk       an OKP Elliptic curve key
	 * @param algorithm an OKP Elliptic curve algorithm
	 *
	 * @return an OKP ECDH-ES key manager.
	 *
	 * @throws JWAProcessingException if there was an error creating the key manager
	 */
	private static OKP_ECDH_ESKeyManager createOKP_ECDH_ESKeyManager(XECJWK jwk, XECAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerOKP_ECDH_ESKeyManager(jwk);
	}
	
	/**
	 * <p>
	 * An inner OKP ECDH-ES key manager.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerOKP_ECDH_ESKeyManager extends OKP_ECDH_ESKeyManager {
		
		/**
		 * <p>
		 * Creates an OKP ECDH-ES key manager.
		 * </p>
		 * 
		 * @param jwk an OKP Elliptic curve key
		 * 
		 * @throws JWAProcessingException if there was an error creating the key manager
		 */
		InnerOKP_ECDH_ESKeyManager(XECJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = XECAlgorithm.this;
			this.init();
		}
	}
	
	/**
	 * <p>
	 * Creates an OKP ECDH-ES with key wrapping key manager.
	 * </p>
	 *
	 * @param jwk       an OKP Elliptic curve key
	 * @param algorithm an OKP Elliptic curve algorithm
	 *
	 * @return an OKP ECDH-ES with key wrapping key manager.
	 *
	 * @throws JWAProcessingException if there was an error creating the key manager
	 */
	private static OKP_ECDH_ES_AESKWKeyManager createOKP_ECDH_ES_AESKWKeyManager(XECJWK jwk, XECAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerOKP_ECDH_ES_AESKWKeyManager(jwk);
	}
	
	/**
	 * <p>
	 * An inner OKP ECDH-ES with key wrapping key manager.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerOKP_ECDH_ES_AESKWKeyManager extends OKP_ECDH_ES_AESKWKeyManager {
		
		/**
		 * <p>
		 * Creates an OKP ECDH-ES with key wrapping key manager.
		 * </p>
		 * 
		 * @param jwk an OKP Elliptic curve key
		 * 
		 * @throws JWAProcessingException if there was an error creating the key manager
		 */
		InnerOKP_ECDH_ES_AESKWKeyManager(XECJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = XECAlgorithm.this;
			this.init();
		}
	}
}
