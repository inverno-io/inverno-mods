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

import io.inverno.mod.security.jose.internal.jwa.EdDSASigner;
import io.inverno.mod.security.jose.jwk.okp.EdECJWK;
import java.util.function.BiFunction;

/**
 * <p>
 * Edward-curve digital signature algorithms as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.1">RFC8037 Section 3.1</a>.
 * </p>
 * 
 * <p>
 * Signature algorithms:
 * </p>
 * 
 * <ul>
 * <li>EdDSA with Ed25519 curve</li>
 * <li>EdDSA with Ed448 curve</li>
 * </ul>
 * 
 * <p>These algorithms are bound to the following Elliptic curves respectively: {@link OKPCurve#ED25519} and {@link OKPCurve#ED448}.</p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum EdECAlgorithm implements JWAAlgorithm<EdECJWK> {
	
	/**
	 * EdDSA with Ed25519 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.1">RFC8037 Section 3.1</a>
	 */
	EDDSA_ED25519("EdDSA", EdECAlgorithm::createEdDSASigner, null, null, "Ed25519", OKPCurve.ED25519),
	/**
	 * EdDSA with Ed448 curve as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8037#section-3.1">RFC8037 Section 3.1</a>
	 */
	EDDSA_ED448("EdDSA", EdECAlgorithm::createEdDSASigner, null, null, "Ed448", OKPCurve.ED448);
	
	/**
	 * The JWA registered algorithm name.
	 */
	private final String alg;
	/**
	 * The JWA signer factory.
	 */
	private final BiFunction<EdECJWK, EdECAlgorithm, JWASigner> signerFactory;
	/**
	 * The JWA key manager factory.
	 */
	private final BiFunction<EdECJWK, EdECAlgorithm, JWAKeyManager> keyManagerFactory;
	/**
	 * The JWA cipher factory.
	 */
	private final BiFunction<EdECJWK, EdECAlgorithm, JWACipher> cipherFactory;
	/**
	 * The corresponding JCA signing algorithm.
	 */
	private final String jcaAlg;
	/**
	 * The Octet Key Pair curve bound to the algorithm.
	 */
	private final OKPCurve curve;

	/**
	 * <p>
	 * Creates an Edward-curve digital Signature algorithm.
	 * </p>
	 *
	 * @param alg               the JWA registered algorithm name
	 * @param signerFactory     the JWA signer factory
	 * @param keyManagerFactory the JWA key manager factory
	 * @param cipherFactory     the JWA cipher factory
	 * @param jcaAlg            the JCA signing algorithm
	 * @param curve             the Octet Key Pair curve
	 */
	private EdECAlgorithm(String alg, BiFunction<EdECJWK, EdECAlgorithm, JWASigner> signerFactory, BiFunction<EdECJWK, EdECAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<EdECJWK, EdECAlgorithm, JWACipher> cipherFactory, String jcaAlg, OKPCurve curve) {
		this.alg = alg;
		this.signerFactory = signerFactory;
		this.keyManagerFactory = keyManagerFactory;
		this.cipherFactory = cipherFactory;
		this.jcaAlg = jcaAlg;
		this.curve = curve;
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
	public JWASigner createSigner(EdECJWK jwk) throws JWAProcessingException {
		if(this.signerFactory == null) {
			throw new JWAProcessingException("Not a signature algorithm: " + this.alg);
		}
		return this.signerFactory.apply(jwk, this);
	}

	@Override
	public JWAKeyManager createKeyManager(EdECJWK jwk) throws JWAProcessingException {
		if(this.keyManagerFactory == null) {
			throw new JWAProcessingException("Not a key management algorithm: " + this.alg);
		}
		return this.keyManagerFactory.apply(jwk, this);
	}

	@Override
	public JWACipher createCipher(EdECJWK jwk) throws JWAProcessingException {
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
	 * Returns the Octet Key Pair curve bound to the algorithm.
	 * </p>
	 * 
	 * @return an Octet Key Pair curve
	 */
	public OKPCurve getCurve() {
		return curve;
	}
	
	/**
	 * <p>
	 * Returns the Edward-curve digital Signature algorithm corresponding to the specified JWA registered algorithm name and Octet Key Pair curve.
	 * </p>
	 *
	 * @param alg   a JWA registered algorithm name
	 * @param curve the corresponding OKP curve
	 *
	 * @return an Edward-curve digital Signature algorithm
	 *
	 * @throws IllegalArgumentException if the specified algorithm is not an EdEC algorithm or if the curve does not correspond to the algorithm
	 */
	public static EdECAlgorithm fromAlgorithm(String alg, OKPCurve curve) throws IllegalArgumentException {
		switch(alg) {
			case "EdDSA": {
					switch(curve) {
						case ED25519:
							return EDDSA_ED25519;
						case ED448:
							return EDDSA_ED448;
						default:
							throw new IllegalArgumentException("Unkown EdDSA algorithm curve " + curve);
					}
				}
			default: 
				throw new IllegalArgumentException("Unknown Ed algorithm " + alg);
		}
	}
	
	/**
	 * <p>
	 * Creates an Edward-curve digital signer.
	 * </p>
	 *
	 * @param jwk       an Edward-curve digital signing key
	 * @param algorithm an Edward-curve digital algorithm
	 *
	 * @return an Edward-curve digital signer
	 *
	 * @throws JWAProcessingException if there was an error creating the signer
	 */
	private static EdDSASigner createEdDSASigner(EdECJWK jwk, EdECAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerEdDSASigner(jwk);
	}
	
	/**
	 * <p>
	 * An inner Edward-curve digital signer.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerEdDSASigner extends EdDSASigner {

		/**
		 * <p>
		 * Creates an inner Edward-curve digital signer.
		 * </p>
		 * 
		 * @param jwk an Edward-curve digital signing key
		 * 
		 * @throws JWAProcessingException if there was an error creating the signer
		 */
		InnerEdDSASigner(EdECJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = EdECAlgorithm.this;
			this.init();
		}
	}
}
