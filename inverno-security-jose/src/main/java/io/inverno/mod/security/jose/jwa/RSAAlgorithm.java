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

import io.inverno.mod.security.jose.internal.jwa.RSAKeyManager;
import io.inverno.mod.security.jose.internal.jwa.RSASigner;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.function.BiFunction;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * <p>
 * RSA algorithms as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518">RFC7518</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public enum RSAAlgorithm implements JWAAlgorithm<RSAJWK> {
	
	/**
	 * RS1 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc8812#section-2">RFC8812 Section 2</a>.
	 */
	RS1("RS1", RSAAlgorithm::createRSASigner, null, null, "SHA1withRSA"),
	/**
	 * RS256 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.3">RFC7518 Section 3.3</a>.
	 */
	RS256("RS256", RSAAlgorithm::createRSASigner, null, null, "SHA256withRSA"),
	/**
	 * RS384 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.3">RFC7518 Section 3.3</a>.
	 */
	RS384("RS384", RSAAlgorithm::createRSASigner, null, null, "SHA384withRSA"),
	/**
	 * RS512 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.3">RFC7518 Section 3.3</a>.
	 */
	RS512("RS512", RSAAlgorithm::createRSASigner, null, null, "SHA512withRSA"),
	
	/**
	 * PS256 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.5">RFC7518 Section 3.5</a>.
	 */
	PS256("PS256", RSAAlgorithm::createRSASigner, null, null, "RSASSA-PSS", new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1)),
	/**
	 * PS384 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.5">RFC7518 Section 3.5</a>.
	 */
	PS384("PS384", RSAAlgorithm::createRSASigner, null, null, "RSASSA-PSS", new PSSParameterSpec("SHA-384", "MGF1", MGF1ParameterSpec.SHA384, 48, 1)),
	/**
	 * PS512 signature algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-3.5">RFC7518 Section 3.5</a>.
	 */
	PS512("PS512", RSAAlgorithm::createRSASigner, null, null, "RSASSA-PSS", new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1)),
	
	/**
	 * RSA1_5 key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.2">RFC7518 Section 4.2</a>.
	 */
	RSA1_5("RSA1_5", null, RSAAlgorithm::createRSAKeyManager, null, "RSA/ECB/PKCS1Padding"),
	
	/**
	 * RSA-OAEP key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.3">RFC7518 Section 4.3</a>.
	 */
	RSA_OAEP("RSA-OAEP", null, RSAAlgorithm::createRSAKeyManager, null, "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"),
	/**
	 * RSA-OAEP-256 key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.3">RFC7518 Section 4.3</a>.
	 */
	RSA_OAEP_256("RSA-OAEP-256", null, RSAAlgorithm::createRSAKeyManager, null, "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)),
	/**
	 * RSA-OAEP-384 key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.3">RFC7518 Section 4.3</a>.
	 */
	RSA_OAEP_384("RSA-OAEP-384", null, RSAAlgorithm::createRSAKeyManager, null, "RSA/ECB/OAEPWithSHA-384AndMGF1Padding", new OAEPParameterSpec("SHA-384", "MGF1", MGF1ParameterSpec.SHA384, PSource.PSpecified.DEFAULT)),
	/**
	 * RSA-OAEP-512 key management algorithm as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4.3">RFC7518 Section 4.3</a>.
	 */
	RSA_OAEP_512("RSA-OAEP-512", null, RSAAlgorithm::createRSAKeyManager, null, "RSA/ECB/OAEPWithSHA-512AndMGF1Padding", new OAEPParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT));
	
	/**
	 * The JWA registered algorithm name.
	 */
	private final String alg;
	/**
	 * The JWA signer factory.
	 */
	private final BiFunction<RSAJWK, RSAAlgorithm, JWASigner> signerFactory;
	/**
	 * The JWA key manager factory.
	 */
	private final BiFunction<RSAJWK, RSAAlgorithm, JWAKeyManager> keyManagerFactory;
	/**
	 * The JWA cipher factory.
	 */
	private final BiFunction<RSAJWK, RSAAlgorithm, JWACipher> cipherFactory;
	/**
	 * The corresponding JCA algorithm.
	 */
	private final String jcaAlg;
	/**
	 * The signature algorithm parameters when applicable.
	 */
	private final AlgorithmParameterSpec signatureParams;
	
	/**
	 * <p>
	 * Creates a RSA algorithm.
	 * </p>
	 * 
	 * @param alg               the JWA registered algorithm name
	 * @param signerFactory     the JWA signer factory
	 * @param keyManagerFactory the JWA key manager factory
	 * @param cipherFactory     the JWA cipher factory
	 * @param jcaAlg            the JCA algorithm
	 */
	private RSAAlgorithm(String alg, BiFunction<RSAJWK, RSAAlgorithm, JWASigner> signerFactory, BiFunction<RSAJWK, RSAAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<RSAJWK, RSAAlgorithm, JWACipher> cipherFactory, String jcaAlg) {
		this(alg, signerFactory, keyManagerFactory, cipherFactory, jcaAlg, null);
	}
	
	/**
	 * <p>
	 * Creates a RSA algorithm.
	 * </p>
	 *
	 * @param alg               the JWA registered algorithm name
	 * @param signerFactory     the JWA signer factory
	 * @param keyManagerFactory the JWA key manager factory
	 * @param cipherFactory     the JWA cipher factory
	 * @param jcaAlg            the JCA algorithm
	 * @param signatureParams   the signature algorithm parameters
	 */
	private RSAAlgorithm(String alg, BiFunction<RSAJWK, RSAAlgorithm, JWASigner> signerFactory, BiFunction<RSAJWK, RSAAlgorithm, JWAKeyManager> keyManagerFactory, BiFunction<RSAJWK, RSAAlgorithm, JWACipher> cipherFactory, String jcaAlg, AlgorithmParameterSpec signatureParams) {
		this.alg = alg;
		this.jcaAlg = jcaAlg;
		this.signerFactory = signerFactory;
		this.keyManagerFactory = keyManagerFactory;
		this.cipherFactory = cipherFactory;
		this.signatureParams = signatureParams;
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
	public JWASigner createSigner(RSAJWK jwk) throws JWAProcessingException {
		if(this.signerFactory == null) {
			throw new JWAProcessingException("Not a signature algorithm: " + this.alg);
		}
		return this.signerFactory.apply(jwk, this);
	}

	@Override
	public JWAKeyManager createKeyManager(RSAJWK jwk) throws JWAProcessingException {
		if(this.keyManagerFactory == null) {
			throw new JWAProcessingException("Not a key management algorithm: " + this.alg);
		}
		return this.keyManagerFactory.apply(jwk, this);
	}

	@Override
	public JWACipher createCipher(RSAJWK jwk) throws JWAProcessingException {
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
	 * Returns the JCA signature algorithm parameters corresponding to the JWA algorithm.
	 * </p>
	 * 
	 * <p>
	 * Note that this only applies to signature algorithms.
	 * </p>
	 * 
	 * @return algorithm parameters or null
	 */
	public AlgorithmParameterSpec getSignatureParameter() {
		return signatureParams;
	}
	
	/**
	 * <p>
	 * Returns the RSA algorithm corresponding to the specified JWA registered algorithm name.
	 * </p>
	 * 
	 * @param alg a JWA registered algorithm name
	 * 
	 * @return a RSA algorithm
	 * 
	 * @throws IllegalArgumentException if the specified algorithm is not a RSA algorithm.
	 */
	public static RSAAlgorithm fromAlgorithm(String alg) {
		switch(alg) {
			case "RS1": 
				return RS1;
			case "RS256": 
				return RS256;
			case "RS384": 
				return RS384;
			case "RS512": 
				return RS512;
			case "PS256": 
				return PS256;
			case "PS384": 
				return PS384;
			case "PS512": 
				return PS512;
			case "RSA1_5": 
				return RSA1_5;
			case "RSA-OAEP": 
				return RSA_OAEP;
			case "RSA-OAEP-256": 
				return RSA_OAEP_256;
			case "RSA-OAEP-384": 
				return RSA_OAEP_384;
			case "RSA-OAEP-512": 
				return RSA_OAEP_512;
			default: 
				throw new IllegalArgumentException("Unknown RSA algorithm " + alg);
		}
	}
	
	/**
	 * <p>
	 * Creates a RSA signer.
	 * </p>
	 *
	 * @param jwk       a RSA key
	 * @param algorithm a RSA signature algorithm
	 *
	 * @return a RSA signer
	 *
	 * @throws JWAProcessingException if there was an error creating the signer
	 */
	private static RSASigner createRSASigner(RSAJWK jwk, RSAAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerRSASigner(jwk);
	}
	
	/**
	 * <p>
	 * An inner RSA signer.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerRSASigner extends RSASigner {

		/**
		 * <p>
		 * Creates an inner RSA signer.
		 * </p>
		 * 
		 * @param jwk a RSA key
		 * 
		 * @throws JWAProcessingException if there was an error creating the signer
		 */
		InnerRSASigner(RSAJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = RSAAlgorithm.this;
			this.init();
		}
	}
	
	/**
	 * <p>
	 * Creates a RSA key manager.
	 * </p>
	 *
	 * @param jwk       a RSA key
	 * @param algorithm a RSA key management algorithm
	 *
	 * @return a RSA key manager
	 *
	 * @throws JWAProcessingException if there was an error creating the key manager
	 */
	private static RSAKeyManager createRSAKeyManager(RSAJWK jwk, RSAAlgorithm algorithm) throws JWAProcessingException {
		return algorithm.new InnerRSAKeyManager(jwk);
	}
	
	/**
	 * <p>
	 * An inner RSA key manager.
	 * </p>
	 *
	 * <p>
	 * This is basically used to bypass checks on the algorithm.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private class InnerRSAKeyManager extends RSAKeyManager {

		/**
		 * <p>
		 * Creates an inner RSA key manager.
		 * </p>
		 * 
		 * @param jwk a RSA key
		 * 
		 * @throws JWAProcessingException if there was an error creating the key manager
		 */
		InnerRSAKeyManager(RSAJWK jwk) throws JWAProcessingException {
			super(jwk);
			this.algorithm = RSAAlgorithm.this;
			this.init();
		}
	}
}
