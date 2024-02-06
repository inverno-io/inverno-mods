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
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import javax.crypto.Mac;

/**
 * <p>
 * HMAC signer implementation.
 * </p>
 * 
 * <p>
 * It supports the following signature algorithms:
 * </p>
 * 
 * <ul>
 * <li>HS256</li>
 * <li>HS384</li>
 * <li>HS512</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class HMACSigner extends AbstractJWASigner<OCTJWK, OCTAlgorithm> {

	/**
	 * The set of algorithms supported by the signer.
	 */
	public static final Set<OCTAlgorithm> SUPPORTED_ALGORITHMS = Set.of(OCTAlgorithm.HS256, OCTAlgorithm.HS384, OCTAlgorithm.HS512);
	
	/**
	 * <p>
	 * Creates an HMAC signer.
	 * </p>
	 *
	 * @param jwk       an octet JWK
	 * @param algorithm an octet JWA algorithm
	 *
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public HMACSigner(OCTJWK jwk, OCTAlgorithm algorithm) throws JWAProcessingException {
		super(jwk, algorithm);
		if(!SUPPORTED_ALGORITHMS.contains(algorithm)) {
			throw new JWAProcessingException("Unsupported algorithm: " + algorithm.getAlgorithm());
		}
		this.init();
	}
	
	/**
	 * <p>
	 * Creates an HMAC signer.
	 * </p>
	 *
	 * @param jwk an octet JWK
	 */
	protected HMACSigner(OCTJWK jwk) {
		super(jwk);
	}

	@Override
	protected final void init() throws JWAProcessingException{
	
	}
	
	@Override
	protected byte[] doSign(byte[] data) throws JWASignatureException {
		return this.jwk.toSecretKey().map(secretKey -> {
			try {
				Mac sig = Mac.getInstance(this.algorithm.getJcaAlgorithm());
				sig.init(secretKey);
				return sig.doFinal(data);
			} 
			catch (NoSuchAlgorithmException | InvalidKeyException e) {
				throw new JWASignatureException(e);
			}
		})
		.orElseThrow(() -> new JWASignatureException("JWK is missing secret key"));
	}

	@Override
	protected boolean doVerify(byte[] data, byte[] signature) {
		return MessageDigest.isEqual(this.sign(data), signature);
	}
}
