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

import io.inverno.mod.security.jose.jwa.EdECAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.JWASignatureException;
import io.inverno.mod.security.jose.jwk.okp.EdECJWK;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;

/**
 * <p>
 * Edwards-curve Digital Signature signer implementation.
 * </p>
 * 
 * <p>
 * It supports the following signature algorithms:
 * </p>
 * 
 * <ul>
 * <li>EdDSA with elliptic curve Ed25519 and Ed448.</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class EdDSASigner extends AbstractJWASigner<EdECJWK, EdECAlgorithm> {

	/**
	 * <p>
	 * Creates an EdDSA signer.
	 * </p>
	 *
	 * @param jwk       an Edward Elliptic Curve JWK
	 * @param algorithm an Edward Elliptic Curve JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public EdDSASigner(EdECJWK jwk, EdECAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!jwk.getCurve().equals(algorithm.getCurve().getCurve())) {
			throw new JWAProcessingException("JWK with curve " + jwk.getCurve() + " does not support algorithm " + algorithm);
		}
	}
	
	/**
	 * <p>
	 * Creates an EdDSA signer.
	 * </p>
	 *
	 * @param jwk an Edward Elliptic Curve JWK
	 */
	protected EdDSASigner(EdECJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException {
	
	}
	
	@Override
	protected byte[] doSign(byte[] data) throws JWASignatureException {
		return this.jwk.toPrivateKey()
			.map(privateKey -> {
				try {
					Signature sig = Signature.getInstance(this.algorithm.getJcaAlgorithm());
					sig.initSign(privateKey);
					sig.update(data);
					return sig.sign();
				} 
				catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
					throw new JWASignatureException(e);
				}
			})
			.orElseThrow(() -> new JWASignatureException("JWK is missing OKP private key"));
	}
	
	@Override
	protected boolean doVerify(byte[] data, byte[] signature) throws JWASignatureException {
		try {
			Signature sig = Signature.getInstance(this.algorithm.getJcaAlgorithm());
			sig.initVerify(this.jwk.toPublicKey());
			sig.update(data);
			return sig.verify(signature);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new JWASignatureException(e);
		}
	}
}
