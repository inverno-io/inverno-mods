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

import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.JWASignatureException;
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Set;

/**
 * <p>
 * RSA signer implementation.
 * </p>
 * 
 * <p>
 * It supports the following signature algorithms:
 * </p>
 * 
 * <ul>
 * <li>RS1</li>
 * <li>RS256</li>
 * <li>RS384</li>
 * <li>RS512</li>
 * <li>PS256</li>
 * <li>PS384</li>
 * <li>PS512</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class RSASigner extends AbstractJWASigner<RSAJWK, RSAAlgorithm> {

	/**
	 * The set of algorithms supported by the signer.
	 */
	public static final Set<RSAAlgorithm> SUPPORTED_ALGORITHMS = Set.of(RSAAlgorithm.RS1, RSAAlgorithm.RS256, RSAAlgorithm.RS384, RSAAlgorithm.RS512, RSAAlgorithm.PS256, RSAAlgorithm.PS384, RSAAlgorithm.PS512);
	
	/**
	 * <p>
	 * Creates a RSA signer.
	 * </p>
	 *
	 * @param jwk       a RSA JWK
	 * @param algorithm a RSA JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public RSASigner(RSAJWK jwk, RSAAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates a RSA signer.
	 * </p>
	 * 
	 * @param jwk a RSA JWK
	 */
	protected RSASigner(RSAJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException{
	}
	
	@Override
	protected byte[] doSign(byte[] data) throws JWASignatureException {
		return this.jwk.toPrivateKey()
			.map(privateKey -> {
				try {
					Signature sig = Signature.getInstance(this.algorithm.getJcaAlgorithm());
					AlgorithmParameterSpec signatureParameter = this.algorithm.getSignatureParameter();
					if (signatureParameter != null) {
						sig.setParameter(signatureParameter);
					}
					sig.initSign(privateKey);
					sig.update(data);
					return sig.sign();
				} 
				catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | SignatureException e) {
					throw new JWASignatureException(e);
				}
			})
			.orElseThrow(() -> new JWASignatureException("JWK is missing RSA private exponent"));
	}
	
	@Override
	protected boolean doVerify(byte[] data, byte[] signature) throws JWASignatureException {
		try {
			Signature sig = Signature.getInstance(this.algorithm.getJcaAlgorithm());
			AlgorithmParameterSpec signatureParameter = this.algorithm.getSignatureParameter();
			if(signatureParameter != null) {
				sig.setParameter(signatureParameter);
			}
			sig.initVerify(this.jwk.toPublicKey());
			sig.update(data);
			
			return sig.verify(signature);
		} 
		catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | SignatureException e) {
			throw new JWASignatureException(e);
		}
	}
}
