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

import io.inverno.mod.security.jose.jwa.JWAAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.JWASignatureException;
import io.inverno.mod.security.jose.jwa.JWASigner;
import io.inverno.mod.security.jose.jwk.JWK;

/**
 * <p>
 * Base JWA signer implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWK type
 * @param <B> the JWA algorithm type
 */
public abstract class AbstractJWASigner<A extends JWK, B extends JWAAlgorithm<A>> extends AbstractJWA implements JWASigner {
	
	/**
	 * The key.
	 */
	protected final A jwk;
	
	/**
	 * The algorithm.
	 */
	protected B algorithm;

	/**
	 * <p>
	 * Creates a JWA signer.
	 * </p>
	 * 
	 * @param jwk       a JWK
	 * @param algorithm a JWA algorithm
	 * 
	 * @throws JWAProcessingException if the specified algorithm is not supported
	 */
	public AbstractJWASigner(A jwk, B algorithm) throws JWAProcessingException {
		if(!algorithm.isSignature()) {
			throw new JWAProcessingException("Not a signature algorithm: " + algorithm.getAlgorithm());
		}
		this.jwk = jwk;
		this.algorithm = algorithm;
	}
	
	/**
	 * <p>
	 * Creates a JWA signer.
	 * </p>
	 *
	 * @param jwk a JWK
	 */
	protected AbstractJWASigner(A jwk) {
		this.jwk = jwk;
	}
	
	@Override
	public final byte[] sign(byte[] data) throws JWASignatureException {
		if(this.jwk.getKeyOperations() != null && !this.jwk.getKeyOperations().contains(JWK.KEY_OP_SIGN)) {
			throw new JWASignatureException("JWK does not support sign operation");
		}
		return this.doSign(data);
	}
	
	/**
	 * <p>
	 * Signs the specified data.
	 * </p>
	 * 
	 * @param data the data to sign
	 * 
	 * @return a signature
	 * 
	 * @throws JWASignatureException if there was an error signing the data
	 */
	protected abstract byte[] doSign(byte[] data) throws JWASignatureException;

	@Override
	public final boolean verify(byte[] data, byte[] signature) throws JWASignatureException {
		if(this.jwk.getKeyOperations() != null && !this.jwk.getKeyOperations().contains(JWK.KEY_OP_VERIFY)) {
			throw new JWASignatureException("JWK does not support verify operation");
		}
		return this.doVerify(data, signature);
	}
	
	/**
	 * <p>
	 * Verifies the specified signature.
	 * </p>
	 *
	 * @param data      the signed data
	 * @param signature the signature
	 *
	 * @return true if the signature is valid, false otherwise
	 *
	 * @throws JWASignatureException if there was an error verifying the signature
	 */
	protected abstract boolean doVerify(byte[] data, byte[] signature) throws JWASignatureException;
}
