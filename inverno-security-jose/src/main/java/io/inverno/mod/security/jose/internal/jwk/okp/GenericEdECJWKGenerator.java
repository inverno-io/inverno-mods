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
package io.inverno.mod.security.jose.internal.jwk.okp;

import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.jwa.EdECAlgorithm;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.okp.EdECJWKGenerator;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic Edward-Curve JSON Web Key generator.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericEdECJWKGenerator extends AbstractOKPJWKGenerator<EdECPublicKey, EdECPrivateKey, GenericEdECJWK, GenericEdECJWKGenerator> implements EdECJWKGenerator<GenericEdECJWK, GenericEdECJWKGenerator> {

	/**
	 * The default OKP curve
	 */
	public static final OKPCurve DEFAULT_CURVE = OKPCurve.ED25519;
	
	/**
	 * <p>
	 * Creates a generic EdEC JWK generator.
	 * </p>
	 */
	public GenericEdECJWKGenerator() {
		this(null);
	}

	/**
	 * <p>
	 * Creates a generic EdEC JWK generator initialized with the specified parameters map.
	 * </p>
	 * 
	 * @param parameters a parameters map used to initialize the generator
	 * 
	 * @throws JWKGenerateException if there was an error reading the parameters map
	 */
	public GenericEdECJWKGenerator(Map<String, Object> parameters) throws JWKGenerateException {
		super(parameters);
		if(this.curve == null) {
			this.curve = DEFAULT_CURVE;
		}
	}

	@Override
	protected Mono<Void> verify() throws JWKGenerateException, JWKProcessingException {
		return super.verify().then(Mono.fromRunnable(() -> {
			if(this.alg != null) {
				try {
					EdECAlgorithm.fromAlgorithm(this.alg, this.curve);
				}
				catch(IllegalArgumentException e) {
					throw new JWKGenerateException("Unsupported algorithm: " + this.alg + " + " + this.curve.getCurve());
				}
			}
			if(!GenericEdECJWK.SUPPORTED_CURVES.contains(this.curve)) {
				throw new JWKGenerateException("Unsupported OKP curve: " + this.curve.getCurve());
			}
		}));
	}
	
	@Override
	protected Mono<GenericEdECJWK> doGenerate() throws JWKGenerateException, JWKProcessingException {
		return Mono.fromSupplier(() -> {
			try {
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(this.curve.getJCAName());
				KeyPair keyPair = keyPairGenerator.generateKeyPair();
				
				EdECPublicKey publicKey = (EdECPublicKey)keyPair.getPublic();
				EdECPrivateKey privateKey = (EdECPrivateKey)keyPair.getPrivate();
				
				byte[] xBytes = new byte[this.curve.getKeyLength()];
				byte[] encodedKeyBytes = publicKey.getEncoded();
				System.arraycopy(encodedKeyBytes, encodedKeyBytes.length - xBytes.length, xBytes, 0, xBytes.length);
				
				GenericEdECJWK jwk = new GenericEdECJWK(
					this.curve, 
					JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(xBytes),
					privateKey.getBytes().map(JOSEUtils.BASE64_NOPAD_URL_ENCODER::encodeToString).orElse(null)
				);
				jwk.setPublicKeyUse(this.use);
				jwk.setKeyOperations(this.key_ops);
				jwk.setKeyId(this.kid);
				jwk.setAlgorithm(this.alg);
				
				return jwk;
			} 
			catch(NoSuchAlgorithmException e) {
				throw new JWKGenerateException("Error generating OKP JWK", e);
			}
		});
	}
}
